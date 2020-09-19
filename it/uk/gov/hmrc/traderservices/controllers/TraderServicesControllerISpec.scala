package uk.gov.hmrc.traderservices.controllers

import org.scalatest.Suite
import org.scalatestplus.play.ServerProvider
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.test.FakeRequest
import uk.gov.hmrc.traderservices.stubs.AuthStubs
import uk.gov.hmrc.traderservices.support.ServerBaseISpec

class TraderServicesControllerISpec extends ServerBaseISpec with AuthStubs {

  this: Suite with ServerProvider =>

  val url = s"http://localhost:$port/trader-services"

  val wsClient = app.injector.instanceOf[WSClient]

  def entity(): WSResponse =
    wsClient
      .url(s"$url/entities")
      .get()
      .futureValue

  "TraderServicesController" when {

    "GET /entities" should {
      "respond with some data" in {
        givenAuthorisedAsValidTrader("xyz")
        val result = entity()
        result.status shouldBe 200
        result.json shouldBe Json.obj("parameter1" -> "hello xyz")
      }
    }
  }
}
