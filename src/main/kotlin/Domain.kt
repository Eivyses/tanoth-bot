package org.example

data class Adventures(
    val adventuresMadeToday: Int,
    val freeAdventuresPerDay: Int,
    val adventures: List<Adventure>
) {
  fun toPrettyString(): List<String> {
    val result = mutableListOf<String>()
    result += "adventuresMadeToday: $adventuresMadeToday\n"
    result += "freeAdventuresPerDay: $freeAdventuresPerDay\n"
    result += "adventures:"
    adventures.forEach { adventure ->
      result +=
          "  gold: ${adventure.gold}, exp: ${adventure.experience}, difficulty: ${adventure.difficulty}"
    }
    return result.toList()
  }
}

enum class Difficulty(val value: Int) {
  EASY(-1),
  MEDIUM(0),
  DIFFICULT(1),
  VERY_DIFFICULT(2);

  companion object {
    fun fromInt(value: Int): Difficulty = entries.first { it.value == value }
  }
}

data class Adventure(
    val difficulty: Difficulty,
    val duration: Int,
    val experience: Int,
    val fightChance: Int,
    val gold: Int,
    val questId: Int
)

data class OtherPlayerInfo(val name: String, val guildName: String?, val id: Int, val level: Int)

data class RunningAdventure(val id: Int, val timeRemaining: Int, val timeTotal: Int)

data class RandomEnemyResponse(
    val freeReattacks: Int?,
    val reattackCountdown: Int,
    val otherPlayerInfo: OtherPlayerInfo
)

data class AttackResult(
    val haveWon: Boolean,
    val fame: Int,
    val robbedGold: Int,
    val xpEarned: Int
)

data class AdventureResult(
    val haveWon: Boolean,
    val xpEarned: Int,
    val goldEarned: Int,
    val itemFound: Boolean,
    val gemsFound: Int
)

data class AdventureResponse(
    val timeRemaining: Int?,
    val adventures: Adventures?,
    val adventureResult: AdventureResult?
)

data class UserAttributesResponse(val userAttributes: Map<AttributeType, UserAttribute>)

enum class ArcaneCircleItemType(val id: Int, val effect: String) {
  AMETHYST(id = 1, effect = "More gold in adventures"),
  AMBER(id = 2, effect = "Higher wages"),
  TOPAZ(id = 3, effect = "Inventory slot every 10 refinement grades"),
  RUBY(id = 4, effect = "Potion effectiveness"),
  EMERALD(id = 5, effect = "Better prices when selling to Merchant"),
  SAPPHIRE(id = 6, effect = "Fame earned"),
  AQUAMARINE(id = 7, effect = "Potion duration"),
  JADE(id = 8, effect = "Experience earned"),
  TIGERS_EYE(id = 9, effect = "Travel speed"),
  DIAMOND(id = 10, effect = "Cheaper Alchemist and Merchant items"),
  RUNE_OF_COURAGE(id = 11, effect = "Strength"),
  RUNE_OF_DILIGENCE(id = 12, effect = "Dexterity"),
  RUNE_OF_WISDOM(id = 13, effect = "Constitution"),
  RUNE_OF_NEGOTIATION(id = 14, effect = "Intelligence"),
  RUNE_OF_GLORY(id = 15, effect = "Loot chance on adventures"),
  DEMON_SKULL(id = 16, effect = "Many")
}

data class ArcaneCircleItem(
    val arcaneCircleItemType: ArcaneCircleItemType,
    val level: Int,
    val maxLevel: Int,
    val goldPrice: Int,
    val effectPower: Int
)

data class CurrentPlayerInfo(val gems: Int, val gold: Int, val fame: Int)

enum class AttributeType(val value: String) {
  CON("CON"),
  STR("STR"),
  DEX("DEX"),
  INT("INT")
}

data class UserAttribute(val attributeCost: Int, val attributeBase: Int)
