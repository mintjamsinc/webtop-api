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

	def params = WebRequest.create(context).with(request).parseRequest();
	def identifier = params.id?.trim();
	def path = params.path?.trim();
	if (!identifier || !path) {
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

		def destItem = Item.create(context).findByPath(path);
		def existsDestination = destItem.exists();

		if (existsDestination) {
			if (destItem.isVersionControlled()) {
				if (!destItem.isCheckedOut()) {
					destItem.checkout();
				}
			}
		}

		destItem = item.copyTo(destItem.path);
		destItem.setAttribute("jcr:lastModified", new Date());
		destItem.calculate();
		repositorySession.commit();

		if (existsDestination) {
			if (destItem.isVersionControlled()) {
				if (destItem.isCheckedOut()) {
					destItem.checkin();
				}
			}
		} else {
			if (destItem.isVersionControlled()) {
				if (!destItem.isCheckedOut()) {
					destItem.checkout();
				}
				if (destItem.isCheckedOut()) {
					destItem.checkin();
				}
			}
		}

		// No Content
		WebResponse.create(context).with(response).setStatus(existsDestination ? 200 : 201);
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
