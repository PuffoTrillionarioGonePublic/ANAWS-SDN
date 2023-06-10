package net.floodlightcontroller.exam_adaptiveflowmanager.web.serializers;

import java.io.IOException;

import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.TransportPort;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import net.floodlightcontroller.exam_adaptiveflowmanager.FlowRequest;

// json -> FlowRequest
public class FlowRequestDeserializer extends StdDeserializer<FlowRequest> {

	public FlowRequestDeserializer() {
		this(null);
	}

	public FlowRequestDeserializer(Class<?> vc) {
		super(vc);
	}

	@Override
	public FlowRequest deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		JsonNode node = jp.getCodec().readTree(jp);

		IpProtocol nw_proto = IpProtocol.of((short) Integer.parseInt(node.get("nw_proto").asText()));
		IPv4Address nw_src = IPv4Address.of(node.get("nw_src").asText()); 		
		IPv4Address nw_dst = IPv4Address.of(node.get("nw_dst").asText());
		TransportPort tp_src = TransportPort.of(Integer.parseInt(node.get("tp_src").asText()));
		TransportPort tp_dst = TransportPort.of(Integer.parseInt(node.get("tp_dst").asText()));
		int duration = Integer.parseUnsignedInt(node.get("duration").asText());

		FlowRequest req = new FlowRequest(nw_proto, nw_src, nw_dst, tp_src, tp_dst, duration);
		return req;
	}
	
}
