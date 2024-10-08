import kotlin.test.Test
import kotlin.test.assertEquals
import org.example.parseSessionIdFromHtml

class UtilTest {

  @Test
  fun testSessionIdParse() {
    val fileContent = UtilTest::class.java.getResource("ingame_body.html")!!.readText()
    val sessionId = parseSessionIdFromHtml(fileContent)
    assertEquals("0i6m39u6c10b", sessionId)
  }
}
