// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.cms.Item;
import api.http.WebRequest;
import api.http.WebResponse;

{->
	if (repositorySession.isAnonymous()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	def params = WebRequest.create(request).parseRequest();
	def identifier = params.id?.trim();
	if (!identifier || params.acl == null) {
		// Bad Request
		response.setStatus(400);
		return;
	}

	try {
		def item = Item.create(context).findByIdentifier(identifier);
		if (!item.exists()) {
			// Not Found
			response.setStatus(404);
			return;
		}

		def acl = repositorySession.accessControlManager.getAccessControlList(item.path);
		acl.clear();
		for (ace in params.acl) {
			if (ace.isAllow == null) {
				// Bad Request
				response.setStatus(400);
				return;
			}

			if (!ace.grantee?.trim()) {
				// Bad Request
				response.setStatus(400);
				return;
			}

			if (ace.privileges == null || ace.privileges.size() == 0) {
				// Bad Request
				response.setStatus(400);
				return;
			}

			if (ace.isAllow) {
				acl.allow(ace.grantee, ace.privileges as String[]);
			} else {
				acl.deny(ace.grantee, ace.privileges as String[]);
			}
		};

		repositorySession.commit();

		// OK
		WebResponse.create(response).setStatus(200);
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
