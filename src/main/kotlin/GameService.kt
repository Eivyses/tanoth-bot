package org.example

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay

private val logger = KotlinLogging.logger {}

class GameService(sessionId: String, gfToken: String?) {

  private val tService = TService(sessionId, gfToken)
  private var attackTarget: OtherPlayerInfo? = null
  private var stolenGold: Int = 0

  suspend fun run(
      runeToUpgrade: ArcaneCircleItemType?,
      useGemsForAdventures: Boolean,
      maxAttackPlayerLevel: Int?,
      prioritizeGold: Boolean,
      autoRunes: Boolean,
      attributeToUpgrade: AttributeType?,
      autoAttributes: Boolean,
      maxDifficulty: Difficulty
  ) {
    while (true) {
      try {
        logger.debug { "Player info:" }
        val currentPlayerInfo = tService.getCurrentPlayerInfo()
        logger.debug { currentPlayerInfo }

        runAdventureChecks(
            playerGemsCount = currentPlayerInfo.gems,
            useGemsForAdventures = useGemsForAdventures,
            prioritizeGold = prioritizeGold,
            maxDifficulty = maxDifficulty)
        runAttackChecks(maxAttackPlayerLevel)
        runArcaneCircleChecks(
            currentPlayerInfo = currentPlayerInfo,
            runeToUpgrade = runeToUpgrade,
            autoRunes = autoRunes)
        runAttributeUpgradeChecks(
            currentPlayerInfo = currentPlayerInfo,
            attributeToUpgrade = attributeToUpgrade,
            autoAttributes = autoAttributes)
        logger.debug {}
      } catch (ex: Exception) {
        if (ex.message != null && ex.message!!.contains("503 Service Unavailable")) {
          logger.warn { "Service unavailable, waiting..." }
          delay(120_000)
        } else if (ex.message != null &&
            ex.message!!.contains("java.net.SocketException: Connection reset")) {
          logger.warn { "Connection reset, waiting..." }
          delay(120_000)
        } else {
          logger.error { "Something went wrong: $ex ${ex.message}" }
        }
      }

      delay(30_000)
    }
  }

  private suspend fun runAdventureChecks(
      playerGemsCount: Int,
      useGemsForAdventures: Boolean,
      prioritizeGold: Boolean,
      maxDifficulty: Difficulty
  ) {
    logger.debug {}
    logger.debug { "Adventure check..." }
    val adventureResponse = tService.getAdventures()
    if (adventureResponse == null) {
      logger.debug { "In work..." }
      return
    }
    val (runningTime, adventures, adventureResult) = adventureResponse
    if (runningTime != null) {
      logger.debug { "Adventure in progress for another ${runningTime}s" }
      return
    }
    if (adventureResult != null) {
      logger.info { "Adventure finished!" }
      logger.info { adventureResult }
      return
    }

    if (adventures == null) {
      logger.warn { "something went wrong..." }
      return
    }
    if ((useGemsForAdventures && playerGemsCount > 0) ||
        adventures.adventuresMadeToday < adventures.freeAdventuresPerDay) {
      val bestAdventure =
          pickNextAdventure(
              adventures = adventures,
              prioritizeGold = prioritizeGold,
              maxDifficulty = maxDifficulty)
      adventures.toPrettyString().forEach { logger.info { it } }
      logger.info { "Best adventure: $bestAdventure" }
      tService.postAdventure(bestAdventure.questId)
    }
  }

  private fun pickNextAdventure(
      adventures: Adventures,
      prioritizeGold: Boolean,
      maxDifficulty: Difficulty
  ): Adventure {
    val sortedByPriority =
        adventures.adventures.sortedByDescending {
          if (prioritizeGold) {
            it.gold
          } else {
            it.experience
          }
        }

    return sortedByPriority.first { it.difficulty.value <= maxDifficulty.value }
  }

  private suspend fun runAttackChecks(maxAttackPlayerLevel: Int?) {
    while (true) {
      delay(1_000)
      logger.debug {}
      logger.debug { "Attack check..." }
      val randomEnemyResponse = tService.getRandomEnemy()
      if (randomEnemyResponse.reattackCountdown > 0) {
        logger.debug { "Not yet available for ${randomEnemyResponse.reattackCountdown}s" }
        return
      }
      if (attackTarget != null) {
        val gold = attackAndGetGold(attackTarget!!)
        // player name not found
        if (gold == null) {
          attackTarget = null
          stolenGold = 0
          continue
        }
        stolenGold += gold
        if (stolenGold > 50) {
          logger.info { "Currently stolen from player $stolenGold" }
        }
        if (gold < 50) {
          attackTarget = null
          stolenGold = 0
        }
        return
      }

      if (randomEnemyResponse.otherPlayerInfo.guildName == "NarcoS") {
        continue
      }
      if (maxAttackPlayerLevel != null &&
          randomEnemyResponse.otherPlayerInfo.level > maxAttackPlayerLevel) {
        continue
      }
      attackTarget = randomEnemyResponse.otherPlayerInfo
    }
  }

  private suspend fun attackAndGetGold(target: OtherPlayerInfo): Int? {
    logger.info { "Attack target: $target" }
    val attackResult = tService.attackPlayer(target.name)
    logger.info { attackResult }
    return attackResult?.robbedGold
  }

  private suspend fun runAttributeUpgradeChecks(
      currentPlayerInfo: CurrentPlayerInfo,
      attributeToUpgrade: AttributeType?,
      autoAttributes: Boolean
  ) {
    if (attributeToUpgrade == null && !autoAttributes) {
      return
    }
    logger.debug {}
    logger.debug { "Attribute upgrade check..." }
    var playerGold = currentPlayerInfo.gold

    do {
      val didUpgrade = pickAndUpgradeAttribute(playerGold, attributeToUpgrade, autoAttributes)
      playerGold = tService.getCurrentPlayerInfo().gold
    } while (didUpgrade)
  }

  private suspend fun runArcaneCircleChecks(
      currentPlayerInfo: CurrentPlayerInfo,
      runeToUpgrade: ArcaneCircleItemType?,
      autoRunes: Boolean
  ) {
    if (runeToUpgrade == null && !autoRunes) {
      return
    }
    logger.debug {}
    logger.debug { "Arcane Circle check..." }
    var playerGold = currentPlayerInfo.gold

    do {
      val didUpgrade = pickAndUpgradeRune(playerGold, runeToUpgrade, autoRunes)
      playerGold = tService.getCurrentPlayerInfo().gold
    } while (didUpgrade)
  }

  private suspend fun pickAndUpgradeRune(
      playerGold: Int,
      runeToUpgrade: ArcaneCircleItemType?,
      autoRunes: Boolean
  ): Boolean {
    val arcaneCircleItems = tService.getArcaneCircle()
    arcaneCircleItems.forEach { logger.trace { it } }

    val targetRune =
        if (runeToUpgrade != null) {
          arcaneCircleItems.first { it.arcaneCircleItemType == runeToUpgrade }
        } else if (autoRunes) {
          getNextRuneToUpgrade(arcaneCircleItems)
        } else {
          null
        }
    if (targetRune == null) {
      logger.debug { "No rune was found to upgrade" }
      return false
    }

    if (targetRune.goldPrice < playerGold) {
      logger.info { "Upgrading rune $targetRune" }
      tService.upgradeArcaneCircleNode(targetRune.arcaneCircleItemType.id)
      return true
    } else {
      logger.debug { "Not enough gold to upgrade $targetRune" }
      return false
    }
  }

  private fun getNextRuneToUpgrade(arcaneCircleItems: List<ArcaneCircleItem>): ArcaneCircleItem {
    val skullLevel =
        arcaneCircleItems
            .first { it.arcaneCircleItemType == ArcaneCircleItemType.DEMON_SKULL }
            .level
    logger.debug { "Current skull level: $skullLevel" }
    orderedRunes.forEach { rune ->
      val targetLevel =
          when {
            rune == ArcaneCircleItemType.JADE -> (skullLevel + 2) * 100
            rune.name.startsWith("RUNE") -> (skullLevel + 1) * 10
            rune == ArcaneCircleItemType.DEMON_SKULL -> skullLevel + 1
            else -> (skullLevel + 1) * 100
          }
      val currentRune = arcaneCircleItems.first { it.arcaneCircleItemType == rune }
      if (currentRune.level < targetLevel) {
        return currentRune
      }
    }
    throw RuntimeException("This should never be reached")
  }

  private suspend fun pickAndUpgradeAttribute(
      playerGold: Int,
      attributeToUpgrade: AttributeType?,
      autoAttributes: Boolean
  ): Boolean {
    val attributes = tService.geUserAttributes().userAttributes
    attributes.forEach { logger.trace { it } }

    val targetAttribute =
        if (attributeToUpgrade != null) {
          attributeToUpgrade
        } else if (autoAttributes) {
          getNextAttributeToUpgrade(attributes)
        } else {
          null
        }

    if (targetAttribute == null) {
      logger.debug { "No attribute was found to upgrade" }
      return false
    }

    val attribute = attributes.getValue(targetAttribute)
    if (attribute.attributeCost < playerGold) {
      logger.info { "Upgrading attribute $attribute" }
      tService.upgradeAttribute(targetAttribute)
      return true
    } else {
      logger.debug { "Not enough gold to upgrade $attribute" }
      return false
    }
  }

  private fun getNextAttributeToUpgrade(
      attributes: Map<AttributeType, UserAttribute>
  ): AttributeType {
    val cheapest = attributes.minBy { it.value.attributeCost }
    if (cheapest.value.attributeCost < 2000) {
      return cheapest.key
    }
    val intPrice = attributes.getValue(AttributeType.INT).attributeCost
    if (cheapest.value.attributeCost * 2 < intPrice) {
      return cheapest.key
    }
    return AttributeType.INT
  }
}

private val orderedRunes =
    listOf(
        ArcaneCircleItemType.JADE,
        ArcaneCircleItemType.EMERALD,
        ArcaneCircleItemType.AMETHYST,
        ArcaneCircleItemType.DIAMOND,
        ArcaneCircleItemType.TIGERS_EYE,
        ArcaneCircleItemType.RUNE_OF_GLORY,
        ArcaneCircleItemType.AMBER,
        ArcaneCircleItemType.RUBY,
        ArcaneCircleItemType.TOPAZ,
        ArcaneCircleItemType.SAPPHIRE,
        ArcaneCircleItemType.AQUAMARINE,
        ArcaneCircleItemType.RUNE_OF_NEGOTIATION,
        ArcaneCircleItemType.RUNE_OF_WISDOM,
        ArcaneCircleItemType.RUNE_OF_DILIGENCE,
        ArcaneCircleItemType.RUNE_OF_COURAGE,
        ArcaneCircleItemType.DEMON_SKULL)
