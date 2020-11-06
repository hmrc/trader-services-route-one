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

@Singleton
class TraderServicesRouteOneController @Inject() (
  val authConnector: MicroserviceAuthConnector,
  val createCaseConnector: PegaCreateCaseConnector,
  val env: Environment,
  val appConfig: AppConfig,
  cc: ControllerComponents
)(implicit val configuration: Configuration, ec: ExecutionContext)
    extends BackendController(cc) with AuthActions {

  def createCase: Action[AnyContent] =
    Action.async { implicit request =>
      withAuthorisedAsTrader { eori =>
        val createCaseRequest: TraderServicesCreateCaseRequest =
          TraderServicesCreateCaseRequest.formats.reads(request.body.asJson.get).get

        val pegaCreateCaseRequest = PegaCreateCaseRequest(
          AcknowledgementReference = ju.UUID.randomUUID().toString().replace("-", ""),
          ApplicationType = "Route1",
          OriginatingSystem = "Digital",
          Content = PegaCreateCaseRequestContent.from(createCaseRequest)
        )
        createCaseConnector.processCreateCaseRequest(pegaCreateCaseRequest, eori) map {
          case cce: PegaCreateCaseError   => Ok(PegaCreateCaseError.formats.writes(cce))
          case ccs: PegaCreateCaseSuccess => Ok(PegaCreateCaseSuccess.formats.writes(ccs))
        }
      }
    }
}
