package uk.gov.hmrc.traderservices.connectors

import play.api.Application
import uk.gov.hmrc.traderservices.models._
import uk.gov.hmrc.traderservices.stubs.CreateCaseStubs
import uk.gov.hmrc.traderservices.support.AppBaseISpec
import uk.gov.hmrc.http._

import scala.concurrent.ExecutionContext.Implicits.global

class PegaCreateCaseConnectorISpec extends PegaCreateCaseConnectorISpecSetup {

  "PegaCreateCaseConnector" when {
    "createCase" should {
      "return case reference id if success" in {
        givenPegaCreateCaseRequestSucceeds()
        givenAuditConnector()

        val request = testRequest

        val result = await(connector.createCase(request, correlationId))

        result shouldBe PegaCaseSuccess(
          "PCE201103470D2CC8K0NH3",
          "2020-11-03T15:29:28.601Z",
          "Success",
          "Case created successfully"
        )

      }

      "return error code and message if 500" in {
        givenPegaCreateCaseRequestFails(500, "500", "Foo Bar")
        givenAuditConnector()

        val request = testRequest

        val result = await(connector.createCase(request, correlationId))

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
        givenPegaCreateCaseRequestFails(403, "403", "Bar Foo")
        givenAuditConnector()

        val request = testRequest

        val result = await(connector.createCase(request, correlationId))

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

trait PegaCreateCaseConnectorISpecSetup extends AppBaseISpec with CreateCaseStubs {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def fakeApplication: Application = defaultAppBuilder.build()

  lazy val connector: PegaCreateCaseConnector =
    app.injector.instanceOf[PegaCreateCaseConnector]

  val correlationId = java.util.UUID.randomUUID().toString()

  val testRequest = PegaCreateCaseRequest(
    AcknowledgementReference = "XYZ123",
    ApplicationType = "Route1",
    OriginatingSystem = "Digital",
    Content = PegaCreateCaseRequest.Content(
      EntryType = "Import",
      RequestType = "New",
      EntryProcessingUnit = "002",
      Route = "Route 1",
      EntryNumber = "A23456A",
      VesselName = Some("Vessel Name"),
      EntryDate = "20200902",
      VesselEstimatedDate = Some("20201029"),
      VesselEstimatedTime = Some("234500"),
      FreightOption = "Maritime",
      EORI = Some("GB123456789012345"),
      TelephoneNumber = Some("07123456789"),
      EmailAddress = "sampelname@gmail.com"
    )
  )

}
