// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.bpm.Task;
import api.http.WebRequest;
import api.http.WebResponse;

{->
	if (!repositorySession.isAuthorized()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	try {
		def params = WebRequest.create(context).with(request).parseRequest();
		def taskId = params.id?.trim();
		def task = Task.create(context).findByIdentifier(taskId);
		if (!task.exists()) {
			// Not Found
			WebResponse.create(context).with(response).setStatus(404);
			return;
		}

		// OK
		WebResponse.create(context).with(response)
			.setStatus(200)
			.setContentType("application/json");
		out.print(task.toJson());
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(context).with(response).sendError(ex);
	}
}();
