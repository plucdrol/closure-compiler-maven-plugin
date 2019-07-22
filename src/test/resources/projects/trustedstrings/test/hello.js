const div = document.createElement("div");
div.innerHTML = `
	<form>
	  <label for="answer">If x &lt; 10 + 2 &gt; z, find the value of R!</label>
	  <input id="answer" type="number" value="42">
	</form>
`;
document.body.appendChild(div);