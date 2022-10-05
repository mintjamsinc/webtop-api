// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.cms.Item;
import api.http.WebRequest;
import api.http.WebResponse;
import api.util.YAML;

{->
	if (repositorySession.isAnonymous()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	try {
		def params = WebRequest.create(context).with(request).parseRequest();
		def identifier = params.id?.trim();
		if (!identifier) {
			// Bad Request
			WebResponse.create(context).with(response).setStatus(400);
			return;
		}

		def item = Item.create(context).findByIdentifier(identifier);
		if (!item.exists()) {
			// Not Found
			WebResponse.create(context).with(response).setStatus(404);
			return;
		}
		if (!item.contains("web.template")) {
			// Not Found
			WebResponse.create(context).with(response).setStatus(404);
			return;
		}

		def tmpl = item.getTemplate(params.prefix?.trim(), params.suffix?.trim());
		if (!tmpl) {
			// Not Found
			WebResponse.create(context).with(response).setStatus(404);
			return;
		}

		tmpl.item.contentAsStream.withCloseable { stream ->
			WebResponse
				.create(context)
				.with(response)
				.setStatus(200)
				.enableContentCache()
				.setContentType(tmpl.item.contentType)
				.write(stream);
		}
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(context).with(response).sendError(ex);
	}
}();
