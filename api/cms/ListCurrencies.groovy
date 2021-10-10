// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.util.ISO4217;
import api.http.WebRequest;
import api.http.WebResponse;
import api.util.JSON;

{->
	if (repositorySession.isAnonymous()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	try {
		def resp = [
			"nextOffset": -1,
			"currencies": [],
			"total": 0
		];

		resp.currencies = ISO4217.currencies(request.getLocale());
		resp.total = resp.currencies.size();

		def requestTag = request.getHeader("X-Request-Tag");
		if (requestTag) {
			response.setHeader("X-Request-Tag", requestTag);
		}

		// OK
		WebResponse.create(response)
			.setStatus(200)
			.setContentType("application/json");
		out.print(JSON.stringify(resp));
		return;
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(response).sendError(ex);
	}
}();
