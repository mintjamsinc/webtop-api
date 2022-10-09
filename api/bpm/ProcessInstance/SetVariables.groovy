// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.bpm.ProcessInstance;
import api.bpm.ProcessInstanceHelper;
import api.http.WebRequest;
import api.http.WebResponse;

{->
	if (!repositorySession.isAuthorized()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	try {
		def params = WebRequest.create(context).with(request).parseRequest();
		def processInstanceId = params.id?.trim();
		def pi = ProcessInstance.create(context).findByIdentifier(processInstanceId);
		if (!pi.exists()) {
			// Not Found
			response.setStatus(404);
			return;
		}
		if (!params.variables) {
			// Bad Request
			response.setStatus(400);
			return;
		}

		ProcessInstanceHelper.create(context).with(pi).importVariables(params.variables);
		pi = ProcessInstance.create(context).findByIdentifier(processInstanceId);

		// OK
		WebResponse.create(context).with(response)
			.setStatus(200)
			.setContentType("application/json");
		out.print(pi.toJson());
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(context).with(response).sendError(ex);
	}
}();
