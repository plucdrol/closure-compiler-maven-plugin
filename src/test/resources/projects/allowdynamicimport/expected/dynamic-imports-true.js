'use strict';
async function onMoreClicked(a) {
  ({enableAwesomeFeature:a} = await Promise.resolve(module$import));
  a();
}
document.getElementById("loadMore").addEventListener(a => onMoreClicked(a));
function enableAwesomeFeature$$module$import() {
  console.log("The awesome feature shall now be enabled.");
}
var module$import = {};
module$import.enableAwesomeFeature = enableAwesomeFeature$$module$import;

