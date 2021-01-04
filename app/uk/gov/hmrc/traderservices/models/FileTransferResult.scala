package uk.gov.hmrc.traderservices.models

import play.api.libs.json.Format
import play.api.libs.json.Json
import java.time.LocalDateTime

final case class FileTransferResult(
  upscanReference: String,
  success: Boolean,
  httpStatus: Int,
  uploadedAt: LocalDateTime,
  error: Option[String] = None
)

object FileTransferResult {
  implicit val formats: Format[FileTransferResult] =
    Json.format[FileTransferResult]
}
