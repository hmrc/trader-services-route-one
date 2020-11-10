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
import uk.gov.hmrc.http.HttpReads
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.{util => ju}
import java.time.ZonedDateTime
import uk.gov.hmrc.http.logging.Authorization

@Singleton
class PegaCreateCaseConnector @Inject() (val config: AppConfig, val http: HttpPost, metrics: Metrics)
    extends ReadSuccessOrFailure[PegaCreateCaseResponse, PegaCreateCaseSuccess, PegaCreateCaseError]
    with HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  val httpDateFormat = DateTimeFormatter
    .ofPattern("EEE, dd MMM yyyy HH:mm:ss z", ju.Locale.ENGLISH)
    .withZone(ZoneId.of("GMT"))

  val url = config.createCaseApiBaseUrl + config.createCaseApiPath

  def createCase(createCaseRequest: PegaCreateCaseRequest, correlationId: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[PegaCreateCaseResponse] =
    monitor(s"ConsumedAPI-eis-pega-create-case-api-POST") {
      http
        .POST[PegaCreateCaseRequest, PegaCreateCaseResponse](url, createCaseRequest)(
          implicitly[Writes[PegaCreateCaseRequest]],
          readFromJsonSuccessOrFailure,
          HeaderCarrier(authorization = Some(Authorization(s"Bearer ${config.createCaseApiAuthorizationToken}")))
            .withExtraHeaders(
              "x-correlation-id" -> correlationId,
              "x-forwarded-host" -> config.appName,
              "date"             -> httpDateFormat.format(ZonedDateTime.now),
              "accept"           -> "application/json",
              "authorization"    -> s"Bearer ${config.createCaseApiAuthorizationToken}",
              "environment"      -> config.createCaseApiEnvironment
            ),
          implicitly[ExecutionContext]
        )
    }

}
