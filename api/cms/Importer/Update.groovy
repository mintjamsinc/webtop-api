// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.cms.Importer;
import api.http.WebRequest;
import api.http.WebResponse;

{->
	if (repositorySession.isAnonymous()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	def imp = Importer.create(context);
	try {
		def params = WebRequest.create(request).parseRequest();
		def identifier = params.identifier?.trim();
		if (!identifier) {
			// Bad Request
			WebResponse.create(response).setStatus(400);
			return;
		}

		imp.resolve(identifier);
		if (!imp.exists()) {
			// Not Found
			WebResponse.create(response).setStatus(404);
			return;
		}

		// OK
		WebResponse.create(response)
			.setStatus(200)
			.setContentType("application/json");
		out.print(imp.toJson());
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(response).sendError(ex);
	}
}();
