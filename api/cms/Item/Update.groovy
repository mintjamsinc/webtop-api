// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.cms.Item;
import api.cms.ItemHelper;
import api.cms.MultipartUpload;
import api.http.WebRequest;
import api.http.WebResponse;

{->
	if (repositorySession.isAnonymous()) {
		// Unauthorized
		response.setStatus(401);
		return;
	}

	def params = WebRequest.create(request).parseRequest();
	def identifier = params.id?.trim();
	if (!identifier) {
		// Bad Request
		response.setStatus(400);
		return;
	}

	try {
		def isAutoCheckedout = false;
		def item = Item.create(context).findByIdentifier(identifier);
		if (!item.exists()) {
			// Not Found
			WebResponse.create(response).setStatus(404);
			return;
		}

		if (params.checkout != null) {
			if (params.checkout instanceof Boolean && params.checkout) {
				if (item.isCollection()) {
					// Bad Request
					response.setStatus(400);
					return;
				}
				if (!item.isVersionControlled()) {
					// Conflict
					response.setStatus(409);
					return;
				}
				if (item.isLocked() && !item.holdsLock()) {
					// Locked
					response.setStatus(423);
					return;
				}
				if (!item.isLocked()) {
					item.lock(false, false);
				}
				if (!item.isCheckedOut()) {
					item.checkout();
				}
			}
		} else {
			if (params.lock != null) {
				if (params.lock instanceof Boolean && params.lock) {
					if (item.isLocked()) {
						// Locked
						response.setStatus(423);
						return;
					}

					item.lock();
				} else if (params.lock instanceof Map) {
					if (item.isLocked()) {
						// Locked
						response.setStatus(423);
						return;
					}

					def isDeep = !!params.lock.isDeep;
					def isSessionScoped = !!params.lock.isSessionScoped;
					item.lock(isDeep, isSessionScoped);
				}
			}
		}

		if (!item.isCollection()) {
			if (item.isVersionControlled()) {
				if (!item.isCheckedOut()) {
					item.checkout();
					isAutoCheckedout = true;
				}
			}

			item.allowAnyProperties();

			if (params.mimeType?.trim()) {
				item.setAttribute("jcr:mimeType", params.mimeType?.trim());
			}

			if (params.encoding?.trim()) {
				item.setAttribute("jcr:encoding", params.encoding?.trim());
			}

			if (params.uploadID != null) {
				def mu = MultipartUpload.create(context).resolve(params.uploadID);
				if (!mu.exists()) {
					// Bad Request
					response.setStatus(400);
					return;
				}

				ItemHelper.create(context).with(item).importContent(mu, item.contentType);
				mu.remove();
				item.setAttribute("jcr:lastModified", new Date());
			}

			if (params.properties != null) {
				ItemHelper.create(context).with(item).importAttributes(params.properties);
				item.setAttribute("jcr:lastModified", new Date());
			}
		}

		if (params.referenceable != null) {
			if (params.referenceable) {
				item.addReferenceable();
			} else {
				item.removeReferenceable();
			}
		}

		if (!item.isCollection()) {
			item.calculate();
		}

		repositorySession.commit();

		if (!item.isCollection()) {
			if (params.versionControl != null && params.versionControl) {
				if (item.isVersionControlled()) {
					// Bad Request
					response.setStatus(400);
					return;
				}

				if (params.versionControl) {
					item.addVersionControl();
					repositorySession.commit();
					item.checkin();
				}
			}

			if (isAutoCheckedout) {
				if (item.isVersionControlled()) {
					if (item.isCheckedOut()) {
						item.checkin();
					}
				}
			}
		}

		if (params.checkout != null) {
			if (params.checkout instanceof Boolean && !params.checkout) {
				if (item.isCollection()) {
					// Bad Request
					response.setStatus(400);
					return;
				}
				if (!item.isVersionControlled()) {
					// Conflict
					response.setStatus(409);
					return;
				}
				if (item.isLocked() && !item.holdsLock()) {
					// Locked
					response.setStatus(423);
					return;
				}
				if (item.isCheckedOut()) {
					item.checkin();
				}
				if (item.isLocked()) {
					item.unlock();
				}
			}
		} else {
			if (params.lock != null) {
				if (params.lock instanceof Boolean && !params.lock) {
					if (!item.isLocked()) {
						// Bad Request
						response.setStatus(400);
						return;
					}

					item.unlock();
				}
			}
		}

		// OK
		WebResponse.create(response)
			.setStatus(200)
			.setContentType("application/json");
		out.print(item.toJson());
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
