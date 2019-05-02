/* https://github.com/jonataswalker/es6-sample-project */
/*
The MIT License (MIT)

Copyright (c) 2016 Jonatas Walker

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
const VARS = {
	"namespace": "_some-original-namespace",
	"container": "-container",
	"dragging": "dragging",
	"width": "250",
	"height": "140"
};

class Html {
	/**
	 * @constructor
	 * @param {new (...args: any[]) => any} base Base class.
	 */
	constructor(base) {
		this.Base = base;
	}

	/**
	 * @returns {HTMLDivElement}
	 */
	createContainer() {
		const container = document.createElement('div');
		container.className = VARS.namespace + VARS.container;
		container.textContent = 'Drag me';
		document.body.appendChild(container);
		return container;
	}

	htmlTest() {
		console.info('htmlTest invoked'); // eslint-disable-line no-console
	}
}