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

import java.time.LocalDateTime
import java.util.UUID

import javax.inject.{Inject, Singleton}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Success
import akka.actor.ActorSystem
import play.api.libs.json.Json
import play.api.mvc._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.traderservices.connectors.{PegaCreateCaseRequest, _}
import uk.gov.hmrc.traderservices.models._
import uk.gov.hmrc.traderservices.services.AuditService
import uk.gov.hmrc.traderservices.wiring.AppConfig

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
        .getOrElse(UUID.randomUUID().toString)

      withAuthorised {
        withPayload[TraderServicesCreateCaseRequest] { createCaseRequest =>
          createCaseInPegaAndUploadFiles(
            createCaseRequest,
            correlationId,
            createCaseResult => {
              CreateCaseLog.log(createCaseRequest, createCaseResult)
              auditService.auditCreateCaseEvent(createCaseRequest)(createCaseResult)
            }
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
        .getOrElse(UUID.randomUUID().toString)

      withAuthorised {
        withPayload[TraderServicesUpdateCaseRequest] { updateCaseRequest =>
          updateCaseInPega(
            updateCaseRequest,
            correlationId,
            updateCaseResult => {
              UpdateCaseLog.log(updateCaseRequest, updateCaseResult)
              auditService.auditUpdateCaseEvent(updateCaseRequest)(updateCaseResult)
            }
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

  final def createCaseInPegaAndUploadFiles(
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
            createCaseRequest.questionsAnswers.reason,
            appConfig.transferFilesAsync,
            auditFileTransferResults(audit, correlationId, success)
          )
            .map { multiFileTransferResult =>
              val response = TraderServicesCaseResponse(
                correlationId = correlationId,
                result = Option(
                  TraderServicesResult(
                    success.CaseID,
                    LocalDateTime.now(),
                    multiFileTransferResult.map(_.results).getOrElse(Seq.empty),
                    multiFileTransferResult.map(_.totalDurationMillis)
                  )
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

  final def updateCaseInPega(
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
            None,
            appConfig.transferFilesAsync,
            auditFileTransferResults(audit, correlationId, success)
          )
            .map { multiFileTransferResult =>
              val response = TraderServicesCaseResponse(
                correlationId = correlationId,
                result = Option(
                  TraderServicesResult(
                    success.CaseID,
                    LocalDateTime.now(),
                    multiFileTransferResult.map(_.results).getOrElse(Seq.empty),
                    multiFileTransferResult.map(_.totalDurationMillis)
                  )
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
  ): Option[MultiFileTransferResult] => Future[Unit] =
    multiFileTransferResult => {
      val response = TraderServicesCaseResponse(
        correlationId = correlationId,
        result = Option(
          TraderServicesResult(
            success.CaseID,
            LocalDateTime.now(),
            multiFileTransferResult.map(_.results).getOrElse(Seq.empty),
            multiFileTransferResult.map(_.totalDurationMillis)
          )
        )
      )
      audit(response)
    }

  final def transferFilesToPega(
    caseReferenceNumber: String,
    conversationId: String,
    uploadedFiles: Seq[UploadedFile],
    reason: Option[String],
    async: Boolean,
    audit: Option[MultiFileTransferResult] => Future[Unit]
  )(implicit hc: HeaderCarrier): Future[Option[MultiFileTransferResult]] = {
    val fileTransferRequest: MultiFileTransferRequest =
      MultiFileTransferRequest(
        conversationId,
        caseReferenceNumber,
        "Route1",
        FileTransferData.fromUploadedFilesAndReason(uploadedFiles, reason)
      )

    def doTransferFiles: Future[Option[MultiFileTransferResult]] = {
      if (fileTransferRequest.files.nonEmpty)
        fileTransferConnector
          .transferMultipleFiles(fileTransferRequest, conversationId)
          .map {
            case Left(status)  => None
            case Right(result) => Some(result)
          }
      else
        Future.successful(None)
    }.andThen { case Success(results) =>
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
      Future.successful(None)
    } else
      doTransferFiles

  }
}
