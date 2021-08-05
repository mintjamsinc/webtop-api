// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.cms.Crawler;
import api.http.WebRequest;
import api.http.WebResponse;

{->
	if (repositorySession.isAnonymous()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	def crawler = Crawler.create(context);
	try {
		def params = WebRequest.create(request).parseRequest();
		def identifier = params.identifier?.trim();
		if (!identifier) {
			// Bad Request
			WebResponse.create(response).setStatus(400);
			return;
		}

		crawler.resolve(identifier);
		if (!crawler.exists()) {
			// Not Found
			WebResponse.create(response).setStatus(404);
			return;
		}

		crawler.remove();

		// No Content
		WebResponse.create(response).setStatus(204);
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(response).sendError(ex);
	}
}();
