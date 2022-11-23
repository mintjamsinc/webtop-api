// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.http.WebRequest;
import api.http.WebResponse;
import api.security.Authorizable;

{->
	if (!repositorySession.isAuthorized()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	try {
		def params = WebRequest.create(context).with(request).parseRequest();
		def id = params.id?.trim();
		if (!id) {
			// Bad Request
			response.setStatus(400);
			return;
		}
		if (params.isGroup == null) {
			// Bad Request
			response.setStatus(400);
			return;
		}

		def principal;
		if (params.isGroup) {
			principal = repositorySession.userManager.getGroupPrincipal(id);
		} else {
			principal = repositorySession.userManager.getUserPrincipal(id);
		}

		def authorizable = Authorizable.create(context).with(principal);
		if (!authorizable.exists()) {
			// Not Found
			WebResponse.create(context).with(response).setStatus(404);
			return;
		}

		// OK
		WebResponse.create(context).with(response)
			.setStatus(200)
			.setContentType("application/json");
		out.print(authorizable.toJson());
		return;
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(context).with(response).sendError(ex);
	}
}();
