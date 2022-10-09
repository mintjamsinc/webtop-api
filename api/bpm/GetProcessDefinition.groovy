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
		def processDefinitionId = params.id?.trim();
		if (processDefinitionId) {
			def pd = ProcessDefinition.create(context).findByIdentifier(processDefinitionId);
			if (!pd.exists()) {
				// Not Found
				WebResponse.create(context).with(response).setStatus(404);
				return;
			}

			// OK
			WebResponse.create(context).with(response)
				.setStatus(200)
				.setContentType("application/json");
			out.print(pd.toJson());
			return;
		}

		def processDefinitionKey = params.key?.trim();
		if (processDefinitionKey) {
			def pd = ProcessDefinition.create(context).findByKey(processDefinitionKey);
			if (!pd.exists()) {
				// Not Found
				WebResponse.create(context).with(response).setStatus(404);
				return;
			}

			// OK
			WebResponse.create(context).with(response)
				.setStatus(200)
				.setContentType("application/json");
			out.print(pd.toJson());
			return;
		}

		// Bad Request
		WebResponse.create(context).with(response).setStatus(400);
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(context).with(response).sendError(ex);
	}
}();
