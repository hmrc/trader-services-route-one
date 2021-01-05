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

package uk.gov.hmrc.traderservices.controllers

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.HttpEntity
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.Flow
import scala.util.Try
import akka.NotUsed
import akka.http.scaladsl.model.HttpResponse
import scala.util.Success
import scala.util.Failure
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.headers.Date
import akka.http.scaladsl.model.DateTime
import uk.gov.hmrc.traderservices.models._
import akka.http.scaladsl.Http
import akka.actor.ActorSystem
import uk.gov.hmrc.traderservices.wiring.AppConfig
import akka.stream.scaladsl.Source
import scala.concurrent.Future
import play.api.mvc.Result
import play.api.mvc.Results._
import akka.util.ByteString
import play.api.Logger

trait FileTransferFlow {

  val appConfig: AppConfig
  implicit val actorSystem: ActorSystem

  final val downloadPool: Flow[
    (HttpRequest, TraderServicesFileTransferRequest),
    (Try[HttpResponse], TraderServicesFileTransferRequest),
    NotUsed
  ] = Http()
    .superPool[TraderServicesFileTransferRequest]()

  final val uploadPool: Flow[
    (HttpRequest, TraderServicesFileTransferRequest),
    (Try[HttpResponse], TraderServicesFileTransferRequest),
    Http.HostConnectionPool
  ] = Http()
    .cachedHostConnectionPool[TraderServicesFileTransferRequest](
      appConfig.eisFileTransferHost,
      appConfig.eisFileTransferPort
    )

  /**
    * Akka Stream flow:
    * - requests downloading the file,
    * - encodes file content stream using base64,
    * - wraps base64 content in a json payload,
    * - forwards to the upstream endpoint.
    */
  final val fileTransferFlow
    : Flow[TraderServicesFileTransferRequest, (Try[HttpResponse], TraderServicesFileTransferRequest), NotUsed] =
    Flow[TraderServicesFileTransferRequest]
      .map { request =>
        (
          HttpRequest(
            method = HttpMethods.GET,
            uri = request.downloadUrl
          ),
          request
        )
      }
      .via(downloadPool)
      .flatMapConcat {
        case (Success(fileDownloadResponse), fileTransferRequest) =>
          if (fileDownloadResponse.status.isSuccess()) {

            val fileEncodeAndWrapSource: Source[ByteString, Future[FileSizeAndChecksum]] =
              fileDownloadResponse.entity.dataBytes
                .viaMat(EncodeFileBase64)(Keep.right)
                .viaMat(new WrapInEnvelope(s"""{
                                              |    "CaseReferenceNumber":"${fileTransferRequest.caseReferenceNumber}",
                                              |    "ApplicationType":"Route1",
                                              |    "OriginatingSystem":"Digital",
                                              |    "Content":"""".stripMargin, "\"\n}"))(Keep.left)

            val eisUploadRequest = HttpRequest(
              method = HttpMethods.POST,
              uri = appConfig.eisBaseUrl + appConfig.eisFileTransferApiPath,
              headers = collection.immutable.Seq(
                RawHeader("x-conversation-id", fileTransferRequest.conversationId),
                RawHeader("x-correlation-id", fileTransferRequest.correlationId.getOrElse("")),
                RawHeader("customprocesseshost", "Digital"),
                RawHeader("accept", "application/json"),
                RawHeader("authorization", s"Bearer ${appConfig.eisAuthorizationToken}"),
                RawHeader("checksumAlgorithm", "SHA-256"),
                RawHeader("checksum", fileTransferRequest.checksum),
                Date(DateTime.now),
                RawHeader(
                  "x-metadata",
                  FileTransferMetadataHeader(
                    caseReferenceNumber = fileTransferRequest.caseReferenceNumber,
                    applicationName = fileTransferRequest.applicationName,
                    correlationId = fileTransferRequest.correlationId.getOrElse(""),
                    conversationId = fileTransferRequest.conversationId,
                    sourceFileName = fileTransferRequest.fileName,
                    sourceFileMimeType = fileTransferRequest.fileMimeType,
                    checksum = fileTransferRequest.checksum,
                    batchSize = fileTransferRequest.batchSize,
                    batchCount = fileTransferRequest.batchCount
                  ).toXmlString
                )
              ),
              entity = HttpEntity.apply(ContentTypes.`application/json`, fileEncodeAndWrapSource)
            )

            val source: Source[(Try[HttpResponse], TraderServicesFileTransferRequest), NotUsed] =
              Source
                .single((eisUploadRequest, fileTransferRequest))
                .via(uploadPool)

            source
          } else {
            val error = new Exception(
              s"File download request [${fileTransferRequest.downloadUrl}] failed with status [${fileDownloadResponse.status.intValue()}]."
            )
            Source
              .failed(error)
          }

        case (Failure(fileDownloadError), fileTransferRequest) =>
          Source
            .failed(fileDownloadError)
      }

  final def executeSingleFileTransfer(
    fileTransferRequest: TraderServicesFileTransferRequest
  ): Future[Result] =
    Source
      .single(fileTransferRequest)
      .via(fileTransferFlow)
      .runFold[Result](Ok) {
        case (_, (Success(uploadResponse), fileTransferRequest)) =>
          uploadResponse.discardEntityBytes()
          Logger(getClass).info(s"Successful transfer of [${fileTransferRequest.upscanReference}].")
          Status(uploadResponse.status.intValue())

        case (_, (Failure(uploadError), fileTransferRequest)) =>
          Logger(getClass).error(
            s"Transfer of [${fileTransferRequest.upscanReference}] failed because of [${uploadError.getMessage()}]."
          )
          InternalServerError
      }

}
