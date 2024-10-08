package org.example

fun parseSessionIdFromHtml(html: String): String {
  val searchString = "sessionID: \""
  val startIndex = html.indexOf(searchString) + searchString.length
  val endIndex = html.indexOf(string = "\"", startIndex = startIndex)
  return html.substring(startIndex = startIndex, endIndex = endIndex)
}
