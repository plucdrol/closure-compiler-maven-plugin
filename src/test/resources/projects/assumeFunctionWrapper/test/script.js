var longVariable = 9;
class Foo {
  init() {
    return longVariable + 6;
  }
  exit() {
    return longVariable - 6;
  }
}
console.log(typeof new Foo);
console.log(x);