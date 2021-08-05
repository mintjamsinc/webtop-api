let _baseUrl = window.location.href;
_baseUrl = _baseUrl.substring(0, _baseUrl.lastIndexOf('/')) + '/api';
let _axios;

export class UserClient {
	constructor({axios}) {
		_axios = axios;
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
			if (typeof instance.$options.onUpdated == 'function') {
				instance.$options.onUpdated(response.data);
			}
			return instance;
		});
	}

	remove() {
		return _axios.post(_baseUrl + '/user/Authorizable/Remove.groovy', {
			'id': this.$data.id
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
		params.id = instance.$data.id;
		return _axios.post(_baseUrl + '/user/Authorizable/ChangePassword.groovy', params
		).then(function() {
			return instance;
		});
	}
}

export class Group extends Authorizable {
	constructor(data, options) {
		super(data);
		this.$options = options;
		if (!this.$options) {
			this.$options = {};
		}
	}
}
