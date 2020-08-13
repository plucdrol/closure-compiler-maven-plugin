class Foo1 {
	foo() {
		console.log(1);
	}
}

class Foo2 {
	foo() {
		console.log(2);
	}
}

console.log(Foo1, Foo2);

/** @type {Foo1} */
const x = new Foo1();

x.foo();
