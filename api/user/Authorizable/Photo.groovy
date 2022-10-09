// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.http.WebRequest;
import api.http.WebResponse;
import api.security.Authorizable;
import org.mintjams.jcr.security.UnknownUserPrincipal;

{->
	if (!repositorySession.isAuthorized()) {
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
		def principal;
		try {
			if (params.id.endsWith("@group")) {
				principal = repositorySession.principalProvider.getGroupPrincipal(params.id.substring(0, params.id.lastIndexOf("@")));
			} else if (params.id.endsWith("@user")) {
				principal = repositorySession.principalProvider.getUserPrincipal(params.id.substring(0, params.id.lastIndexOf("@")));
			} else {
				principal = repositorySession.principalProvider.getUserPrincipal(params.id);
			}
		} catch (Throwable ignore) {
			def id = params.id;
			if (id.endsWith("@group") || id.endsWith("@user")) {
				id = id.substring(0, id.lastIndexOf("@"));
			}
			principal = new UnknownUserPrincipal(id);
		}
		def authorizable = Authorizable.create(context).with(principal);
		if (!authorizable.exists()) {
			// Not Found
			response.setStatus(404);
			return;
		}
		def attributes = authorizable.attributes;
		if (!attributes.contains("mi:photo")) {
			// Not Found
			response.setStatus(404);
			return;
		}

		WebResponse
			.create(context)
			.with(response)
			.setStatus(200)
			.enableContentCache()
			.setContentType(attributes.getString("mi:photoType"))
			.setContentLength(attributes.getDataLength("mi:photo"))
			.setETag(attributes.lastModified.time as String)
			.writePartial(attributes.getStream("mi:photo"), params.range);
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(context).with(response).sendError(ex);
	}
}();
