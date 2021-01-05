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

import play.api.libs.json.Json
import play.api.libs.json.Format
import uk.gov.hmrc.traderservices.models.TypeOfAmendment._

case class TraderServicesUpdateCaseRequest(
  caseReferenceNumber: String,
  typeOfAmendment: TypeOfAmendment,
  responseText: Option[String] = None,
  uploadedFiles: Seq[UploadedFile] = Seq.empty,
  eori: String
)

object TraderServicesUpdateCaseRequest {

  implicit val formats: Format[TraderServicesUpdateCaseRequest] =
    Json.format[TraderServicesUpdateCaseRequest]

  import Validator._

  val caseReferenceNumberValidator: Validate[String] =
    check(
      _.lengthMinMaxInclusive(1, 32),
      s""""Invalid caseReferenceNumber, should be between 1 and 32 (inclusive) character long."""
    )

  val responseTextValidator: Validate[String] =
    check(
      _.lengthMinMaxInclusive(1, 1024),
      s""""Invalid responseText, should be between 1 and 1024 (inclusive) character long."""
    )

  implicit val validate: Validate[TraderServicesUpdateCaseRequest] =
    Validator(
      checkProperty(_.caseReferenceNumber, caseReferenceNumberValidator),
      checkIfSome(_.responseText, responseTextValidator),
      check(
        r =>
          r.typeOfAmendment match {
            case UploadDocuments                 => r.uploadedFiles.nonEmpty && r.responseText.isEmpty
            case WriteResponse                   => r.uploadedFiles.isEmpty && r.responseText.nonEmpty
            case WriteResponseAndUploadDocuments => r.uploadedFiles.nonEmpty && r.responseText.nonEmpty
          },
        "Invalid request, responseText and fileUploads must respect typeOfAmendment"
      )
    )
}
