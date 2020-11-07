package uk.gov.hmrc.traderservices.controllers

import java.time.LocalDateTime
import java.time.LocalDate

import org.scalatest.Suite
import org.scalatestplus.play.ServerProvider
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import uk.gov.hmrc.traderservices.models._
import uk.gov.hmrc.traderservices.stubs._
import uk.gov.hmrc.traderservices.support.ServerBaseISpec
import java.time.LocalTime
import uk.gov.hmrc.traderservices.support.JsonMatchers
import play.api.libs.json.JsObject
import java.{util => ju}

class TraderServicesRouteOneISpec extends ServerBaseISpec with AuthStubs with CreateCaseStubs with JsonMatchers {

  this: Suite with ServerProvider =>

  val url = s"http://localhost:$port"

  val dateTime = LocalDateTime.now()

  val wsClient = app.injector.instanceOf[WSClient]

  "TraderServicesRouteOneController" when {
    "POST /create-case" should {
      "return CaseID as a result if successful PEGA API call" in {

        val createCaseRequest = TraderServicesCreateCaseRequest(
          DeclarationDetails(EPU(2), EntryNumber("A23456A"), LocalDate.parse("2020-09-02")),
          ImportQuestions(
            requestType = ImportRequestType.New,
            routeType = ImportRouteType.Route1,
            priorityGoods = None,
            hasALVS = false,
            freightType = ImportFreightType.Maritime,
            vesselDetails = Some(
              VesselDetails(
                vesselName = Some("Vessel Name"),
                dateOfArrival = Some(LocalDate.of(2020, 10, 29)),
                timeOfArrival = Some(LocalTime.of(23, 45, 0))
              )
            ),
            contactInfo = ImportContactInfo(
              contactName = "Full Name",
              contactNumber = Some("07123456789"),
              contactEmail = "sampelname@gmail.com"
            )
          ),
          Seq(),
          "GB123456789012345"
        )

        givenAuthorisedAsValidTrader("xyz")
        givenPegaCreateCaseRequestSucceeds()

        val correlationId = ju.UUID.randomUUID().toString()

        val result = wsClient
          .url(s"$url/create-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(createCaseRequest))
          .futureValue

        result.status shouldBe 201
        result.json.as[JsObject] should (
          haveProperty[String]("correlationId", be(correlationId)) and
            haveProperty[String]("result", be("PCE201103470D2CC8K0NH3"))
        )
      }
    }
  }
}
