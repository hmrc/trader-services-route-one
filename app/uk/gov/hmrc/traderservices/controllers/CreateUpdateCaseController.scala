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

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import play.api.Configuration
import play.api.Environment
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.traderservices.connectors.PegaCreateCaseRequest
import uk.gov.hmrc.traderservices.connectors._
import uk.gov.hmrc.traderservices.models._
import uk.gov.hmrc.traderservices.services.AuditService
import uk.gov.hmrc.traderservices.wiring.AppConfig

import java.time.LocalDateTime
import java.{util => ju}
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.Success
import scala.concurrent.Await

@Singleton
class CreateUpdateCaseController @Inject() (
  val authConnector: MicroserviceAuthConnector,
  val createCaseConnector: PegaCreateCaseConnector,
  val updateCaseConnector: PegaUpdateCaseConnector,
  val fileTransferConnector: FileTransferConnector,
  val env: Environment,
  val appConfig: AppConfig,
  val auditService: AuditService,
  actorSystem: ActorSystem,
  cc: ControllerComponents
)(implicit val configuration: Configuration, ec: ExecutionContext)
    extends BackendController(cc) with AuthActions with ControllerHelper {
  // POST /create-case
  def createCase: Action[String] =
    Action.async(parseTolerantTextUtf8) { implicit request =>
      val correlationId = request.headers
        .get("X-Correlation-Id")
        .orElse(
          request.headers
            .get("X-Request-Id")
            .map(_.takeRight(36))
        )
        .getOrElse(ju.UUID.randomUUID().toString())

      withAuthorised {
        withPayload[TraderServicesCreateCaseRequest] { createCaseRequest =>
          createCaseInPegaAndUploadFiles(
            createCaseRequest,
            correlationId,
            auditService.auditCreateCaseEvent(createCaseRequest)
          )

        } {
          // when incoming request's payload validation fails
          case (errorCode, errorMessage) =>
            val response = TraderServicesCaseResponse(
              correlationId = correlationId,
              error = Some(
                ApiError(errorCode, Some(errorMessage))
              )
            )
            auditService
              .auditCreateCaseErrorEvent(response)
              .map(_ => BadRequest(Json.toJson(response)))
        }
      }
        .recoverWith {
          // last resort fallback when request processing fails
          case e =>
            val response = TraderServicesCaseResponse(
              correlationId = correlationId,
              error = Some(
                ApiError("500", Some(e.getMessage()))
              )
            )
            auditService
              .auditCreateCaseErrorEvent(response)
              .map(_ => InternalServerError(Json.toJson(response)))
        }
    }

  // POST /update-case
  def updateCase: Action[String] =
    Action.async(parseTolerantTextUtf8) { implicit request =>
      val correlationId = request.headers
        .get("x-correlation-id")
        .orElse(
          request.headers
            .get("X-Request-Id")
            .map(_.takeRight(36))
        )
        .getOrElse(ju.UUID.randomUUID().toString())

      withAuthorised {
        withPayload[TraderServicesUpdateCaseRequest] { updateCaseRequest =>
          updateCaseInPega(
            updateCaseRequest,
            correlationId,
            auditService.auditUpdateCaseEvent(updateCaseRequest)
          )

        } {
          // when incoming request's payload validation fails
          case (errorCode, errorMessage) =>
            val response = TraderServicesCaseResponse(
              correlationId = correlationId,
              error = Some(
                ApiError(errorCode, Some(errorMessage))
              )
            )
            auditService
              .auditUpdateCaseErrorEvent(response)
              .map(_ => BadRequest(Json.toJson(response)))
        }
      }
        .recoverWith {
          // last resort fallback when request processing fails
          case e =>
            val response = TraderServicesCaseResponse(
              correlationId = correlationId,
              error = Some(
                ApiError("500", Some(e.getMessage()))
              )
            )
            auditService
              .auditUpdateCaseErrorEvent(response)
              .map(_ => InternalServerError(Json.toJson(response)))
        }
    }

  private def createCaseInPegaAndUploadFiles(
    createCaseRequest: TraderServicesCreateCaseRequest,
    correlationId: String,
    audit: TraderServicesCaseResponse => Future[Unit]
  )(implicit
    hc: HeaderCarrier
  ): Future[Result] = {
    val pegaCreateCaseRequest = PegaCreateCaseRequest(
      AcknowledgementReference = correlationId.replace("-", ""),
      ApplicationType = "Route1",
      OriginatingSystem = "Digital",
      Content = PegaCreateCaseRequest.Content.from(createCaseRequest)
    )

    createCaseConnector
      .createCase(pegaCreateCaseRequest, correlationId)
      .flatMap {
        case success: PegaCaseSuccess =>
          transferFilesToPega(
            success.CaseID,
            correlationId,
            createCaseRequest.uploadedFiles,
            appConfig.transferFilesAsync,
            auditFileTransferResults(audit, correlationId, success)
          )
            .map { fileTransferResults =>
              val response = TraderServicesCaseResponse(
                correlationId = correlationId,
                result = Option(
                  TraderServicesResult(success.CaseID, LocalDateTime.now(), fileTransferResults)
                )
              )
              Created(Json.toJson(response))
            }
        // when request to the upstream api returns an error
        case error: PegaCaseError =>
          if (error.isDuplicateCaseError) {
            val response = TraderServicesCaseResponse(
              correlationId = correlationId,
              error = Some(
                ApiError(
                  errorCode = "409",
                  errorMessage = error.duplicateCaseID
                )
              )
            )
            audit(response)
              .map(_ => Conflict(Json.toJson(response)))
          } else {
            val response = TraderServicesCaseResponse(
              correlationId = correlationId,
              error = Some(
                ApiError(
                  errorCode = error.errorCode.getOrElse("ERROR_UPSTREAM_UNDEFINED"),
                  errorMessage = error.errorMessage
                )
              )
            )
            audit(response)
              .map(_ => BadRequest(Json.toJson(response)))
          }
      }
  }

  private def updateCaseInPega(
    updateCaseRequest: TraderServicesUpdateCaseRequest,
    correlationId: String,
    audit: TraderServicesCaseResponse => Future[Unit]
  )(implicit
    hc: HeaderCarrier
  ): Future[Result] = {
    val pegaUpdateCaseRequest = PegaUpdateCaseRequest(
      AcknowledgementReference = correlationId.replace("-", ""),
      ApplicationType = "Route1",
      OriginatingSystem = "Digital",
      Content = PegaUpdateCaseRequest.Content.from(updateCaseRequest)
    )

    updateCaseConnector
      .updateCase(pegaUpdateCaseRequest, correlationId)
      .flatMap {
        case success: PegaCaseSuccess =>
          transferFilesToPega(
            updateCaseRequest.caseReferenceNumber,
            correlationId,
            updateCaseRequest.uploadedFiles,
            appConfig.transferFilesAsync,
            auditFileTransferResults(audit, correlationId, success)
          )
            .map { fileTransferResults =>
              val response = TraderServicesCaseResponse(
                correlationId = correlationId,
                result = Option(
                  TraderServicesResult(success.CaseID, LocalDateTime.now(), fileTransferResults)
                )
              )
              Created(Json.toJson(response))
            }
        // when request to the upstream api returns an error
        case error: PegaCaseError =>
          val response = TraderServicesCaseResponse(
            correlationId = correlationId,
            error = Some(
              ApiError(
                errorCode = error.errorCode.getOrElse("ERROR_UPSTREAM_UNDEFINED"),
                errorMessage = error.errorMessage
              )
            )
          )
          audit(response)
            .map(_ => BadRequest(Json.toJson(response)))
      }
  }

  private def auditFileTransferResults(
    audit: TraderServicesCaseResponse => Future[Unit],
    correlationId: String,
    success: PegaCaseSuccess
  ): Seq[FileTransferResult] => Future[Unit] =
    fileTransferResults => {
      val response = TraderServicesCaseResponse(
        correlationId = correlationId,
        result = Option(
          TraderServicesResult(success.CaseID, LocalDateTime.now(), fileTransferResults)
        )
      )
      audit(response)
    }

  private def transferFilesToPega(
    caseReferenceNumber: String,
    conversationId: String,
    uploadedFiles: Seq[UploadedFile],
    async: Boolean,
    audit: Seq[FileTransferResult] => Future[Unit]
  )(implicit hc: HeaderCarrier): Future[Seq[FileTransferResult]] = {

    val fileTransferRequest: MultiFileTransferRequest =
      MultiFileTransferRequest(
        conversationId,
        caseReferenceNumber,
        "Route1",
        uploadedFiles.map(FileTransferData.fromUploadedFile)
      )

    def doTransferFiles: Future[Seq[FileTransferResult]] = {
      if (fileTransferRequest.files.nonEmpty)
        fileTransferConnector
          .transferMultipleFiles(fileTransferRequest, conversationId)
          .map {
            case Left(status)  => Seq.empty
            case Right(result) => result.results
          }
      else
        Future.successful(Seq.empty)
    }.andThen {
      case Success(results) =>
        audit(results)
    }

    if (async) {
      actorSystem.scheduler.scheduleOnce(
        FiniteDuration(100, "ms"),
        new Runnable {
          override def run(): Unit =
            Await.ready(doTransferFiles, FiniteDuration(5, "min"))
        }
      )
      Future.successful(Seq.empty)
    } else
      doTransferFiles

  }
}
