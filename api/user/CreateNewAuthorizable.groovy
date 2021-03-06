// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.cms.Item;
import api.http.WebRequest;
import api.http.WebResponse;
import api.security.Authorizable;
import api.security.Group;
import api.security.User;

{->
	if (repositorySession.isAnonymous()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	def params = WebRequest.create(request).parseRequest();
	def id = params.id?.trim();
	if (!id) {
		// Bad Request
		response.setStatus(400);
		return;
	}
	if (params.isGroup == null) {
		// Bad Request
		response.setStatus(400);
		return;
	}
	if (!params.isGroup && !params.password?.trim()) {
		// Bad Request
		response.setStatus(400);
		return;
	}

	try {
		def authorizable = Authorizable.create(context).findByName(id);
		if (authorizable.exists()) {
			// Conflict
			WebResponse.create(response).setStatus(409);
			return;
		}

		def um = repositorySession.userManager;
		if (params.isGroup) {
			authorizable = Group.create(context).with(um.createGroup(id));
		} else {
			authorizable = User.create(context).with(um.createUser(id, params.password?.trim()));
		}
		repositorySession.commit();

		if (!params.isGroup) {
			def webtopGroup = Authorizable.create(context).findByName("webtop-users");
			if (!webtopGroup.exists()) {
				webtopGroup = Group.create(context).with(um.createGroup("webtop-users"));
				repositorySession.commit();
				repositorySession.accessControlManager.getAccessControlList("/home").allow(webtopGroup.name, "jcr:read");
				repositorySession.commit();
			}

			webtopGroup.addMembers([authorizable.name] as String[]);
			def userFolder = Item.create(context).findByPath("/var/webtop/home/" + authorizable.name);
			if (!userFolder.exists()) {
				userFolder.mkdirs();
				def acl = repositorySession.accessControlManager.getAccessControlList(userFolder.path);
				acl.clear();
				acl.allow(authorizable.name, "jcr:all");
			}
			def appsFolder = Item.create(context).findByPath("/var/webtop/apps");
			if (!appsFolder.exists()) {
				appsFolder.mkdirs();
				def acl = repositorySession.accessControlManager.getAccessControlList(appsFolder.path);
				acl.clear();
				acl.allow(webtopGroup.name, ["jcr:read", "rep:write"] as String[]);
			}
			repositorySession.commit();
		}

		// No Content
		WebResponse.create(response).setStatus(201);
		out.print(authorizable.toJson());
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
