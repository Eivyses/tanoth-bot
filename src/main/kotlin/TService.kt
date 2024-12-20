package org.example

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.network.sockets.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import org.example.rest.*
import org.w3c.dom.Document
import java.io.IOException
import java.net.SocketException
import java.util.*

private val logger = KotlinLogging.logger {}

class TService(private var sessionId: String, private val gfToken: String?) {

  private val client = HttpClient(CIO)
  private val postUrl = "https://s1-en.tanoth.gameforge.com/xmlrpc"

  suspend fun getAdventures(): AdventureResponse? {
    return postBodyAndParseResult(
        createPostBody(methodName = "GetAdventures")) {
          it.parseAsAdventureResponse()
        }
  }

  suspend fun postAdventure(questId: Int) {
    postBody(
        createPostBody(
            methodName = "StartAdventure", extraIntParam = questId))
  }

  suspend fun getRandomEnemy(): RandomEnemyResponse {
    return postBodyAndParseResult(
        createPostBody(methodName = "GetPvpData")) {
          it.parseAsRandomEnemy()
        }
  }

  suspend fun attackPlayer(playerName: String): AttackResult? {
    return postBodyAndParseResult(
        createPostBody(
            methodName = "Fight", extraStringParam = playerName)) {
          it.parseAsAttackResult()
        }
  }

  suspend fun getArcaneCircle(): List<ArcaneCircleItem> {
    return postBodyAndParseResult(
        createPostBody(methodName = "EvocationCircle_getCircle")) {
          it.parseAsArcaneCircle()
        }
  }

  suspend fun upgradeArcaneCircleNode(nodeId: Int) {
    postBody(
        createPostBody(
            methodName = "EvocationCircle_buyNode",
            extraStringParam = "gold",
            extraIntParam = nodeId))
  }

  suspend fun upgradeAttribute(attribute: AttributeType) {
    postBody( 
        createPostBody(
            methodName = "RaiseAttribute",
            extraStringParam = attribute.value))
  }

  suspend fun getWorkData(): WorkDataResponse? {
    return postBodyAndParseResult(
        createPostBody(methodName = "GetWorkData")) {
          it.parseAsWorkDataResponse()
        }
  }

  suspend fun goToWork(hours: Int) {
    postBody(createPostBody(methodName = "StartWork", extraIntParam = hours))
  }

  suspend fun getCurrentPlayerInfo(): CurrentPlayerInfo {
    return postBodyAndParseResult(
        createPostBody(methodName = "MiniUpdate")) {
          it.parseAsCurrentPlayerInfo()
        }
  }

  suspend fun geUserAttributes(): UserAttributesResponse {
    return postBodyAndParseResult(
        createPostBody(methodName = "GetUserAttributes")) {
          it.parseAsUserAttributesResponse()
        }
  }

  suspend fun getMapDetails(): MapDetailsResponse? {
    return postBodyAndParseResult(createPostBody(methodName = "GetMapDetails")) {
      it.parseAsMapDetailsResponse()
    }
  }

  suspend fun doMap() {
    postBody(createPostBody(methodName = "StartIllusionCave"))
  }

  private suspend fun postBody(body: String): String {
    val gfTokenCookie = gfToken ?: UUID.randomUUID().toString()
    while (true) {
      try {
        val response =
            client.post(postUrl) {
              contentType(ContentType.Application.Xml)
              setBody(body)
              headers { append("Cookie", "gf-token-production=$gfTokenCookie") }
            }
        return response.readRawBytes().decodeToString()
      } catch (ex: ConnectTimeoutException) {
        logger.warn { "Timeout, retry..." }
        delay(10_000)
      } catch (ex: HttpRequestTimeoutException) {
        logger.warn { "Timeout, retry..." }
        delay(10_000)
      } catch (ex: SocketException) {
        if (ex.message!!.contains("Connection reset")) {
          logger.warn { "Timeout, retry..." }
          delay(10_000)
        } else {
          throw ex
        }
      } catch (ex: IOException) {
        if (ex.message!!.contains("Connection timed out")) {
          logger.warn { "Timeout, retry..." }
          delay(10_000)
        } else {
          throw ex
        }
      } catch (ex: Exception) {
        logger.error { "Unexpected error while posting $body" }
        throw ex
      }
    }
  }

  private suspend fun <T> postBodyAndParseResult(body: String, resultParser: (Document) -> T): T {
    val rawXml = postBody(body)
    try {
      val xml = rawXml.asXml()
      return resultParser(xml)
    } catch (ex: Exception) {
      if (rawXml.contains("no_valid_session") && gfToken != null) {
        logger.warn { "Session expired, refreshing..." }
        val newSessionId = TBrowser().getNewSessionId(gfToken)
        val newBody = body.replace(sessionId, newSessionId)
        sessionId = newSessionId
        logger.warn { "New session id: $sessionId" }
        return postBodyAndParseResult(body = newBody, resultParser = resultParser)
      } else {
        throw RuntimeException("Failed to read response for xml $rawXml", ex)
      }
    }
  }

  private fun createPostBody(
    methodName: String,
    extraIntParam: Int? = null,
    extraStringParam: String? = null
  ): String {
    val extraIntParamBody =
      if (extraIntParam != null)
        """
            <param>
                <value>
                    <int>$extraIntParam</int>
                </value>
            </param>
          """
          .trimIndent()
      else ""

    val extraStringParamBody =
      if (extraStringParam != null)
        """
            <param>
                <value>
                    <string>$extraStringParam</string>
                </value>
            </param>
          """
          .trimIndent()
      else ""

    return """
          <methodCall>
              <methodName>$methodName</methodName>
              <params>
                  <param>
                      <value>
                          <string>$sessionId</string>
                      </value>
                  </param>
                  $extraStringParamBody
                  $extraIntParamBody
              </params>
          </methodCall>
        """
      .trimIndent()
  }
}
