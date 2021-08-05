// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.cms.Importer;
import api.cms.MultipartUpload;
import api.http.WebRequest;
import api.http.WebResponse;

{->
	if (repositorySession.isAnonymous()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	def imp = Importer.create(context);
	def mu = MultipartUpload.create(context);
	try {
		def params = WebRequest.create(request).parseRequest();
		def path = params.path?.trim();
		def uploadID = params.uploadID?.trim();
		if (!path || !uploadID) {
			// Bad Request
			WebResponse.create(response).setStatus(400);
			return;
		}

		mu.resolve(uploadID);
		if (!mu.exists()) {
			// Bad Request
			WebResponse.create(response).setStatus(400);
			return;
		}

		imp.execute(path, mu.file);

		// Created
		WebResponse.create(response)
			.setStatus(201)
			.setContentType("application/json");
		out.print(imp.toJson());
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(response).sendError(ex);
	}
}();
