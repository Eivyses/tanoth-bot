package org.example

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay

private val logger = KotlinLogging.logger {}

class GameService(sessionId: String, gfToken: String?) {

  private val tService = TService(sessionId, gfToken)
  private var attackTarget: OtherPlayerInfo? = null

  suspend fun run(
      runeToUpgrade: ArcaneCircleItemType?,
      useGemsForAdventures: Boolean,
      maxAttackPlayerLevel: Int?,
      prioritizeGold: Boolean
  ) {
    while (true) {
      try {
        logger.debug { "Player info:" }
        val currentPlayerInfo = tService.getCurrentPlayerInfo()
        logger.debug { currentPlayerInfo }

        runAdventureChecks(
            playerGemsCount = currentPlayerInfo.gems,
            useGemsForAdventures = useGemsForAdventures,
            prioritizeGold = prioritizeGold)
        runAttackChecks(maxAttackPlayerLevel)
        if (runeToUpgrade != null) {
          runArcaneCircleChecks(currentPlayerInfo, runeToUpgrade)
        }
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
          throw ex
        }
      }

      delay(30_000)
    }
  }

  private suspend fun runAdventureChecks(
      playerGemsCount: Int,
      useGemsForAdventures: Boolean,
      prioritizeGold: Boolean
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
      logger.debug { "Adventure finished!" }
      logger.debug { adventureResult }
      return
    }

    if (adventures == null) {
      logger.warn { "something went wrong..." }
      return
    }
    if ((useGemsForAdventures && playerGemsCount > 0) ||
        adventures.adventuresMadeToday < adventures.freeAdventuresPerDay) {
      val bestAdventure =
          pickNextAdventure(adventures = adventures, prioritizeGold = prioritizeGold)
      logger.info { adventures.toPrettyString() }
      logger.info { "Best adventure: $bestAdventure" }
      tService.postAdventure(bestAdventure.questId)
    }
  }

  private fun pickNextAdventure(adventures: Adventures, prioritizeGold: Boolean): Adventure {
    return if (prioritizeGold) {
      adventures.adventures
          .sortedByDescending { it.gold }
          .first { it.difficulty != Difficulty.VERY_DIFFICULT }
    } else {
      adventures.adventures
          .sortedByDescending { it.experience }
          .first { it.difficulty != Difficulty.VERY_DIFFICULT }
    }
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
          continue
        }
        if (gold < 50) {
          attackTarget = null
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

  private suspend fun runArcaneCircleChecks(
      currentPlayerInfo: CurrentPlayerInfo,
      runeToUpgrade: ArcaneCircleItemType
  ) {
    logger.debug {}
    logger.debug { "Arcane Circle check..." }
    val arcaneCircleItems = tService.getArcaneCircle()
    arcaneCircleItems.forEach { logger.trace { it } }

    val targetRune = arcaneCircleItems.first { it.arcaneCircleItemType == runeToUpgrade }
    if (targetRune.goldPrice < currentPlayerInfo.gold) {
      logger.info { "Upgrading rune $targetRune" }
      tService.upgradeArcaneCircleNode(targetRune.arcaneCircleItemType.id)
    } else {
      logger.debug { "Not enough gold to upgrade $targetRune" }
    }
  }
}
