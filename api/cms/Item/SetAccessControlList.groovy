// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.cms.Item;
import api.http.WebRequest;
import api.http.WebResponse;
import org.mintjams.jcr.security.PrincipalNotFoundException;

{->
	if (!repositorySession.isAuthorized()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	def params = WebRequest.create(context).with(request).parseRequest();
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

			def principal = null;
			try {
				if (ace.grantee.endsWith("@user")) {
					principal = repositorySession.principalProvider.getUserPrincipal(ace.grantee.split("@")[0]);
				} else if (ace.grantee.endsWith("@group")) {
					principal = repositorySession.principalProvider.getGroupPrincipal(ace.grantee.split("@")[0]);
				} else {
					try {
						principal = repositorySession.principalProvider.getGroupPrincipal(ace.grantee);
					} catch (PrincipalNotFoundException ignore) {
						principal = repositorySession.principalProvider.getUserPrincipal(ace.grantee);
					}
				}
			} catch (PrincipalNotFoundException ignore) {
				// Bad Request
				response.setStatus(400);
				return;
			}
			acl.addAccessControlEntry(principal, ace.isAllow, ace.privileges as String[]);
		};
		repositorySession.accessControlManager.setPolicy(item.path, acl);

		repositorySession.commit();

		// OK
		WebResponse.create(context).with(response).setStatus(200);
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
