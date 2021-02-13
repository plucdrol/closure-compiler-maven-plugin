async function onMoreClicked(event) {
    const { enableAwesomeFeature } = await import("./import.js");
    enableAwesomeFeature();
}

document.getElementById("loadMore").addEventListener(e => onMoreClicked(e));