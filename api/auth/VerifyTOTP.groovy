// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.http.WebRequest;
import api.http.WebResponse;
import api.security.otp.TOTP;

{->
	if (repositorySession.isAnonymous()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	try {
		def params = WebRequest.create(request).parseRequest();
		if (!params.secret?.trim() || !params.code?.trim()) {
			// Bad Request
			response.setStatus(400);
			return;
		}

		try {
			if (!TOTP.create(context).verify(params.secret?.trim(), params.code?.trim())) {
				// Unauthorized
				response.setStatus(401);
				return;
			}
		} catch (Throwable ex) {
			log.error(ex.message, ex);
			// Unauthorized
			response.setStatus(401);
			return;
		}

		// OK
		WebResponse.create(response).setStatus(200);
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(response).sendError(ex);
	}
}();
