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

	try {
		def params = WebRequest.create(request).parseRequest();
		def item;
		if (params.id?.trim()) {
			def itemId = params.id?.trim();
			item = Item.create(context).findByIdentifier(itemId);
			// OK
			WebResponse.create(response)
				.setStatus(200)
				.setContentType("application/json");
			out.print(item.toJson());
			return;
		} else if (params.path?.trim()) {
			def itemPath = params.path?.trim();
			item = Item.create(context).findByPath(itemPath);
		} else {
			// Bad Request
			WebResponse.create(response).setStatus(400);
			return;
		}

		if (!item.exists()) {
			// OK
			WebResponse.create(response)
				.setStatus(200)
				.setContentType("application/json");
			out.print(item.toJson());
			return;
		}

		if (params.version) {
			try {
				item = item.versionHistory.getVersion(params.version).frozen;
			} catch (Throwable ex) {
				// Not Found
				WebResponse.create(response).setStatus(404);
				return;
			}
		}

		// OK
		WebResponse.create(response)
			.setStatus(200)
			.setContentType("application/json");
		out.print(item.toJson());
		return;
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(response).sendError(ex);
	}
}();
