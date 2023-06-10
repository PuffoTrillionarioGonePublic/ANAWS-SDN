package net.floodlightcontroller.exam_adaptiveflowmanager;

import com.google.common.collect.ImmutableCollection;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.exam_adaptiveflowmanager.FlowRequest;

public interface IExamService extends IFloodlightService {
    public boolean submitFlowRequest(FlowRequest fr);
	public ImmutableCollection<FlowRequest> getFlowRequests();
}
