'use strict';
async function onMoreClicked(a) {
  ({enableAwesomeFeature:a} = await import("./awesome-feature.js"));
  a();
}
document.getElementById("loadMore").addEventListener(a => onMoreClicked(a));
export{};