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
import uk.gov.hmrc.traderservices.models.DeclarationDetails
import uk.gov.hmrc.traderservices.models.TypeOfAmendment
import uk.gov.hmrc.traderservices.models.FileTransferResult
import uk.gov.hmrc.traderservices.models.FileTransferAudit
import java.time.LocalDate
import java.time.LocalTime
import uk.gov.hmrc.traderservices.models.ImportQuestions
import uk.gov.hmrc.traderservices.models.ExportQuestions

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
    val details: JsValue = pegaResponseToDetails(updateResponse, false)
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
  )(implicit hc: HeaderCarrier, request: Request[Any], ec: ExecutionContext): ExtendedDataEvent = {
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
    eori: String,
    caseReferenceNumber: Option[String],
    declarationType: String,
    declarationDetails: DeclarationDetails,
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
    uploadedFiles: Seq[FileTransferAudit]
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
                declarationDetails = createRequest.declarationDetails,
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
                  createResponse.result.map(_.fileTransferResults)
                )
              )

            case q: ExportQuestions =>
              CreateCaseAuditEventDetails(
                success = true,
                eori = createRequest.eori,
                caseReferenceNumber = createResponse.result.map(_.caseId),
                declarationType = "export",
                declarationDetails = createRequest.declarationDetails,
                requestType = Some(q.requestType.toString()),
                routeType = Some(q.routeType.toString()),
                priorityGoods = Some(q.priorityGoods.toString()),
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
                  createResponse.result.map(_.fileTransferResults)
                )
              )
          }
        )
        .as[JsObject]

      if (createResponse.result.isDefined) requestDetails
      else
        (requestDetails ++ pegaResponseToDetails(createResponse, true))
    }

    implicit val formats: Format[CreateCaseAuditEventDetails] =
      Json.format[CreateCaseAuditEventDetails]
  }

  case class UpdateCaseAuditEventDetails(
    success: Boolean,
    caseReferenceNumber: String,
    typeOfAmendment: TypeOfAmendment,
    responseText: Option[String] = None,
    numberOfFilesUploaded: Int,
    uploadedFiles: Seq[FileTransferAudit]
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
            caseReferenceNumber = updateRequest.caseReferenceNumber,
            typeOfAmendment = updateRequest.typeOfAmendment,
            responseText = updateRequest.responseText,
            numberOfFilesUploaded = updateRequest.uploadedFiles.size,
            uploadedFiles = combineFileUploadAndTransferResults(
              updateRequest.uploadedFiles,
              updateResponse.result.map(_.fileTransferResults)
            )
          )
        )
        .as[JsObject]

      if (updateResponse.result.isDefined) requestDetails
      else
        (requestDetails ++ pegaResponseToDetails(updateResponse, false))
    }

    implicit val formats: Format[UpdateCaseAuditEventDetails] =
      Json.format[UpdateCaseAuditEventDetails]
  }

  def pegaResponseToDetails(
    caseResponse: TraderServicesCaseResponse,
    reportDuplicate: Boolean
  ): JsObject =
    Json.obj(
      "success" -> caseResponse.isSuccess
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
    fileTransferResults: Option[Seq[FileTransferResult]]
  ): Seq[FileTransferAudit] =
    uploadedFiles.map { upload =>
      val transferResultOpt = fileTransferResults.flatMap(_.find(_.upscanReference == upload.upscanReference))
      FileTransferAudit(
        upscanReference = upload.upscanReference,
        downloadUrl = upload.downloadUrl,
        uploadTimestamp = upload.uploadTimestamp,
        checksum = upload.checksum,
        fileName = upload.fileName,
        fileMimeType = upload.fileMimeType,
        transferSuccess = transferResultOpt.map(_.success).orElse(Some(false)),
        transferHttpStatus = transferResultOpt.map(_.httpStatus),
        transferredAt = transferResultOpt.map(_.transferredAt),
        transferError = transferResultOpt.flatMap(_.error)
      )
    }

}
