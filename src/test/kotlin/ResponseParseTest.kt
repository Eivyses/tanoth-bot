import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.example.rest.asXml
import org.example.rest.parseAsAdventureResult
import org.example.rest.parseAsAttackResult
import org.example.rest.parseAsCurrentPlayerInfo

class ResponseParseTest {

  @Test
  fun testAttackParse() {
    val fileContent = ResponseParseTest::class.java.getResource("attack_result1.xml")!!.readText()
    // opponent damage: 2798
    // my health: 7533
    val attackResult = fileContent.asXml().parseAsAttackResult()!!
    assertTrue(attackResult.haveWon)
  }

  @Test
  fun testAdventureResultParse1() {
    val fileContent =
        ResponseParseTest::class.java.getResource("adventure_result1.xml")!!.readText()
    val adventureResult = fileContent.asXml().parseAsAdventureResult()
    assertTrue(adventureResult.haveWon)
    assertTrue(adventureResult.itemFound)
  }

  @Test
  fun testAdventureResultParse2() {
    val fileContent =
        ResponseParseTest::class.java.getResource("adventure_result2.xml")!!.readText()
    val adventureResult = fileContent.asXml().parseAsAdventureResult()
    assertTrue(adventureResult.haveWon)
    assertFalse(adventureResult.itemFound)
  }

  @Test
  fun testPlayerInfoParse1() {
    val fileContent = ResponseParseTest::class.java.getResource("player_info1.xml")!!.readText()
    val currentPlayerInfo = fileContent.asXml().parseAsCurrentPlayerInfo()
    assertEquals(2892, currentPlayerInfo.gold)
    assertEquals(1639, currentPlayerInfo.fame)
  }

  @Test
  fun testPlayerInfoParse2() {
    val fileContent = ResponseParseTest::class.java.getResource("player_info2.xml")!!.readText()
    val currentPlayerInfo = fileContent.asXml().parseAsCurrentPlayerInfo()
    assertEquals(3604, currentPlayerInfo.gold)
    assertEquals(1672, currentPlayerInfo.fame)
  }
}
