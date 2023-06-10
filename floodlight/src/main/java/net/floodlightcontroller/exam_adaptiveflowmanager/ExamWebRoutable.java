package net.floodlightcontroller.exam_adaptiveflowmanager;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import net.floodlightcontroller.exam_adaptiveflowmanager.web.ExamConnectResource;
import net.floodlightcontroller.exam_adaptiveflowmanager.web.ExamListResource;
import net.floodlightcontroller.restserver.RestletRoutable;

public class ExamWebRoutable implements RestletRoutable {

    @Override
    public Restlet getRestlet(Context context) {
        Router router = new Router(context);
        
		// curl -X POST -H 'Content-Type: application/json' -d '{"nw_proto": 6, "nw_src": "10.0.0.11", "nw_dst": "10.0.0.21", "tp_src":1234, "tp_dst":1234, "duration": 10}' http://localhost:8080/wm/exam/add/json
        router.attach("/add/json", ExamConnectResource.class);

		// curl http://localhost:8080/wm/exam/list/json
        router.attach("/list/json", ExamListResource.class);

        return router;
    }

    @Override
    public String basePath() {
        return "/wm/exam";
    }
    
}