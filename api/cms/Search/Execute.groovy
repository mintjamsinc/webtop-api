// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.cms.Search;
import api.http.WebRequest;
import api.http.WebResponse;
import api.util.JSON;

{->
	if (!repositorySession.isAuthorized()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	try {
		def params = WebRequest.create(context).with(request).parseRequest();
		if (params.offset == null) {
			params.offset = 0;
		}
		if (params.limit == null) {
			params.limit = 100;
		}

		def resp = Search.create(context).execute(params).toObject();
		if (resp.hasMore) {
			resp.nextOffset = params.offset + params.limit;
		} else {
			resp.nextOffset = -1;
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
