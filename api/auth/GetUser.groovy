// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.http.WebRequest;
import api.http.WebResponse;
import api.security.Session;
import api.security.User;

{->
	if (!repositorySession.isAuthorized()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	try {
		def user = User.create(context).with(repositorySession.userPrincipal);

		def eTag = (new Date().getTime() as String) + "-" + java.util.UUID.randomUUID().toString();
		Session.create(context, repositorySession.userID, eTag);

		// OK
		WebResponse.create(context).with(response)
			.setStatus(200)
			.setContentType("application/json")
			.setETag(eTag);
		out.print(user.toJson());
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(context).with(response).sendError(ex);
	}
}();
