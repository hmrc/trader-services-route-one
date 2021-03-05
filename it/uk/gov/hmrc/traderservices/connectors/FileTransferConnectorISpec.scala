package uk.gov.hmrc.traderservices.connectors

import play.api.Application
import uk.gov.hmrc.traderservices.models._
import uk.gov.hmrc.traderservices.support.AppBaseISpec
import uk.gov.hmrc.http._

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.Json
import uk.gov.hmrc.traderservices.stubs.FileTransferStubs

class FileTransferConnectorISpec extends FileTransferConnectorISpecSetup {

  "FileTransferConnector" when {
    "transferFile" should {
      "return 202 if success" in {
        givenTraderServicesFileTransferSucceeds()
        givenAuditConnector()
        val request = testRequest
        val result = await(connector.transferFile(request, correlationId))
        result.success shouldBe true
        result.httpStatus shouldBe 202
        verifyTraderServicesFileTransferHasHappened(times = 1)
      }

      "return failure if 403" in {
        givenTraderServicesFileTransferFailure(403)
        givenAuditConnector()
        val request = testRequest
        val result = await(connector.transferFile(request, correlationId))
        result.success shouldBe false
        result.httpStatus shouldBe 403
        verifyTraderServicesFileTransferHasHappened(times = 1)
      }

      "retry if 499 failure" in {
        givenTraderServicesFileTransferFailure(499)
        givenAuditConnector()
        val request = testRequest
        val result = await(connector.transferFile(request, correlationId))
        result.success shouldBe false
        result.httpStatus shouldBe 499
        verifyTraderServicesFileTransferHasHappened(times = 3)
      }

      "retry if 500 failure" in {
        givenTraderServicesFileTransferFailure(500)
        givenAuditConnector()
        val request = testRequest
        val result = await(connector.transferFile(request, correlationId))
        result.success shouldBe false
        result.httpStatus shouldBe 500
        verifyTraderServicesFileTransferHasHappened(times = 3)
      }

      "retry if 502 failure" in {
        givenTraderServicesFileTransferFailure(502)
        givenAuditConnector()
        val request = testRequest
        val result = await(connector.transferFile(request, correlationId))
        result.success shouldBe false
        result.httpStatus shouldBe 502
        verifyTraderServicesFileTransferHasHappened(times = 3)
      }
    }
  }
}

trait FileTransferConnectorISpecSetup extends AppBaseISpec with FileTransferStubs {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def fakeApplication: Application = defaultAppBuilder.build()

  lazy val connector: FileTransferConnector =
    app.injector.instanceOf[FileTransferConnector]

  val correlationId = java.util.UUID.randomUUID().toString()
  val conversationId = java.util.UUID.randomUUID().toString()

  val testRequest = Json
    .parse(s"""{
              |"conversationId":"$conversationId",
              |"caseReferenceNumber":"Risk-123",
              |"applicationName":"Route1",
              |"upscanReference":"XYZ0123456789",
              |"downloadUrl":"/dummy.jpeg",
              |"fileName":"dummy.jpeg",
              |"fileMimeType":"image/jpeg",
              |"checksum":"${"0" * 64}",
              |"batchSize": 1,
              |"batchCount": 1
              |}""".stripMargin)
    .as[TraderServicesFileTransferRequest]

}
