// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.cms.Item;
import api.cms.Version;
import api.http.WebRequest;
import api.http.WebResponse;
import api.security.Authorizable;

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
		if (params.version == "jcr:rootVersion") {
			// Bad Request
			response.setStatus(400);
			return;
		}

		def version;
		try {
			version = item.versionHistory.getVersion(params.version);
		} catch (Throwable ex) {
			// Bad Request
			response.setStatus(400);
			return;
		}

		version.restore();

		if (!item.isCheckedOut()) {
			item.checkout();
		}
		item.setAttribute("jcr:lastModified", new Date());
		repositorySession.commit();
		if (item.isCheckedOut()) {
			item.checkin();
		}

		// OK
		WebResponse.create(context).with(response)
			.setStatus(200)
			.setContentType("application/json");
		out.print(item.toJson());
		return;
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(context).with(response).sendError(ex);
	}
}();
