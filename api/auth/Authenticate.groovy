// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.http.WebRequest;
import api.http.WebResponse;
import api.security.User;
import api.security.otp.TOTP;
import api.util.JSON;

{->
	try {
		def params = WebRequest.create(context).with(request).parseRequest();

		def resp = [
			"options": []
		];

		if (params.option == 'totp') {
			if (!params.code?.trim()) {
				// Bad Request
				response.setStatus(400);
				return;
			}

			def user = User.create(context).with(repositorySession.userPrincipal);
			def attributes = user.attributes;
			if (!attributes.exists()) {
				// Unauthorized
				response.setStatus(401);
				return;

			}

			def secret = attributes.getString("mi:totpSecret")?.trim();
			if (!secret) {
				// Unauthorized
				response.setStatus(401);
				return;
			}

			def code = params.code?.trim();
			if (!TOTP.create(context).verify(secret, code)) {
				def recoveryCodes = attributes.getStringArray("mi:totpRecoveryCodes") as List;
				if (!recoveryCodes.contains(code)) {
					// Unauthorized
					response.setStatus(401);
					return;
				}

				recoveryCodes.remove(code);
				attributes.setAttribute("mi:totpRecoveryCodes", recoveryCodes as String[], true);
				repositorySession.commit();
			}

			def authenticatedFactors = request.getSession().getAttribute("org.mintjams.cms.security.auth.AuthenticatedFactors");
			if (!authenticatedFactors) {
				// Unauthorized
				response.setStatus(401);
				return;
			}
			authenticatedFactors += ",totp";
			request.getSession().setAttribute("org.mintjams.cms.security.auth.AuthenticatedFactors", authenticatedFactors);
		}

		// OK
		WebResponse.create(context).with(response)
			.setStatus(200)
			.setContentType("application/json");
		out.print(JSON.stringify(resp));
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(context).with(response).sendError(ex);
	}
}();
