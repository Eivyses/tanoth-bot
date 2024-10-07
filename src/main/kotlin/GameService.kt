package org.example

import kotlinx.coroutines.delay

class GameService(sessionId: String) {

  private val tService = TService(sessionId)
  private var attackTarget: OtherPlayerInfo? = null

  suspend fun run(
      runeToUpgrade: ArcaneCircleItemType?,
      useGemsForAdventures: Boolean,
      maxAttackPlayerLevel: Int?
  ) {
    while (true) {
      try {
        //      println("Player info:")
        val currentPlayerInfo = tService.getCurrentPlayerInfo()
        //      println(currentPlayerInfo)

        runAdventureChecks(
            playerGemsCount = currentPlayerInfo.gems, useGemsForAdventures = useGemsForAdventures)
        runAttackChecks(maxAttackPlayerLevel)
        if (runeToUpgrade != null) {
          runArcaneCircleChecks(currentPlayerInfo, runeToUpgrade)
        }
        //      println()
      } catch (ex: Exception) {
        if (ex.message != null && ex.message!!.contains("503 Service Unavailable")) {
          println("Service unavailable, waiting...")
          delay(120_000)
        } else if (ex.message != null &&
            ex.message!!.contains("java.net.SocketException: Connection reset")) {
          println("Connection reset, waiting...")
          delay(120_000)
        } else {
          throw ex
        }
      }

      delay(30_000)
    }
  }

  private suspend fun runAdventureChecks(playerGemsCount: Int, useGemsForAdventures: Boolean) {
    //    println()
    //    println("Adventure check...")
    val adventureResponse = tService.getAdventures()
    if (adventureResponse == null) {
      //      println("In work...")
      return
    }
    val (runningTime, adventures, adventureResult) = adventureResponse
    if (runningTime != null) {
      //      println("Adventure in progress for another ${runningTime}s")
      return
    }
    if (adventureResult != null) {
      println("Adventure finished!")
      println(adventureResult)
      return
    }

    if (adventures == null) {
      println("something went wrong...")
      return
    }
    if ((useGemsForAdventures && playerGemsCount > 0) ||
        adventures.adventuresMadeToday < adventures.freeAdventuresPerDay) {
      val bestAdventure = pickNextAdventure(adventures)
      println(adventures.toPrettyString())
      println("Best adventure: $bestAdventure")
      tService.postAdventure(bestAdventure.questId)
    }
  }

  private fun pickNextAdventure(adventures: Adventures): Adventure {
    val bestAdventure =
        adventures.adventures
            .sortedByDescending { it.experience }
            .first { it.difficulty != Difficulty.VERY_DIFFICULT }

    return bestAdventure
  }

  private suspend fun runAttackChecks(maxAttackPlayerLevel: Int?) {
    while (true) {
      delay(1_000)
      //    println()
      //    println("Attack check...")
      val randomEnemyResponse = tService.getRandomEnemy()
      if (randomEnemyResponse.reattackCountdown > 0) {
        //      println("Not yet available for ${countdown}s")
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
    println("Attack target: $target")
    val attackResult = tService.attackPlayer(target.name)
    println(attackResult)
    return attackResult?.robbedGold
  }

  private suspend fun runArcaneCircleChecks(
      currentPlayerInfo: CurrentPlayerInfo,
      runeToUpgrade: ArcaneCircleItemType
  ) {
    //    println()
    //    println("Arcane Circle check...")
    val arcaneCircleItems = tService.getArcaneCircle()
    //      arcaneCircleItems.forEach { println(it) }

    val targetRune = arcaneCircleItems.first { it.arcaneCircleItemType == runeToUpgrade }
    if (targetRune.goldPrice < currentPlayerInfo.gold) {
      println("Upgrading rune $targetRune")
      tService.upgradeArcaneCircleNode(targetRune.arcaneCircleItemType.id)
    } else {
      //      println("Not enough gold to upgrade $targetRune")
    }
  }
}
