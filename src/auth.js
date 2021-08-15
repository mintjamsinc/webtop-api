// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

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

export class Authorizable {
	constructor(data) {
		this.$data = data;
		if (!this.$data.properties) {
			this.$data.properties = {};
		}
		for (let k of Object.keys(this.$data)) {
			Object.defineProperty(this, k, {
				'get': function() {
					return this.$data[k];
				}
			});
		}
	}

	update(params) {
		let instance = this;
		if (!params) {
			params = {};
		}
		return _axios.post(_baseUrl + '/auth/Authorizable/Update.groovy', params).then(function(response) {
			instance.$data = response.data;
			if (typeof instance.$options.onUpdated == 'function') {
				instance.$options.onUpdated(response.data);
			}
			return instance;
		});
	}

	get photoURL() {
		let instance = this;
		if (!instance.$data.properties['photo']) {
			return '';
		}

		let modified;
		try {
			modified = new Date(instance.$data.properties['lastModified'].value);
		} catch (ignore) {
			modified = new Date();
		}
		return _baseUrl + '/user/Authorizable/Photo.groovy?id=' + encodeURIComponent(instance.$data.id) + '&modified=' + modified.getTime();
	}
}

export class User extends Authorizable {
	constructor(data, options) {
		super(data);
		this.$options = options;
		if (!this.$options) {
			this.$options = {};
		}
	}

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
