// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.cms.Item;
import api.http.WebRequest;
import api.http.WebResponse;

{->
	if (!repositorySession.isAuthorized()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	def params = WebRequest.create(context).with(request).parseRequest();
	def identifier = params.id?.trim();
	if (!identifier) {
		// Bad Request
		response.setStatus(400);
		return;
	}
	def newName = params.newName?.trim();
	if (!newName) {
		// Bad Request
		response.setStatus(400);
		return;
	}

	try {
		def item = Item.create(context).findByIdentifier(identifier);
		if (!item.exists()) {
			// Not Found
			WebResponse.create(context).with(response).setStatus(404);
			return;
		}

		def destItem = item.parent.getItem(newName);
		if (destItem.exists()) {
			// Conflict
			WebResponse.create(context).with(response).setStatus(409);
			return;
		}

		item.moveTo(destItem.path);
		item.calculate();
		repositorySession.commit();

		// OK
		WebResponse.create(context).with(response)
			.setStatus(200)
			.setContentType("application/json");
		out.print(item.toJson());
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
