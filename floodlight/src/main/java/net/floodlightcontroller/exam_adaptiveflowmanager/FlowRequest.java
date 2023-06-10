package net.floodlightcontroller.exam_adaptiveflowmanager;

import java.util.Set;

import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.TransportPort;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableSet;

import net.floodlightcontroller.exam_adaptiveflowmanager.web.serializers.FlowRequestDeserializer;
import net.floodlightcontroller.exam_adaptiveflowmanager.web.serializers.FlowRequestSerializer;

/**
 * This class represents a flow request
 * it is just a glorified tuple of 6 elements:
 * - nw_proto: the network protocol (TCP or UDP)
 * - nw_src: the source IP address
 * - nw_dst: the destination IP address
 * - tp_src: the source transport port
 * - tp_dst: the destination transport port
 * - duration: the duration of the flow in seconds
 */
@JsonDeserialize(using = FlowRequestDeserializer.class)
@JsonSerialize(using = FlowRequestSerializer.class)
public class FlowRequest {
	/**
	 * This class represents the fields that uniquely identify a flow request
	 * it is used as a key in the flowRequests map, so it must implement Comparable
	 */
    public static class FlowRequestKey implements Comparable<FlowRequestKey> {
        public final IpProtocol nw_proto;
        public final IPv4Address nw_src; 
        public final IPv4Address nw_dst;
        public final TransportPort tp_src;
        public final TransportPort tp_dst;

        public FlowRequestKey(
            IpProtocol nw_proto,
            IPv4Address nw_src,
            IPv4Address nw_dst,
            TransportPort tp_src,
            TransportPort tp_dst
        ) {
            this.nw_proto = nw_proto;
            this.nw_src = nw_src;
            this.nw_dst = nw_dst;
            this.tp_src = tp_src;
            this.tp_dst = tp_dst;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof FlowRequestKey)) {
                return false;
            }
            return this.compareTo((FlowRequestKey) obj) == 0;
        }

        @Override
        public int compareTo(FlowRequestKey o) {
            if (!this.nw_proto.equals(o.nw_proto)) {
                return this.nw_proto.compareTo(o.nw_proto);
            }
            if (!this.nw_src.equals(o.nw_src)) {
                return nw_src.compareTo(o.nw_src);
            }
            if (!this.nw_dst.equals(o.nw_dst)) {
                return nw_dst.compareTo(o.nw_dst);
            }
            if (!this.tp_src.equals(o.tp_src)) {
                return this.tp_src.compareTo(o.tp_src);
            }
            return this.tp_dst.compareTo(o.tp_dst);
        }
    }

	public static final Set<IpProtocol> SUPPORTED_NW_PROTO = ImmutableSet.of(IpProtocol.TCP, IpProtocol.UDP);

    public final IpProtocol nw_proto;
    public final IPv4Address nw_src; 
    public final IPv4Address nw_dst;
    public final TransportPort tp_src;
    public final TransportPort tp_dst;
    public final int duration;

    public FlowRequest(
        IpProtocol nw_proto,
        IPv4Address nw_src,
        IPv4Address nw_dst,
        TransportPort tp_src,
        TransportPort tp_dst,
        int duration
    ) {
		if (!SUPPORTED_NW_PROTO.contains(nw_proto)) {
			throw new IllegalArgumentException("Unsupported nw_proto: " + nw_proto);
		}
        this.nw_proto = nw_proto;
        this.nw_src = nw_src;
        this.nw_dst = nw_dst;
        this.tp_src = tp_src;
        this.tp_dst = tp_dst;
        this.duration = duration;
    }

    public FlowRequestKey getRequestKey() {
        return new FlowRequestKey(
            this.nw_proto,
            this.nw_src,
            this.nw_dst,
            this.tp_src,
            this.tp_dst
        );
    }
}
