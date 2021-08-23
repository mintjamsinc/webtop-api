// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

import md5 from 'crypto-js/md5';

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

	hasProperty(key) {
		let instance = this;
		return !!instance.$data.properties[key];
	}

	getProperty(key, defaultValue) {
		let instance = this;
		let p = instance.$data.properties[key];
		if (!p) {
			return defaultValue;
		}

		if (p.value == undefined) {
			return defaultValue;
		}
		return p.value;
	}

	get photoURL() {
		let instance = this;
		if (instance.getProperty('gravatarEmail')) {
			let hash = md5(instance.getProperty('gravatarEmail'));
			return 'https://www.gravatar.com/avatar/' + encodeURIComponent(hash) + '?s=288';
		}

		if (instance.hasProperty('photo')) {
			let modified;
			try {
				modified = new Date(instance.getProperty('lastModified'));
			} catch (ignore) {
				modified = new Date();
			}
			return _baseUrl + '/user/Authorizable/Photo.groovy?id=' + encodeURIComponent(instance.$data.id) + '&modified=' + modified.getTime();
		}

		return '';
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
