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
import play.api.libs.json.Json.toJson
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.traderservices.connectors.{CreateCaseConnector, CreateCaseError, CreateCaseSuccess, MicroserviceAuthConnector}
import uk.gov.hmrc.traderservices.models.{CreateImportCaseRequest, TraderServicesModel}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.traderservices.wiring.AppConfig

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TraderServicesController @Inject() (
  val authConnector: MicroserviceAuthConnector,
  val createCaseConnector: CreateCaseConnector,
  val env: Environment,
  val appConfig: AppConfig,
  cc: ControllerComponents
)(implicit val configuration: Configuration, ec: ExecutionContext)
    extends BackendController(cc) with AuthActions {

  def entities: Action[AnyContent] =
    Action.async { implicit request =>
      withAuthorisedAsTrader { eori =>
        Future.successful(Ok(toJson(TraderServicesModel(s"hello $eori", None, None, None))))
      }
    }

  def entitiesByUtr(utr: Utr): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthorisedAsTrader { eori =>
        Future.successful(
          Ok(toJson(TraderServicesModel(s"hello $utr and $eori", None, None, None)))
        )
      }
    }

  def createCase: Action[AnyContent] =
    Action.async { implicit request =>
      withAuthorisedAsTrader { eori =>
        val createCaseRequest = TraderServicesCreateCaseRequest.formats.reads(request.body.asJson.get).get
        createCaseConnector.processCreateCaseRequest(createCaseRequest, eori) map {
          case cce: CreateCaseError   => Ok(CreateCaseError.formats.writes(cce))
          case ccs: CreateCaseSuccess => Ok(CreateCaseSuccess.formats.writes(ccs))
        }
      }
    }
}
