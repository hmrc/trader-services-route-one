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

import play.api.libs.json.{Format, JsObject, Json, Reads, Writes}

sealed trait PegaCaseResponse

case class PegaCaseSuccess(
  CaseID: String,
  ProcessingDate: String,
  Status: String,
  StatusText: String
) extends PegaCaseResponse

object PegaCaseSuccess {
  implicit val formats: Format[PegaCaseSuccess] =
    Json.format[PegaCaseSuccess]
}

case class PegaCaseError(
  errorDetail: PegaCaseError.ErrorDetail
) extends PegaCaseResponse {

  final def errorCode: Option[String] = errorDetail.errorCode
  final def errorMessage: Option[String] = errorDetail.errorMessage

  final def isDuplicateCaseError: Boolean =
    errorDetail.errorMessage.exists(_.replace(" ", "").startsWith("999:"))

  final def duplicateCaseID: Option[String] =
    errorDetail.errorMessage.map(_.replace(" ", "").drop(4))

  final def isIntermittent: Boolean =
    !isDuplicateCaseError &&
      (errorDetail.errorCode.contains("499") ||
        errorDetail.errorCode.exists(_.startsWith("5")))

}

object PegaCaseError {

  def fromStatusAndMessage(status: Int, message: String): PegaCaseError =
    PegaCaseError(errorDetail = ErrorDetail(None, None, Some(status.toString), Some(message)))

  case class ErrorDetail(
    correlationId: Option[String] = None,
    timestamp: Option[String] = None,
    errorCode: Option[String] = None,
    errorMessage: Option[String] = None,
    source: Option[String] = None,
    sourceFaultDetail: Option[PegaCaseError.ErrorDetail.SourceFaultDetail] = None
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

  implicit val formats: Format[PegaCaseError] =
    Json.format[PegaCaseError]

}

object PegaCaseResponse {

  final def shouldRetry(response: PegaCaseResponse): Boolean =
    response match {
      case e: PegaCaseError if e.isIntermittent => true
      case _                                    => false
    }

  final def errorMessage(response: PegaCaseResponse): String =
    response match {
      case PegaCaseError(errorDetail) =>
        s"${errorDetail.errorCode.getOrElse("")} ${errorDetail.errorMessage.getOrElse("")}"
      case _ => ""
    }

  def reads: Reads[PegaCaseResponse] =
    PegaCaseSuccess.formats.widen[PegaCaseResponse] orElse
      PegaCaseError.formats.widen[PegaCaseResponse]

  def writes: Writes[PegaCaseResponse] = Writes {
    case u: PegaCaseSuccess => Json.toJson(u)(PegaCaseSuccess.formats)
    case i: PegaCaseError   => Json.toJson(i)(PegaCaseError.formats)
  }

  implicit def format: Format[PegaCaseResponse] = Format(reads, writes)

}
