// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.bpm.ProcessInstance;
import api.http.WebRequest;
import api.http.WebResponse;
import api.util.JSON;

{->
	if (repositorySession.isAnonymous()) {
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
			"processInstances": []
		];

		def query = ProcessAPI.engine.runtimeService.createProcessInstanceQuery()
			.orderByProcessInstanceId().desc();
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
						query.variableValueLike(name, value);
					} else {
						query.variableValueEquals(name, value);
					}
				} else {
					query.variableValueEquals(name, value);
				}
			} else if (cnd.operator == "not") {
				query.variableValueNotEquals(name, value);
			} else if (cnd.operator == "greaterThan") {
				query.variableValueGreaterThan(name, value);
			} else if (cnd.operator == "greaterThanEqual") {
				query.variableValueGreaterThanOrEqual(name, value);
			} else if (cnd.operator == "lessThan") {
				query.variableValueLessThan(name, value);
			} else if (cnd.operator == "lessThanEqual") {
				query.variableValueLessThanOrEqual(name, value);
			}
		}

		for (; resp.processInstances.size() < limit; offset += limit) {
			def results = query.listPage(offset, offset + limit);

			for (item in results) {
				resp.nextOffset++;

				resp.processInstances.add(ProcessInstance.create(context).with(item).toObject());
				if (resp.processInstances.size() >= limit) {
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
