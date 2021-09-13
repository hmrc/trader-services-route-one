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

package uk.gov.hmrc.traderservices.models

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.traderservices.stubs.MessageUtils

case class MultiFileTransferRequest(
  conversationId: String,
  caseReferenceNumber: String,
  applicationName: String,
  files: Seq[FileTransferData],
  callbackUrl: Option[String] = None
)

case class FileTransferData(
  upscanReference: String,
  downloadUrl: String,
  checksum: String,
  fileName: String,
  fileMimeType: String,
  fileSize: Option[Int] = None
)

object FileTransferData {

  val REASON_REFERENCE = "data:reason"
  val REASON_FILENAME = "reason.txt"

  implicit val formats: Format[FileTransferData] =
    Json.format[FileTransferData]

  def fromUploadedFilesAndReason(
    uploadedFiles: Seq[UploadedFile],
    reason: Option[String]
  ): Seq[FileTransferData] = {
    val files = uploadedFiles.map(FileTransferData.fromUploadedFile)
    reason
      .map(e => files :+ FileTransferData.fromReason(e))
      .getOrElse(files)
  }

  def fromUploadedFile(file: UploadedFile): FileTransferData =
    FileTransferData(
      upscanReference = file.upscanReference,
      downloadUrl = file.downloadUrl,
      checksum = file.checksum,
      fileName = file.fileName,
      fileSize = file.fileSize,
      fileMimeType = file.fileMimeType
    )

  def fromReason(reason: String): FileTransferData = {
    val (base64Message, sha256Checksum) =
      MessageUtils.encodeBase64AndCalculateSHA256(reason)

    FileTransferData(
      upscanReference = REASON_REFERENCE,
      downloadUrl = s"data:text/plain;charset=UTF-8;base64,$base64Message",
      checksum = sha256Checksum,
      fileName = REASON_FILENAME,
      fileSize = Some(reason.length),
      fileMimeType = "text/plain"
    )
  }

}

object MultiFileTransferRequest {

  implicit val formats: Format[MultiFileTransferRequest] =
    Json.format[MultiFileTransferRequest]
}
