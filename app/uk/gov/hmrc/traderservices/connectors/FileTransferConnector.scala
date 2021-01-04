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

package uk.gov.hmrc.traderservices.connectors

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.{HeaderCarrier, HttpPost}
import uk.gov.hmrc.traderservices.wiring.AppConfig

import scala.concurrent.{ExecutionContext, Future}
import com.kenshoo.play.metrics.Metrics
import com.codahale.metrics.MetricRegistry
import uk.gov.hmrc.traderservices.models.{FileTransferResult, TraderServicesFileTransferRequest}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.HttpReads.Implicits._
import java.time.LocalDateTime

@Singleton
class FileTransferConnector @Inject() (
  val config: AppConfig,
  val http: HttpPost,
  metrics: Metrics
) extends HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  final lazy val url = config.fileTransferUrl

  final def transferFile(fileTransferRequest: TraderServicesFileTransferRequest, correlationId: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[FileTransferResult] =
    monitor(s"ConsumedAPI-trader-services-transfer-file-api-POST") {
      http
        .POST[TraderServicesFileTransferRequest, HttpResponse](url, fileTransferRequest)
        .map(response =>
          FileTransferResult(
            fileTransferRequest.upscanReference,
            isSuccess(response),
            response.status,
            LocalDateTime.now(),
            None
          )
        )
    }

  private def isSuccess(response: HttpResponse): Boolean =
    response.status >= 200 && response.status < 300

}
