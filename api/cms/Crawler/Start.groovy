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
		def params = WebRequest.create(context).with(request).parseRequest();
		def statement = params.statement?.trim();
		def language = params.language?.trim();
		if (!statement) {
			// Bad Request
			WebResponse.create(context).with(response).setStatus(400);
			return;
		}

		crawler.start(statement, language);

		// Created
		WebResponse.create(context).with(response)
			.setStatus(201)
			.setContentType("application/json");
		out.print(crawler.toJson());
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(context).with(response).sendError(ex);
	}
}();
