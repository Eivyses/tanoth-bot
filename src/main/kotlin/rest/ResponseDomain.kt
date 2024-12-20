package org.example.rest

import org.example.*
import org.w3c.dom.Document

fun Document.parseAsAdventures(): Adventures {
  val adventuresMadeToday = this.getValueFromXml<Int>("adventures_made_today")!!
  val freeAdventuresPerDay = this.getValueFromXml<Int>("free_adventures_per_day")!!
  val difficulties = this.getValuesFromXml<Int>("difficulty")
  val durations = this.getValuesFromXml<Int>("duration")
  val experiences = this.getValuesFromXml<Int>("exp")
  val fightChances = this.getValuesFromXml<Int>("fight_chance")
  val golds = this.getValuesFromXml<Int>("gold")
  val questIds = this.getValuesFromXml<Int>("quest_id")

  val adventures =
      difficulties.indices.map { index ->
        Adventure(
            difficulty = Difficulty.fromInt(difficulties[index]),
            duration = durations[index],
            experience = experiences[index],
            fightChance = fightChances[index],
            gold = golds[index],
            questId = questIds[index])
      }

  return Adventures(
      adventuresMadeToday = adventuresMadeToday,
      freeAdventuresPerDay = freeAdventuresPerDay,
      adventures = adventures)
}

fun Document.parseAsAdventureResponse(): AdventureResponse? {
  val timeRemaining = this.getValueFromXml<Int>("running_adventure_time_remain")
  val goldReward = this.getValueFromXml<Int>("reward_gold")
  if (timeRemaining == null && goldReward == null) {
    // TODO: make better check for no response object
    if (this.getValueFromXml<Int>("adventures_made_today") == null) {
      // currently in work, no response object was found
      return null
    }
    // not in adventure and no fight results
    val adventures = this.parseAsAdventures()
    return AdventureResponse(timeRemaining = null, adventures = adventures, adventureResult = null)
  } else if (goldReward != null) {
    val adventureResult = this.parseAsAdventureResult()
    return AdventureResponse(
        timeRemaining = null, adventures = null, adventureResult = adventureResult)
  }
  val runningAdventure = this.parseAsRunningAdventure()
  return AdventureResponse(
      timeRemaining = runningAdventure.timeRemaining, adventures = null, adventureResult = null)
}

fun Document.parseAsRunningAdventure(): RunningAdventure {
  val id = this.getValueFromXml<Int>("running_adventure_id")!!
  val timeRemaining = this.getValueFromXml<Int>("running_adventure_time_remain")!!
  val timeTotal = this.getValueFromXml<Int>("running_adventure_time_total")!!

  return RunningAdventure(id = id, timeRemaining = timeRemaining, timeTotal = timeTotal)
}

fun Document.parseAsRandomEnemy(): RandomEnemyResponse {
  val freeReattacks = this.getValueFromXml<Int>("free_reattacks")
  val reattackCountdown = this.getValueFromXml<Int>("reattack_countdown") ?: 0
  val playerInfo = this.parseAsOtherPlayerInfo()
  return RandomEnemyResponse(
      freeReattacks = freeReattacks,
      reattackCountdown = reattackCountdown,
      otherPlayerInfo = playerInfo)
}

fun Document.parseAsOtherPlayerInfo(): OtherPlayerInfo {
  val guildName = this.getValueFromXml<String>("guild_name")
  val id = this.getValueFromXml<Int>("id")!!
  val level = this.getValueFromXml<Int>("level")!!
  val name = this.getValueFromXml<String>("name")!!

  return OtherPlayerInfo(name = name, level = level, id = id, guildName = guildName)
}

fun Document.parseAsAttackResult(): AttackResult? {
  // rare case where username is not found
  val fame = this.getValueFromXml<Int>("achieved_fame") ?: return null
  val gold = this.getValueFromXml<Int>("robbed_gold")!!
  val xp = this.getValueFromXml<Int>("xp")!!

  val enemyPhysicalDamage = this.getInnerValueFromXml<Int>("opponent", "total_damage")!!
  val enemySpellDamage = this.getInnerValueFromXml<Int>("opponent", "magic_damage")!!
  val playerHealth = this.getInnerValueFromXml<Int>("self", "hitpoints")!!

  val haveWon = playerHealth > enemyPhysicalDamage + enemySpellDamage
  val resultFame = if (haveWon) fame else -fame
  val resultGold = if (haveWon) gold else -gold
  return AttackResult(haveWon = haveWon, fame = resultFame, robbedGold = resultGold, xpEarned = xp)
}

fun Document.parseAsAdventureResult(): AdventureResult {
  val gemsFound = this.getValueFromXml<Int>("reward_bloodstones")!!
  val goldEarned = this.getValueFromXml<Int>("reward_gold")!!
  val xpEarned = this.getValueFromXml<Int>("reward_exp")!!
  val itemFound = this.getValueFromXml<Boolean>("item_found")!!

  val haveWon = goldEarned > 0
  return AdventureResult(
      haveWon = haveWon,
      xpEarned = xpEarned,
      goldEarned = goldEarned,
      itemFound = itemFound,
      gemsFound = gemsFound)
}

fun Document.parseAsArcaneCircle(): List<ArcaneCircleItem> {
  val arcaneCircleItems =
      ArcaneCircleItemType.entries.map {
        val values = this.getValueFromXml<String>(it.id.toString())!!.split(":").map { it.toInt() }
        val level = values[0]
        val maxLevel = values[1]
        val gemPrice = values[2]
        val goldPrice = values[3]
        val something = values[4]
        val somethingMore = values[5]
        val somethingElse = values[6]
        val effectPower = values[7]
        ArcaneCircleItem(
            arcaneCircleItemType = it,
            level = level,
            maxLevel = maxLevel,
            goldPrice = goldPrice,
            effectPower = effectPower)
      }
  return arcaneCircleItems
}

fun Document.parseAsCurrentPlayerInfo(): CurrentPlayerInfo {
  val gems = this.getValueFromXml<Int>("bs")!!
  val fame = this.getValueFromXml<Int>("fame")!!
  val gold = this.getValueFromXml<Int>("gold")!!

  return CurrentPlayerInfo(gems = gems, gold = gold, fame = fame)
}

fun Document.parseAsUserAttributesResponse(): UserAttributesResponse {
  val conBase = this.getValueFromXml<Int>("con_base")!!
  val costCon = this.getValueFromXml<Int>("cost_con")!!

  val dexBase = this.getValueFromXml<Int>("dex_base")!!
  val costDex = this.getValueFromXml<Int>("cost_dex")!!

  val intBase = this.getValueFromXml<Int>("int_base")!!
  val costInt = this.getValueFromXml<Int>("cost_int")!!

  val strBase = this.getValueFromXml<Int>("str_base")!!
  val costStr = this.getValueFromXml<Int>("cost_str")!!

  return UserAttributesResponse(
      mapOf(
          AttributeType.CON to UserAttribute(attributeCost = costCon, attributeBase = conBase),
          AttributeType.DEX to UserAttribute(attributeCost = costDex, attributeBase = dexBase),
          AttributeType.INT to UserAttribute(attributeCost = costInt, attributeBase = intBase),
          AttributeType.STR to UserAttribute(attributeCost = costStr, attributeBase = strBase)))
}

fun Document.parseAsWorkDataResponse(): WorkDataResponse? {
  val goldFee = this.getValueFromXml<Int>("gold_fee") ?: return null
  val maxWorkingHours = this.getValueFromXml<Int>("max_working_hours")!!
  val xp = this.getValueFromXml<Int>("xp")!!
  return WorkDataResponse(goldFee = goldFee, maxWorkingHours = maxWorkingHours, xp = xp)
}

fun Document.parseAsMapDetailsResponse(): MapDetailsResponse? {
  val cost = this.getValueFromXml<Int>("illusion_cave_bloodstone_cost") ?: return null
  val level = this.getValueFromXml<Int>("illusion_cave_level")!!
  val duration = this.getValueFromXml<Int>("illusion_duration")!!
  val enemy = this.getValueFromXml<Int>("illusion_enemy")!!
  val showCave = this.getValueFromXml<Boolean>("show_illusion_cave")!!
  return MapDetailsResponse(bsCost = cost, level = level, duration = duration, enemy = enemy, showCave = showCave)
}