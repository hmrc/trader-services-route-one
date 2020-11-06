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

@Singleton
class PegaCreateCaseConnector @Inject() (val config: AppConfig, val http: HttpPost, metrics: Metrics)
    extends HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  val url = config.caseBaseUrl + config.createCaseUrl

  def processCreateCaseRequest(createCaseRequest: PegaCreateCaseRequest, eori: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[PegaCreateCaseResponse] =
    monitor(s"ConsumedAPI-eis-pega-create-case-api-POST") {
      http.POST[PegaCreateCaseRequest, PegaCreateCaseResponse](url, createCaseRequest)
    }

}
