// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.bpm.Task;
import api.http.WebRequest;
import api.http.WebResponse;
import api.util.ISO8601;
import groovy.json.JsonOutput;

{->
	if (repositorySession.isAnonymous()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	try {
		def params = WebRequest.create(request).parseRequest();
		def offset = (params.offset > 0) ? params.offset : 0;
		def limit = (params.limit > 0) ? params.limit : 100;
		def resp = [
			"nextOffset": offset,
			"tasks": []
		];

		def taskService = ProcessAPI.engine.taskService;
		def query = taskService.createTaskQuery();
		if (params.businessKey?.trim()) {
			def businessKey = params.businessKey?.trim();
			if (businessKey.startsWith("%") || businessKey.endsWith("%")) {
				query.processInstanceBusinessKeyLike(businessKey);
			} else {
				query.processInstanceBusinessKey(businessKey);
			}
		}
		if (params.processDefinitionId?.trim()) {
			def processDefinitionId = params.processDefinitionId?.trim();
			query.processDefinitionId(processDefinitionId);
		}
		for (cnd in params.variables) {
			def name = cnd.name?.trim();
			def value = cnd.value;
			if (value instanceof String) {
				value = value?.trim();
			}
			if (cnd.operator == "equals") {
				if (value instanceof String) {
					if (value.startsWith("%") || value.endsWith("%")) {
						query.processVariableValueLike(name, value);
					} else {
						query.processVariableValueEquals(name, value);
					}
				} else {
					query.processVariableValueEquals(name, value);
				}
			} else if (cnd.operator == "not") {
				query.processVariableValueNotEquals(name, value);
			} else if (cnd.operator == "greaterThan") {
				query.processVariableValueGreaterThan(name, value);
			} else if (cnd.operator == "greaterThanEqual") {
				query.processVariableValueGreaterThanOrEqual(name, value);
			} else if (cnd.operator == "lessThan") {
				query.processVariableValueLessThan(name, value);
			} else if (cnd.operator == "lessThanEqual") {
				query.processVariableValueLessThanOrEqual(name, value);
			}
		}
		if (params.assignee?.trim()) {
			def assignee = params.assignee?.trim();
			query.taskAssignee(assignee);
		}
		if (params.candidateUser?.trim()) {
			def candidateUser = params.candidateUser?.trim();
			query.taskCandidateUser(candidateUser);
		}
		if (params.candidateGroup?.trim()) {
			def candidateGroup = params.candidateGroup?.trim();
			query.taskCandidateGroup(candidateGroup);
		}
		if (params.candidateUser?.trim() || params.candidateGroup?.trim()) {
			query.includeAssignedTasks();
		}
		if (params.dueDateAfter?.trim()) {
			def dueDateAfter = ISO8601.parseDate(params.dueDateAfter?.trim());
			query.dueAfter(dueDateAfter);
		}
		if (params.dueDateBefore?.trim()) {
			def dueDateBefore = ISO8601.parseDate(params.dueDateBefore?.trim());
			query.dueBefore(dueDateBefore);
		}
		if (params.followUpDateAfter?.trim()) {
			def followUpDateAfter = ISO8601.parseDate(params.followUpDateAfter?.trim());
			query.followUpAfter(followUpDateAfter);
		}
		if (params.followUpDateBefore?.trim()) {
			def followUpDateBefore = ISO8601.parseDate(params.followUpDateBefore?.trim());
			query.followUpBefore(followUpDateBefore);
		}
		query.initializeFormKeys()
			.orderByDueDate().desc()
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
		WebResponse.create(response)
			.setStatus(200)
			.setContentType("application/json");
		out.print(JsonOutput.toJson(resp));
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(response).sendError(ex);
	}
}();
