// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.cms.Importer;
import api.cms.MultipartUpload;
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
		def params = WebRequest.create(context).with(request).parseRequest();
		if (!params.path?.trim() || !params.uploadID?.trim()) {
			// Bad Request
			response.setStatus(400);
			return;
		}

		def mu = MultipartUpload.create(context).resolve(params.uploadID);
		if (!mu.exists()) {
			// Bad Request
			response.setStatus(400);
			return;
		}

		def imp = Importer.create(context).prepare(params.path, mu.file);

		// Created
		WebResponse.create(context).with(response)
			.setStatus(201)
			.setContentType("application/json");
		out.print(imp.toJson());
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

		def imp = Importer.create(context).resolve(identifier);
		if (!imp.exists()) {
			// Bad Request
			response.setStatus(400);
			return;
		}

		WebResponse.create(context).with(response).setContentType("text/event-stream").setCharacterEncoding("UTF-8");
		imp.setStatusMonitor([
			setStatus: { status ->
				out.print("data: " + JSON.stringify(status) + "\n\n");
				out.flush();
			},
		]).execute();
	} catch (Throwable ex) {
		log.error(ex.message, ex);
	} finally {
		try {
			repositorySession.rollback();
		} catch (Throwable ignore) {}
		try {
			Thread.sleep(1000);
		} catch (Throwable ignore) {}
	}
}
