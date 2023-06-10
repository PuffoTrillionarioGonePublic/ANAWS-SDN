package net.floodlightcontroller.exam_adaptiveflowmanager.web.serializers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import net.floodlightcontroller.exam_adaptiveflowmanager.FlowRequest;

// FlowRequest -> json
public class FlowRequestSerializer extends StdSerializer<FlowRequest> {

	public FlowRequestSerializer() {
		this(null);
	}

	public FlowRequestSerializer(Class<FlowRequest> t) {
		super(t);
	}

	@Override
	public void serialize(FlowRequest req, JsonGenerator jgen, SerializerProvider provider) throws IOException {
		jgen.writeStartObject();
		jgen.writeNumberField("nw_proto", req.nw_proto.getIpProtocolNumber());
		jgen.writeStringField("nw_src", req.nw_src.toString());
		jgen.writeStringField("nw_dst", req.nw_dst.toString());
		jgen.writeNumberField("tp_src", req.tp_src.getPort());
		jgen.writeNumberField("tp_dst", req.tp_dst.getPort());
		jgen.writeNumberField("duration", req.duration);
		jgen.writeEndObject();
	}
}
