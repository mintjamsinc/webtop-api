// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.cms.Exporter;
import api.http.WebRequest;
import api.http.WebResponse;
import api.util.JSON;

{->
	if (repositorySession.isAnonymous()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	if ("POST".equalsIgnoreCase(request.getMethod())) {
		prepare();
		return;
	}

	if ("GET".equalsIgnoreCase(request.getMethod())) {
		execute();
		return;
	}

	// Method Not Allowed
	response.setStatus(405);
}();

def prepare() {
	try {
		def params = WebRequest.create(request).parseRequest();
		if (!params.paths || params.paths.isEmpty()) {
			// Bad Request
			WebResponse.create(response).setStatus(400);
			return;
		}

		def exp = Exporter.create(context).prepare(params.paths as String[], !!params.noMetadata as boolean);

		// Created
		WebResponse.create(response)
			.setStatus(201)
			.setContentType("application/json");
		out.print(exp.toJson());
	} catch (Throwable ex) {
		log.error(ex.message, ex);
	}
}

def execute() {
	try {
		def identifier = WebAPI.getParameter("identifier").defaultString().trim();
		if (!identifier) {
			// Bad Request
			response.setStatus(400);
			return;
		}

		def exp = Exporter.create(context).resolve(identifier);
		if (!exp.exists()) {
			// Bad Request
			response.setStatus(400);
			return;
		}

		WebResponse.create(response).setContentType("text/event-stream");
		exp.setStatusMonitor([
			setStatus: { status ->
				out.print("data: " + JSON.stringify(status) + "\n\n");
				out.flush();
			},
		]).execute();
	} catch (Throwable ex) {
		log.error(ex.message, ex);
	} finally {
		try {
			Thread.sleep(1000);
		} catch (Throwable ignore) {}
	}
}
