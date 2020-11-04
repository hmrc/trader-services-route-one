package uk.gov.hmrc.traderservices.controllers

import java.time.LocalDateTime
import java.time.LocalDate
import java.time.temporal.ChronoUnit

import org.scalatest.Suite
import org.scalatestplus.play.ServerProvider
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import uk.gov.hmrc.traderservices.models._
import uk.gov.hmrc.traderservices.stubs.{AuthStubs, CreateCaseStubs}
import uk.gov.hmrc.traderservices.support.ServerBaseISpec

class TraderServicesControllerISpec extends ServerBaseISpec with AuthStubs with CreateCaseStubs {

  this: Suite with ServerProvider =>

  val url = s"http://localhost:$port/trader-services"

  val dateTime = LocalDateTime.now()

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

    "POST /create-import-case" should {
      "respond with case id for valid data" in {
        givenAuthorisedAsValidTrader("xyz")
        val dateTimeOfArrival = dateTime.plusDays(1).truncatedTo(ChronoUnit.MINUTES)
        val createImportCaseRequest = CreateImportCaseRequest(
          DeclarationDetails(EPU(235), EntryNumber("111111X"), LocalDate.parse("2020-09-23")),
          ImportQuestions(
            requestType = ImportRequestType.New,
            routeType = ImportRouteType.Route6,
            priorityGoods = Some(ImportPriorityGoods.HighValueArt),
            hasPriorityGoods = true,
            hasALVS = false,
            freightType = Some(ImportFreightType.Air),
            vesselDetails = Some(
              VesselDetails(
                vesselName = Some("Foo Bar"),
                dateOfArrival = Some(dateTimeOfArrival.toLocalDate()),
                timeOfArrival = Some(dateTimeOfArrival.toLocalTime())
              )
            ),
            contactInfo = ImportContactInfo(
              contactName = "Full Name",
              contactNumber = Some("07777888999"),
              contactEmail = "someone@email.com"
            )
          )
        )
        val payload = CreateImportCaseRequest.formats.writes(createImportCaseRequest)

        givenImportCreateRequestWithVesselDetails(createImportCaseRequest, "xyz")

        val result = wsClient
          .url(s"$url/create-import-case")
          .post(payload)
          .futureValue
        result.status shouldBe 200
        result.json shouldBe Json.obj(
          "CaseID"         -> "Risk-363",
          "ProcessingDate" -> "2020-08-24T09:16:10.047Z",
          "Status"         -> "Success",
          "StatusText"     -> "Case created successfully"
        )
      }
    }
  }
}
