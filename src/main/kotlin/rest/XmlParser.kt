package org.example.rest

import io.ktor.client.call.*
import io.ktor.client.statement.*
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document
import org.w3c.dom.Element

suspend fun HttpResponse.asXml(): Document {
  val factory = DocumentBuilderFactory.newInstance()
  val builder = factory.newDocumentBuilder()
  val body = this.body<String>()
  body.byteInputStream().use {
    val document = builder.parse(it)
    return document
  }
}

fun String.asXml(): Document {
  val factory = DocumentBuilderFactory.newInstance()
  val builder = factory.newDocumentBuilder()
  val body = this.clean()
  body.byteInputStream().use {
    val document = builder.parse(it)
    return document
  }
}

private fun String.clean(): String {
  return this.replace(Regex("[^\\x20-\\x7E]"), "")
}

inline fun <reified T> Document.getValueFromXml(key: String): T? {
  val members = this.getElementsByTagName("member")
  for (i in 0 until members.length) {
    val member = members.item(i) as Element
    val name = member.getElementsByTagName("name").item(0).textContent
    if (name == key) {
      val rawValue = getValueFromMember(member)
      val value =
          when (T::class) {
            Int::class -> rawValue.toInt() as T
            String::class -> rawValue as T
            Boolean::class -> (rawValue.toInt() == 1) as T
            else -> throw IllegalStateException("No generic type")
          }
      return value
    }
  }
  return null
}

inline fun <reified T> Document.getInnerValueFromXml(outerKey: String, key: String): T? {
  val members = this.getElementsByTagName("member")
  for (i in 0 until members.length) {
    val member = members.item(i) as Element
    val name = member.getElementsByTagName("name").item(0).textContent
    if (name == outerKey) {
      val innerMembers = member.getElementsByTagName("member")
      for (j in 0 until innerMembers.length) {
        val innerMember = innerMembers.item(j) as Element
        val innerName = innerMember.getElementsByTagName("name").item(0).textContent
        if (innerName == key) {
          val rawValue = getValueFromMember(innerMember)
          val value =
              when (T::class) {
                Int::class -> rawValue.toInt() as T
                String::class -> rawValue as T
                Boolean::class -> (rawValue.toInt() == 1) as T
                else -> throw IllegalStateException("No generic type")
              }
          return value
        }
      }
    }
  }
  return null
}

inline fun <reified T> Document.getValuesFromXml(key: String): List<T> {
  val list = mutableListOf<T>()
  val members = this.getElementsByTagName("member")
  for (i in 0 until members.length) {
    val member = members.item(i) as Element
    val name = member.getElementsByTagName("name").item(0).textContent
    if (name == key) {
      val rawValue = getValueFromMember(member)
      val value =
          when (T::class) {
            Int::class -> rawValue.toInt() as T
            String::class -> rawValue as T
            Boolean::class -> (rawValue.toInt() == 1) as T
            else -> throw IllegalStateException("No generic type")
          }
      list.add(value)
    }
  }
  return list
}

fun getValueFromMember(member: Element): String {
  val valueNode = member.getElementsByTagName("value").item(0)
  val childNodes = valueNode.childNodes
  var rawValue = ""
  for (j in 0 until childNodes.length) {
    val node = childNodes.item(j)
    when (node.nodeName) {
      "i4",
      "boolean",
      "string" -> rawValue = node.textContent
    }
  }
  return rawValue
}
