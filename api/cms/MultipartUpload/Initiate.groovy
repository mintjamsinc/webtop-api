// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.cms.MultipartUpload;
import api.http.WebResponse;

{->
	if (!repositorySession.isAuthorized()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	def mu = MultipartUpload.create(context);
	try {
		mu.initiate();
		// OK
		WebResponse.create(context).with(response)
			.setStatus(201)
			.setContentType("application/json");
		out.print(mu.toJson());
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(context).with(response).sendError(ex);

		try {
			mu.deleteFile();
		} catch (Throwable ignore) {}
	}
}();
