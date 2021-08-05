// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.http.WebRequest;
import api.http.WebResponse;
import api.security.Authorizable;
import api.util.Text;
import groovy.json.JsonOutput;

{->
	if (repositorySession.isAnonymous()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	try {
		def params = WebRequest.create(request).parseRequest();

		if (params.identifiers != null) {
			def resp = [
				"nextOffset": -1,
				"authorizables": []
			];

			for (id in params.identifiers) {
				def e = Authorizable.create(context).findByName(id);
				if (e.exists()) {
					resp.authorizables.add(e.toObject());
				}
			}

			// OK
			WebResponse.create(response)
				.setStatus(200)
				.setContentType("application/json");
			out.print(JsonOutput.toJson(resp));
			return;
		}

		def offset = (params.offset > 0) ? params.offset : 0;
		def limit = (params.limit > 0) ? params.limit : 100;
		def resp = [
			"nextOffset": -1,
			"authorizables": []
		];

		def builder = repositorySession.userManager.createQueryBuilder();
		if (!params.selector) {
			builder.setSelector(new String[0]);
		} else {
			builder.setSelector(params.selector);
		}
		def conditions = [];
		if (params.q) {
			Text.split(params.q, " ").each { q ->
				conditions.add(builder.or(
					builder.like("@rep:principalName", "%" + q + "%"),
					builder.contains("@rep:fullName", q)
				));
			}
		}
		def condition = null;
		conditions.reverse().each { c ->
			if (!condition) {
				condition = c;
				return;
			}

			condition = builder.and(condition, c);
		}
		if (condition) {
			builder.setCondition(condition);
		}
		builder.setSortOrder("@rep:fullName", true, true);

		def i = builder.build().offset(offset).limit(limit + 1).execute();
		while (i.hasNext()) {
			if (resp.authorizables.size() < limit) {
				resp.authorizables.add(Authorizable.create(context).with(i.next()).toObject());
			} else {
				resp.nextOffset = offset + limit;
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
		return;
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(response).sendError(ex);
	}
}();
