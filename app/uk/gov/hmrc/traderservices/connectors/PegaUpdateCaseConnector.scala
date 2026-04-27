/*
 * Copyright 2026 HM Revenue & Customs
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

import com.codahale.metrics.MetricRegistry
import org.apache.pekko.actor.ActorSystem
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.metrics.Metrics
import uk.gov.hmrc.traderservices.wiring.AppConfig

import java.net.URI
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PegaUpdateCaseConnector @Inject() (
  val config: AppConfig,
  val http: HttpClientV2,
  metrics: Metrics,
  val actorSystem: ActorSystem
) extends ReadSuccessOrFailure[PegaCaseResponse, PegaCaseSuccess, PegaCaseError](
      PegaCaseError.fromStatusAndMessage
    ) with PegaConnector with HttpAPIMonitor with Retries {

  override val metricRegistry: MetricRegistry = metrics.defaultRegistry

  final val url = config.eisBaseUrl + config.eisUpdateCaseApiPath

  final def updateCase(createCaseRequest: PegaUpdateCaseRequest, correlationId: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[PegaCaseResponse] =
    retry(1.second, 2.seconds)(PegaCaseResponse.shouldRetry, PegaCaseResponse.errorMessage) {
      monitor(s"ConsumedAPI-eis-pega-update-case-api-POST") {
        http
          .post(new URI(url).toURL)
          .withBody(Json.toJson(createCaseRequest))
          .setHeader(pegaApiHeaders(correlationId, config.eisEnvironment, config.eisAuthorizationToken): _*)
          .execute[PegaCaseResponse](readFromJsonSuccessOrFailure)
      }
    }

}
