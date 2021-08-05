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
	def name = params.name?.trim();
	if (!identifier) {
		// Bad Request
		response.setStatus(400);
		return;
	}

	try {
		def parentItem = Item.create(context).findByIdentifier(identifier);
		if (!parentItem.exists()) {
			// Not Found
			response.setStatus(404);
			return;
		}

		def item;
		if (name) {
			item = parentItem.getItem(name);
		} else {
			for (def i = 0; i < 100; i++) {
				if (i == 0) {
					name = "New folder"
				} else {
					name = "New folder (" + i + ")"
				}

				item = parentItem.getItem(name);
				if (!item.exists()) {
					break;
				}
			}
		}
		if (item.exists()) {
			// Conflict
			response.setStatus(409);
			return;
		}

		item.mkdirs();
		repositorySession.commit();

		// No Content
		WebResponse.create(response).setStatus(201);
		out.print(item.toJson());
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
