/* Copyright (c) 2021 MintJams Inc. Licensed under MIT License. */

export class Loader {
	static load(url) {
		let p = Promise.resolve();
		if (!window.$import) {
			p = p.then(function() {
				return new Promise(function(resolve, reject) {
					let el = document.createElement('script');
					el.innerHTML = 'window.$import = function(url) {return import(url);}';
					document.body.appendChild(el);
					let millis = 0;
					let waitFor = function() {
						if (window.$import) {
							resolve();
							return;
						}
						if (millis > 5000) {
							reject();
							return;
						}

						window.top.setTimeout(function() {
							millis += 100;
							waitFor();
						}, 100);
					};
					waitFor();
				});
			});
		}
		p = p.then(function() {
			return window.$import(url);
		});
		return p;
	}
}
