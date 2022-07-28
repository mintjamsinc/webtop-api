// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.cms.Exporter;
import api.http.WebRequest;
import api.http.WebResponse;

{->
	if (repositorySession.isAnonymous()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	def params = [
		"identifier": WebAPI.getParameter("identifier").defaultString(),
		"range": WebRequest.create(context).with(request).range
	];
	if (!params.identifier) {
		// Bad Request
		response.setStatus(400);
		return;
	}
	if (params.range && !params.range.isValid()) {
		// Bad Request
		response.setStatus(400);
		return;
	}

	def exp = Exporter.create(context);
	try {
		def identifier = params.identifier?.trim();
		if (!identifier) {
			// Bad Request
			WebResponse.create(context).with(response).setStatus(400);
			return;
		}

		exp.resolve(identifier);
		if (!exp.exists()) {
			// Not Found
			WebResponse.create(context).with(response).setStatus(404);
			return;
		}

		// OK
		WebResponse
			.create(response)
			.setStatus(200)
			.setAttachment(exp.status.filename)
			.disableContentCache()
			.setContentType(MimeTypeAPI.getMimeType(exp.status.filename))
			.setContentLength(exp.file.length())
			.setETag(exp.status.eTag)
			.writePartial(exp.file.newInputStream(), params.range);
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(context).with(response).sendError(ex);
	}
}();
