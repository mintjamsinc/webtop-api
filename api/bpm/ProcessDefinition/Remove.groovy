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
		def params = WebRequest.create(request).parseRequest();
		def processDefinitionId = params.id?.trim();
		def pd = ProcessDefinition.create(context).findByIdentifier(processDefinitionId);
		if (!pd.exists()) {
			// Not Found
			response.setStatus(404);
			return;
		}
		if (pd.countProcessInstances() > 0) {
			// Conflict
			response.setStatus(409);
			return;
		}

		pd.remove();

		// No Content
		WebResponse.create(response).setStatus(204);
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(response).sendError(ex);
	}
}();
