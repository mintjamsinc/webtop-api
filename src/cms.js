/* Copyright (c) 2021 MintJams Inc. Licensed under MIT License. */

let _baseUrl = window.location.href;
_baseUrl = _baseUrl.substring(0, _baseUrl.lastIndexOf('/')) + '/api';
let _axios;

export class CmsClient {
	constructor({axios}) {
		_axios = axios;
	}

	createNewFolder(params) {
		if (!params) {
			params = {};
		}
		return _axios.post(_baseUrl + '/cms/CreateNewFolder.groovy', params
		).then(function(response) {
			return new Item(response.data);
		});
	}

	createNewFile(params) {
		if (!params) {
			params = {};
		}
		return _axios.post(_baseUrl + '/cms/CreateNewFile.groovy', params
		).then(function(response) {
			return new Item(response.data);
		});
	}

	getItem(params) {
		if (!params) {
			params = {};
		}
		return _axios.post(_baseUrl + '/cms/GetItem.groovy', params
		).then(function(response) {
			return new Item(response.data);
		});
	}

	listFacetDefinitions(params, options) {
		if (!params) {
			params = {};
		}
		if (!options) {
			options = {};
		}
		if (!options.headers) {
			options.headers = {};
		}
		return _axios.post(_baseUrl + '/cms/ListFacetDefinitions.groovy', params, {
			'headers': options.headers
		}).then(function(response) {
			if (typeof options.onResponse == 'function') {
				return options.onResponse(response);
			}
			return response;
		}).then(function(response) {
			let r = {
				'facetDefinitions': []
			};
			for (let item of response.data.facetDefinitions) {
				r.facetDefinitions.push(new FacetDefinition(item));
			}
			for (let k of Object.keys(response.data)) {
				if (k == 'facetDefinitions') {
					continue;
				}
				r[k] = response.data[k];
			}
			return r;
		});
	}

	listCurrencies(params, options) {
		if (!params) {
			params = {};
		}
		if (!options) {
			options = {};
		}
		if (!options.headers) {
			options.headers = {};
		}
		return _axios.post(_baseUrl + '/cms/ListCurrencies.groovy', params, {
			'headers': options.headers
		}).then(function(response) {
			if (typeof options.onResponse == 'function') {
				return options.onResponse(response);
			}
			return response;
		}).then(function(response) {
			let r = {
				'currencies': []
			};
			for (let item of response.data.currencies) {
				r.currencies.push(new Currency(item));
			}
			for (let k of Object.keys(response.data)) {
				if (k == 'currencies') {
					continue;
				}
				r[k] = response.data[k];
			}
			return r;
		});
	}

	createMultipartUpload(params) {
		if (!params) {
			params = {};
		}
		return new MultipartUpload(params);
	}

	createSearch(statement, language) {
		if (!language) {
			language = 'XPath';
		}
		return new Search(statement, language);
	}

	createCrawler(statement, language) {
		if (!language) {
			language = 'XPath';
		}
		return new Crawler(statement, language);
	}

	createExporter(params) {
		if (!params) {
			params = {};
		}
		return new Exporter(params);
	}

	createImporter(params) {
		if (!params) {
			params = {};
		}
		return new Importer(params);
	}

	newItem(data) {
		return new Item(data);
	}

	newFacetDefinition(data) {
		return new FacetDefinition(data);
	}
}

export class Item {
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

	get thumbnailURL() {
		return _baseUrl + '/cms/Item/Thumbnail.groovy?id=' + encodeURIComponent(this.$data.id) + '&modified=' + encodeURIComponent('' + new Date(this.$data.lastModificationTime).getTime());
	}

	get downloadURL() {
		let url = _baseUrl + '/cms/Item/Download.groovy?id=' + encodeURIComponent(this.$data.id);
		if (!this.$data.isFrozen) {
			url += '&modified=' + encodeURIComponent('' + new Date(this.$data.lastModificationTime).getTime());
		}
		return url;
	}

	get previewURL() {
		return new PreviewURLBuilder(this).url;
	}

	createPreviewURLBuilder(options) {
		return new PreviewURLBuilder(this, options);
	}

	download(options) {
		if (!options) {
			options = {};
		}

		let url = _baseUrl + '/cms/Item/Download.groovy?id=' + encodeURIComponent(this.$data.id);
		if (options.version) {
			url += '&version=' + encodeURIComponent(options.version);
		} else {
			url += '&modified=' + encodeURIComponent('' + new Date(this.$data.lastModificationTime).getTime());
		}
		url += '&attachment';
		window.location.href = url;
	}

	rename(newName) {
		return _axios.post(_baseUrl + '/cms/Item/Rename.groovy', {
			'id': this.$data.id,
			'newName': newName
		}).then(function(response) {
			return new Item(response.data);
		});
	}

	update(params) {
		params.id = this.$data.id;
		return _axios.post(_baseUrl + '/cms/Item/Update.groovy', params).then(function(response) {
			return new Item(response.data);
		});
	}

	remove() {
		return _axios.post(_baseUrl + '/cms/Item/Remove.groovy', {
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

	duplicate() {
		return _axios.post(_baseUrl + '/cms/Item/Duplicate.groovy', {
			'id': this.$data.id
		}).then(function(response) {
			return new Item(response.data);
		});
	}

	addFolder(params) {
		if (!params) {
			params = {};
		}
		params.id = this.$data.id;
		return _axios.post(_baseUrl + '/cms/Item/AddFolder.groovy', params).then(function(response) {
			return new Item(response.data);
		});
	}

	addFile(params) {
		if (!params) {
			params = {};
		}
		params.id = this.$data.id;
		return _axios.post(_baseUrl + '/cms/Item/AddFile.groovy', params
		).then(function(response) {
			return new Item(response.data);
		});
	}

	copyTo(destAbsPath) {
		return _axios.post(_baseUrl + '/cms/Item/CopyTo.groovy', {
			'id': this.$data.id,
			'path': destAbsPath
		}).then(function(response) {
			return new Item(response.data);
		});
	}

	listItems(params, options) {
		if (!params) {
			params = {};
		}
		if (!options) {
			options = {};
		}
		if (!options.headers) {
			options.headers = {};
		}
		params.id = this.$data.id;
		return _axios.post(_baseUrl + '/cms/Item/ListItems.groovy', params, {
			'headers': options.headers
		}).then(function(response) {
			if (typeof options.onResponse == 'function') {
				return options.onResponse(response);
			}
			return response;
		}).then(function(response) {
			let r = {
				'items': []
			};
			for (let item of response.data.items) {
				r.items.push(new Item(item));
			}
			for (let k of Object.keys(response.data)) {
				if (k == 'items') {
					continue;
				}
				r[k] = response.data[k];
			}
			return r;
		});
	}

	get versionHistory() {
		if (!this.isVersionControlled) {
			return undefined;
		}
		return new VersionHistory(this);
	}

	getAccessControlList(params, options) {
		if (!params) {
			params = {};
		}
		if (!options) {
			options = {};
		}
		if (!options.headers) {
			options.headers = {};
		}
		params.id = this.$data.id;
		return _axios.post(_baseUrl + '/cms/Item/GetAccessControlList.groovy', params, {
			'headers': options.headers
		}).then(function(response) {
			if (typeof options.onResponse == 'function') {
				return options.onResponse(response);
			}
			return response;
		}).then(function(response) {
			let r = {};
			for (let k of Object.keys(response.data)) {
				r[k] = response.data[k];
			}
			return r;
		});
	}

	setAccessControlList(params) {
		params.id = this.$data.id;
		return _axios.post(_baseUrl + '/cms/Item/SetAccessControlList.groovy', params);
	}

	getTemplate(params) {
		if (!this.$data.properties['web.template']) {
			return Promise.resolve(/*undefined*/);
		}

		if (!params) {
			params = {};
		}
		if (!params.prefix) {
			params.prefix = "GET";
		}
		params.id = this.$data.id;
		return _axios.post(_baseUrl + '/cms/Item/GetTemplate.groovy', params).then(function(response) {
			return response.data;
		});
	}
}

export class PreviewURLBuilder {
	constructor(item, options) {
		if (!options) {
			options = {};
		}
		if (!options.urlPrefix) {
			options.urlPrefix = '/bin/cms.cgi';
		}
		this.$options = options;
		this.$item = item;
	}

	get url() {
		let version = this.$item.version;
		if (!this.$item.isFrozen) {
			if (this.$item.isCheckedOut && this.$item.isLocked && (this.$item.lockedBy == window.Webtop.user.id)) {
				version = undefined;
			}
		}
		let modified = new Date(this.$item.lastModificationTime).getTime();

		{
			let path = this.$item.path;
			// /WEB-INF/themes/default/resources/index.html --> /index.html
			let prefix = path.match('^/WEB-INF/themes/.+/resources/');
			if (prefix) {
				prefix = prefix[0];
				if (path == prefix) {
					path = '';
				} else if (path.startsWith(prefix)) {
					path = path.substring(prefix.length);
				}

				let pathnames = path.split('/');
				for (let i = 0; i < pathnames.length; i++) {
					pathnames[i] = encodeURIComponent(pathnames[i]);
				}

				return this.$options.urlPrefix + '/' + pathnames.join('/')
					+ (this.$options.suffix ? this.$options.suffix : '')
					+ '?mx.preview'
					+ (modified ? ('&mx.modified=' + encodeURIComponent(modified)) : '')
					+ (version ? ('&mx.version=' + encodeURIComponent(version)) : '');
			}
		}

		{
			let path = this.$item.path;
			if (path.startsWith('/content/')) {
				// /content/... --> /...
				path = path.substring('/content/'.length);

				let pathnames = path.split('/');
				for (let i = 0; i < pathnames.length; i++) {
					pathnames[i] = encodeURIComponent(pathnames[i]);
				}

				return this.$options.urlPrefix + '/' + pathnames.join('/')
					+ (this.$options.suffix ? this.$options.suffix : '')
					+ '?mx.preview'
					+ (modified ? ('&mx.modified=' + encodeURIComponent(modified)) : '')
					+ (version ? ('&mx.version=' + encodeURIComponent(version)) : '');
			}
		}

		return this.$item.downloadURL;
	}
}

export class VersionHistory {
	constructor(item) {
		this.$item = item;
	}

	listAllVersions(params, options) {
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
		params.id = this.$item.id;
		return _axios.post(_baseUrl + '/cms/Item/VersionHistory/ListAllVersions.groovy', params, {
			'headers': options.headers
		}).then(function(response) {
			if (typeof options.onResponse == 'function') {
				return options.onResponse(response);
			}
			return response;
		}).then(function(response) {
			let r = {
				'versions': []
			};
			for (let data of response.data.versions) {
				r.versions.push(new Version(data, instance));
			}
			for (let k of Object.keys(response.data)) {
				if (k == 'versions') {
					continue;
				}
				r[k] = response.data[k];
			}
			return r;
		});
	}

	newVersion(data) {
		return new Version(data, this.$item);
	}
}

export class Version {
	constructor(data, item) {
		this.$data = data;
		this.$item = item;
		for (let k of Object.keys(this.$data)) {
			Object.defineProperty(this, k, {
				'get': function() {
					return this.$data[k];
				}
			});
		}
	}

	download() {
		this.$item.download({
			'version': this.$data.name
		});
	}

	update(params) {
		let instance = this;
		params.id = instance.$item.id;
		params.version = instance.$data.name;
		return _axios.post(_baseUrl + '/cms/Item/Version/Update.groovy', params).then(function(response) {
			return new Version(response.data, instance.$item);
		});
	}

	restore() {
		let instance = this;
		return _axios.post(_baseUrl + '/cms/Item/Version/Restore.groovy', {
			'id': instance.$item.id,
			'version': instance.$data.name,
		}).then(function(response) {
			return new Item(response.data);
		});
	}

	remove() {
		let instance = this;
		return _axios.post(_baseUrl + '/cms/Item/Version/Remove.groovy', {
			'id': instance.$item.id,
			'version': instance.$data.name,
		});
	}
}

export class FacetDefinition {
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

	getItem() {
		let instance = this;
		return _axios.post(_baseUrl + '/cms/GetItem.groovy', {
			'path': '/WEB-INF/facets/' + instance.key + '.yml'
		}).then(function(response) {
			return new Item(response.data);
		});
	}
}

export class Currency {
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

export class MultipartUpload {
	constructor({file, options}) {
		let instance = this;
		if (!file) {
			throw new Error("Missing object.");
		}

		instance.options = options;
		if (!instance.options) {
			instance.options = {};
		}
		instance.file = (function() {
			if (file == undefined) {
				return undefined;
			}
			if (file.constructor.name == 'File') {
				return file;
			}
			if (file.getAsFile) {
				return file.getAsFile();
			}
			if (file.webkitSlice) {
				return file;
			}
			if (file.mozSlice) {
				return file;
			}
			if (file.slice) {
				return file;
			}
			if (file.constructor.name == 'Blob') {
				return file;
			}
			return undefined;
		})();
		instance.entry = (function() {
			if (instance.file == undefined) {
				return undefined;
			}
			if (instance.file.isDirectory != undefined) {
				return file;
			}
			if (instance.file.getAsEntry) {
				return file.getAsEntry();
			}
			if (instance.file.webkitGetAsEntry) {
				return file.webkitGetAsEntry();
			}
			return undefined;
		})();
		let slice = (function() {
			if (instance.file == undefined) {
				return undefined;
			}
			if (instance.file.webkitSlice) {
				return instance.file.webkitSlice;
			}
			if (instance.file.mozSlice) {
				return instance.file.mozSlice;
			}
			if (instance.file.slice) {
				return instance.file.slice;
			}
			return undefined;
		})();

		if (!instance.file && !instance.entry) {
			throw new Error("Missing object.");
		}
		if (instance.entry && instance.entry.isDirectory) {
			throw new Error("The object is directory: " + instance.entry.fullPath);
		}
		if (!slice) {
			throw new Error("The object does not have a slice method.");
		}
	}

	get filename() {
		let instance = this;
		if (!(instance.file.constructor.name == 'File')) {
			return undefined;
		}

		let path;
		if (instance.entry) {
			path = instance.entry.fullPath;
		} else {
			path = instance.file.name;
		}
		let p = path.lastIndexOf('/');
		if (p != -1) {
			path = path.substring(p + 1);
		}
		return path;
	}

	get mimeType() {
		let instance = this;
		return instance.file.type;
	}

	get totalSize() {
		let instance = this;
		if (!instance.file) {
			return undefined;
		}
		return instance.file.size;
	}

	get uploadedSize() {
		let instance = this;
		if (!instance.data) {
			return 0;
		}
		return instance.data.file.length;
	}

	cancel() {
		let instance = this;
		instance.cancelled = true;
		return instance;
	}

	get id() {
		let instance = this;
		if (!instance.data) {
			return undefined;
		}
		return instance.data.id;
	}

	get chunkSize() {
		let instance = this;
		let chunkSize = 0;
		if (instance.options && (typeof instance.options.chunkSize == 'number')) {
			chunkSize = instance.options.chunkSize;
		}
		if (chunkSize <= 0) {
			chunkSize = 1024 * 1024;
		}
		return chunkSize;
	}

	start() {
		let instance = this;
		let fireEvent = function() {
			if (typeof instance.options.onprogress != 'function') {
				return;
			}
			if (!instance.data) {
				return;
			}

			try {
				instance.options.onprogress(instance);
			} catch (ex) {
				// ignore
			}
		};

		return _axios.post(_baseUrl + '/cms/MultipartUpload/Initiate.groovy', {
		}).then(function(response) {
			instance.data = response.data;
			fireEvent();
		}).then(function() {
			if (instance.totalSize == 0) {
				return instance;
			}

			return new Promise(function(resolve, reject) {
				let reader = new FileReader();
				let uploadChunked = function() {
					reader.onloadend = function(event) {
						if (event.target.readyState == FileReader.DONE) {
							let encoded = event.target.result;
							encoded = encoded.substring(encoded.indexOf(";base64,") + 8);
							_axios.post(_baseUrl + '/cms/MultipartUpload/Append.groovy', {
								'id': instance.id,
								'data': encoded
							}).then(function(response) {
								instance.data = response.data;
								fireEvent();

								if (instance.data.file.length >= instance.totalSize) {
									resolve(instance);
									return;
								}

								(function() {
									uploadChunked();
								})();
							}).catch(function(error) {
								reject(error);
							});
						}
					};

					let offset = instance.uploadedSize;
					let limit = instance.chunkSize;
					let blob;
					if (instance.file.webkitSlice) {
						blob = instance.file.webkitSlice(offset, offset + limit);
					} else if (instance.file.mozSlice) {
						blob = instance.file.mozSlice(offset, offset + limit);
					} else if (instance.file.slice) {
						blob = instance.file.slice(offset, offset + limit);
					}
					reader.readAsDataURL(blob);
				};
				uploadChunked();
			});
		});
	}
}

export class Search {
	constructor(statement, language) {
		this.$data = {
			'statement': statement,
			'language': language,
			'offset': 0,
			'limit': 1000
		};
		for (let k of Object.keys(this.$data)) {
			if (['offset', 'limit'].indexOf(k) != -1) {
				continue;
			}
			Object.defineProperty(this, k, {
				'get': function() {
					return this.$data[k];
				}
			});
		}
	}

	get offset() {
		return this.$data.offset;
	}
	set offset(value) {
		this.$data.offset = value;
	}

	get limit() {
		return this.$data.limit;
	}
	set limit(value) {
		this.$data.limit = value;
	}

	execute(options) {
		let instance = this;
		if (!options) {
			options = {};
		}
		if (!options.headers) {
			options.headers = {};
		}
		return _axios.post(_baseUrl + '/cms/Search/Execute.groovy', {
			'statement': instance.$data.statement,
			'language': instance.$data.language,
			'offset': instance.$data.offset,
			'limit': instance.$data.limit
		},
		{
			'headers': options.headers
		}).then(function(response) {
			if (typeof options.onResponse == 'function') {
				return options.onResponse(response);
			}
			return response;
		}).then(function(response) {
			return new SearchResult(response.data);
		});
	}
}

export class SearchResult {
	constructor(data) {
		this.$data = data;
		for (let k of Object.keys(this.$data)) {
			if (k == 'items') {
				continue;
			}
			Object.defineProperty(this, k, {
				'get': function() {
					return this.$data[k];
				}
			});
		}
	}

	get items() {
		let l = [];
		for (let e of this.$data.items) {
			l.push(new Item(e));
		}
		return l;
	}
}

export class Crawler {
	constructor(statement, language) {
		this.$data = {
			'statement': statement,
			'language': language,
		};
		for (let k of Object.keys(this.$data)) {
			Object.defineProperty(this, k, {
				'get': function() {
					return this.$data[k];
				}
			});
		}
	}

	get status() {
		return this.$data.status;
	}

	get statusText() {
		return this.$data.statusText;
	}

	start() {
		let instance = this;
		return _axios.post(_baseUrl + '/cms/Crawler/Start.groovy', {
			'statement': instance.$data.statement,
			'language': instance.$data.language,
		}).then(function(response) {
			let data = response.data;
			instance.$data.identifier = data.identifier;
			instance.$data.status = data.status;
			instance.$data.statusText = data.statusText;
			return instance;
		});
	}

	update() {
		let instance = this;
		return _axios.post(_baseUrl + '/cms/Crawler/Update.groovy', {
			'identifier': instance.$data.identifier,
		}).then(function(response) {
			let data = response.data;
			instance.$data.identifier = data.identifier;
			instance.$data.status = data.status;
			instance.$data.statusText = data.statusText;
			return instance;
		});
	}

	remove() {
		let instance = this;
		return _axios.post(_baseUrl + '/cms/Crawler/Remove.groovy', {
			'identifier': instance.$data.identifier,
		});
	}
}

export class Exporter {
	constructor(data) {
		this.$data = data;
		if (!this.$data.options) {
			this.$data.options = {};
		}
	}

	get status() {
		return this.$data.status;
	}

	get statusText() {
		return this.$data.statusText;
	}

	execute() {
		let instance = this;
		if (instance.$data.paths.length == 0) {
			return Promise.reject("paths");
		}

		return _axios.post(_baseUrl + '/cms/Exporter/Execute.groovy', {
			'paths': instance.$data.paths,
			'noMetadata': instance.$data.noMetadata,
		}).then(function(response) {
			instance.$data.identifier = response.data.identifier;
			return new Promise(function(resolve, reject) {
				const fireEvent = function() {
					if (typeof instance.$data.options.onprogress != 'function') {
						return;
					}

					try {
						instance.$data.options.onprogress(instance);
					} catch (ex) {
						// ignore
					}
				};

				let source = new EventSource(_baseUrl + '/cms/Exporter/Execute.groovy?identifier=' + encodeURIComponent(instance.$data.identifier), {
					'withCredentials': true,
				});
				source.onmessage = function(event) {
					let e = event.data;
					if (!e) {
						return;
					}

					e = JSON.parse(e);
					instance.$data.status = e.status;
					instance.$data.statusText = e.statusText;
					if (['done', 'error'].indexOf(e.status) != -1) {
						source.close();
						if (e.status == 'error') {
							reject(e.statusText);
						} else {
							resolve(instance);
						}
					}
					fireEvent();
				};
				source.onerror = function() {
					source.close();
				};
			});
		});
	}

	download() {
		let instance = this;
		window.location.href = _baseUrl + '/cms/Exporter/Download.groovy?identifier=' + encodeURIComponent(instance.$data.identifier);
	}
}

export class Importer {
	constructor(data) {
		this.$data = data;
		if (!this.$data.options) {
			this.$data.options = {};
		}
	}

	get status() {
		return this.$data.status;
	}

	get statusText() {
		return this.$data.statusText;
	}

	execute() {
		let instance = this;
		if (!instance.$data.path) {
			return Promise.reject("path");
		}
		if (!instance.$data.uploadID) {
			return Promise.reject("uploadID");
		}

		return _axios.post(_baseUrl + '/cms/Importer/Execute.groovy', {
			'path': instance.$data.path,
			'uploadID': instance.$data.uploadID,
		}).then(function(response) {
			instance.$data.identifier = response.data.identifier;
			return new Promise(function(resolve, reject) {
				const fireEvent = function() {
					if (typeof instance.$data.options.onprogress != 'function') {
						return;
					}

					try {
						instance.$data.options.onprogress(instance);
					} catch (ex) {
						// ignore
					}
				};

				let source = new EventSource(_baseUrl + '/cms/Importer/Execute.groovy?identifier=' + encodeURIComponent(instance.$data.identifier), {
					'withCredentials': true,
				});
				source.onmessage = function(event) {
					let e = event.data;
					if (!e) {
						return;
					}

					e = JSON.parse(e);
					instance.$data.status = e.status;
					instance.$data.statusText = e.statusText;
					if (['done', 'error'].indexOf(e.status) != -1) {
						source.close();
						if (e.status == 'error') {
							reject(e.statusText);
						} else {
							resolve(instance);
						}
					}
					fireEvent();
				};
				source.onerror = function() {
					source.close();
				};
			});
		});
	}
}
