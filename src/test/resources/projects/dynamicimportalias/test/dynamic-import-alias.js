async function onMoreClicked(event) {
    const module = "./import.js";
    const { enableAwesomeFeature } = await import(module);
    enableAwesomeFeature();
}

document.getElementById("loadMore").addEventListener(e => onMoreClicked(e));