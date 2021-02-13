'use strict';
function makeUnique(a) {
  a = new Set(a);
  return Array.from(a);
}
function uniqueByKey(a, c) {
  a = new Map(a.map(b => [c(b), b]));
  return Array.from(a.values());
}
;