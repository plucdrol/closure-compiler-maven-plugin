class Vector2 {
  constructor(x, y) {
    this.x = x;
    this.y =y;
  }
  add(other) {
    return new Vector2(this.x + other.x, this.y + other.y);
  }
}