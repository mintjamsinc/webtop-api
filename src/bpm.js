// Copyright (c) 2021 MintJams Inc. Licensed under MIT License.

let _baseUrl = window.location.href;
_baseUrl = _baseUrl.substring(0, _baseUrl.lastIndexOf('/')) + '/api';
let _axios;

export class BpmClient {
	constructor({axios}) {
		_axios = axios;
	}

	listProcessDefinitions(params, options) {
		if (!params) {
			params = {};
		}
		if (!options) {
			options = {};
		}
		if (!options.headers) {
			options.headers = {};
		}
		return _axios.post(_baseUrl + '/bpm/ListProcessDefinitions.groovy', params, {
			'headers': options.headers
		}).then(function(response) {
			if (typeof options.onResponse == 'function') {
				return options.onResponse(response);
			}
			return response;
		}).then(function(response) {
			let r = {
				'processDefinitions': []
			};
			for (let item of response.data.processDefinitions) {
				r.processDefinitions.push(new ProcessDefinition(item));
			}
			for (let k of Object.keys(response.data)) {
				if (k == 'processDefinitions') {
					continue;
				}
				r[k] = response.data[k];
			}
			return r;
		});
	}

	listProcessInstances(params, options) {
		if (!params) {
			params = {};
		}
		if (!options) {
			options = {};
		}
		if (!options.headers) {
			options.headers = {};
		}
		return _axios.post(_baseUrl + '/bpm/ListProcessInstances.groovy', params, {
			'headers': options.headers
		}).then(function(response) {
			if (typeof options.onResponse == 'function') {
				return options.onResponse(response);
			}
			return response;
		}).then(function(response) {
			let r = {
				'processInstances': []
			};
			for (let item of response.data.processInstances) {
				r.processInstances.push(new ProcessInstance(item));
			}
			for (let k of Object.keys(response.data)) {
				if (k == 'processInstances') {
					continue;
				}
				r[k] = response.data[k];
			}
			return r;
		});
	}

	listStartables(params, options) {
		if (!params) {
			params = {};
		}
		if (!options) {
			options = {};
		}
		if (!options.headers) {
			options.headers = {};
		}
		return _axios.post(_baseUrl + '/bpm/ListStartables.groovy', params, {
			'headers': options.headers
		}).then(function(response) {
			if (typeof options.onResponse == 'function') {
				return options.onResponse(response);
			}
			return response;
		}).then(function(response) {
			let r = {
				'startables': []
			};
			for (let startable of response.data.startables) {
				r.startables.push(new ProcessDefinition(startable));
			}
			for (let k of Object.keys(response.data)) {
				if (k == 'startables') {
					continue;
				}
				r[k] = response.data[k];
			}
			return r;
		});
	}

	listMyTasks(params, options) {
		if (!params) {
			params = {};
		}
		if (!options) {
			options = {};
		}
		if (!options.headers) {
			options.headers = {};
		}
		return _axios.post(_baseUrl + '/bpm/ListMyTasks.groovy', params, {
			'headers': options.headers
		}).then(function(response) {
			if (typeof options.onResponse == 'function') {
				return options.onResponse(response);
			}
			return response;
		}).then(function(response) {
			let r = {
				'tasks': []
			};
			for (let task of response.data.tasks) {
				r.tasks.push(new Task(task));
			}
			for (let k of Object.keys(response.data)) {
				if (k == 'tasks') {
					continue;
				}
				r[k] = response.data[k];
			}
			return r;
		});
	}

	searchTasks(params, options) {
		if (!params) {
			params = {};
		}
		if (!options) {
			options = {};
		}
		if (!options.headers) {
			options.headers = {};
		}
		return _axios.post(_baseUrl + '/bpm/SearchTasks.groovy', params, {
			'headers': options.headers
		}).then(function(response) {
			if (typeof options.onResponse == 'function') {
				return options.onResponse(response);
			}
			return response;
		}).then(function(response) {
			let r = {
				'tasks': []
			};
			for (let task of response.data.tasks) {
				r.tasks.push(new Task(task));
			}
			for (let k of Object.keys(response.data)) {
				if (k == 'tasks') {
					continue;
				}
				r[k] = response.data[k];
			}
			return r;
		});
	}

	newProcessDefinition(data) {
		return new ProcessDefinition(data);
	}

	newProcessInstance(data) {
		return new ProcessInstance(data);
	}

	newDeployment(data) {
		return new Deployment(data);
	}

	newTask(data) {
		return new Task(data);
	}

	getProcessDefinition(params) {
		if (!params) {
			params = {};
		}
		return _axios.post(_baseUrl + '/bpm/GetProcessDefinition.groovy', params).then(function(response) {
			return new ProcessDefinition(response.data);
		});
	}

	getProcessInstance(params) {
		if (!params) {
			params = {};
		}
		return _axios.post(_baseUrl + '/bpm/GetProcessInstance.groovy', params).then(function(response) {
			return new ProcessInstance(response.data);
		});
	}

	getDeployment(params) {
		if (!params) {
			params = {};
		}
		return _axios.post(_baseUrl + '/bpm/GetDeployment.groovy', params).then(function(response) {
			return new Deployment(response.data);
		});
	}

	getTask(params) {
		if (!params) {
			params = {};
		}
		return _axios.post(_baseUrl + '/bpm/GetTask.groovy', params).then(function(response) {
			return new Task(response.data);
		});
	}
}

export class ProcessDefinition {
	constructor(data) {
		this.$data = data;
		for (let k of Object.keys(this.$data)) {
			if (['deployment'].indexOf(k) != -1) {
				continue;
			}

			Object.defineProperty(this, k, {
				'get': function() {
					return this.$data[k];
				}
			});
		}
		this.$deployment = new Deployment(this.$data.deployment);
	}

	start(params) {
		if (!params) {
			params = {};
		}
		params.id = this.$data.id;
		return _axios.post(_baseUrl + '/bpm/ProcessDefinition/Start.groovy', params
		).then(function(response) {
			return new ProcessInstance(response.data);
		});
	}

	activate() {
		return _axios.post(_baseUrl + '/bpm/ProcessDefinition/Activate.groovy', {
			'id': this.$data.id,
		}).then(function(response) {
			return new ProcessInstance(response.data);
		});
	}

	suspend() {
		return _axios.post(_baseUrl + '/bpm/ProcessDefinition/Suspend.groovy', {
			'id': this.$data.id,
		}).then(function(response) {
			return new ProcessInstance(response.data);
		});
	}

	remove() {
		return _axios.post(_baseUrl + '/bpm/ProcessDefinition/Remove.groovy', {
			'id': this.$data.id,
		});
	}

	get deployment() {
		return this.$deployment;
	}

	getProcessModel() {
		return _axios.post(_baseUrl + '/bpm/ProcessDefinition/GetProcessModel.groovy', {
			'id': this.$data.id,
		}).then(function(response) {
			return response.data;
		});
	}
}

export class ProcessInstance {
	constructor(data) {
		this.$data = data;
		for (let k of Object.keys(this.$data)) {
			if (['processDefinition'].indexOf(k) != -1) {
				continue;
			}

			Object.defineProperty(this, k, {
				'get': function() {
					return this.$data[k];
				}
			});
		}
		this.$processDefinition = new ProcessDefinition(this.$data.processDefinition);
	}

	activate() {
		return _axios.post(_baseUrl + '/bpm/ProcessInstance/Activate.groovy', {
			'id': this.$data.id,
		}).then(function(response) {
			return new ProcessInstance(response.data);
		});
	}

	suspend() {
		return _axios.post(_baseUrl + '/bpm/ProcessInstance/Suspend.groovy', {
			'id': this.$data.id,
		}).then(function(response) {
			return new ProcessInstance(response.data);
		});
	}

	remove() {
		return _axios.post(_baseUrl + '/bpm/ProcessInstance/Remove.groovy', {
			'id': this.$data.id,
		});
	}

	get processDefinition() {
		return this.$processDefinition;
	}

	getActivityInstance() {
		return _axios.post(_baseUrl + '/bpm/ProcessInstance/GetActivityInstance.groovy', {
			'id': this.$data.id,
		}).then(function(response) {
			return new ActivityInstance(response.data);
		});
	}

	getVariables() {
		return _axios.post(_baseUrl + '/bpm/ProcessInstance/GetVariables.groovy', {
			'id': this.$data.id,
		}).then(function(response) {
			return response.data;
		});
	}

	setVariables(variables) {
		let params = {
			'id': this.$data.id,
			'variables': variables,
		};
		return _axios.post(_baseUrl + '/bpm/ProcessInstance/SetVariables.groovy', params).then(function(response) {
			return new ProcessInstance(response.data);
		});
	}
}

export class ActivityInstance {
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
}

export class Deployment {
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
}

export class Task {
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
		params.taskId = this.$data.id;
		return _axios.post(_baseUrl + '/bpm/Task/Update.groovy', params
		).then(function(response) {
			instance.$data = response.data;
			return instance;
		});
	}

	complete(params) {
		let instance = this;
		if (!params) {
			params = {};
		}
		params.taskId = this.$data.id;
		return _axios.post(_baseUrl + '/bpm/Task/Complete.groovy', params
		).then(function(response) {
			instance.$data = response.data;
			return instance;
		});
	}
}
