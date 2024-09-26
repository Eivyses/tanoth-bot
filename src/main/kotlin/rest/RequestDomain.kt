package org.example.rest

fun createPostBody(
    methodName: String,
    sessionId: String,
    extraIntParam: Int? = null,
    extraStringParam: String? = null
): String {
  val extraIntParamBody =
      if (extraIntParam != null)
          """
            <param>
                <value>
                    <int>$extraIntParam</int>
                </value>
            </param>
          """
              .trimIndent()
      else ""

  val extraStringParamBody =
      if (extraStringParam != null)
          """
            <param>
                <value>
                    <string>$extraStringParam</string>
                </value>
            </param>
          """
              .trimIndent()
      else ""

  return """
          <methodCall>
              <methodName>$methodName</methodName>
              <params>
                  <param>
                      <value>
                          <string>$sessionId</string>
                      </value>
                  </param>
                  $extraStringParamBody
                  $extraIntParamBody
              </params>
          </methodCall>
        """
      .trimIndent()
}
