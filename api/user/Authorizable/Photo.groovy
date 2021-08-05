// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.security.Authorizable;
import api.http.WebRequest;
import api.http.WebResponse;

{->
	if (repositorySession.isAnonymous()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	def params = [
		"id": WebAPI.getParameter("id").defaultString(),
		"range": WebRequest.create(request).range
	];
	if (!params.id) {
		// Bad Request
		response.setStatus(400);
		return;
	}
	if (params.range && !params.range.isValid()) {
		// Bad Request
		response.setStatus(400);
		return;
	}

	try {
		def authorizable = Authorizable.create(context).findByName(params.id);
		if (!authorizable.exists() || !authorizable.contains("photo")) {
			// Not Found
			response.setStatus(404);
			return;
		}

		WebResponse
			.create(response)
			.setStatus(200)
			.enableContentCache()
			.setContentType(authorizable.getBinaryType("photo"))
			.setContentLength(authorizable.getBinaryLength("photo"))
			.setETag(authorizable.getDate("lastModified").time as String)
			.writePartial(authorizable.getStream("photo"), params.range);
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(response).sendError(ex);
	}
}();
