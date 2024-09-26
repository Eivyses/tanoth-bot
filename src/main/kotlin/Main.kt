package org.example

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import org.slf4j.LoggerFactory

suspend fun main(args: Array<String>) {
  disableKtorLogging()
  val argsMap = args.toList().chunked(2).associate { it[0] to it[1] }
  val sessionId = argsMap["-sessionId"]
  val rune = argsMap["-rune"]
  val useGemsForAdventures = (argsMap["-useGemsForAdventures"] ?: "false").toBoolean()

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

  val gameService = GameService(sessionId)
  gameService.run(runeToUpgrade = runeToUpgrade, useGemsForAdventures = useGemsForAdventures)
}

fun disableKtorLogging() {
  val ktorLogger = LoggerFactory.getLogger("io.ktor") as Logger
  ktorLogger.level = Level.OFF
}
