// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.bpm.Task;
import api.http.WebRequest;
import api.http.WebResponse;

{->
	if (repositorySession.isAnonymous()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	try {
		def params = WebRequest.create(context).with(request).parseRequest();
		def task = Task.create(context).findByIdentifier(params.taskId);
		if (!(params.checkAssignee == false)) {
			if (task.assignee != repositorySession.userID) {
				// Conflict
				response.setStatus(409);
				return;
			}
		}

		def variables = params.variables;
		if (!variables) {
			variables = [:];
		}
		task.complete(variables);

		// OK
		WebResponse.create(context).with(response).setStatus(204);
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(context).with(response).sendError(ex);
	}
}();
