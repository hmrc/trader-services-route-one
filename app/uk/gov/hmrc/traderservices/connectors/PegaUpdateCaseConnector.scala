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

package uk.gov.hmrc.traderservices.connectors

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.{HeaderCarrier, HttpPost}
import uk.gov.hmrc.traderservices.wiring.AppConfig

import scala.concurrent.{ExecutionContext, Future}
import com.kenshoo.play.metrics.Metrics
import com.codahale.metrics.MetricRegistry
import play.api.libs.json.Writes
import uk.gov.hmrc.http.logging.Authorization

@Singleton
class PegaUpdateCaseConnector @Inject() (val config: AppConfig, val http: HttpPost, metrics: Metrics)
    extends ReadSuccessOrFailure[PegaCaseResponse, PegaCaseSuccess, PegaCaseError](
      PegaCaseError.fromStatusAndMessage
    ) with PegaConnector with HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  val url = config.eisBaseUrl + config.eisUpdateCaseApiPath

  def updateCase(createCaseRequest: PegaUpdateCaseRequest, correlationId: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[PegaCaseResponse] =
    monitor(s"ConsumedAPI-eis-pega-update-case-api-POST") {
      http
        .POST[PegaUpdateCaseRequest, PegaCaseResponse](url, createCaseRequest)(
          implicitly[Writes[PegaUpdateCaseRequest]],
          readFromJsonSuccessOrFailure,
          HeaderCarrier(
            authorization = Some(Authorization(s"Bearer ${config.eisAuthorizationToken}"))
          )
            .withExtraHeaders(pegaApiHeaders(correlationId, config.eisEnvironment): _*),
          implicitly[ExecutionContext]
        )
    }

}
