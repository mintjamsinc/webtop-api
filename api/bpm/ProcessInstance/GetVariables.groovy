// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.bpm.ProcessInstance;
import api.bpm.ProcessInstanceHelper;
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
		def params = WebRequest.create(request).parseRequest();
		def processInstanceId = params.id?.trim();
		def pi = ProcessInstance.create(context).findByIdentifier(processInstanceId);
		if (!pi.exists()) {
			// Not Found
			response.setStatus(404);
			return;
		}

		// OK
		WebResponse.create(response)
			.setStatus(200)
			.setContentType("application/json");
		out.print(JSON.stringify(ProcessInstanceHelper.create(context).with(pi).exportVariables()));
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(response).sendError(ex);
	}
}();
