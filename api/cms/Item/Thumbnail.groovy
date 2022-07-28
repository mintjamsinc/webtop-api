// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.cms.Item;
import api.http.WebRequest;
import api.http.WebResponse;
import api.util.YAML;

{->
	if (repositorySession.isAnonymous()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	def identifier = WebAPI.getParameter("id").defaultString().trim();
	if (!identifier) {
		// Bad Request
		WebResponse.create(context).with(response).setStatus(400);
		return;
	}

	try {
		def item = Item.create(context).findByIdentifier(identifier);
		if (!item.exists()) {
			// Not Found
			WebResponse.create(context).with(response).setStatus(404);
			return;
		}

		if (item.contains("mi:thumbnail")) {
			item.getStream("mi:thumbnail").withCloseable { stream ->
				WebResponse
					.create(response)
					.setStatus(200)
					.enableContentCache()
					.setContentType(item.contentType)
					.write(stream);
			}
		} else {
			if (item.contentType.startsWith("image/")) {
				item.contentAsStream.withCloseable { stream ->
					WebResponse
						.create(response)
						.setStatus(200)
						.enableContentCache()
						.setContentType(item.contentType)
						.write(stream);
				}
				return;
			}

			// Not Found
			WebResponse.create(context).with(response).setStatus(404);
		}
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(context).with(response).sendError(ex);
	}
}();
