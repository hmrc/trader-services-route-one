/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.traderservices.stubs.UpdateCaseStubs
import uk.gov.hmrc.traderservices.support.AppBaseISpec
import uk.gov.hmrc.http._

import scala.concurrent.ExecutionContext.Implicits.global

class PegaUpdateCaseConnectorISpec extends PegaUpdateCaseConnectorISpecSetup {

  "PegaUpdateCaseConnector" when {
    "updateCase" should {
      "return case reference id if success" in {
        givenPegaUpdateCaseRequestSucceeds()
        givenAuditConnector()

        val request = testRequest

        val result = await(connector.updateCase(request, correlationId))

        result shouldBe PegaCaseSuccess(
          "PCE201103470D2CC8K0NH3",
          "2020-11-03T15:29:28.601Z",
          "Success",
          "Case Updated successfully"
        )

        verifyPegaUpdateCaseRequestHasHappened(times = 1)
      }

      "return error code and message if 500" in {
        givenPegaUpdateCaseRequestFails(500, "500", "Foo Bar")
        givenAuditConnector()

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

        verifyPegaUpdateCaseRequestHasHappened(times = 3)
      }

      "return error code and message if 403" in {
        givenPegaUpdateCaseRequestFails(403, "403", "Bar Foo")
        givenAuditConnector()

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

        verifyPegaUpdateCaseRequestHasHappened(times = 1)
      }
    }
  }

}

trait PegaUpdateCaseConnectorISpecSetup extends AppBaseISpec with UpdateCaseStubs {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def fakeApplication: Application = defaultAppBuilder.build()

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
