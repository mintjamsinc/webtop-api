// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.cms.Item;
import api.http.WebRequest;
import api.http.WebResponse;
import api.security.Authorizable;

{->
	if (repositorySession.isAnonymous()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	try {
		def now = new Date();
		def params = WebRequest.create(request).parseRequest();
		if (!params.id?.trim()) {
			// Bad Request
			response.setStatus(400);
			return;
		}

		def authorizable = Authorizable.create(context).findByName(params.id?.trim());
		if (!authorizable.exists()) {
			// Not Found
			WebResponse.create(response).setStatus(404);
			return;
		}

		authorizable.remove();

		def userFolder = Item.create(context).findByPath("/var/webtop/home/" + params.id?.trim());
		if (userFolder.exists()) {
			userFolder.remove();
		}

		repositorySession.commit();

		// No Content
		WebResponse.create(response).setStatus(204);
		return;
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(response).sendError(ex);
	} finally {
		try {
			repositorySession.rollback();
		} catch (Throwable ignore) {}
	}
}();
