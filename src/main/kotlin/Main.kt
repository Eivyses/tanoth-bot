package org.example

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.github.oshai.kotlinlogging.KotlinLogging
import org.slf4j.LoggerFactory

private enum class Args(val value: String, val descriptions: String) {
  SESSION_ID("-sessionId", "Currently active session id"),
  RUNE("-rune", "Rune to upgrade, optional"),
  USE_GEMS_FOR_ADVENTURES("-useGemsForAdventures", "Whether to use gems or not, default false"),
  MAX_ATTACK_PLAYER_LEVEL(
      "-maxAttackPlayerLevel",
      "Maximum player level for attacking randomly. If too low level is selected bot might get stuck in infinite loop on higher positions as it will not find low level players, optional"),
  GF_TOKEN("-gfToken", "GameForge token, used to automatically refresh session"),
  PRIORITIZE_GOLD(
      "-prioritizeGold",
      "Whether to prioritize adventures that give more gold or not, if not will prioritize xp, default false"),
  AUTO_RUNES(
      "-autoRunes",
      "Automatically upgrade runes in predefined order so that skull can be upgraded optimally, default false")
}

private val logger = KotlinLogging.logger {}

suspend fun main(args: Array<String>) {
  disableKtorLogging()
  disableChromiumLogging()

  if (args.isEmpty()) {
    logger.error { "Please provide at least ${Args.SESSION_ID.value}. Other options are:" }
    Args.entries.forEach { logger.error { "  ${it.value} - ${it.descriptions}" } }
    return
  }

  val argsMap = args.toList().chunked(2).associate { it[0] to it[1] }
  var sessionId = argsMap[Args.SESSION_ID.value]
  val rune = argsMap[Args.RUNE.value]
  val useGemsForAdventures = argsMap[Args.USE_GEMS_FOR_ADVENTURES.value]?.toBoolean() ?: false
  val maxAttackPlayerLevel = argsMap[Args.MAX_ATTACK_PLAYER_LEVEL.value]?.toInt()
  val gfToken = argsMap[Args.GF_TOKEN.value]
  val prioritizeGold = argsMap[Args.PRIORITIZE_GOLD.value]?.toBoolean() ?: false
  val autoRunes = argsMap[Args.AUTO_RUNES.value]?.toBoolean() ?: false

  if (sessionId == null && gfToken == null) {
    logger.error { "No sessionId or gfToken provided" }
    return
  }

  if (autoRunes && rune != null) {
    logger.error { "Cannot use both ${Args.AUTO_RUNES.value} and ${Args.RUNE.value} together" }
    return
  }

  if (sessionId == null) {
    sessionId = TBrowser().getNewSessionId(gfToken!!)
  }

  val runeToUpgrade = rune?.let { ArcaneCircleItemType.valueOf(it) }

  logger.info { "Preparing app for usage..." }
  logger.info { "Using sessionId $sessionId" }
  logger.info { "Can use gems for adventures: $useGemsForAdventures" }
  logger.info { "Prioritize gold: $prioritizeGold" }
  logger.info { "Auto upgrade runes: $autoRunes" }
  runeToUpgrade?.let { logger.info { "Using rune $it to upgrade" } }
  maxAttackPlayerLevel?.let { logger.info { "Attacking players that are max $it level" } }
  gfToken?.let { logger.info { "Using provided gf token $it" } }

  val gameService = GameService(sessionId = sessionId, gfToken = gfToken)
  gameService.run(
      runeToUpgrade = runeToUpgrade,
      useGemsForAdventures = useGemsForAdventures,
      maxAttackPlayerLevel = maxAttackPlayerLevel,
      prioritizeGold = prioritizeGold,
      autoRunes = autoRunes)
}

fun disableChromiumLogging() {
  System.setProperty("webdriver.chrome.silentOutput", "true")
  val seleniumLogger = LoggerFactory.getLogger("org.openqa.selenium") as Logger
  seleniumLogger.level = Level.OFF
}

fun disableKtorLogging() {
  val ktorLogger = LoggerFactory.getLogger("io.ktor") as Logger
  ktorLogger.level = Level.OFF
}
