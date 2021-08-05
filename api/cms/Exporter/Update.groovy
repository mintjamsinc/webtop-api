// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.cms.Exporter;
import api.http.WebRequest;
import api.http.WebResponse;

{->
	if (repositorySession.isAnonymous()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	def exp = Exporter.create(context);
	try {
		def params = WebRequest.create(request).parseRequest();
		def identifier = params.identifier?.trim();
		if (!identifier) {
			// Bad Request
			WebResponse.create(response).setStatus(400);
			return;
		}

		exp.resolve(identifier);
		if (!exp.exists()) {
			// Not Found
			WebResponse.create(response).setStatus(404);
			return;
		}

		// OK
		WebResponse.create(response)
			.setStatus(200)
			.setContentType("application/json");
		out.print(exp.toJson());
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(response).sendError(ex);
	}
}();
