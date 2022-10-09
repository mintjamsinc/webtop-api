// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.cms.Crawler;
import api.http.WebRequest;
import api.http.WebResponse;

{->
	if (repositorySession.isAuthorized()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	def crawler = Crawler.create(context);
	try {
		def params = WebRequest.create(context).with(request).parseRequest();
		def identifier = params.identifier?.trim();
		if (!identifier) {
			// Bad Request
			WebResponse.create(context).with(response).setStatus(400);
			return;
		}

		crawler.resolve(identifier);
		if (!crawler.exists()) {
			// Not Found
			WebResponse.create(context).with(response).setStatus(404);
			return;
		}

		crawler.remove();

		// No Content
		WebResponse.create(context).with(response).setStatus(204);
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(context).with(response).sendError(ex);
	}
}();
