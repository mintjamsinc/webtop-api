// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.cms.Item;
import api.cms.Search;
import api.http.WebRequest;
import api.http.WebResponse;
import api.util.JSON;

{->
	if (!repositorySession.isAuthorized()) {
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

		def offset = (params.offset > 0) ? params.offset : 0;
		def limit = (params.limit > 0) ? params.limit : 20;
		def resp = [
			"nextOffset": -1,
			"items": []
		];

		def i = item.list();
		if (offset > 0) {
			i.skip(offset);
		}
		while (i.hasNext() && resp.items.size() < limit) {
			resp.items.add(i.next().toObject());
		}
		if (i.hasNext()) {
			resp.nextOffset = offset + resp.items.size();
		}

		def requestTag = request.getHeader("X-Request-Tag");
		if (requestTag) {
			response.setHeader("X-Request-Tag", requestTag);
		}

		// OK
		WebResponse.create(context).with(response)
			.setStatus(200)
			.setContentType("application/json");
		out.print(JSON.stringify(resp));
		return;
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(context).with(response).sendError(ex);
	}
}();
