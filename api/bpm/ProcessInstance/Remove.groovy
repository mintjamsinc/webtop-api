// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.bpm.ProcessInstance;
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
		def processInstanceId = params.id?.trim();
		def pi = ProcessInstance.create(context).findByIdentifier(processInstanceId);
		if (!pi.exists()) {
			// Not Found
			response.setStatus(404);
			return;
		}

		pi.remove();

		// No Content
		WebResponse.create(context).with(response).setStatus(204);
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(context).with(response).sendError(ex);
	}
}();
