package uk.gov.hmrc.traderservices.controllers

import akka.actor.Actor
import akka.actor.ActorRef
import akka.pattern.pipe
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.traderservices.connectors._
import uk.gov.hmrc.traderservices.models._

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class FileTransferActor(
  caseReferenceNumber: String,
  fileTransferConnector: FileTransferConnector,
  conversationId: String,
  audit: Seq[FileTransferResult] => Future[Unit]
) extends Actor {

  import FileTransferActor._
  import context.dispatcher

  var results: Seq[FileTransferResult] = Seq.empty
  var clientRef: ActorRef = ActorRef.noSender
  var startTimestamp: Long = 0

  def transferFileRequest(file: UploadedFile, index: Int, batchSize: Int): TraderServicesFileTransferRequest =
    TraderServicesFileTransferRequest
      .fromUploadedFile(
        caseReferenceNumber,
        conversationId,
        correlationId = UUID.randomUUID().toString(),
        applicationName = "Route1",
        batchSize = batchSize,
        batchCount = index + 1,
        uploadedFile = file
      )

  def doTransferFile(file: UploadedFile, index: Int, batchSize: Int)(implicit
    hc: HeaderCarrier
  ): Future[FileTransferResult] =
    fileTransferConnector
      .transferFile(transferFileRequest(file, index, batchSize), conversationId)

  override def receive: Receive = {
    case TransferMultipleFiles(files, batchSize, headerCarrier) =>
      startTimestamp = System.currentTimeMillis()
      clientRef = sender()
      files
        .map {
          case (file, index) => TransferSingleFile(file, index, batchSize, headerCarrier)
        }
        .foreach(request => self ! request)
      self ! CheckComplete(batchSize)

    case TransferSingleFile(file, index, batchSize, headerCarrier) =>
      doTransferFile(file, index, batchSize)(headerCarrier)
        .pipeTo(sender())

    case result: FileTransferResult =>
      results = results :+ result

    case akka.actor.Status.Failure(error @ UpstreamErrorResponse(message, code, _, _)) =>
      Logger(getClass).error(error.toString())
      results = results :+ FileTransferResult(
        upscanReference = "<unknown>",
        success = false,
        httpStatus = code,
        LocalDateTime.now(),
        error = Some(message)
      )

    case akka.actor.Status.Failure(error) =>
      Logger(getClass).error(error.toString())
      results = results :+ FileTransferResult(
        upscanReference = "<unknown>",
        success = false,
        httpStatus = 0,
        LocalDateTime.now(),
        error = Some(error.toString())
      )

    case CheckComplete(batchSize) =>
      if (results.size == batchSize || System.currentTimeMillis() - startTimestamp > 3600000 /*hour*/ ) {
        clientRef ! results
        audit(results)
        context.stop(self)
        Logger(getClass).info(
          s"Transferred ${results.size} out of $batchSize files in ${(System
            .currentTimeMillis() - startTimestamp) / 1000} seconds. It was ${results
            .count(_.success)} successes and ${results.count(f => !f.success)} failures."
        )
      } else
        context.system.scheduler
          .scheduleOnce(FiniteDuration(500, "ms"), self, CheckComplete(batchSize))
  }
}

object FileTransferActor {
  case class TransferMultipleFiles(files: Seq[(UploadedFile, Int)], batchSize: Int, hc: HeaderCarrier)
  case class TransferSingleFile(file: UploadedFile, index: Int, batchSize: Int, hc: HeaderCarrier)
  case class CheckComplete(batchSize: Int)
}
