package uk.gov.hmrc.traderservices.stubs

import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.libs.json.Json
import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.traderservices.services.TraderServicesEvent.TraderServicesEvent
import uk.gov.hmrc.traderservices.support.WireMockSupport

trait DataStreamStubs extends Eventually {
  me: WireMockSupport =>

  override implicit val patienceConfig =
    PatienceConfig(scaled(Span(5, Seconds)), scaled(Span(500, Millis)))

  def verifyAuditRequestSent(
    count: Int,
    event: TraderServicesEvent,
    tags: Map[String, String] = Map.empty,
    detail: Map[String, String] = Map.empty
  ): Unit =
    eventually {
      verify(
        count,
        postRequestedFor(urlPathEqualTo(auditUrl))
          .withRequestBody(similarToJson(s"""{
          |  "auditSource": "trader-services",
          |  "auditType": "$event",
          |  "tags": ${Json.toJson(tags)},
          |  "detail": ${Json.toJson(detail)}
          |}"""))
      )
    }

  def verifyAuditRequestNotSent(event: TraderServicesEvent): Unit =
    eventually {
      verify(
        0,
        postRequestedFor(urlPathEqualTo(auditUrl))
          .withRequestBody(similarToJson(s"""{
          |  "auditSource": "trader-services",
          |  "auditType": "$event"
          |}"""))
      )
    }

  def givenAuditConnector(): Unit = {
    stubFor(post(urlPathEqualTo(auditUrl)).willReturn(aResponse().withStatus(204)))
    stubFor(post(urlPathEqualTo(auditUrl + "/merged")).willReturn(aResponse().withStatus(204)))
  }

  private def auditUrl = "/write/audit"

  private def similarToJson(value: String) = equalToJson(value.stripMargin, true, true)

}
