// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.cms.Item;
import api.http.WebRequest;
import api.http.WebResponse;

{->
	if (repositorySession.isAuthorized()) {
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

	try {
		def item = Item.create(context).findByIdentifier(identifier);
		if (!item.exists()) {
			// Not Found
			WebResponse.create(context).with(response).setStatus(404);
			return;
		}
		if (item.isCollection()) {
			// Bad Request
			WebResponse.create(context).with(response).setStatus(400);
			return;
		}

		def destItem;
		for (int i = 1; i <= 100; i++) {
			def filename = item.name;
			def p = filename.lastIndexOf(".");
			if (p == -1) {
				filename = filename + " (" + i + ")";
			} else {
				filename = filename.substring(0, p) + " (" + i + ")" + filename.substring(p);
			}

			destItem = context.repositorySession.resourceResolver.getResource(item.parent.path + "/" + filename);
			if (!destItem.exists()) {
				break;
			}
		}
		if (destItem.exists()) {
			// Conflict
			WebResponse.create(context).with(response).setStatus(409);
			return;
		}

		destItem = item.copyTo(destItem.path);
		repositorySession.commit();

		if (destItem.isVersionControlled()) {
			if (!destItem.isCheckedOut()) {
				destItem.checkout();
			}
			if (destItem.isCheckedOut()) {
				destItem.checkin();
			}
		}

		// OK
		WebResponse.create(context).with(response)
			.setStatus(200)
			.setContentType("application/json");
		out.print(destItem.toJson());
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
