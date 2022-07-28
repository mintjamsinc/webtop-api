// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.cms.Item;
import api.http.WebRequest;
import api.http.WebResponse;

{->
	if (repositorySession.isAnonymous()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	def params = [
		"id": WebAPI.getParameter("id").defaultString(),
		"version": WebAPI.getParameter("version").defaultString(),
		"range": WebRequest.create(context).with(request).range,
		"isAttachment": WebAPI.getParameter("attachment").isSpecified()
	];
	if (!params.id) {
		// Bad Request
		response.setStatus(400);
		return;
	}
	if (params.range && !params.range.isValid()) {
		// Bad Request
		response.setStatus(400);
		return;
	}

	try {
		def item = Item.create(context).findByIdentifier(params.id);
		if (!item.exists()) {
			// Not Found
			WebResponse.create(context).with(response).setStatus(404);
			return;
		}

		if (params.version) {
			try {
				item = item.versionHistory.getVersion(params.version).frozen;
			} catch (Throwable ex) {
				// Not Found
				WebResponse.create(context).with(response).setStatus(404);
				return;
			}
		}

		def resp = WebResponse.create(context).with(response);
		if (params.isAttachment) {
			resp.setAttachment(item.name);
		}
		resp.enableContentCache()
			.setContentType(item.contentType)
			.setCharacterEncoding(item.contentEncoding)
			.setContentLength(item.contentLength)
			.setETag(item.lastModified.time as String)
			.writePartial(item.contentAsStream, params.range);
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(context).with(response).sendError(ex);
		return;
	}
}();
