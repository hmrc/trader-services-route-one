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

package uk.gov.hmrc.traderservices.controllers

import play.api.Logger
import play.api.libs.json.{Format, JsObject, JsString, JsValue, Json, Writes}
import uk.gov.hmrc.traderservices.connectors.{ApiError, TraderServicesCaseResponse}
import uk.gov.hmrc.traderservices.models.{ExportQuestions, ImportQuestions, QuestionsAnswers, TraderServicesCreateCaseRequest}

case class CreateCaseLog(
  action: String = "CreateCase",
  success: Boolean,
  correlationId: String,
  questionsAnswers: QuestionsAnswers,
  numberOfFiles: Int,
  error: Option[ApiError],
  fileTransferSuccesses: Option[Int],
  fileTransferFailures: Option[Int],
  fileCorrelationIds: Seq[String],
  filesSize: Option[Int]
)

object CreateCaseLog {

  implicit val format: Format[CreateCaseLog] = Json.format[CreateCaseLog]

  private def stripSensitiveData(v: JsValue): JsValue =
    v match {
      case o: JsObject => o.-("contactInfo").-("reason")
      case x           => x
    }

  private def addExportTag(v: JsValue): JsValue =
    v match {
      case o: JsObject => o.+("variant" -> JsString(ExportQuestions.tag))
      case x           => x
    }

  private def addImportTag(v: JsValue): JsValue =
    v match {
      case o: JsObject => o.+("variant" -> JsString(ImportQuestions.tag))
      case x           => x
    }

  implicit lazy val writes: Writes[QuestionsAnswers] =
    new Writes[QuestionsAnswers] {
      override def writes(o: QuestionsAnswers): JsValue =
        o match {
          case e: ExportQuestions =>
            ExportQuestions.formats
              .transform(stripSensitiveData _)
              .transform(addExportTag _)
              .writes(e)
          case i: ImportQuestions =>
            ImportQuestions.formats
              .transform(stripSensitiveData _)
              .transform(addImportTag _)
              .writes(i)
          case _ => throw new IllegalArgumentException("Unknown QuestionsAnswers type")
        }
    }

  def log(request: TraderServicesCreateCaseRequest, response: TraderServicesCaseResponse): Unit = {
    val log: CreateCaseLog =
      CreateCaseLog(
        success = response.isSuccess,
        correlationId = response.correlationId,
        questionsAnswers = request.questionsAnswers,
        numberOfFiles = request.uploadedFiles.size,
        error = response.error.map(_.sanitized),
        fileTransferSuccesses = response.result.map(_.fileTransferResults.count(_.success)),
        fileTransferFailures = response.result.map(_.fileTransferResults.count(f => !f.success)),
        fileCorrelationIds = response.result.map(_.fileTransferResults.map(_.correlationId)).getOrElse(Seq.empty),
        filesSize = response.result.map(_.fileTransferResults.map(_.fileSize).sum)
      )
    Logger(getClass()).info(s"json${Json.stringify(Json.toJson(log))}")
  }
}
