// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.bpm.ProcessDefinition;
import api.http.WebRequest;
import api.http.WebResponse;

{->
	if (repositorySession.isAnonymous()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	try {
		def params = WebRequest.create(context).with(request).parseRequest();
		def processDefinitionId = params.id?.trim();
		def pd = ProcessDefinition.create(context).findByIdentifier(processDefinitionId);
		if (!pd.exists()) {
			// Not Found
			response.setStatus(404);
			return;
		}
		if (!pd.isSuspended()) {
			// Conflict
			response.setStatus(409);
			return;
		}

		pd.activate();

		// OK
		WebResponse.create(context).with(response)
			.setStatus(200)
			.setContentType("application/json");
		out.print(pd.toJson());
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(context).with(response).sendError(ex);
	}
}();
