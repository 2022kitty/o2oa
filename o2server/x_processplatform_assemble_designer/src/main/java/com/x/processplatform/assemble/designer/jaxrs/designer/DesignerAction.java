package com.x.processplatform.assemble.designer.jaxrs.designer;

import com.google.gson.JsonElement;
import com.x.base.core.project.annotation.JaxrsDescribe;
import com.x.base.core.project.annotation.JaxrsMethodDescribe;
import com.x.base.core.project.http.ActionResult;
import com.x.base.core.project.http.EffectivePerson;
import com.x.base.core.project.http.HttpMediaType;
import com.x.base.core.project.jaxrs.ResponseFactory;
import com.x.base.core.project.jaxrs.StandardJaxrsAction;
import com.x.base.core.project.logger.Logger;
import com.x.base.core.project.logger.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("designer")
@JaxrsDescribe("设计")
public class DesignerAction extends StandardJaxrsAction {

	private static Logger logger = LoggerFactory.getLogger(DesignerAction.class);

	@JaxrsMethodDescribe(value = "根据关键字搜索设计对象.", action = ActionSearch.class)
	@POST
	@Path("search")
	@Produces(HttpMediaType.APPLICATION_JSON_UTF_8)
	@Consumes(MediaType.APPLICATION_JSON)
	public void managerList(@Suspended final AsyncResponse asyncResponse, @Context HttpServletRequest request,
							JsonElement jsonElement) {
		ActionResult<List<ActionSearch.Wo>> result = new ActionResult<>();
		EffectivePerson effectivePerson = this.effectivePerson(request);
		try {
			result = new ActionSearch().execute(effectivePerson, jsonElement);
		} catch (Exception e) {
			logger.error(e, effectivePerson, request, null);
			result.error(e);
		}
		asyncResponse.resume(ResponseFactory.getEntityTagActionResultResponse(request, result));
	}
}
