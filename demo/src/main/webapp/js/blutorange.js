
class Blutorange {
	constructor()  {
		this.version = "final";
	}
	sayHello(name) {
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