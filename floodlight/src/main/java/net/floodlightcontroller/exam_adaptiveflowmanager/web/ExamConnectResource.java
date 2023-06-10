package net.floodlightcontroller.exam_adaptiveflowmanager.web;

import org.restlet.resource.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import net.floodlightcontroller.devicemanager.web.AbstractDeviceResource;
import net.floodlightcontroller.exam_adaptiveflowmanager.FlowRequest;
import net.floodlightcontroller.exam_adaptiveflowmanager.IExamService;


public class ExamConnectResource extends AbstractDeviceResource {
    protected static Logger log = LoggerFactory.getLogger(ExamConnectResource.class);

	@Post("json")
	public Object add(String json) {
		try {
			FlowRequest req = new ObjectMapper().readValue(json, FlowRequest.class);

			// submit the request to the exam module
			IExamService exam = (IExamService) getContext().getAttributes().get(IExamService.class.getCanonicalName());
			if(!exam.submitFlowRequest(req)) {
				throw new Exception("request not submitted (is it already present?)");
			} 

			log.info("request submitted: {}", req);
			return ImmutableMap.of("message", "request submitted");
		} catch (Exception e) {
			log.info("request not submitted: {}", e.getMessage());
			return ImmutableMap.of("message", "ERROR: " + e.getMessage());
		}
	}
}
