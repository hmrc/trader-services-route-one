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
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.HttpRequest
import scala.util.Try

case class TraderServicesCreateCaseRequest(
  entryDetails: EntryDetails,
  questionsAnswers: QuestionsAnswers,
  uploadedFiles: Seq[UploadedFile],
  eori: Option[String]
)

object TraderServicesCreateCaseRequest {

  implicit val formats: Format[TraderServicesCreateCaseRequest] =
    Json.format[TraderServicesCreateCaseRequest]

  import Validator._

  val epuValidator: Validate[EPU] =
    check(_.value.inRange(1, 669), "EPU must be between 1 and 699 inclusive")

  val entryNumberValidator: Validate[EntryNumber] =
    Validator(
      check(_.value.length == 7, "EntryNumber must have 7 characters"),
      check(_.value.forall(_.isLetterOrDigit), "EntryNumber must contain only letters and digits"),
      check(
        s => s.value.lastOption.forall(_.isLetter) || s.value.drop(6).headOption.forall(_.isLetter),
        "EntryNumber must end with a letter"
      ),
      check(
        _.value.slice(1, 6).forall(_.isDigit),
        "EntryNumber must be digits except for the start and the end"
      )
    )

  val entryDetailsValidator: Validate[EntryDetails] =
    Validator(
      checkProperty(_.epu, epuValidator),
      checkProperty(_.entryNumber, entryNumberValidator)
    )

  val exportQuestionsValidator: Validate[ExportQuestions] =
    Validator.always

  val importQuestionsValidator: Validate[ImportQuestions] =
    Validator.always

  val questionsAnswersValidator: Validate[QuestionsAnswers] =
    (q: QuestionsAnswers) =>
      q match {
        case q: ExportQuestions => exportQuestionsValidator(q)
        case q: ImportQuestions => importQuestionsValidator(q)
      }

  val uploadedFileValidator: Validate[UploadedFile] =
    Validator(
      check(_.upscanReference.nonEmpty, "upscanReference must be not empty"),
      check(_.checksum.nonEmpty, "checksum must be not empty"),
      check(_.checksum.length() == 64, "checksum SHA-256 must be 64 characters long"),
      check(_.downloadUrl.nonEmpty, "downloadUrl must be not empty"),
      check(
        e => {
          val r = Try {
            val uri = Uri(e.downloadUrl)
            HttpRequest.verifyUri(uri)
            uri
          }
          r.isSuccess && r.get.isAbsolute
        },
        "downloadUrl must be a valid absolute URI"
      ),
      check(_.fileMimeType.nonEmpty, "fileMimeType must be not empty"),
      check(_.fileName.nonEmpty, "fileName must be not empty"),
      check(
        _.fileName.length < 94,
        "fileName must be less than 94 characters"
      ),
      check(_.fileSize.forall(_ > 0), "fileSize must be greater than zero"),
      check(_.fileSize.forall(_ <= 6 * 1024 * 1024), "fileSize must be lower or equal to 6MB")
    )

  val eoriValidator: Validate[String] =
    Validator.always

  implicit val validate: Validator.Validate[TraderServicesCreateCaseRequest] =
    Validator(
      checkProperty(_.entryDetails, entryDetailsValidator),
      checkProperty(_.questionsAnswers, questionsAnswersValidator),
      checkEach(_.uploadedFiles, uploadedFileValidator),
      checkIfSome(_.eori, eoriValidator)
    )
}
