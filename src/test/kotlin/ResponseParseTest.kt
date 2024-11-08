import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.example.AttributeType
import org.example.rest.*

class ResponseParseTest {

  @Test
  fun testAttackParse1() {
    val fileContent = ResponseParseTest::class.java.getResource("attack_result1.xml")!!.readText()
    // opponent damage: 2798
    // my health: 7533
    val attackResult = fileContent.asXml().parseAsAttackResult()!!
    assertTrue(attackResult.haveWon)
  }

  @Test
  fun testAttackParse2() {
    val fileContent = ResponseParseTest::class.java.getResource("attack_result2.xml")!!.readText()
    // opponent damage: 177360
    // my health: 18792
    val attackResult = fileContent.asXml().parseAsAttackResult()!!
    assertFalse(attackResult.haveWon)
    assertEquals(-76, attackResult.fame)
    assertEquals(0, attackResult.robbedGold)
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

  @Test
  fun testUserAttributes() {
    val fileContent = ResponseParseTest::class.java.getResource("user_attributes.xml")!!.readText()
    val userAttributes = fileContent.asXml().parseAsUserAttributesResponse().userAttributes

    assertEquals(2000, userAttributes.getValue(AttributeType.CON).attributeCost)
    assertEquals(3000, userAttributes.getValue(AttributeType.DEX).attributeCost)
    assertEquals(1800, userAttributes.getValue(AttributeType.INT).attributeCost)
    assertEquals(1000, userAttributes.getValue(AttributeType.STR).attributeCost)

    assertEquals(623, userAttributes.getValue(AttributeType.CON).attributeBase)
    assertEquals(700, userAttributes.getValue(AttributeType.DEX).attributeBase)
    assertEquals(583, userAttributes.getValue(AttributeType.INT).attributeBase)
    assertEquals(500, userAttributes.getValue(AttributeType.STR).attributeBase)
  }
}
