package org.example

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import org.slf4j.LoggerFactory

private enum class Args(val value: String) {
  SESSION_ID("-sessionId"),
  RUNE("-rune"),
  USE_GEMS_FOR_ADVENTURES("-useGemsForAdventures"),
  MAX_ATTACK_PLAYER_LEVEL("-maxAttackPlayerLevel")
}

suspend fun main(args: Array<String>) {
  disableKtorLogging()
  if (args.isEmpty()) {
    println("Please provide at least ${Args.SESSION_ID.value}. Other options are:")
    Args.entries.forEach { println("  ${it.value}") }
    return
  }

  val argsMap = args.toList().chunked(2).associate { it[0] to it[1] }
  val sessionId = argsMap[Args.SESSION_ID.value]
  val rune = argsMap[Args.RUNE.value]
  val useGemsForAdventures = argsMap[Args.USE_GEMS_FOR_ADVENTURES.value]?.toBoolean() ?: false
  val maxAttackPlayerLevel = argsMap[Args.MAX_ATTACK_PLAYER_LEVEL.value]?.toInt()

  if (sessionId == null) {
    throw RuntimeException("No sessionId provided")
  }

  val runeToUpgrade = rune?.let { ArcaneCircleItemType.valueOf(it) }

  println("Preparing app for usage...")
  println("Using sessionId $sessionId")
  println("Can use gems for adventures: $useGemsForAdventures")
  if (runeToUpgrade != null) {
    println("Using rune $runeToUpgrade to upgrade")
  }
  if (maxAttackPlayerLevel != null) {
    println("Attacking players that are max $maxAttackPlayerLevel level")
  }

  val gameService = GameService(sessionId)
  gameService.run(
      runeToUpgrade = runeToUpgrade,
      useGemsForAdventures = useGemsForAdventures,
      maxAttackPlayerLevel = maxAttackPlayerLevel)
}

fun disableKtorLogging() {
  val ktorLogger = LoggerFactory.getLogger("io.ktor") as Logger
  ktorLogger.level = Level.OFF
}
