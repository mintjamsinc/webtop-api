// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import * as user from "./user";

let _baseUrl = window.location.href;
_baseUrl = _baseUrl.substring(0, _baseUrl.lastIndexOf('/')) + '/api';
let _axios;

export class AuthClient {
	constructor({axios}) {
		_axios = axios;
	}

	getUser(params) {
		if (!params) {
			params = {};
		}
		return _axios.post(_baseUrl + '/auth/GetUser.groovy', params
		).then(function(response) {
			return new User(response.data);
		});
	}
}

export class User extends user.User {
	changePassword(params) {
		let instance = this;
		if (!params) {
			params = {};
		}
		return _axios.post(_baseUrl + '/auth/Authorizable/ChangePassword.groovy', params
		).then(function() {
			return instance;
		});
	}
}
