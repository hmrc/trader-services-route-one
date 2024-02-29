/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.traderservices.connectors

import play.api.Application
import uk.gov.hmrc.traderservices.stubs.CreateCaseStubs
import uk.gov.hmrc.traderservices.support.AppBaseISpec
import uk.gov.hmrc.http._

import scala.concurrent.ExecutionContext.Implicits.global

class PegaCreateCaseConnectorISpec extends PegaCreateCaseConnectorISpecSetup {

  "PegaCreateCaseConnector" when {
    "createCase" should {
      "return case reference ID if success" in {
        givenPegaCreateImportCaseRequestSucceeds(200)
        givenAuditConnector()

        val request = testCreateImportCaseRequest

        val result = await(connector.createCase(request, correlationId))
        result shouldBe PegaCaseSuccess(
          "PCE201103470D2CC8K0NH3",
          "2020-11-03T15:29:28.601Z",
          "Success",
          "Case created successfully"
        )
        verifyPegaCreateCaseRequestHasHappened(times = 1)
      }

      "return error code and message if 500" in {
        givenPegaCreateCaseRequestFails(500, "500", "Foo Bar")
        givenAuditConnector()

        val request = testCreateImportCaseRequest

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
        verifyPegaCreateCaseRequestHasHappened(times = 3)
      }

      "return error code and message if 403" in {
        givenPegaCreateCaseRequestFails(403, "403", "Bar Foo")
        givenAuditConnector()

        val request = testCreateImportCaseRequest

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
        verifyPegaCreateCaseRequestHasHappened(times = 1)
      }

      "return error code and message if empty content-type" in {
        givenPegaCreateImportCaseRespondsWithoutContentType(200)
        givenAuditConnector()

        val request = testCreateImportCaseRequest

        val result = await(connector.createCase(request, correlationId))

        result shouldBe PegaCaseError(errorDetail =
          PegaCaseError
            .ErrorDetail(
              errorCode = Some("200"),
              errorMessage = Some("Error: missing content-type header")
            )
        )
        verifyPegaCreateCaseRequestHasHappened(times = 1)
      }

      "return error code and message if 200 with invalid success response content" in {
        givenPegaCreateImportRespondsWithInvalidSuccessMessage()
        givenAuditConnector()

        val request = testCreateImportCaseRequest

        val result = await(connector.createCase(request, correlationId))

        result shouldBe PegaCaseError(errorDetail =
          PegaCaseError
            .ErrorDetail(
              errorCode = Some("200"),
              errorMessage = Some(
                s"POST of '$wireMockBaseUrlAsString/cpr/caserequest/route1/create/v1' returned invalid json. Attempting to convert to uk.gov.hmrc.traderservices.connectors.PegaCaseResponse gave errors: List((/Status,List(JsonValidationError(List(error.path.missing),List()))), (/CaseID,List(JsonValidationError(List(error.path.missing),List()))))"
              )
            )
        )
        verifyPegaCreateCaseRequestHasHappened(times = 1)
      }

      "return error code and message if 403 with invalid error response content" in {
        givenPegaCreateImportRespondsWithInvalidErrorMessage()
        givenAuditConnector()

        val request = testCreateImportCaseRequest

        val result = await(connector.createCase(request, correlationId))

        result shouldBe PegaCaseError(errorDetail =
          PegaCaseError
            .ErrorDetail(
              errorCode = Some("403"),
              errorMessage = Some(
                s"POST of '$wireMockBaseUrlAsString/cpr/caserequest/route1/create/v1' returned invalid json. Attempting to convert to uk.gov.hmrc.traderservices.connectors.PegaCaseResponse gave errors: List((/errorDetail,List(JsonValidationError(List(error.path.missing),List()))))"
              )
            )
        )
        verifyPegaCreateCaseRequestHasHappened(times = 1)
      }

      "throw an error if strange response status" in {
        givenPegaCreateImportCaseRequestSucceeds(300)
        givenAuditConnector()

        val request = testCreateImportCaseRequest

        an[UpstreamErrorResponse] shouldBe thrownBy {
          await(connector.createCase(request, correlationId))
        }
        verifyPegaCreateCaseRequestHasHappened(times = 1)
      }
    }
  }

}

trait PegaCreateCaseConnectorISpecSetup extends AppBaseISpec with CreateCaseStubs {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def fakeApplication: Application = defaultAppBuilder.build()

  lazy val connector: PegaCreateCaseConnector =
    app.injector.instanceOf[PegaCreateCaseConnector]

  val correlationId: String = java.util.UUID.randomUUID().toString

  val testCreateImportCaseRequest: PegaCreateCaseRequest = PegaCreateCaseRequest(
    AcknowledgementReference = "XYZ123",
    ApplicationType = "Route1",
    OriginatingSystem = "Digital",
    Content = PegaCreateCaseRequest.Content(
      EntryType = "Import",
      RequestType = "New",
      EntryProcessingUnit = "002",
      Route = "Route 1",
      EntryNumber = "223456A",
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
