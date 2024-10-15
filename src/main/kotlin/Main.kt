package org.example

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.github.oshai.kotlinlogging.KotlinLogging
import org.slf4j.LoggerFactory

private enum class Args(val value: String, val descriptions: String) {
  SESSION_ID(
      "--session-id",
      "<string> Currently active session id, --session-id or --gf-token must be provided"),
  RUNE("--rune", "<string> Rune to upgrade, optional"),
  USE_GEMS_FOR_ADVENTURES("--use-gems-for-adventures", "Use gems when doing adventures, optional"),
  MAX_ATTACK_PLAYER_LEVEL(
      "--max-attack-player-level",
      "<int> Maximum player level for attacking randomly. If too low level is selected bot might get stuck in infinite loop on higher positions as it will not find low level players, optional"),
  GF_TOKEN(
      "--gf-token",
      "<string> GameForge token, used to automatically refresh session, --session-id or --gf-token must be provided"),
  PRIORITIZE_GOLD(
      "--prioritize-gold",
      "Whether to prioritize adventures that give more gold or not, if not will prioritize xp, optional"),
  AUTO_RUNES(
      "--auto-runes",
      "Automatically upgrade runes in predefined order so that skull can be upgraded optimally, optional")
}

private val logger = KotlinLogging.logger {}

suspend fun main(args: Array<String>) {
  disableKtorLogging()
  disableChromiumLogging()

  if (args.isEmpty()) {
    logger.error {
      "Please provide at least ${Args.SESSION_ID.value} or ${Args.GF_TOKEN.value}. Other options are:"
    }
    Args.entries.forEach { logger.error { "  ${it.value} - ${it.descriptions}" } }
    return
  }
  val argsMap = parseArgs(args)
  val validArgs = Args.entries.map { it.value }.toSet()
  argsMap.keys.forEach { key ->
    if (!validArgs.contains(key)) {
      logger.error { "Invalid argument $key provided" }
      return
    }
  }

  var sessionId = argsMap[Args.SESSION_ID.value]
  val rune = argsMap[Args.RUNE.value]
  val useGemsForAdventures = Args.USE_GEMS_FOR_ADVENTURES.value in argsMap
  val maxAttackPlayerLevel = argsMap[Args.MAX_ATTACK_PLAYER_LEVEL.value]?.toInt()
  val gfToken = argsMap[Args.GF_TOKEN.value]
  val prioritizeGold = Args.PRIORITIZE_GOLD.value in argsMap
  val autoRunes = Args.AUTO_RUNES.value in argsMap

  if (sessionId == null && gfToken == null) {
    logger.error { "No sessionId or gfToken provided" }
    return
  }

  if (autoRunes && rune != null) {
    logger.error { "Cannot use both ${Args.AUTO_RUNES.value} and ${Args.RUNE.value} together" }
    return
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

  if (sessionId == null) {
    logger.info { "Session id not provided, retrieving..." }
    sessionId = TBrowser().getNewSessionId(gfToken!!)
    logger.info { "New session id: $sessionId" }
  }

  val gameService = GameService(sessionId = sessionId, gfToken = gfToken)
  gameService.run(
      runeToUpgrade = runeToUpgrade,
      useGemsForAdventures = useGemsForAdventures,
      maxAttackPlayerLevel = maxAttackPlayerLevel,
      prioritizeGold = prioritizeGold,
      autoRunes = autoRunes)
}

private fun parseArgs(args: Array<String>): Map<String, String?> {
  val parsedArgs = mutableMapOf<String, String?>()
  var i = 0
  while (i < args.size) {
    val arg = args[i]
    if (arg.startsWith("--")) {
      val value =
          if (i + 1 < args.size && !args[i + 1].startsWith("--")) {
            args[i + 1]
          } else {
            null
          }
      parsedArgs[arg] = value
      i += if (value == null) 1 else 2
    } else {
      i++
    }
  }
  return parsedArgs
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
