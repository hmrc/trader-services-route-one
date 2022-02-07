/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.traderservices.connectors.{ApiError, TraderServicesCaseResponse}
import uk.gov.hmrc.traderservices.models.{TraderServicesUpdateCaseRequest, TypeOfAmendment}

case class UpdateCaseLog(
  action: String = "UpdateCase",
  success: Boolean,
  correlationId: String,
  typeOfAmendment: TypeOfAmendment,
  numberOfFiles: Int,
  error: Option[ApiError],
  fileTransferSuccesses: Option[Int],
  fileTransferFailures: Option[Int],
  fileCorrelationIds: Seq[String],
  fileTransferDurations: Seq[Int],
  filesSize: Option[Int],
  totalFileTransferDurationMillis: Option[Int]
)

object UpdateCaseLog {

  implicit val format: Format[UpdateCaseLog] = Json.format[UpdateCaseLog]

  def log(request: TraderServicesUpdateCaseRequest, response: TraderServicesCaseResponse): Unit = {

    val log: UpdateCaseLog =
      UpdateCaseLog(
        success = response.isSuccess,
        correlationId = response.correlationId,
        typeOfAmendment = request.typeOfAmendment,
        numberOfFiles = request.uploadedFiles.size,
        error = response.error,
        fileTransferSuccesses = response.result.map(_.fileTransferResults.count(_.success)),
        fileTransferFailures = response.result.map(_.fileTransferResults.count(f => !f.success)),
        fileCorrelationIds = response.result.map(_.fileTransferResults.map(_.correlationId)).getOrElse(Seq.empty),
        fileTransferDurations = response.result.map(_.fileTransferResults.map(_.durationMillis)).getOrElse(Seq.empty),
        filesSize = response.result.map(_.fileTransferResults.map(_.fileSize).sum),
        totalFileTransferDurationMillis = response.result.flatMap(_.totalFileTransferDurationMillis)
      )
    Logger(getClass()).info(s"json${Json.stringify(Json.toJson(log))}")

  }
}
