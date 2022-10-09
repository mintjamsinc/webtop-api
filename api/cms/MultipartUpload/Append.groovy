// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.cms.MultipartUpload;
import api.http.WebRequest;
import api.http.WebResponse;

{->
	if (repositorySession.isAuthorized()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	try {
		def params = WebRequest.create(context).with(request).parseRequest();
		def uploadID = params.id?.trim();
		def mu = MultipartUpload.create(context).resolve(uploadID);
		mu.append(params.data?.trim());
		// OK
		WebResponse.create(context).with(response)
			.setStatus(200)
			.setContentType("application/json");
		out.print(mu.toJson());
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(context).with(response).sendError(ex);
	}
}();
