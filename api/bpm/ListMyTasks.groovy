// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.bpm.Task;
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
			"tasks": []
		];

		def taskService = ProcessAPI.engine.taskService;
		def query = taskService.createTaskQuery()
			.taskAssignee(repositorySession.userID)
			.initializeFormKeys()
			.orderByDueDate().asc()
			.orderByTaskCreateTime().asc();

		for (; resp.tasks.size() < limit; offset += limit) {
			def results = query.listPage(offset, offset + limit);

			for (item in results) {
				resp.nextOffset++;

				resp.tasks.add(Task.create(context).with(item).toObject());
				if (resp.tasks.size() >= limit) {
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
