package net.floodlightcontroller.exam_adaptiveflowmanager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.validation.constraints.NotNull;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TransportPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.exam_adaptiveflowmanager.FlowRequest.FlowRequestKey;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.packet.UDP;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.routing.IRoutingDecision;
import net.floodlightcontroller.routing.RoutingDecision;
import net.floodlightcontroller.statistics.IStatisticsService;
import net.floodlightcontroller.util.OFMessageUtils;

public class ExamModule implements IFloodlightModule, IExamService, IOFMessageListener {

    protected int TIMEOUT_WHEN_OVERLOADED = 10;
	protected float UTILIZATION_THRESHOLD = 0.7f;
	protected int MAX_FLOWS = 32;
	protected IFloodlightProviderService floodlightProvider;
    protected Logger logger;
    protected IRestApiService restApi;
	protected IStatisticsService statisticsService;

    // storage for flow requests
    protected Map<FlowRequestKey, FlowRequest> flowRequests;

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        return Collections.singleton(IExamService.class);
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        return Collections.singletonMap(IExamService.class, this);
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        Collection<Class<? extends IFloodlightService>> deps = new ArrayList<>();
        deps.add(IFloodlightProviderService.class);
        deps.add(IRestApiService.class);
		deps.add(IStatisticsService.class);
        return deps;
    }

    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
        restApi = context.getServiceImpl(IRestApiService.class);
		statisticsService = context.getServiceImpl(IStatisticsService.class);
		logger = LoggerFactory.getLogger(ExamModule.class);

        flowRequests = new TreeMap<>();

		// parse parameters from floodlight config file
        Map<String, String> configParameters = context.getConfigParams(this);
        String tmp = configParameters.get("timeout-when-overloaded");
        if (tmp != null) {
			TIMEOUT_WHEN_OVERLOADED = Integer.parseInt(tmp);
            logger.debug("Default timeout when overloaded set to {}.", TIMEOUT_WHEN_OVERLOADED);
        } else {
            logger.debug("Default timeout when overloaded not configured. Using {}.", TIMEOUT_WHEN_OVERLOADED);
        }
		tmp = configParameters.get("utilization-threshold");
		if (tmp != null) {
			UTILIZATION_THRESHOLD = Float.parseFloat(tmp);
			logger.debug("Default utilization threshold set to {}.", UTILIZATION_THRESHOLD);
		} else {
			logger.debug("Default utilization threshold not configured. Using {}.", UTILIZATION_THRESHOLD);
		}
		tmp = configParameters.get("max-flows");
		if (tmp != null) {
			MAX_FLOWS = Integer.parseInt(tmp);
			logger.debug("Default max flows set to {}.", MAX_FLOWS);
		} else {
			logger.debug("Default max flows not configured. Using {}.", MAX_FLOWS);
		}
    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        // inform the controller of the packet to handle
        floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
        // specify HTTP request to handle
        restApi.addRestletRoutable(new ExamWebRoutable());
		// enable statistics collection
		statisticsService.collectStatistics(true);
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public boolean isCallbackOrderingPrereq(OFType type, String name) {
        return false;
    }

    @Override
    public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// force this module to be called before the forwarding module
		return (type.equals(OFType.PACKET_IN) && name.equals("forwarding"));
    }

	/**
	 * This method calculates the hard timeout for a flow request
	 * if the utilization of the flow table is above a certain threshold
	 * it sets the hard timeout to a fixed value, otherwise it sets it to
	 * the duration specified in the flow request
	 * The utilization is calculated as the number of flows divided by the
	 * maximum number of flows
	 */
	protected short calculate_hard_timeout(IOFSwitch sw, OFPacketIn pi, IRoutingDecision decision, FloodlightContext cntx, FlowRequest fr) {
		Set<?> flowStats = statisticsService.getFlowStats(sw.getId());
		int num_flows = flowStats.size();
		logger.info("Number of flows: {} for switch {}", num_flows, sw.getId());

		if (num_flows > UTILIZATION_THRESHOLD * MAX_FLOWS) {
			return (short) TIMEOUT_WHEN_OVERLOADED;
		}
		return (short) fr.duration;

	}

	/**
	 * This method is called when a packet is received by the controller
	 * it is used to decide whether to allow the packet to be forwarded or not,
	 * by default it allows only ARP and ICMP traffic, if the packet is TCP or UDP
	 * it checks if it matches a flow request, if it does it allows the traffic,
	 * otherwise it drops it
	 */
	public Command processPacketInMessage(IOFSwitch sw, OFPacketIn pi, IRoutingDecision decision, FloodlightContext cntx) {
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		OFPort inPort = OFMessageUtils.getInPort(pi);

		// drop by default
		decision = new RoutingDecision(sw.getId(), inPort,
						IDeviceService.fcStore.get(cntx, IDeviceService.CONTEXT_SRC_DEVICE),
						IRoutingDecision.RoutingAction.DROP);

		
		// always allow ARP traffic
		if (eth.getEtherType() == EthType.ARP) {
			logger.info("allowing ARP traffic");
			decision.setRoutingAction(IRoutingDecision.RoutingAction.FORWARD_OR_FLOOD);
		} else if (eth.getEtherType() == EthType.IPv4) {
			IPv4 ipv4 = (IPv4) eth.getPayload();

			IPv4Address nw_src = ipv4.getSourceAddress();
			IPv4Address nw_dst = ipv4.getDestinationAddress();
		
		// always allow ICMP traffic
			if (ipv4.getProtocol() == IpProtocol.ICMP) {
				logger.info("allowing ICMP traffic");
				decision.setRoutingAction(IRoutingDecision.RoutingAction.FORWARD_OR_FLOOD);
		
		// allow TCP/UDP traffic only if it matches a flow request
			} else if (ipv4.getProtocol() == IpProtocol.TCP || ipv4.getProtocol() == IpProtocol.UDP) {
				TransportPort tp_src, tp_dst;
				if (ipv4.getProtocol() == IpProtocol.TCP) {
					tp_src = ((TCP) ipv4.getPayload()).getSourcePort();
					tp_dst = ((TCP) ipv4.getPayload()).getDestinationPort();
				} else {
					tp_src = ((UDP) ipv4.getPayload()).getSourcePort();
					tp_dst = ((UDP) ipv4.getPayload()).getDestinationPort();
				}

				FlowRequestKey frk = new FlowRequestKey(ipv4.getProtocol(), nw_src, nw_dst, tp_src, tp_dst);

				FlowRequest fr;
				synchronized (flowRequests) {
					fr = flowRequests.remove(frk);
				}
				if (fr != null) { // the flow request is present, allow the traffic
					logger.info("allowing L4[{}] traffic from {}:{} to {}:{}", 
							ipv4.getProtocol(), nw_src, tp_src, nw_dst, tp_dst);
					
					short hard_timeout = calculate_hard_timeout(sw, pi, decision, cntx, fr);
					decision.setRoutingAction(IRoutingDecision.RoutingAction.FORWARD_OR_FLOOD);
					decision.setHardTimeout(hard_timeout);
					decision.setIdleTimeout((short) 0);
				} else { // the flow request is not present, drop the traffic
					if (ipv4.getProtocol() == IpProtocol.TCP) {
						decision.setRoutingAction(IRoutingDecision.RoutingAction.DROP_TCP);
					} // else if UDP is already dropped by default

					logger.info("dropping L4[{}] traffic from {}:{} to {}:{} due to missing flow request", 
							ipv4.getProtocol(), nw_src, tp_src, nw_dst, tp_dst);
				}
			} else {
				logger.info("dropping L4[{}] traffic from {} to {} due to unsupported protocol", 
						ipv4.getProtocol(), nw_src, nw_dst);
			}
		} else {
			logger.info("dropping L3[{}] traffic due to unsupported ethertype", eth.getEtherType());
		}

		decision.addToContext(cntx);
		return Command.CONTINUE;
	}

	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {

		switch (msg.getType()) {
		case PACKET_IN:
			IRoutingDecision decision = null;
			if (cntx == null) {
				logger.warn("{} unable to request packet drop: FloodlightContext is null.", this.getName());
			} else {
				decision = IRoutingDecision.rtStore.get(cntx, IRoutingDecision.CONTEXT_DECISION);
				return this.processPacketInMessage(sw, (OFPacketIn) msg, decision, cntx);
			}
			break;
		default:
			break;
		}

		return Command.CONTINUE;
	}

    @Override
    public boolean submitFlowRequest(@NotNull FlowRequest fr) {
		synchronized (flowRequests) {
			return flowRequests.put(fr.getRequestKey(), fr) == null;
		}
    }

	@Override
	public ImmutableCollection<FlowRequest> getFlowRequests() {
		synchronized (flowRequests) {
			return ImmutableMap.copyOf(flowRequests).values();
		}
	}

}
