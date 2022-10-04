// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.cms.Item;
import api.cms.Version;
import api.http.WebRequest;
import api.http.WebResponse;
import api.security.Authorizable;
import api.util.JSON;

{->
	if (repositorySession.isAnonymous()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	try {
		def params = WebRequest.create(context).with(request).parseRequest();
		def item;
		if (params.id?.trim()) {
			item = Item.create(context).findByIdentifier(params.id?.trim());
		} else if (params.path?.trim()) {
			item = Item.create(context).findByPath(params.path?.trim());
		}

		if (!item) {
			// Bad Request
			WebResponse.create(context).with(response).setStatus(400);
			return;
		}

		if (!item.exists()) {
			// Not Found
			WebResponse.create(context).with(response).setStatus(404);
			return;
		}
		if (!item.isVersionControlled()) {
			// Bad Request
			response.setStatus(400);
			return;
		}

		def resp = [
			"versions": [],
			"authorizables": [:]
		];

		for (version in item.versionHistory.allVersions) {
			if (version.name == "jcr:rootVersion") {
				continue;
			}
			resp.versions.add(version.toObject());

			if (!resp.authorizables[version.frozen.lastModifiedBy]) {
				def a = Authorizable.create(context).with(context.session.principalProvider.getUserPrincipal(version.frozen.lastModifiedBy));
				if (a.exists()) {
					resp.authorizables[version.frozen.lastModifiedBy] = a.toObject();
				}
			}
		}
		resp.versions = resp.versions.reverse();

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
