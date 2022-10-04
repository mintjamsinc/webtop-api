/* Copyright (c) 2021 MintJams Inc. Licensed under MIT License. */

import * as user from "./user";
import * as util from "./util";
import CryptoJS from 'crypto-js';

let _baseUrl = util.Env.getBaseUrl();
let _axios;

export class AuthClient {
	constructor({axios}) {
		_axios = axios;
	}

	authenticate(params) {
		if (!params) {
			params = {};
		}
		return _axios.post(_baseUrl + '/auth/Authenticate.groovy', params).then(function(response) {
			return response.data;
		});
	}

	logout(params) {
		if (!params) {
			params = {};
		}
		return _axios.post(_baseUrl + '/auth/Logout.groovy', params);
	}

	getUser(params) {
		let instance = this;
		if (!params) {
			params = {};
		}
		return _axios.post(_baseUrl + '/auth/GetUser.groovy', params).then(function(response) {
			instance.$session = {
				'id': response.data.id,
				'eTag': response.headers['etag'],
			};
			let secret = CryptoJS.enc.Utf8.parse(instance.$session.eTag);
			let salt = CryptoJS.SHA256(instance.$session.id);
			instance.$session.key = CryptoJS.PBKDF2(secret, salt, {
				'iterations': 10240,
				'keySize': 128 / 32,
				'hasher': CryptoJS.algo.SHA256,
			});
			return new user.User(response.data);
		});
	}

	verifyTOTP(params) {
		if (!params) {
			params = {};
		}
		return _axios.post(_baseUrl + '/auth/VerifyTOTP.groovy', params);
	}

	unmask(value) {
		if (!value.startsWith('{AES}')) {
			return value;
		}

		let hexData = CryptoJS.enc.Base64.parse(value.substring(5)).toString();
		let iv = CryptoJS.enc.Hex.parse(hexData.substring(0, 16 * 2));
		let encrypted = CryptoJS.enc.Hex.parse(hexData.substring(16 * 2));
		let cipherParams = CryptoJS.lib.CipherParams.create({
			ciphertext: encrypted,
		});
		let decrypted = CryptoJS.AES.decrypt(cipherParams, this.$session.key, {
			'iv': iv,
			'mode': CryptoJS.mode.CBC,
			'padding': CryptoJS.pad.Pkcs7,
		});
		return decrypted.toString(CryptoJS.enc.Utf8);
	}
}
