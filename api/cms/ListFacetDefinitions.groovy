// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.http.WebRequest;
import api.http.WebResponse;
import api.util.YAML;
import api.util.JSON;

{->
	if (!repositorySession.isAuthorized()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	try {
		def params = WebRequest.create(context).with(request).parseRequest();

		def offset = (params.offset > 0) ? params.offset : 0;
		def limit = (params.limit > 0) ? params.limit : 2000;
		def resp = [
			"nextOffset": -1,
			"facetDefinitions": [],
			"total": 0
		];

		if (params.identifiers != null) {
			repositorySession.workspace.facetProvider.getFacets(params.identifiers as String[]).each { identifier, definition ->
				resp.facetDefinitions.add(definition);
				resp.total++;
			}
		} else {
			def allFacets = repositorySession.workspace.facetProvider.allFacets;
			resp.total = allFacets.size();
			def skipNum = offset;
			for (e in allFacets) {
				if (skipNum > 0) {
					skipNum--;
					continue;
				}
				if (resp.facetDefinitions.size() >= limit) {
					resp.nextOffset = offset + limit;
					break;
				}

				resp.facetDefinitions.add(e.value);
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
