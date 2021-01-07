package uk.gov.hmrc.traderservices.controllers

import java.time.LocalDateTime
import org.scalatest.Suite
import org.scalatestplus.play.ServerProvider
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import uk.gov.hmrc.traderservices.stubs._
import uk.gov.hmrc.traderservices.support.ServerBaseISpec
import uk.gov.hmrc.traderservices.support.JsonMatchers
import com.github.tomakehurst.wiremock.http.Fault

class FileTransferControllerISpec extends ServerBaseISpec with AuthStubs with FileTransferStubs with JsonMatchers {
  this: Suite with ServerProvider =>

  val url = s"http://localhost:$port"

  val dateTime = LocalDateTime.now()

  val wsClient = app.injector.instanceOf[WSClient]

  "FileTransferController" when {
    "POST /transfer-file" should {
      testFileTransferSuccess("prod.routes")
      testFileTransferSuccess("app.routes")
      testFileTransferSuccess("schema.json")
      testFileTransferSuccess("logback.xml")
      testFileTransferSuccess("test1.jpeg")

      testFileUploadFailure("prod.routes", 500)
      testFileUploadFailure("app.routes", 404)
      testFileUploadFailure("schema.json", 501)
      testFileUploadFailure("logback.xml", 409)
      testFileUploadFailure("test1.jpeg", 403)

      testFileDownloadFailure("prod.routes", 400)
      testFileDownloadFailure("app.routes", 403)
      testFileDownloadFailure("schema.json", 500)
      testFileDownloadFailure("logback.xml", 501)
      testFileDownloadFailure("test1.jpeg", 404)

      testFileDownloadFault("test1.jpeg", 200, Fault.RANDOM_DATA_THEN_CLOSE)
      testFileDownloadFault("test1.jpeg", 200, Fault.MALFORMED_RESPONSE_CHUNK)
      testFileDownloadFault("test1.jpeg", 200, Fault.CONNECTION_RESET_BY_PEER)
      testFileDownloadFault("test1.jpeg", 200, Fault.EMPTY_RESPONSE)
    }
  }

  def testFileTransferSuccess(fileName: String) {
    s"return 202 when transferring $fileName succeeds" in new FileTransferTest(fileName) {
      givenAuthorised()
      val fileUrl =
        givenFileTransferSucceeds("Risk-123", fileName, bytes, base64Content, checksum, fileSize, xmlMetadataHeader)

      val result = wsClient
        .url(s"$url/transfer-file")
        .withHttpHeaders("X-Correlation-ID" -> correlationId, "Authorization" -> "Bearer ABC0123456789")
        .post(Json.parse(jsonPayload))
        .futureValue

      result.status shouldBe 202
      verifyAuthorisationHasHappened()
    }
  }

  def testFileUploadFailure(fileName: String, status: Int) {
    s"return 500 when uploading $fileName fails because of $status" in new FileTransferTest(fileName) {
      givenAuthorised()
      val fileUrl =
        givenFileUploadFails(status, "Risk-123", fileName, bytes, base64Content, checksum, fileSize, xmlMetadataHeader)

      val result = wsClient
        .url(s"$url/transfer-file")
        .withHttpHeaders("X-Correlation-ID" -> correlationId, "Authorization" -> "Bearer ABC0123456789")
        .post(Json.parse(jsonPayload))
        .futureValue

      result.status shouldBe status
      verifyAuthorisationHasHappened()
    }
  }

  def testFileDownloadFailure(fileName: String, status: Int) {
    s"return 500 when downloading $fileName fails because of $status" in new FileTransferTest(fileName) {
      givenAuthorised()
      val fileUrl =
        givenFileDownloadFails(
          status,
          "Risk-123",
          fileName,
          s"This is an expected error requested by the test, no worries.",
          base64Content,
          checksum,
          fileSize,
          xmlMetadataHeader
        )

      val result = wsClient
        .url(s"$url/transfer-file")
        .withHttpHeaders("X-Correlation-ID" -> correlationId, "Authorization" -> "Bearer ABC0123456789")
        .post(Json.parse(jsonPayload))
        .futureValue

      result.status shouldBe 500
      verifyAuthorisationHasHappened()
    }
  }

  def testFileDownloadFault(fileName: String, status: Int, fault: Fault) {
    s"return 500 when downloading $fileName fails because of $fault" in new FileTransferTest(fileName) {
      givenAuthorised()
      val fileUrl =
        givenFileDownloadFault(
          status,
          fault,
          "Risk-123",
          fileName,
          bytes,
          base64Content,
          checksum,
          fileSize,
          xmlMetadataHeader
        )

      val result = wsClient
        .url(s"$url/transfer-file")
        .withHttpHeaders("X-Correlation-ID" -> correlationId, "Authorization" -> "Bearer ABC0123456789")
        .post(Json.parse(jsonPayload))
        .futureValue

      result.status shouldBe 500
      verifyAuthorisationHasHappened()
    }
  }

}
