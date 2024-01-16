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

package uk.gov.hmrc.traderservices.models

import uk.gov.hmrc.traderservices.support.UnitSpec
import java.time.ZonedDateTime
import java.time.LocalDate

class TraderServicesCreateCaseRequestSpec extends UnitSpec {

  import TraderServicesCreateCaseRequest._

  "TraderServicesCreateCaseRequestSpec" should {
    "validate EPU" in {
      epuValidator(EPU(0)).isValid shouldBe false
      epuValidator(EPU(1)).isValid shouldBe true
      epuValidator(EPU(123)).isValid shouldBe true
      epuValidator(EPU(669)).isValid shouldBe true
      epuValidator(EPU(700)).isValid shouldBe false
      epuValidator(EPU(999)).isValid shouldBe false
    }

    "validate entry number" in {
      entryNumberValidator(EntryNumber("A00000A")).isValid shouldBe true
      entryNumberValidator(EntryNumber("000000A")).isValid shouldBe true
      entryNumberValidator(EntryNumber("")).isValid shouldBe false
      entryNumberValidator(EntryNumber("A")).isValid shouldBe false
      entryNumberValidator(EntryNumber("1")).isValid shouldBe false
      entryNumberValidator(EntryNumber("0000000")).isValid shouldBe false
      entryNumberValidator(EntryNumber("1234567")).isValid shouldBe false
      entryNumberValidator(EntryNumber("0000000A")).isValid shouldBe false
      entryNumberValidator(EntryNumber("A0000000")).isValid shouldBe false
      entryNumberValidator(EntryNumber("000000AA")).isValid shouldBe false
      entryNumberValidator(EntryNumber("AA00000A")).isValid shouldBe false
      entryNumberValidator(EntryNumber("AAAAAAA")).isValid shouldBe false
      entryNumberValidator(EntryNumber("A0AAAAA")).isValid shouldBe false
      entryNumberValidator(EntryNumber("A123456")).isValid shouldBe false
      entryNumberValidator(EntryNumber("A12345A ")).isValid shouldBe false
      entryNumberValidator(EntryNumber(" A12345A")).isValid shouldBe false
      entryNumberValidator(EntryNumber("A 2345A")).isValid shouldBe false
      entryNumberValidator(EntryNumber("0 2345A")).isValid shouldBe false
      entryNumberValidator(EntryNumber("0+2345A")).isValid shouldBe false
      entryNumberValidator(EntryNumber("0-2345A")).isValid shouldBe false
    }

    "validate entry details" in {
      entryDetailsValidator(
        EntryDetails(EPU(123), EntryNumber("A00000A"), LocalDate.parse("2020-07-07"))
      ).isValid shouldBe true
    }

    "validate uploaded files" in {
      uploadedFileValidator(
        UploadedFile(
          "aaaa",
          "https://aaa.aa/aaa.aa",
          ZonedDateTime.now,
          "a" * 64,
          "a" * 93,
          "aaaa/aaaa",
          Some(6 * 1024 * 1024)
        )
      ).isValid shouldBe true
      uploadedFileValidator(
        UploadedFile(
          "a",
          "https://aaa.aa/aaa.aa",
          ZonedDateTime.now,
          "a" * 64,
          "a" * 93,
          "aaaa/aaaa",
          Some(1)
        )
      ).isValid shouldBe true
      uploadedFileValidator(
        UploadedFile(
          "a",
          "https://aaa.aa/aaa.aa",
          ZonedDateTime.now,
          "a" * 64,
          "a" * 93,
          "aaaa/aaaa",
          Some(0)
        )
      ).isValid shouldBe false
      uploadedFileValidator(
        UploadedFile(
          "a",
          "https://aaa.aa/aaa.aa",
          ZonedDateTime.now,
          "a" * 65,
          "a",
          "aaaa/aaaa",
          Some(1)
        )
      ).isValid shouldBe false
      uploadedFileValidator(
        UploadedFile(
          "a",
          "https://aaa.aa/aaa.aa",
          ZonedDateTime.now,
          "a" * 63,
          "a",
          "aaaa/aaaa",
          Some(1)
        )
      ).isValid shouldBe false
      uploadedFileValidator(
        UploadedFile(
          "a",
          "https://aaa.aa/aaa.aa",
          ZonedDateTime.now,
          "a" * 64,
          "a" * 256,
          "aaaa/aaaa",
          Some(1)
        )
      ).isValid shouldBe true
      uploadedFileValidator(
        UploadedFile("", "", ZonedDateTime.now, "", "", "", None)
      ).isValid shouldBe false
      uploadedFileValidator(
        UploadedFile("aaaa", "", ZonedDateTime.now, "", "", "", None)
      ).isValid shouldBe false
      uploadedFileValidator(
        UploadedFile("aaaa", "aaaa", ZonedDateTime.now, "", "", "", None)
      ).isValid shouldBe false
      uploadedFileValidator(
        UploadedFile("aaaa", "foo:aaaa", ZonedDateTime.now, "a" * 64, "a" * 93, "aaaa", Some(1))
      ).isValid shouldBe false
    }
  }

}
