/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.traderservices.wiring

import java.net.InetAddress
import java.nio.charset.StandardCharsets

import ch.qos.logback.classic.spi.{ILoggingEvent, ThrowableProxyUtil}
import ch.qos.logback.core.encoder.EncoderBase
import com.fasterxml.jackson.core.json.JsonWriteFeature.ESCAPE_NON_ASCII
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.lang3.time.FastDateFormat
import com.typesafe.config.ConfigFactory

import scala.util.{Success, Try}
import scala.collection.JavaConverters._
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import play.api.Logger

class JsonEncoder extends EncoderBase[ILoggingEvent] {

  val mapper = new ObjectMapper().configure(ESCAPE_NON_ASCII.mappedFeature(), true)

  lazy val appName: String = Try(ConfigFactory.load().getString("appName")) match {
    case Success(name) => name
    case _             => "APP NAME NOT SET"
  }

  private val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSZZ"

  private lazy val dateFormat = {
    val dformat = Try(ConfigFactory.load().getString("logger.json.dateformat")) match {
      case Success(date) => date
      case _             => DATE_FORMAT
    }
    FastDateFormat.getInstance(dformat)
  }

  lazy val jsonDataPrefix: Seq[String] =
    Try(ConfigFactory.load().getString("logger.json.data.prefix")) match {
      case Success(prefix) if prefix.nonEmpty =>
        prefix.split('.').map(_.trim()).filter(_.nonEmpty)
      case _ => throw new Exception("Missing config property [logger.json.data.prefix]")
    }

  override def encode(event: ILoggingEvent): Array[Byte] = {
    val eventNode = mapper.createObjectNode

    eventNode.put("app", appName)
    eventNode.put("hostname", InetAddress.getLocalHost.getHostName)
    eventNode.put("timestamp", dateFormat.format(event.getTimeStamp))

    decodeMessage(eventNode, event.getFormattedMessage)

    Option(event.getThrowableProxy).map(p => eventNode.put("exception", ThrowableProxyUtil.asString(p)))

    eventNode.put("logger", event.getLoggerName)
    eventNode.put("thread", event.getThreadName)
    eventNode.put("level", event.getLevel.toString)

    Option(getContext).foreach(c =>
      c.getCopyOfPropertyMap.asScala foreach { case (k, v) => eventNode.put(k.toLowerCase, v) }
    )
    event.getMDCPropertyMap.asScala foreach { case (k, v) => eventNode.put(k.toLowerCase, v) }

    s"${mapper.writeValueAsString(eventNode)}${System.lineSeparator()}".getBytes(StandardCharsets.UTF_8)
  }

  def decodeMessage(eventNode: ObjectNode, message: String): Unit = {
    eventNode.put("message", message.drop(4))
    if (message.startsWith("json{") && jsonDataPrefix.nonEmpty) {
      val messageNode: JsonNode = mapper.readTree(message.drop(4))
      try {
        val dataNode = if (jsonDataPrefix.size > 1) {
          val intermediaryDataNodes: Seq[(String, ObjectNode)] =
            jsonDataPrefix.init.map(p => (p, mapper.createObjectNode))
          intermediaryDataNodes.foldLeft[ObjectNode](eventNode) { case (parent, (name, child)) =>
            parent.replace(name, child); child
          }
        } else eventNode
        dataNode.replace(jsonDataPrefix.last, messageNode)
      } catch {
        case e: Exception =>
          Logger(getClass).error(e.getMessage)
      }
    }
  }

  override def footerBytes(): Array[Byte] =
    System.lineSeparator().getBytes(StandardCharsets.UTF_8)

  override def headerBytes(): Array[Byte] =
    System.lineSeparator().getBytes(StandardCharsets.UTF_8)

}
