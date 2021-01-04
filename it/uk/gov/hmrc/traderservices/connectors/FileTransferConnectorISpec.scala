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
      "return 200 if success" in {
        givenTraderServicesFileTransferSucceeds()
        givenAuditConnector()
        val request = testRequest
        val result = await(connector.transferFile(request, correlationId))
        result.success shouldBe true
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
