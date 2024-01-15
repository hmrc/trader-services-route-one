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

import play.api.libs.json.Json
import uk.gov.hmrc.traderservices.services.AuditService._
import uk.gov.hmrc.traderservices.support.UnitSpec

import java.time.LocalDateTime

class AuditServiceSpec extends UnitSpec {

  "pegaResponseToDetails" should {

    "return a JSON object when caseResponse is successful" in {
      val caseResponse = TraderServicesCaseResponse(
        "12345",
        None,
        Some(TraderServicesResult("ABC-123", LocalDateTime.now(), Seq(), None))
      )
      val reportDuplicate = false
      val expectedJson = Json.obj(
        "success"             -> true,
        "correlationId"       -> "12345",
        "caseReferenceNumber" -> "ABC-123"
      )
      val actualJson = pegaResponseToDetails(caseResponse, reportDuplicate)
      actualJson shouldEqual expectedJson
    }

    "return a JSON object when a create case request is a duplicate" in {
      val error = ApiError("409", Some("duplicate create case request"))
      val caseResponse = TraderServicesCaseResponse("12345", Some(error), None)
      val reportDuplicate = true
      val expectedJson = Json.obj(
        "success"       -> false,
        "correlationId" -> "12345",
        "duplicate"     -> true,
        "errorCode"     -> "409",
        "errorMessage"  -> "duplicate create case request"
      )
      val actualJson = pegaResponseToDetails(caseResponse, reportDuplicate)
      actualJson shouldEqual expectedJson
    }
  }
}
