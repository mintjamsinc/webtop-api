// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.cms.Item;
import api.cms.ItemHelper;
import api.cms.MultipartUpload;
import api.http.WebRequest;
import api.http.WebResponse;
import api.security.Authorizable;

{->
	if (repositorySession.isAnonymous()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	try {
		def now = new Date();
		def params = WebRequest.create(context).with(request).parseRequest();
		if (!params.id?.trim()) {
			// Bad Request
			response.setStatus(400);
			return;
		}

		repositorySession.userManager.registerIfNotExists();

		def authorizable = Authorizable.create(context).with(repositorySession.userPrincipal);
		def attributes = authorizable.attributes;

		if (params["mi:photo"] != null) {
			if (params["mi:photo"].uploadID) {
				def mu = MultipartUpload.create(context).resolve(params["mi:photo"].uploadID);
				if (!mu.exists()) {
					// Bad Request
					WebResponse.create(context).with(response).setStatus(400);
					return;
				}

				def mimeType = params["mi:photo"].mimeType;
				if (!mimeType || mimeType == "application/octet-stream") {
					def type = MimeTypeAPI.getMimeType(mu.filename);
					if (type) {
						mimeType = type;
					}
				}
				if (!mimeType) {
					mimeType = "application/octet-stream";
				}

				attributes.setAttribute("mi:photo", mu.file.bytes);
				attributes.setAttribute("mi:photoType", mimeType);
				attributes.setAttribute("jcr:lastModified", new Date());
			}

			if (params["mi:photo"].path) {
				def srcItem = Item.create(context).findByPath(params["mi:photo"].path);
				if (!srcItem.exists()) {
					// Bad Request
					WebResponse.create(context).with(response).setStatus(400);
					return;
				}

				attributes.setAttribute("mi:photo", srcItem.contentAsByteArray, srcItem.contentType);
				attributes.setAttribute("jcr:lastModified", new Date());
			}
		}

		if (params.properties != null) {
			def preferences = Item.create(context).findByPath("/home/" + repositorySession.userID + "/preferences");
			if (!preferences.exists()) {
				preferences.createNewFile();
			}

			def props = [:];
			if (params.properties instanceof List) {
				for (e in params.properties) {
					props[e.key] = e;
				}
			} else {
				props = params.properties;
			}

			if (props["mi:backgroundImage"] != null) {
				preferences.setAttribute("mi:backgroundImage", props["mi:backgroundImage"].value);
				preferences.setAttribute("jcr:lastModified", new Date());
			}
			if (props["mi:backgroundSize"] != null) {
				preferences.setAttribute("mi:backgroundSize", props["mi:backgroundSize"].value);
				preferences.setAttribute("jcr:lastModified", new Date());
			}

			props.remove("identifier");
			props.remove("isGroup");
			props.remove("mi:backgroundImage");
			props.remove("mi:backgroundSize");
			ItemHelper.create(context).with(attributes).importAttributes(props);
			attributes.setAttribute("jcr:lastModified", new Date());
		}

		// commit
		repositorySession.commit();

		// OK
		WebResponse.create(context).with(response)
			.setStatus(200)
			.setContentType("application/json");
		out.print(authorizable.toJson());
		return;
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(context).with(response).sendError(ex);
	} finally {
		try {
			repositorySession.rollback();
		} catch (Throwable ignore) {}
	}
}();
