/*
 * Copyright 2021 HM Revenue & Customs
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

import uk.gov.hmrc.traderservices.support.UnitSpec
import uk.gov.hmrc.traderservices.models._
import java.util.UUID

class PegaUpdateCaseRequestSpec extends UnitSpec {

  "PegaUpdateCaseRequest.Content" should {
    s"build Content from the update case request having" in {
      for {
        caseId      <- Stream.continually(UUID.randomUUID().toString()).take(10)
        description <- Stream.from(0).map(x => (("a" * x) + " ") * x).take(20)
      } PegaUpdateCaseRequest.Content
        .from(serviceUpdateCaseRequest(caseId, description))
        .shouldBe(
          PegaUpdateCaseRequest.Content(
            "Additional Information",
            caseId,
            description
          )
        )
    }

    def serviceUpdateCaseRequest(
      caseId: String,
      description: String
    ): TraderServicesUpdateCaseRequest =
      TraderServicesUpdateCaseRequest(
        caseId,
        TypeOfAmendment.WriteResponseAndUploadDocuments,
        Some(description),
        Seq.empty,
        None
      )

  }

}
