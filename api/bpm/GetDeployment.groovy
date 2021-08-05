// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.bpm.Deployment;
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
		def deploymentId = params.id?.trim();
		def deployment = Deployment.create(deployment).findByIdentifier(deploymentId);

		// OK
		WebResponse.create(response)
			.setStatus(200)
			.setContentType("application/json");
		out.print(deployment.toJson());
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(response).sendError(ex);
	}
}();
