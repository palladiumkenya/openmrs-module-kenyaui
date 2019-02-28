<%
	config.require("id")
	config.require("concepts")
%>

<table class="ke-table-vertical" id="${ config.id }" style="${ config.style ? config.style : "" }">
	<thead>
	<tr>
		<th>Date</th>
		<% concepts.each { %>
		<th nowrap="nowrap">${ ui.format(it) }</th>
		<% } %>
	</tr>
	</thead>
	<tbody>
	<% if (!data) { %>
	<tr>
		<td colspan="${ concepts.size() + 1 }">${ ui.message("general.none") }</td>
	</tr>
	<% } %>
	<% data.each { date, results -> %>
	<tr>
		<td nowrap="nowrap" style="vertical-align: top;"><%= kenyaui.formatDate(date) %></td>
		<% concepts.each { concept -> %>
		<td>
			<%
			def obss = results[concept]
			if (obss) {
				print obss.collect({ obs -> kenyaui.formatObsWrapper(obs) }).join("<br />")
			}
			%>
		</td>
		<% } %>
	</tr>
	<% } %>
	</tbody>
</table>
