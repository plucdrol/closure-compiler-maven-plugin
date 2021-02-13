import { enableAwesomeFeature } from "./import.js";

async function onMoreClicked(event) {
    enableAwesomeFeature();
}

document.getElementById("loadMore").addEventListener(e => onMoreClicked(e));