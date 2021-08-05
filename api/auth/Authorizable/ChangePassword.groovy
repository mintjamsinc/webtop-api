// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.http.WebRequest;
import api.http.WebResponse;
import api.security.User;

{->
	if (repositorySession.isAnonymous()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	try {
		def params = WebRequest.create(request).parseRequest();
		if (!params.newPassword?.trim() || !params.password?.trim()) {
			// Bad Request
			response.setStatus(400);
			return;
		}

		repositorySession.changePassword(params.newPassword?.trim(), params.password?.trim());

		// commit
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
