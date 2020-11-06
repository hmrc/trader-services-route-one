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

class TraderServicesControllerISpec extends ServerBaseISpec with AuthStubs with CreateCaseStubs {

  this: Suite with ServerProvider =>

  val url = s"http://localhost:$port"

  val dateTime = LocalDateTime.now()

  val wsClient = app.injector.instanceOf[WSClient]

  "TraderServicesRouteOneController" when {

    "POST /create-case" should {
      "when import questions submitted will respond with case id for valid data" in {
        givenAuthorisedAsValidTrader("xyz")
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

        givenPegaCreateCaseRequestSucceeds()

        val result = wsClient
          .url(s"$url/create-case")
          .post(Json.toJson(createCaseRequest))
          .futureValue

        result.status shouldBe 200
        result.json shouldBe Json.obj(
          "CaseID"         -> "PCE201103470D2CC8K0NH3",
          "ProcessingDate" -> "2020-11-03T15:29:28.601Z",
          "Status"         -> "Success",
          "StatusText"     -> "Case created successfully"
        )
      }
    }
  }
}
