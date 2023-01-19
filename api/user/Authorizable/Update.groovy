// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.cms.Item;
import api.cms.ItemHelper;
import api.cms.MultipartUpload;
import api.http.WebRequest;
import api.http.WebResponse;
import api.security.Authorizable;

{->
	if (!repositorySession.isAuthorized()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	try {
		def now = new Date();
		def params = WebRequest.create(context).with(request).parseRequest();
		def id = params.id?.trim();
		if (!id) {
			// Bad Request
			response.setStatus(400);
			return;
		}
		if (params.isGroup == null) {
			// Bad Request
			response.setStatus(400);
			return;
		}

		def principal;
		if (params.isGroup) {
			principal = repositorySession.principalProvider.getGroupPrincipal(id);
		} else {
			principal = repositorySession.principalProvider.getUserPrincipal(id);
		}

		repositorySession.userManager.registerIfNotExists(principal);

		def authorizable = Authorizable.create(context).with(principal);
		def attributes = authorizable.attributes;
		def preferences = Item.create(context).findByPath("/home/" + principal.name + "/preferences");
		if (!preferences.exists()) {
			preferences.createNewFile();
		}

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

		if (params["disable"] != null) {
			if (params["disable"]) {
				attributes.setAttribute("disabled", true);
				attributes.setAttribute("disabledReason", params["disabledReason"]?.trim());
			} else {
				attributes.removeAttribute("disabled");
				attributes.removeAttribute("disabledReason");
			}
			attributes.setAttribute("jcr:lastModified", new Date());
		}

		if (params.properties != null) {
			def protectedKeys = [
				"identifier",
				"isGroup",
				"disabled",
				"disabledReason"
			];
			def preferencesKeys = [
				"mi:backgroundImage",
				"mi:backgroundSize"
			];

			ItemHelper.create(context).with(preferences).importAttributes(params.properties.findAll {
				return preferencesKeys.contains(it.key);
			});
			preferences.setAttribute("jcr:lastModified", new Date());

			ItemHelper.create(context).with(attributes).importAttributes(params.properties.findAll {
				if (protectedKeys.contains(it.key) || preferencesKeys.contains(it.key)) {
					return false;
				}
				return true;
			});
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
