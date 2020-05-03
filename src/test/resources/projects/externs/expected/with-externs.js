'use strict';for(var a=[{title:"Note 1",content:"Content of Note 1"},{title:"Note 2",content:"Content of Note 2"}],b=document.getElementById("notes"),c=0;c<a.length;c++){var d=a[c].content,e=b,f=textDiv(a[c].title),g=textDiv(d),h=document.createElement("div");h.appendChild(f);h.appendChild(g);e.appendChild(h)}
;
