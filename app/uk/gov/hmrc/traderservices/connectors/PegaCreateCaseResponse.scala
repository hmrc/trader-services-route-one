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
  errorDetail: PegaCreateCaseError.ErrorDetail
) extends PegaCreateCaseResponse {

  def errorCode: Option[String] = errorDetail.errorCode
  def errorMessage: Option[String] = errorDetail.errorMessage

  def isDuplicateCaseError: Boolean =
    errorDetail.errorMessage.exists(_.replace(" ", "").startsWith("999:"))

  def duplicateCaseID: Option[String] =
    errorDetail.errorMessage.map(_.replace(" ", "").drop(4))

}

object PegaCreateCaseError {

  def apply(
    timestamp: String,
    correlationId: String,
    errorCode: String,
    errorMessage: String
  ): PegaCreateCaseError =
    PegaCreateCaseError(errorDetail =
      ErrorDetail(Some(correlationId), Some(timestamp), Some(errorCode), Some(errorMessage))
    )

  def fromStatusAndMessage(status: Int, message: String): PegaCreateCaseError =
    PegaCreateCaseError(errorDetail = ErrorDetail(None, None, Some(status.toString), Some(message)))

  case class ErrorDetail(
    correlationId: Option[String] = None,
    timestamp: Option[String] = None,
    errorCode: Option[String] = None,
    errorMessage: Option[String] = None,
    source: Option[String] = None,
    sourceFaultDetail: Option[PegaCreateCaseError.ErrorDetail.SourceFaultDetail] = None
  )

  object ErrorDetail {

    case class SourceFaultDetail(
      detail: Option[Seq[String]] = None,
      restFault: Option[JsObject] = None,
      soapFault: Option[JsObject] = None
    )

    object SourceFaultDetail {
      implicit val formats: Format[SourceFaultDetail] =
        Json.format[SourceFaultDetail]

    }

    implicit val formats: Format[ErrorDetail] =
      Json.format[ErrorDetail]
  }

  implicit val formats: Format[PegaCreateCaseError] =
    Json.format[PegaCreateCaseError]

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
