package uk.gov.hmrc.traderservices.connectors

import play.api.Application
import uk.gov.hmrc.traderservices.models._
import uk.gov.hmrc.traderservices.stubs.UpdateCaseStubs
import uk.gov.hmrc.traderservices.support.AppBaseISpec
import uk.gov.hmrc.http._

import scala.concurrent.ExecutionContext.Implicits.global

class PegaUpdateCaseConnectorISpec extends PegaUpdateCaseConnectorISpecSetup {

  "PegaUpdateCaseConnector" when {
    "updateCase" should {
      "return case reference id if success" in {
        givenPegaUpdateCaseRequestSucceeds()

        val request = testRequest

        val result = await(connector.updateCase(request, correlationId))

        result shouldBe PegaCaseSuccess(
          "PCE201103470D2CC8K0NH3",
          "2020-11-03T15:29:28.601Z",
          "Success",
          "Case Updated successfully"
        )

      }

      "return error code and message if 500" in {
        givenPegaUpdateCaseRequestFails(500, "500", "Foo Bar")

        val request = testRequest

        val result = await(connector.updateCase(request, correlationId))

        result shouldBe PegaCaseError(errorDetail =
          PegaCaseError
            .ErrorDetail(
              correlationId = Some("123123123"),
              timestamp = Some("2020-11-03T15:29:28.601Z"),
              errorCode = Some("500"),
              errorMessage = Some("Foo Bar")
            )
        )
      }

      "return error code and message if 403" in {
        givenPegaUpdateCaseRequestFails(403, "403", "Bar Foo")

        val request = testRequest

        val result = await(connector.updateCase(request, correlationId))

        result shouldBe PegaCaseError(errorDetail =
          PegaCaseError
            .ErrorDetail(
              correlationId = Some("123123123"),
              timestamp = Some("2020-11-03T15:29:28.601Z"),
              errorCode = Some("403"),
              errorMessage = Some("Bar Foo")
            )
        )
      }
    }
  }

}

trait PegaUpdateCaseConnectorISpecSetup extends AppBaseISpec with UpdateCaseStubs {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def fakeApplication: Application = appBuilder.build()

  lazy val connector: PegaUpdateCaseConnector =
    app.injector.instanceOf[PegaUpdateCaseConnector]

  val correlationId = java.util.UUID.randomUUID().toString()

  val testRequest = PegaUpdateCaseRequest(
    AcknowledgementReference = "XYZ123",
    ApplicationType = "Route1",
    OriginatingSystem = "Digital",
    Content = PegaUpdateCaseRequest.Content(
      RequestType = "Additional Information",
      CaseID = "PCE201103470D2CC8K0NH3",
      Description = "An example description."
    )
  )

}
