'use strict';var VARS={namespace:"_some-original-namespace",container:"-container",dragging:"dragging",width:"250",height:"140"},Html=function(a){this.Base=a};Html.prototype.createContainer=function(){var a=document.createElement("div");a.className=VARS.namespace+VARS.container;a.textContent="Drag me";document.body.appendChild(a);return a};Html.prototype.htmlTest=function(){console.info("htmlTest invoked")};

//# sourceMappingURL=sourcemap/map-path.js.map
