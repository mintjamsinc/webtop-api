// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.cms.Item;
import api.cms.Search;
import api.http.WebRequest;
import api.http.WebResponse;
import groovy.json.JsonOutput;

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
			item = Item.create(context).findByIdentifier(params.id?.trim());
		} else if (params.path?.trim()) {
			item = Item.create(context).findByPath(params.path?.trim());
		}

		if (!item) {
			// Bad Request
			WebResponse.create(response).setStatus(400);
			return;
		}

		if (!item.exists()) {
			// Not Found
			WebResponse.create(response).setStatus(404);
			return;
		}

		def offset = (params.offset > 0) ? params.offset : 0;
		def limit = (params.limit > 0) ? params.limit : 100;
		def resp = [
			"nextOffset": -1,
			"items": [],
			"total": 0
		];

		def stmt = "/jcr:root";
		if (item.path != "/") {
			stmt += XPath.encodeXML(item.path);
		}
		stmt += "/(element(*,nt:folder)|element(*,nt:file))";
		stmt += " order by @jcr:path";

		def result = Search.create(context).execute([
			"language": "XPath",
			"statement": stmt,
			"offset": offset,
			"limit": limit
		]).toObject();

		resp.total = result.total;
		resp.items = result.items;
		if (result.hasMore) {
			resp.nextOffset = offset + resp.items.size();
		}

		def requestTag = request.getHeader("X-Request-Tag");
		if (requestTag) {
			response.setHeader("X-Request-Tag", requestTag);
		}

		// OK
		WebResponse.create(response)
			.setStatus(200)
			.setContentType("application/json");
		out.print(JsonOutput.toJson(resp));
		return;
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(response).sendError(ex);
	}
}();
