// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.bpm.Task;
import api.util.ISO8601;
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

		if (params.assignee) {
//			task.setAssignee(params.assignee);
			ProcessAPI.engine.taskService.setAssignee(params.taskId, params.assignee);
		}
		if (params.dueDate) {
			task.setDueDate(ISO8601.parseDate(params.dueDate));
		}
		if (params.followUpDate) {
			task.setFollowUpDate(ISO8601.parseDate(params.followUpDate));
		}
		if (params.variables) {
			params.variables.each { k, v ->
				if (k.toLowerCase().endsWith("@remove")) {
					task.removeVariable(k);
					return;
				}

				task.setVariable(k, v);
			}
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
