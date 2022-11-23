// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.cms.Item;
import api.http.WebRequest;
import api.http.WebResponse;
import api.security.Authorizable;
import api.security.Group;
import api.security.User;

{->
	if (!repositorySession.isAuthorized()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

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

	try {
		def principal;
		if (params.isGroup) {
			principal = repositorySession.principalProvider.getGroupPrincipal(id);
		} else {
			principal = repositorySession.principalProvider.getUserPrincipal(id);
		}

		def authorizable = Authorizable.create(context).with(principal);
		if (authorizable.exists()) {
			// Conflict
			WebResponse.create(context).with(response).setStatus(409);
			return;
		}

		repositorySession.userManager.registerIfNotExists(principal);

		// Created
		WebResponse.create(context).with(response).setStatus(201);
		out.print(authorizable.toJson());
		return;
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(context).with(response).sendError(ex);
	} finally {
		try {
			repositorySession.rollback();
		} catch (Throwable ignore) {}
	}
}();
