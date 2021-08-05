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
				if (params.mimeType == "text/plain") {
					if (i == 0) {
						name = "New document.txt"
					} else {
						name = "New document (" + i + ").txt"
					}
				} else if (params.mimeType == "text/html") {
					if (i == 0) {
						name = "New document.html"
					} else {
						name = "New document (" + i + ").html"
					}
				} else if (params.mimeType == "application/bpmn+xml") {
					if (i == 0) {
						name = "New document.bpmn"
					} else {
						name = "New document (" + i + ").bpmn"
					}
				} else if (params.mimeType == "application/cmmn+xml") {
					if (i == 0) {
						name = "New document.cmmn"
					} else {
						name = "New document (" + i + ").cmmn"
					}
				} else if (params.mimeType == "application/dmn+xml") {
					if (i == 0) {
						name = "New document.dmn"
					} else {
						name = "New document (" + i + ").dmn"
					}
				} else {
					// Bad Request
					response.setStatus(400);
					return;
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

		item.createNewFile();

		if (params.uploadID) {
			def mu = MultipartUpload.create(context).resolve(params.uploadID);
			if (!mu.exists()) {
				// Bad Request
				WebResponse.create(response).setStatus(400);
				return;
			}

			ItemHelper.create(context).with(item).importContent(mu, params.mimeType);
		}

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
