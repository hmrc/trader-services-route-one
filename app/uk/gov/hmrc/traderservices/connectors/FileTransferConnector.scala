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
import uk.gov.hmrc.traderservices.models.{FileTransferRequest, FileTransferResult, MultiFileTransferRequest, MultiFileTransferResult}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.HttpReads.Implicits._
import java.time.LocalDateTime
import akka.actor.ActorSystem
import scala.concurrent.duration._

@Singleton
class FileTransferConnector @Inject() (
  val config: AppConfig,
  val http: HttpPost,
  metrics: Metrics,
  val actorSystem: ActorSystem
) extends HttpAPIMonitor with Retries {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  final lazy val fileTransferUrl = config.fileTransferUrl
  final lazy val multiFileTransferUrl = config.multiFileTransferUrl

  final def transferFile(fileTransferRequest: FileTransferRequest, correlationId: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[FileTransferResult] =
    retry[HttpResponse](1.second, 2.seconds)(shouldRetry, errorMessage) {
      monitor(s"ConsumedAPI-trader-services-transfer-file-api-POST") {
        http
          .POST[FileTransferRequest, HttpResponse](fileTransferUrl, fileTransferRequest)
      }
    }.map(response =>
      FileTransferResult(
        fileTransferRequest.upscanReference,
        fileTransferRequest.checksum,
        fileTransferRequest.fileName,
        fileTransferRequest.fileMimeType,
        fileTransferRequest.fileSize,
        isSuccess(response),
        response.status,
        LocalDateTime.now(),
        None
      )
    )

  final def transferMultipleFiles(
    multipleFileTransferRequest: MultiFileTransferRequest,
    correlationId: String
  )(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Either[Int, MultiFileTransferResult]] =
    retry[HttpResponse](1.second, 2.seconds)(shouldRetry, errorMessage) {
      monitor(s"ConsumedAPI-trader-services-transfer-multiple-files-api-POST") {
        http
          .POST[MultiFileTransferRequest, HttpResponse](multiFileTransferUrl, multipleFileTransferRequest)
      }
    }.map(response =>
      if (isSuccess(response))
        Right(response.json.as[MultiFileTransferResult])
      else
        Left(response.status.intValue())
    )

  private def isSuccess(response: HttpResponse): Boolean =
    response.status >= 200 && response.status < 300

  final def shouldRetry(response: HttpResponse): Boolean =
    response.status == 499 || response.status >= 500

  final def errorMessage(response: HttpResponse): String =
    s"HTTP response status ${response.status}"

}
