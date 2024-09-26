package org.example

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.example.rest.*
import org.w3c.dom.Document

class TService(private val sessionId: String) {

  private val client = HttpClient(CIO)
  private val postUrl = "https://s1-en.tanoth.gameforge.com/xmlrpc"

  suspend fun getAdventures(): AdventureResponse? {
    return postBodyAndParseResult(
        createPostBody(methodName = "GetAdventures", sessionId = sessionId)) {
          it.parseAsAdventureResponse()
        }
  }

  suspend fun postAdventure(questId: Int) {
    postBody(
        createPostBody(
            methodName = "StartAdventure", sessionId = sessionId, extraIntParam = questId))
  }

  suspend fun getRandomEnemy(): RandomEnemyResponse {
    return postBodyAndParseResult(
        createPostBody(methodName = "GetPvpData", sessionId = sessionId)) {
          it.parseAsRandomEnemy()
        }
  }

  suspend fun attackPlayer(playerName: String): AttackResult {
    return postBodyAndParseResult(
        createPostBody(
            methodName = "Fight", sessionId = sessionId, extraStringParam = playerName)) {
          it.parseAsAttackResult()
        }
  }

  suspend fun getArcaneCircle(): List<ArcaneCircleItem> {
    return postBodyAndParseResult(
        createPostBody(methodName = "EvocationCircle_getCircle", sessionId = sessionId)) {
          it.parseAsArcaneCircle()
        }
  }

  suspend fun upgradeArcaneCircleNode(nodeId: Int) {
    postBody(
        createPostBody(
            methodName = "EvocationCircle_buyNode",
            sessionId = sessionId,
            extraStringParam = "gold",
            extraIntParam = nodeId))
  }

  suspend fun getCurrentPlayerInfo(): CurrentPlayerInfo {
    return postBodyAndParseResult(
        createPostBody(methodName = "MiniUpdate", sessionId = sessionId)) {
          it.parseAsCurrentPlayerInfo()
        }
  }

  private suspend fun postBody(body: String): String {
    val response =
        client.post(postUrl) {
          contentType(ContentType.Application.Xml)
          setBody(body)
        }
    return response.readBytes().decodeToString()
  }

  private suspend fun <T> postBodyAndParseResult(body: String, resultParser: (Document) -> T): T {
    val rawXml = postBody(body)
    try {
      val xml = rawXml.asXml()
      val errorText = xml.getValueFromXml<String>("error")
      if (errorText != null) {
        throw RuntimeException("Failed to read response, error $errorText")
      }
      return resultParser(xml)
    } catch (ex: Exception) {
      ex.printStackTrace()
      throw RuntimeException("Failed to read response for xml $rawXml")
    }
  }
}
