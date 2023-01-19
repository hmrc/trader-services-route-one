/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.traderservices.services

import javax.inject.Inject

import com.google.inject.Singleton
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import scala.concurrent.ExecutionContext

import scala.concurrent.Future
import scala.util.Try
import uk.gov.hmrc.traderservices.models.TraderServicesCreateCaseRequest
import uk.gov.hmrc.traderservices.models.TraderServicesUpdateCaseRequest
import uk.gov.hmrc.traderservices.connectors.TraderServicesCaseResponse
import play.api.libs.json._
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.traderservices.models.UploadedFile
import uk.gov.hmrc.traderservices.models.EntryDetails
import uk.gov.hmrc.traderservices.models.TypeOfAmendment
import uk.gov.hmrc.traderservices.models.FileTransferResult
import uk.gov.hmrc.traderservices.models.FileTransferAudit
import java.time.LocalDate
import java.time.LocalTime
import uk.gov.hmrc.traderservices.models.ImportQuestions
import uk.gov.hmrc.traderservices.models.ExportQuestions
import play.api.Logger
import uk.gov.hmrc.traderservices.models.FileTransferData

object TraderServicesAuditEvent extends Enumeration {
  type TraderServicesAuditEvent = Value
  val CreateCase, UpdateCase = Value
}

@Singleton
class AuditService @Inject() (val auditConnector: AuditConnector) {

  import TraderServicesAuditEvent._
  import AuditService._

  final def auditCreateCaseEvent(createRequest: TraderServicesCreateCaseRequest)(
    createResponse: TraderServicesCaseResponse
  )(implicit hc: HeaderCarrier, request: Request[Any], ec: ExecutionContext): Future[Unit] = {
    val details: JsValue =
      CreateCaseAuditEventDetails.from(createRequest, createResponse)
    auditExtendedEvent(CreateCase, "create-case", details)
  }

  final def auditCreateCaseErrorEvent(
    createResponse: TraderServicesCaseResponse
  )(implicit hc: HeaderCarrier, request: Request[Any], ec: ExecutionContext): Future[Unit] = {
    val details: JsValue = pegaResponseToDetails(createResponse, true)
    Logger(getClass).error(
      s"""Failure of CreateCase request [correlationId=${createResponse.correlationId}] because of ${createResponse.error
          .map(_.errorCode)
          .getOrElse("")} ${createResponse.error
          .flatMap(_.errorMessage)
          .getOrElse("")}"""
    )
    auditExtendedEvent(CreateCase, "create-case", details)
  }

  final def auditUpdateCaseEvent(updateRequest: TraderServicesUpdateCaseRequest)(
    updateResponse: TraderServicesCaseResponse
  )(implicit hc: HeaderCarrier, request: Request[Any], ec: ExecutionContext): Future[Unit] = {
    val details: JsValue =
      UpdateCaseAuditEventDetails.from(updateRequest, updateResponse)
    auditExtendedEvent(UpdateCase, "update-case", details)
  }

  final def auditUpdateCaseErrorEvent(
    updateResponse: TraderServicesCaseResponse
  )(implicit hc: HeaderCarrier, request: Request[Any], ec: ExecutionContext): Future[Unit] = {
    val details: JsObject = pegaResponseToDetails(updateResponse, false)
    Logger(getClass).error(
      s"""Failure of UpdateCase request [correlationId=${updateResponse.correlationId}] because of ${updateResponse.error
          .map(_.errorCode)
          .getOrElse("")} ${updateResponse.error
          .flatMap(_.errorMessage)
          .getOrElse("")}"""
    )
    auditExtendedEvent(UpdateCase, "update-case", details)
  }

  private def auditExtendedEvent(
    event: TraderServicesAuditEvent,
    transactionName: String,
    details: JsValue
  )(implicit hc: HeaderCarrier, request: Request[Any], ec: ExecutionContext): Future[Unit] =
    sendExtended(createExtendedEvent(event, transactionName, details))

  private def createExtendedEvent(
    event: TraderServicesAuditEvent,
    transactionName: String,
    details: JsValue
  )(implicit hc: HeaderCarrier, request: Request[Any]): ExtendedDataEvent = {
    val tags = hc.toAuditTags(transactionName, request.path)
    ExtendedDataEvent(
      auditSource = "trader-services-route-one",
      auditType = event.toString,
      tags = tags,
      detail = details
    )
  }

  private def sendExtended(
    events: ExtendedDataEvent*
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    Future {
      events.foreach { event =>
        Try(auditConnector.sendExtendedEvent(event))
      }
    }

}

object AuditService {

  case class CreateCaseAuditEventDetails(
    success: Boolean,
    eori: Option[String],
    caseReferenceNumber: Option[String],
    declarationType: String,
    entryDetails: EntryDetails,
    requestType: Option[String],
    routeType: Option[String],
    priorityGoods: Option[String],
    hasALVS: Option[Boolean],
    freightType: Option[String],
    vesselName: Option[String],
    dateOfArrival: Option[LocalDate],
    timeOfArrival: Option[LocalTime],
    contactName: Option[String],
    contactEmail: Option[String],
    contactNumber: Option[String],
    numberOfFilesUploaded: Int,
    uploadedFiles: Seq[FileTransferAudit],
    correlationId: String,
    reason: Option[String],
    totalFileTransferDurationMillis: Option[Int]
  )

  object CreateCaseAuditEventDetails {

    def from(
      createRequest: TraderServicesCreateCaseRequest,
      createResponse: TraderServicesCaseResponse
    ): JsValue = {
      val requestDetails: JsObject = Json
        .toJson(
          createRequest.questionsAnswers match {
            case q: ImportQuestions =>
              CreateCaseAuditEventDetails(
                success = true,
                eori = createRequest.eori,
                caseReferenceNumber = createResponse.result.map(_.caseId),
                declarationType = "import",
                entryDetails = createRequest.entryDetails,
                requestType = Some(q.requestType.toString()),
                routeType = Some(q.routeType.toString()),
                priorityGoods = q.priorityGoods.map(_.toString()),
                hasALVS = Some(q.hasALVS),
                freightType = Some(q.freightType.toString()),
                vesselName = q.vesselDetails.flatMap(_.vesselName),
                dateOfArrival = q.vesselDetails.flatMap(_.dateOfArrival),
                timeOfArrival = q.vesselDetails.flatMap(_.timeOfArrival),
                contactName = q.contactInfo.contactName,
                contactEmail = Some(q.contactInfo.contactEmail),
                contactNumber = q.contactInfo.contactNumber,
                numberOfFilesUploaded = createRequest.uploadedFiles.size,
                uploadedFiles = combineFileUploadAndTransferResults(
                  createRequest.uploadedFiles,
                  createResponse.result.map(_.fileTransferResults),
                  q.reason
                ),
                correlationId = createResponse.correlationId,
                reason = q.reason,
                totalFileTransferDurationMillis = createResponse.result.flatMap(_.totalFileTransferDurationMillis)
              )

            case q: ExportQuestions =>
              CreateCaseAuditEventDetails(
                success = true,
                eori = createRequest.eori,
                caseReferenceNumber = createResponse.result.map(_.caseId),
                declarationType = "export",
                entryDetails = createRequest.entryDetails,
                requestType = Some(q.requestType.toString()),
                routeType = Some(q.routeType.toString()),
                priorityGoods = q.priorityGoods.map(_.toString()),
                hasALVS = None,
                freightType = Some(q.freightType.toString()),
                vesselName = q.vesselDetails.flatMap(_.vesselName),
                dateOfArrival = q.vesselDetails.flatMap(_.dateOfArrival),
                timeOfArrival = q.vesselDetails.flatMap(_.timeOfArrival),
                contactName = q.contactInfo.contactName,
                contactEmail = Some(q.contactInfo.contactEmail),
                contactNumber = q.contactInfo.contactNumber,
                numberOfFilesUploaded = createRequest.uploadedFiles.size,
                uploadedFiles = combineFileUploadAndTransferResults(
                  createRequest.uploadedFiles,
                  createResponse.result.map(_.fileTransferResults),
                  q.reason
                ),
                correlationId = createResponse.correlationId,
                reason = q.reason,
                totalFileTransferDurationMillis = createResponse.result.flatMap(_.totalFileTransferDurationMillis)
              )
          }
        )
        .as[JsObject]

      if (createResponse.result.isDefined) requestDetails
      else
        requestDetails ++ pegaResponseToDetails(createResponse, true)
    }

    implicit val formats: Format[CreateCaseAuditEventDetails] =
      Json.format[CreateCaseAuditEventDetails]
  }

  case class UpdateCaseAuditEventDetails(
    success: Boolean,
    eori: Option[String],
    caseReferenceNumber: String,
    typeOfAmendment: TypeOfAmendment,
    responseText: Option[String] = None,
    numberOfFilesUploaded: Int,
    uploadedFiles: Seq[FileTransferAudit],
    correlationId: String,
    totalFileTransferDurationMillis: Option[Int]
  )

  object UpdateCaseAuditEventDetails {

    def from(
      updateRequest: TraderServicesUpdateCaseRequest,
      updateResponse: TraderServicesCaseResponse
    ): JsValue = {
      val requestDetails: JsObject = Json
        .toJson(
          UpdateCaseAuditEventDetails(
            success = true,
            eori = updateRequest.eori,
            caseReferenceNumber = updateRequest.caseReferenceNumber,
            typeOfAmendment = updateRequest.typeOfAmendment,
            responseText = updateRequest.responseText,
            numberOfFilesUploaded = updateRequest.uploadedFiles.size,
            uploadedFiles = combineFileUploadAndTransferResults(
              updateRequest.uploadedFiles,
              updateResponse.result.map(_.fileTransferResults),
              None
            ),
            correlationId = updateResponse.correlationId,
            totalFileTransferDurationMillis = updateResponse.result.flatMap(_.totalFileTransferDurationMillis)
          )
        )
        .as[JsObject]

      if (updateResponse.result.isDefined) requestDetails
      else
        requestDetails ++ pegaResponseToDetails(updateResponse, false)
    }

    implicit val formats: Format[UpdateCaseAuditEventDetails] =
      Json.format[UpdateCaseAuditEventDetails]
  }

  def pegaResponseToDetails(
    caseResponse: TraderServicesCaseResponse,
    reportDuplicate: Boolean
  ): JsObject =
    Json.obj(
      "success"       -> caseResponse.isSuccess,
      "correlationId" -> caseResponse.correlationId
    ) ++
      (if (caseResponse.isSuccess)
         Json.obj(
           "caseReferenceNumber" -> caseResponse.result.get.caseId
         )
       else
         (if (reportDuplicate)
            Json.obj(
              "duplicate" -> caseResponse.isDuplicate
            )
          else Json.obj()) ++ caseResponse.error.map(e => Json.obj("errorCode" -> e.errorCode)).getOrElse(Json.obj()) ++
           caseResponse.error
             .flatMap(_.errorMessage)
             .map(m => Json.obj("errorMessage" -> m))
             .getOrElse(Json.obj()))

  def combineFileUploadAndTransferResults(
    uploadedFiles: Seq[UploadedFile],
    fileTransferResults: Option[Seq[FileTransferResult]],
    reason: Option[String]
  ): Seq[FileTransferAudit] = {
    val filesAudits =
      uploadedFiles.map { upload =>
        val transferResultOpt = fileTransferResults.flatMap(_.find(_.upscanReference == upload.upscanReference))
        FileTransferAudit(
          upscanReference = upload.upscanReference,
          downloadUrl = upload.downloadUrl,
          uploadTimestamp = Some(upload.uploadTimestamp),
          checksum = upload.checksum,
          fileName = upload.fileName,
          fileMimeType = upload.fileMimeType,
          transferSuccess = transferResultOpt.map(_.success).orElse(Some(false)),
          transferHttpStatus = transferResultOpt.map(_.httpStatus),
          transferredAt = transferResultOpt.map(_.transferredAt),
          transferError = transferResultOpt.flatMap(_.error)
        )
      }
    reason
      .flatMap(e => auditReasonFile(e, fileTransferResults))
      .map(filesAudits :+ _)
      .getOrElse(filesAudits)
  }

  def auditReasonFile(
    reason: String,
    fileTransferResults: Option[Seq[FileTransferResult]]
  ): Option[FileTransferAudit] =
    fileTransferResults
      .flatMap(_.find(_.upscanReference == FileTransferData.REASON_REFERENCE))
      .map { t =>
        val f = FileTransferData.fromReason(reason)
        FileTransferAudit(
          upscanReference = t.upscanReference,
          downloadUrl = f.downloadUrl,
          uploadTimestamp = None,
          checksum = t.checksum,
          fileName = t.fileName,
          fileMimeType = t.fileMimeType,
          transferSuccess = Some(t.success),
          transferHttpStatus = Some(t.httpStatus),
          transferredAt = Some(t.transferredAt),
          transferError = t.error
        )
      }

}
