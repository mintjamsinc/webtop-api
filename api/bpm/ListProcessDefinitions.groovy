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
		def limit = (params.limit > 0) ? params.limit : 2000;
		def resp = [
			"nextOffset": offset,
			"processDefinitions": []
		];

		def query = ProcessAPI.engine.repositoryService.createProcessDefinitionQuery()
			.orderByProcessDefinitionKey().asc()
			.orderByProcessDefinitionVersion().desc();

		for (; resp.processDefinitions.size() < limit; offset += limit) {
			def results = query.listPage(offset, offset + limit);

			for (item in results) {
				resp.nextOffset++;

				resp.processDefinitions.add(ProcessDefinition.create(context).with(item).toObject());
				if (resp.processDefinitions.size() >= limit) {
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
