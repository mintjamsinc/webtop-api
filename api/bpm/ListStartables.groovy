// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.bpm.ProcessDefinition;
import api.http.WebRequest;
import api.http.WebResponse;
import api.util.JSON;

{->
	if (repositorySession.isAuthorized()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	try {
		def params = WebRequest.create(context).with(request).parseRequest();
		def offset = (params.offset > 0) ? params.offset : 0;
		def limit = (params.limit > 0) ? params.limit : 100;
		def resp = [
			"nextOffset": offset,
			"startables": []
		];

		def query = ProcessAPI.engine.repositoryService.createProcessDefinitionQuery()
			.startableInTasklist()
			.active()
			.orderByProcessDefinitionKey().asc()
			.orderByProcessDefinitionVersion().desc();

		for (; resp.startables.size() < limit; offset += limit) {
			def results = query.listPage(offset, offset + limit);

			for (item in results) {
				resp.nextOffset++;

				def startFormKey = ProcessAPI.engine.formService.getStartFormKey(item.id);
				if (!startFormKey) {
					continue;
				}
				if (startFormKey.startsWith("app://")) {
					// application
				} else {
					def canStartFormRead = false;
					for (prefix in ["jcr:///bin/cms.html/"/* deprecated */, "cms://cgi/", "cms://content/"]) {
						if (startFormKey.startsWith(prefix)) {
							startFormKey = "/content/" + startFormKey.substring(prefix.length());
							try {
								if (repositorySession.resourceResolver.getResource(startFormKey).canRead()) {
									canStartFormRead = true;
									break;
								}
							} catch (Throwable ignore) {}
						}
					}
					if (!canStartFormRead) {
						continue;
					}
				}

				resp.startables.add(ProcessDefinition.create(context).with(item).toObject());
				if (resp.startables.size() >= limit) {
					break;
				}
			}

			def hasMore = ((offset + limit) < query.count());
			if (!hasMore) {
				resp.nextOffset = -1;
				break;
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
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(context).with(response).sendError(ex);
	}
}();
