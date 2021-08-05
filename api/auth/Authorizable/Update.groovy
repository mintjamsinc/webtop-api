// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.cms.Item;
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
		def params = WebRequest.create(request).parseRequest();

		def authorizable;
		try {
			authorizable = Authorizable.create(context).findByName(repositorySession.userID);
		} catch (Throwable ex) {
			// Not Found
			WebResponse.create(response).setStatus(404);
			return;
		}
		if (!authorizable.exists()) {
			// Not Found
			WebResponse.create(response).setStatus(404);
			return;
		}

		authorizable.setAttribute("lastModified", now);
		authorizable.setAttribute("lastModifiedBy", repositorySession.userID);

		if (params.photo) {
			if (params.photo.uploadID) {
				def mu = MultipartUpload.create(context).resolve(params.photo.uploadID);
				if (!mu.exists()) {
					// Bad Request
					WebResponse.create(response).setStatus(400);
					return;
				}

				def mimeType = params.photo.mimeType;
				if (!mimeType || mimeType == "application/octet-stream") {
					def type = MimeTypeAPI.getMimeType(mu.filename);
					if (type) {
						mimeType = type;
					}
				}
				if (!mimeType) {
					mimeType = "application/octet-stream";
				}

				authorizable.setAttribute("photo", mu.file.bytes, mimeType);
			}

			if (params.photo.path) {
				def srcItem = Item.create(context).findByPath(params.photo.path);
				if (!srcItem.exists()) {
					// Bad Request
					WebResponse.create(response).setStatus(400);
					return;
				}

				authorizable.setAttribute("photo", srcItem.contentAsByteArray, srcItem.contentType);
			}
		}

		if (params.properties) {
			for (def prop in params.properties) {
				def key = prop.key;
				if (authorizable.contains(key)) {
					authorizable.removeAttribute(key);
				}
				if (prop.value == null) {
					continue;
				}

				def type = prop.type;
				def value = prop.value;
				def mask = !!prop.mask;
				if (type.equalsIgnoreCase("String")) {
					if (value instanceof Collection) {
						authorizable.setAttribute(key, value as String[], mask);
					} else {
						authorizable.setAttribute(key, value as String, mask);
					}
				} else if (type.equalsIgnoreCase("Binary")) {
					if (value instanceof Collection) {
						// Bad Request
						response.setStatus(400);
						return;
					} else {
						authorizable.setAttribute(key, value.decodeBase64());
					}
				} else if (type.equalsIgnoreCase("Long")) {
					if (value instanceof Collection) {
						authorizable.setAttribute(key, value as Long[]);
					} else {
						authorizable.setAttribute(key, value as Long);
					}
				} else if (type.equalsIgnoreCase("Double")) {
					if (value instanceof Collection) {
						authorizable.setAttribute(key, value as Double[]);
					} else {
						authorizable.setAttribute(key, value as Double);
					}
				} else if (type.equalsIgnoreCase("Decimal")) {
					if (value instanceof Collection) {
						authorizable.setAttribute(key, value as BigDecimal[]);
					} else {
						authorizable.setAttribute(key, value as BigDecimal);
					}
				} else if (type.equalsIgnoreCase("Date")) {
					if (value instanceof Collection) {
						def values = [];
						for (def v in value) {
							values.add(ISO8601.parse(v));
						}
						authorizable.setAttribute(key, values as Date[]);
					} else {
						authorizable.setAttribute(key, ISO8601.parse(value));
					}
				} else if (type.equalsIgnoreCase("Boolean")) {
					if (value instanceof Collection) {
						authorizable.setAttribute(key, value as Boolean[]);
					} else {
						authorizable.setAttribute(key, value as Boolean);
					}
				}
			}
		}

		// commit
		repositorySession.commit();

		// OK
		WebResponse.create(response)
			.setStatus(200)
			.setContentType("application/json");
		out.print(authorizable.toJson());
		return;
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(response).sendError(ex);
	} finally {
		try {
			repositorySession.rollback();
		} catch (Throwable ignore) {}
	}
}();
