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

class FileTransferRequestSpec extends UnitSpec {

  "FileTransferRequest" should {
    "create FileTransferRequest from an uploaded file" in {
      val uploadedFile = UploadedFile(
        upscanReference = "ABC123",
        downloadUrl = "http://abc",
        uploadTimestamp = ZonedDateTime.now,
        checksum = "XYZ",
        fileName = "foo.png",
        fileMimeType = "foo/bar"
      )
      FileTransferRequest
        .fromUploadedFile(
          caseReferenceNumber = "A00000A",
          conversationId = "123-456-789",
          correlationId = "abc-efg-ghi",
          applicationName = "bar",
          batchSize = 7,
          batchCount = 1,
          uploadedFile = uploadedFile
        ) shouldBe
        FileTransferRequest(
          conversationId = "123-456-789",
          caseReferenceNumber = "A00000A",
          applicationName = "bar",
          upscanReference = "ABC123",
          downloadUrl = "http://abc",
          checksum = "XYZ",
          fileName = "foo.png",
          fileMimeType = "foo/bar",
          batchSize = 7,
          batchCount = 1,
          correlationId = Some("abc-efg-ghi")
        )
    }
  }

  "FileTransferData" should {
    "create FileTransferData from an uploaded file" in {
      val uploadedFile = UploadedFile(
        upscanReference = "ABC123",
        downloadUrl = "http://abc",
        uploadTimestamp = ZonedDateTime.now,
        checksum = "XYZ",
        fileName = "foo.png",
        fileMimeType = "foo/bar"
      )
      FileTransferData
        .fromUploadedFile(uploadedFile) shouldBe
        FileTransferData(
          upscanReference = "ABC123",
          downloadUrl = "http://abc",
          checksum = "XYZ",
          fileName = "foo.png",
          fileMimeType = "foo/bar"
        )
    }
  }

}
