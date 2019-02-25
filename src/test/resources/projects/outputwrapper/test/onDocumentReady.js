function performCheck(event) {
	console.log("checking form...");
}
const div = document.getElementById("button[name='check']");
div.addEventListener("click", event => performCheck(event));