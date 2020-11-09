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

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.traderservices.connectors.PegaCreateCaseRequest
import java.{util => ju}
import views.html.defaultpages.error

@Singleton
class TraderServicesRouteOneController @Inject() (
  val authConnector: MicroserviceAuthConnector,
  val createCaseConnector: PegaCreateCaseConnector,
  val env: Environment,
  val appConfig: AppConfig,
  cc: ControllerComponents
)(implicit val configuration: Configuration, ec: ExecutionContext)
    extends BackendController(cc) with AuthActions with ControllerHelper {

  def createCase: Action[String] =
    Action.async(parse.tolerantText) { implicit request =>
      withAuthorisedAsTrader { eori =>
        val correlationId = request.headers
          .get("x-correlation-id")
          .getOrElse(ju.UUID.randomUUID().toString())

        withPayload[TraderServicesCreateCaseRequest] { createCaseRequest =>
          val pegaCreateCaseRequest = PegaCreateCaseRequest(
            AcknowledgementReference = correlationId.replace("-", ""),
            ApplicationType = "Route1",
            OriginatingSystem = "Digital",
            Content = PegaCreateCaseRequestContent.from(createCaseRequest)
          )

          createCaseConnector
            .createCase(pegaCreateCaseRequest, eori, correlationId) map {
            case success: PegaCreateCaseSuccess =>
              Created(
                Json.toJson(
                  TraderServicesCreateCaseResponse(
                    correlationId = correlationId,
                    result = Some(success.CaseID)
                  )
                )
              )
            // when request to the upstream api returns an error
            case error: PegaCreateCaseError =>
              if (error.isDuplicateCaseError)
                Conflict(
                  Json.toJson(
                    TraderServicesCreateCaseResponse(
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
                    TraderServicesCreateCaseResponse(
                      correlationId = correlationId,
                      error = Some(
                        ApiError(
                          errorCode = error.ErrorCode.getOrElse("ERROR_UPSTREAM_UNDEFINED"),
                          errorMessage = error.ErrorMessage
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
                TraderServicesCreateCaseResponse(
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
