/* Copyright (c) 2021 MintJams Inc. Licensed under MIT License. */

import * as util from "./util";
import md5 from 'crypto-js/md5';
import sha256 from 'crypto-js/sha256';
import { v4 as uuidv4 } from 'uuid';

let _baseUrl = util.Env.getBaseUrl();
let _axios;

export class UserClient {
	constructor({axios}) {
		_axios = axios;

		this.$everyone = this.newAuthorizable({
			'id': 'everyone',
			'isGroup': true,
		});
	}

	get everyone() {
		return this.$everyone;
	}

	createNewAuthorizable(params) {
		let instance = this;
		if (!params) {
			params = {};
		}
		return _axios.post(_baseUrl + '/user/CreateNewAuthorizable.groovy', params
		).then(function(response) {
			return instance.newAuthorizable(response.data);
		});
	}

	getAuthorizable(params) {
		let instance = this;
		if (!params) {
			params = {};
		}
		return _axios.post(_baseUrl + '/user/GetAuthorizable.groovy', params
		).then(function(response) {
			return instance.newAuthorizable(response.data);
		});
	}

	listAuthorizables(params, options) {
		let instance = this;
		if (!params) {
			params = {};
		}
		if (!options) {
			options = {};
		}
		if (!options.headers) {
			options.headers = {};
		}
		return _axios.post(_baseUrl + '/user/ListAuthorizables.groovy', params, {
			'headers': options.headers
		}).then(function(response) {
			if (typeof options.onResponse == 'function') {
				return options.onResponse(response);
			}
			return response;
		}).then(function(response) {
			let r = {
				'authorizables': []
			};
			for (let a of response.data.authorizables) {
				r.authorizables.push(instance.newAuthorizable(a));
			}
			for (let k of Object.keys(response.data)) {
				if (k == 'authorizables') {
					continue;
				}
				r[k] = response.data[k];
			}
			return r;
		});
	}

	newAuthorizable(data) {
		if (!data || data.isGroup == undefined) {
			return undefined;
		}
		return data.isGroup ? this.newGroup(data) : this.newUser(data);
	}

	newUser(data) {
		return new User(data);
	}

	newGroup(data) {
		return new Group(data);
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
		params.id = instance.$data.id;
		return _axios.post(_baseUrl + '/user/Authorizable/Update.groovy', params).then(function(response) {
			instance.$data = response.data;
			if (typeof instance.onUpdated == 'function') {
				instance.onUpdated(instance);
			}
			return instance;
		});
	}

	remove() {
		return _axios.post(_baseUrl + '/user/Authorizable/Remove.groovy', {
			'id': this.$data.id
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
		if (p.isMasked) {
			if (!Array.isArray(p.value)) {
				return window.Webtop.authClient.unmask(p.value);
			} else {
				let l = [];
				for (let v of p.value) {
					l.push(window.Webtop.authClient.unmask(v));
				}
				return l;
			}
		}
		return p.value;
	}

	get identifier() {
		let instance = this;
		return instance.getProperty('identifier');
	}

	get isGroup() {
		let instance = this;
		return instance.getProperty('isGroup', false);
	}

	get fullName() {
		let instance = this;
		if (instance.getProperty('mi:fullName')) {
			return instance.getProperty('mi:fullName');
		}

		return '';
	}

	get photoURL() {
		let instance = this;
		if (instance.getProperty('mi:gravatarEmail')) {
			let hash = md5(instance.getProperty('mi:gravatarEmail')).toString();
			return 'https://www.gravatar.com/avatar/' + encodeURIComponent(hash) + '?s=288';
		}

		if (instance.hasProperty('mi:photo')) {
			let modified;
			try {
				modified = new Date(instance.$data.lastModificationTime);
			} catch (ignore) {
				modified = new Date();
			}
			return _baseUrl + '/user/Authorizable/Photo.groovy?id=' + encodeURIComponent(instance.$data.id) + '&modified=' + modified.getTime();
		}

		return '';
	}
}

export class User extends Authorizable {
	constructor(data) {
		super(data);
	}

	generateSecret() {
		let instance = this;
		return (sha256(instance.id + ':' + Date.now()).toString() + uuidv4().split('-').join('')).toLowerCase();
	}
}

export class Group extends Authorizable {
	constructor(data) {
		super(data);
	}
}
