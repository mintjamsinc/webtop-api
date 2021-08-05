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
		def params = WebRequest.create(request).parseRequest();
		def processInstanceId = params.id?.trim();
		if (processInstanceId) {
			def pi = ProcessInstance.create(context).findByIdentifier(processInstanceId);
			if (!pi.exists()) {
				// Not Found
				WebResponse.create(response).setStatus(404);
				return;
			}

			// OK
			WebResponse.create(response)
				.setStatus(200)
				.setContentType("application/json");
			out.print(pi.toJson());
			return;
		}

		// Bad Request
		WebResponse.create(response).setStatus(400);
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(response).sendError(ex);
	}
}();
