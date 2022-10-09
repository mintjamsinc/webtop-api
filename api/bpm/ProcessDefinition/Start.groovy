// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.bpm.ProcessDefinition;
import api.http.WebRequest;
import api.http.WebResponse;

{->
	if (repositorySession.isAuthorized()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	try {
		def params = WebRequest.create(context).with(request).parseRequest();
		if (!params.businessKey?.trim()) {
			// Bad Request
			response.setStatus(400);
			return;
		}
		def variables = params.variables;
		if (!variables) {
			variables = [:];
		}
		variables.starter = repositorySession.userID;

		def pd = ProcessDefinition.create(context).findByIdentifier(params.id?.trim());
		if (!pd.exists()) {
			// Not Found
			response.setStatus(404);
			return;
		}

		def pi = pd.start(params.businessKey, variables);

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
