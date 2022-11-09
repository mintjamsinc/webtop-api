// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.http.WebRequest;
import api.http.WebResponse;
import api.security.Authorizable;
import api.util.Text;
import api.util.JSON;

{->
	if (!repositorySession.isAuthorized()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	try {
		def params = WebRequest.create(context).with(request).parseRequest();

		if (params.identifiers != null) {
			def resp = [
				"nextOffset": -1,
				"authorizables": []
			];

			for (id in params.identifiers) {
				try {
					def principal;
					if (id.endsWith("@group")) {
						principal = repositorySession.userManager.getGroupPrincipal(id.substring(0, id.lastIndexOf("@")));
					} else if (id.endsWith("@user")) {
						principal = repositorySession.userManager.getUserPrincipal(id.substring(0, id.lastIndexOf("@")));
					} else {
						principal = repositorySession.userManager.getUserPrincipal(id);
					}
					def e = Authorizable.create(context).with(principal);
					if (e.exists()) {
						resp.authorizables.add(e.toObject());
					}
				} catch (Throwable ignore) {}
			}

			// OK
			WebResponse.create(context).with(response)
				.setStatus(200)
				.setContentType("application/json");
			out.print(JSON.stringify(resp));
			return;
		}

		def offset = (params.offset > 0) ? params.offset : 0;
		def limit = (params.limit > 0) ? params.limit : 20;
		def resp = [
			"nextOffset": -1,
			"authorizables": []
		];

		def stmt = "";
		def condition = "jcr:name = 'attributes' and @identifier";
		if (params.selector) {
			condition += " and @isGroup = " + (params.selector == "groups");
		}
		if (params.q) {
			condition += " and jcr:contains(.,'" + params.q + "')";
		}
		if (!condition) {
			// Bad Request
			response.setStatus(400);
			return;
		}
		stmt += "[" + condition + "]";
		stmt += " order by @mi:fullName, @identifier";

		def i = repositorySession.userManager.search(stmt, offset, limit);
		while (i.hasNext()) {
			if (resp.authorizables.size() >= limit) {
				resp.nextOffset = offset + limit;
				break;
			}

			resp.authorizables.add(Authorizable.create(context).with(i.next()).toObject());
		}

		def requestTag = request.getHeader("X-Request-Tag");
		if (requestTag) {
			response.setHeader("X-Request-Tag", requestTag);
		}

		// OK
		WebResponse.create(context).with(response)
			.setStatus(200)
			.setContentType("application/json");
		out.print(JSON.stringify(resp));
		return;
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(context).with(response).sendError(ex);
	}
}();
