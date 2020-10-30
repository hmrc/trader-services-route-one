package uk.gov.hmrc.traderservices.controllers

import java.time.LocalDateTime
import java.time.LocalDate
import java.time.temporal.ChronoUnit

import org.scalatest.Suite
import org.scalatestplus.play.ServerProvider
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import uk.gov.hmrc.traderservices.models._
import uk.gov.hmrc.traderservices.stubs.AuthStubs
import uk.gov.hmrc.traderservices.support.ServerBaseISpec

class TraderServicesControllerISpec extends ServerBaseISpec with AuthStubs {

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

    "POST /create-import" should {
      "respond with some data" in {
        givenAuthorisedAsValidTrader("xyz")
        val dateTimeOfArrival = dateTime.plusDays(1).truncatedTo(ChronoUnit.MINUTES)
        val payload = Json.format[CreateImportCaseRequest].writes(
          CreateImportCaseRequest(
            DeclarationDetails(EPU(235), EntryNumber("111111X"), LocalDate.parse("2020-09-23")),
            ImportQuestions(
              requestType = Some(ImportRequestType.New),
              routeType = Some(ImportRouteType.Route6),
              priorityGoods = Some(ImportPriorityGoods.HighValueArt),
              freightType = Some(ImportFreightType.Air),
              vesselDetails = Some(
                VesselDetails(
                  vesselName = Some("Foo Bar"),
                  dateOfArrival = Some(dateTimeOfArrival.toLocalDate()),
                  timeOfArrival = Some(dateTimeOfArrival.toLocalTime())
                )
              ),
              contactInfo = Some(ImportContactInfo(contactName = "Full Name", contactEmail = "someone@email.com"))
            )
          )
        )

        val result = wsClient
          .url(s"$url/create-import")
          .post(payload)
          .futureValue
        result.status shouldBe 200
        result.json shouldBe Json.obj("parameter1" -> "hello xyz")
      }
    }

    //    "GET /pre-clearance/import-questions/check-your-answers" should {
    //      "show the import questions summary page" in {
    //        implicit val journeyId: JourneyId = JourneyId()
    //        val dateTimeOfArrival = dateTime.plusDays(1).truncatedTo(ChronoUnit.MINUTES)
    //        val state = ImportQuestionsSummary(
    //          DeclarationDetails(EPU(235), EntryNumber("111111X"), LocalDate.parse("2020-09-23")),
    //          ImportQuestions(
    //            requestType = Some(ImportRequestType.New),
    //            routeType = Some(ImportRouteType.Route6),
    //            priorityGoods = Some(ImportPriorityGoods.HighValueArt),
    //            freightType = Some(ImportFreightType.Air),
    //            vesselDetails = Some(
    //              VesselDetails(
    //                vesselName = Some("Foo Bar"),
    //                dateOfArrival = Some(dateTimeOfArrival.toLocalDate()),
    //                timeOfArrival = Some(dateTimeOfArrival.toLocalTime())
    //              )
    //            ),
    //            contactInfo = Some(ImportContactInfo(contactName = "Full Name", contactEmail = "someone@email.com"))
    //          )
    //        )
    //        journey.setState(state)
    //        givenAuthorisedForEnrolment(Enrolment("HMRC-XYZ", "EORINumber", "foo"))
    //
    //        val result = await(request("/pre-clearance/import-questions/check-your-answers").get())
    //
    //        result.status shouldBe 200
    //        result.body should include(htmlEscapedMessage("view.import-questions.summary.title"))
    //        result.body should include(htmlEscapedMessage("view.import-questions.summary.heading"))
    //        journey.getState shouldBe state
    //      }
    //    }
  }
}
