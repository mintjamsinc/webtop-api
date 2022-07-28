// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.cms.Item;
import api.http.WebRequest;
import api.http.WebResponse;
import api.security.AccessControlEntry;
import api.security.Authorizable;
import api.security.Privilege;
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

		def resp = [
			"privileges": [],
			"local": [
				"path": item.path,
				"acl": []
			],
			"effective": [],
			"authorizables": [:]
		];

		// privileges
		for (privilege in repositorySession.accessControlManager.getSupportedPrivileges(item.path)) {
			resp.privileges.add(Privilege.create(context).with(privilege).toObject());
		}
		// local
		for (ace in repositorySession.accessControlManager.getAccessControlList(item.path)) {
			resp.local.acl.add(AccessControlEntry.create(context).with(ace).toObject());
		}
		// effective
		for (effectiveAcl in repositorySession.accessControlManager.getEffectiveAccessControlList(item.path)) {
			def acl = [
				"path": effectiveAcl.path,
				"acl": []
			];
			resp.effective.add(acl);
			for (ace in effectiveAcl) {
				acl.acl.add(AccessControlEntry.create(context).with(ace).toObject());

				if (!resp.authorizables[ace.principal.name]) {
					def a = Authorizable.create(context).findByName(ace.principal.name);
					if (a.exists()) {
						resp.authorizables[ace.principal.name] = a.toObject();
					}
				}
			}
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
