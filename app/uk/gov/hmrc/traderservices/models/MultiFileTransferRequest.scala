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

  implicit val formats: Format[FileTransferData] =
    Json.format[FileTransferData]
}

object MultiFileTransferRequest {

  implicit val formats: Format[MultiFileTransferRequest] =
    Json.format[MultiFileTransferRequest]
}
