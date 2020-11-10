/*
 * Copyright 2020 HM Revenue & Customs
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

import play.api.libs.json.{Format, Json}
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.libs.json.JsObject
import play.api.libs.json.JsValue

sealed trait PegaCreateCaseResponse

case class PegaCreateCaseSuccess(
  CaseID: String,
  ProcessingDate: String,
  Status: String,
  StatusText: String
) extends PegaCreateCaseResponse

object PegaCreateCaseSuccess {
  implicit val formats: Format[PegaCreateCaseSuccess] =
    Json.format[PegaCreateCaseSuccess]
}

case class PegaCreateCaseError(
  ProcessingDate: Option[String],
  CorrelationID: Option[String],
  ErrorCode: Option[String],
  ErrorMessage: Option[String]
) extends PegaCreateCaseResponse {

  def isDuplicateCaseError: Boolean =
    ErrorMessage.exists(_.replace(" ", "").startsWith("999:"))

  def duplicateCaseID: Option[String] =
    ErrorMessage.map(_.replace(" ", "").drop(4))

}

object PegaCreateCaseError {
  implicit val formats: Format[PegaCreateCaseError] =
    Json.format[PegaCreateCaseError]

  def fromStatus(status: Int): PegaCreateCaseError =
    PegaCreateCaseError(None, None, Some(status.toString), None)
}

object PegaCreateCaseResponse {

  implicit def reads: Reads[PegaCreateCaseResponse] =
    Reads {
      case jsObject: JsObject if (jsObject \ "CaseID").isDefined =>
        PegaCreateCaseSuccess.formats.reads(jsObject)
      case jsValue =>
        PegaCreateCaseError.formats.reads(jsValue)
    }

  implicit def writes: Writes[PegaCreateCaseResponse] =
    new Writes[PegaCreateCaseResponse] {
      override def writes(o: PegaCreateCaseResponse): JsValue =
        o match {
          case s: PegaCreateCaseSuccess =>
            PegaCreateCaseSuccess.formats.writes(s)
          case e: PegaCreateCaseError =>
            PegaCreateCaseError.formats.writes(e)
        }
    }

}
