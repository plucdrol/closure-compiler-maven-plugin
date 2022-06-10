'use strict';for(var a=document.getElementById("notes"),b=[{title:"Note 1",content:"Content of Note 1"},{title:"Note 2",content:"Content of Note 2"}],c=0;c<b.length;c++){var d=b[c].content,e=a,f=textDiv(b[c].title),g=textDiv(d),h=document.createElement("div");h.appendChild(f);h.appendChild(g);e.appendChild(h)}
;
