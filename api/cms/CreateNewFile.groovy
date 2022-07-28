// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.cms.Item;
import api.cms.ItemHelper;
import api.cms.MultipartUpload;
import api.http.WebRequest;
import api.http.WebResponse;

{->
	if (repositorySession.isAnonymous()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	def params = WebRequest.create(context).with(request).parseRequest();
	def path = params.path?.trim();
	if (!path) {
		// Bad Request
		response.setStatus(400);
		return;
	}

	try {
		def item = Item.create(context).findByPath(path);
		if (item.exists()) {
			// Conflict
			WebResponse.create(context).with(response).setStatus(409);
			return;
		}

		item.createNewFile();

		if (params.uploadID) {
			def mu = MultipartUpload.create(context).resolve(params.uploadID);
			if (!mu.exists()) {
				// Bad Request
				WebResponse.create(context).with(response).setStatus(400);
				return;
			}

			ItemHelper.create(context).with(item).importContent(mu, params.mimeType);
			mu.remove();
		}

		repositorySession.commit();

		// No Content
		WebResponse.create(context).with(response).setStatus(201);
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
