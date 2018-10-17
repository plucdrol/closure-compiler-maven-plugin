import { PearceKellyDetector } from "./subdir/pearce-kelly.js";

class Blutorange {
	constructor()  {
		this.version = "final";
	}
	sayHello(name) {
		console.log(new PearceKellyDetector());
		return `こんにちは,${name}!`;
	}
	static getInstance() {
		return Blutorange._instance;
	}
}

try {
    Blutorange._instance = new Blutorange();
} catch (e) {
	console.error(e);
}

/**
 * DEFINE_TEST should be false after Closure Compiler executes
 * @define {boolean}
 */
const DEFINE_TEST = true;