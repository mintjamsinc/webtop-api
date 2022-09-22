// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.cms.Item;
import api.cms.Search;
import api.http.WebRequest;
import api.http.WebResponse;
import api.util.YAML;
import api.util.JSON;

{->
	if (repositorySession.isAnonymous()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	try {
		def params = WebRequest.create(context).with(request).parseRequest();
		def item = Item.create(context).findByPath("/content/WEB-INF/facets");

		def offset = (params.offset > 0) ? params.offset : 0;
		def limit = (params.limit > 0) ? params.limit : 2000;
		def resp = [
			"nextOffset": -1,
			"facetDefinitions": [],
			"total": 0
		];

		if (item && item.exists()) {
			if (params.identifiers != null) {
				params.identifiers.each { identifier ->
					def e = item.getItem(identifier + ".yml");
					if (!e.exists()) {
						return;
					}

					try {
						resp.facetDefinitions.add(YAML.parse(e.resource));
						resp.total++;
					} catch (Throwable ex) {
						log.warn("Could not parse " + e.name, ex);
					}
				}
			} else {
				def stmt = "/jcr:root";
				if (item.path != "/") {
					stmt += XPath.encodeXML(item.path);
				}
				stmt += "/element(*,nt:file)[";
				stmt += "jcr:like(fn:name(),'%.yml')";
				stmt += "]";
				stmt += " order by @jcr:path";

				def result = Search.create(context).execute([
					"language": "XPath",
					"statement": stmt,
					"offset": offset,
					"limit": limit
				]);

				resp.total = result.total;
				result.items.each { e ->
					try {
						resp.facetDefinitions.add(YAML.parse(e.resource));
					} catch (Throwable ex) {
						log.warn("Could not parse " + e.name, ex);
					}
				}
				if (result.hasMore()) {
					resp.nextOffset = offset + result.items.size();
				}
			}
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
