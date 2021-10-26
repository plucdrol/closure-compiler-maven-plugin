async function onMoreClicked(event) {
    const module = "./awesome-feature.js";
    const { enableAwesomeFeature } = await import(module);
    enableAwesomeFeature();
}

document.getElementById("loadMore").addEventListener(e => onMoreClicked(e));