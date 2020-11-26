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

package uk.gov.hmrc.traderservices.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.traderservices.connectors._
import uk.gov.hmrc.traderservices.models._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.traderservices.wiring.AppConfig

import scala.concurrent.ExecutionContext
import uk.gov.hmrc.traderservices.connectors.PegaCreateCaseRequest
import java.{util => ju}
import views.html.defaultpages.error

@Singleton
class TraderServicesRouteOneController @Inject() (
  val authConnector: MicroserviceAuthConnector,
  val createCaseConnector: PegaCreateCaseConnector,
  val updateCaseConnector: PegaUpdateCaseConnector,
  val env: Environment,
  val appConfig: AppConfig,
  cc: ControllerComponents
)(implicit val configuration: Configuration, ec: ExecutionContext)
    extends BackendController(cc) with AuthActions with ControllerHelper {

  // POST /create-case
  def createCase: Action[String] =
    Action.async(parse.tolerantText) { implicit request =>
      withAuthorised {
        val correlationId = request.headers
          .get("x-correlation-id")
          .getOrElse(ju.UUID.randomUUID().toString())

        withPayload[TraderServicesCreateCaseRequest] { createCaseRequest =>
          val pegaCreateCaseRequest = PegaCreateCaseRequest(
            AcknowledgementReference = correlationId.replace("-", ""),
            ApplicationType = "Route1",
            OriginatingSystem = "Digital",
            Content = PegaCreateCaseRequest.Content.from(createCaseRequest)
          )

          createCaseConnector
            .createCase(pegaCreateCaseRequest, correlationId) map {
            case success: PegaCaseSuccess =>
              Created(
                Json.toJson(
                  TraderServicesCaseResponse(
                    correlationId = correlationId,
                    result = Some(success.CaseID)
                  )
                )
              )
            // when request to the upstream api returns an error
            case error: PegaCaseError =>
              if (error.isDuplicateCaseError)
                Conflict(
                  Json.toJson(
                    TraderServicesCaseResponse(
                      correlationId = correlationId,
                      error = Some(
                        ApiError(
                          errorCode = "409",
                          errorMessage = error.duplicateCaseID
                        )
                      )
                    )
                  )
                )
              else
                BadRequest(
                  Json.toJson(
                    TraderServicesCaseResponse(
                      correlationId = correlationId,
                      error = Some(
                        ApiError(
                          errorCode = error.errorCode.getOrElse("ERROR_UPSTREAM_UNDEFINED"),
                          errorMessage = error.errorMessage
                        )
                      )
                    )
                  )
                )
          }

        } {
          // when incoming request's payload validation fails
          case (errorCode, errorMessage) =>
            BadRequest(
              Json.toJson(
                TraderServicesCaseResponse(
                  correlationId = correlationId,
                  error = Some(
                    ApiError(errorCode, Some(errorMessage))
                  )
                )
              )
            )
        }
      }
    }

  // POST /update-case
  def updateCase: Action[String] =
    Action.async(parse.tolerantText) { implicit request =>
      withAuthorised {
        val correlationId = request.headers
          .get("x-correlation-id")
          .getOrElse(ju.UUID.randomUUID().toString())

        withPayload[TraderServicesUpdateCaseRequest] { createCaseRequest =>
          val pegaCreateCaseRequest = PegaUpdateCaseRequest(
            AcknowledgementReference = correlationId.replace("-", ""),
            ApplicationType = "Route1",
            OriginatingSystem = "Digital",
            Content = PegaUpdateCaseRequest.Content.from(createCaseRequest)
          )

          updateCaseConnector
            .updateCase(pegaCreateCaseRequest, correlationId) map {
            case success: PegaCaseSuccess =>
              Created(
                Json.toJson(
                  TraderServicesCaseResponse(
                    correlationId = correlationId,
                    result = Some(success.CaseID)
                  )
                )
              )
            // when request to the upstream api returns an error
            case error: PegaCaseError =>
              BadRequest(
                Json.toJson(
                  TraderServicesCaseResponse(
                    correlationId = correlationId,
                    error = Some(
                      ApiError(
                        errorCode = error.errorCode.getOrElse("ERROR_UPSTREAM_UNDEFINED"),
                        errorMessage = error.errorMessage
                      )
                    )
                  )
                )
              )
          }

        } {
          // when incoming request's payload validation fails
          case (errorCode, errorMessage) =>
            BadRequest(
              Json.toJson(
                TraderServicesCaseResponse(
                  correlationId = correlationId,
                  error = Some(
                    ApiError(errorCode, Some(errorMessage))
                  )
                )
              )
            )
        }
      }
    }
}
