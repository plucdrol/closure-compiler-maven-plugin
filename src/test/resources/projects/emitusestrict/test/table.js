function createTable(rows, cols) {
	var j = 1;
	var output = "<table border='1' width='500' cellspacing='0'cellpadding='5'>";
	for (i = 1; i <= rows; i++) {
		output = output + "<tr>";
		while (j <= cols) {
			output = output + "<td>" + i * j + "</td>";
			j = j + 1;
		}
		output = output + "</tr>";
		j = 1;
	}
	output = output + "</table>";
	document.write(output);
}
createTable(5, 3);