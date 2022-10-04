// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import api.http.WebResponse;

{->
	try {
		session.invalidate();

		// OK
		WebResponse.create(context).with(response).setStatus(200);
	} catch (Throwable ex) {
		log.error(ex.message, ex);
		WebResponse.create(context).with(response).sendError(ex);
	}
}();
