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
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.metrics.Metrics
import uk.gov.hmrc.traderservices.models.{FileTransferRequest, FileTransferResult, MultiFileTransferRequest, MultiFileTransferResult}
import uk.gov.hmrc.traderservices.wiring.AppConfig

import java.net.URI
import java.time.LocalDateTime
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileTransferConnector @Inject() (
  val config: AppConfig,
  val http: HttpClientV2,
  metrics: Metrics,
  val actorSystem: ActorSystem
) extends HttpAPIMonitor with Retries {

  override val metricRegistry: MetricRegistry = metrics.defaultRegistry

  private final lazy val fileTransferUrl = config.fileTransferUrl
  private final lazy val multiFileTransferUrl = config.multiFileTransferUrl

  final def transferFile(fileTransferRequest: FileTransferRequest, correlationId: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[FileTransferResult] =
    retry[HttpResponse](1.second, 2.seconds)(shouldRetry, errorMessage) {
      monitor(s"ConsumedAPI-trader-services-transfer-file-api-POST") {
        http
          .post(new URI(fileTransferUrl).toURL)
          .setHeader(("x-correlation-id", correlationId))
          .withBody(Json.toJson(fileTransferRequest))
          .execute[HttpResponse]
      }
    }.map(response =>
      FileTransferResult(
        fileTransferRequest.upscanReference,
        fileTransferRequest.checksum,
        fileTransferRequest.fileName,
        fileTransferRequest.fileMimeType,
        fileTransferRequest.fileSize.getOrElse(0),
        isSuccess(response),
        response.status,
        LocalDateTime.now(),
        hc.requestId.map(_.value).getOrElse(""),
        0,
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
          .post(new URI(multiFileTransferUrl).toURL)
          .setHeader(("x-correlation-id", correlationId))
          .withBody(Json.toJson(multipleFileTransferRequest))
          .execute[HttpResponse]
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
