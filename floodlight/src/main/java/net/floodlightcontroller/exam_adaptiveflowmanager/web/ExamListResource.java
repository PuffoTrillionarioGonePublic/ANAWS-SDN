package net.floodlightcontroller.exam_adaptiveflowmanager.web;

import org.restlet.resource.Get;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import net.floodlightcontroller.devicemanager.web.AbstractDeviceResource;
import net.floodlightcontroller.exam_adaptiveflowmanager.IExamService;


public class ExamListResource extends AbstractDeviceResource {
    protected static Logger log = LoggerFactory.getLogger(ExamConnectResource.class);

    @Get("json")
    public Object list() {
		IExamService exam = (IExamService) getContext().getAttributes().get(IExamService.class.getCanonicalName());
		return ImmutableMap.of("flowRequests", exam.getFlowRequests());
    }
}
