package uk.gov.hmrc.traderservices.controllers

import java.time.LocalDateTime
import java.time.LocalDate
import java.time.temporal.ChronoUnit

import org.scalatest.Suite
import org.scalatestplus.play.ServerProvider
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import uk.gov.hmrc.traderservices.models._
import uk.gov.hmrc.traderservices.stubs.{AuthStubs, CreateCaseStubs}
import uk.gov.hmrc.traderservices.support.ServerBaseISpec

class TraderServicesControllerISpec extends ServerBaseISpec with AuthStubs with CreateCaseStubs {

  this: Suite with ServerProvider =>

  val url = s"http://localhost:$port"

  val dateTime = LocalDateTime.now()

  val wsClient = app.injector.instanceOf[WSClient]

  "TraderServicesRouteOneController" when {

    "POST /create-case" should {
      "when import questions submitted will respond with case id for valid data" in {
        givenAuthorisedAsValidTrader("xyz")
        val dateTimeOfArrival = dateTime.plusDays(1).truncatedTo(ChronoUnit.MINUTES)
        val createImportCaseRequest = TraderServicesCreateCaseRequest(
          DeclarationDetails(EPU(235), EntryNumber("111111X"), LocalDate.parse("2020-09-23")),
          ImportQuestions(
            requestType = Some(ImportRequestType.New),
            routeType = Some(ImportRouteType.Route6),
            priorityGoods = Some(ImportPriorityGoods.HighValueArt),
            hasPriorityGoods = Some(true),
            hasALVS = Some(false),
            freightType = Some(ImportFreightType.Air),
            vesselDetails = Some(
              VesselDetails(
                vesselName = Some("Foo Bar"),
                dateOfArrival = Some(dateTimeOfArrival.toLocalDate()),
                timeOfArrival = Some(dateTimeOfArrival.toLocalTime())
              )
            ),
            contactInfo = Some(
              ImportContactInfo(
                contactName = "Full Name",
                contactNumber = Some("07777888999"),
                contactEmail = "someone@email.com"
              )
            )
          ),
          Seq()
        )
        val payload = TraderServicesCreateCaseRequest.formats.writes(createImportCaseRequest)

        givenImportCreateRequestWithVesselDetails(createImportCaseRequest, "xyz")

        val result = wsClient
          .url(s"$url/create-case")
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
