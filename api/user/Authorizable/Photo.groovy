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
		"range": WebRequest.create(context).with(request).range
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
		def attributes;
		try {
			def principal;
			if (params.id.endsWith("@group")) {
				principal = repositorySession.userManager.getGroupPrincipal(params.id.substring(0, params.id.lastIndexOf("@")));
			} else if (params.id.endsWith("@user")) {
				principal = repositorySession.userManager.getUserPrincipal(params.id.substring(0, params.id.lastIndexOf("@")));
			} else {
				principal = repositorySession.userManager.getUserPrincipal(params.id);
			}
			def authorizable = Authorizable.create(context).with(principal);
			if (!authorizable.exists()) {
				// Not Found
				response.setStatus(404);
				return;
			}
			attributes = authorizable.attributes;
			if (!attributes.contains("mi:photo")) {
				// Not Found
				response.setStatus(404);
				return;
			}
		} catch (Throwable ignore) {
			// Not Found
			response.setStatus(404);
			return;
		}

		WebResponse
			.create(response)
			.setStatus(200)
			.enableContentCache()
			.setContentType(attributes.getAttribute("mi:photoType"))
			.setContentLength(attributes.getDataLength("mi:photo"))
			.setETag(attributes.lastModified.time as String)
			.writePartial(attributes.getStream("mi:photo"), params.range);
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(context).with(response).sendError(ex);
	}
}();
