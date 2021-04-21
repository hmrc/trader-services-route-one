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

package uk.gov.hmrc.traderservices.models

import uk.gov.hmrc.traderservices.support.UnitSpec
import java.time.ZonedDateTime

class TraderServicesUpdateCaseRequestSpec extends UnitSpec {

  import TraderServicesUpdateCaseRequest._

  "TraderServicesUpdateCaseRequest" should {

    "validate case reference number" in {
      caseReferenceNumberValidator("").isValid shouldBe false
      caseReferenceNumberValidator("1").isValid shouldBe true
      caseReferenceNumberValidator("1" * 32).isValid shouldBe true
      caseReferenceNumberValidator("1" * 33).isValid shouldBe false
    }

    "validate response text" in {
      responseTextValidator("").isValid shouldBe false
      responseTextValidator("a").isValid shouldBe true
      responseTextValidator("a" * 1024).isValid shouldBe true
      responseTextValidator("a" * 1025).isValid shouldBe false
    }

    "validate if uploaded files present" in {
      validate(
        TraderServicesUpdateCaseRequest(
          "a",
          TypeOfAmendment.UploadDocuments,
          responseText = None,
          uploadedFiles = Seq.empty,
          eori = None
        )
      ).isValid shouldBe false

      validate(
        TraderServicesUpdateCaseRequest(
          "a",
          TypeOfAmendment.UploadDocuments,
          responseText = None,
          uploadedFiles = Seq(UploadedFile("", "", ZonedDateTime.now, "", "", "", None)),
          eori = None
        )
      ).isValid shouldBe false

      validate(
        TraderServicesUpdateCaseRequest(
          "a",
          TypeOfAmendment.UploadDocuments,
          responseText = None,
          uploadedFiles =
            Seq(UploadedFile("a", "https://a.a/a.a", ZonedDateTime.now, "a" * 64, "a" * 63, "aaa/aaa", None)),
          eori = None
        )
      ).isValid shouldBe true

      validate(
        TraderServicesUpdateCaseRequest(
          "a",
          TypeOfAmendment.WriteResponse,
          responseText = None,
          uploadedFiles =
            Seq(UploadedFile("a", "https://a.a/a.a", ZonedDateTime.now, "a" * 64, "a" * 63, "aaa/aaa", None)),
          eori = None
        )
      ).isValid shouldBe false

      validate(
        TraderServicesUpdateCaseRequest(
          "a",
          TypeOfAmendment.WriteResponseAndUploadDocuments,
          responseText = None,
          uploadedFiles =
            Seq(UploadedFile("a", "https://a.a/a.a", ZonedDateTime.now, "a" * 64, "a" * 63, "aaa/aaa", None)),
          eori = None
        )
      ).isValid shouldBe false

      validate(
        TraderServicesUpdateCaseRequest(
          "a",
          TypeOfAmendment.WriteResponseAndUploadDocuments,
          responseText = Some(""),
          uploadedFiles =
            Seq(UploadedFile("a", "https://a.a/a.a", ZonedDateTime.now, "a" * 64, "a" * 63, "aaa/aaa", None)),
          eori = None
        )
      ).isValid shouldBe false

      validate(
        TraderServicesUpdateCaseRequest(
          "a",
          TypeOfAmendment.WriteResponseAndUploadDocuments,
          responseText = Some("a"),
          uploadedFiles =
            Seq(UploadedFile("a", "https://a.a/a.a", ZonedDateTime.now, "a" * 64, "a" * 63, "aaa/aaa", None)),
          eori = None
        )
      ).isValid shouldBe true

      validate(
        TraderServicesUpdateCaseRequest(
          "a",
          TypeOfAmendment.WriteResponse,
          responseText = Some("a"),
          uploadedFiles = Seq.empty,
          eori = None
        )
      ).isValid shouldBe true
    }
  }

}
