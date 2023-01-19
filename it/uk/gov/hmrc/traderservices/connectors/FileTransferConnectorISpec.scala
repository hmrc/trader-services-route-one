/*
 * Copyright 2023 HM Revenue & Customs
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
        givenFileTransferSucceeds("Risk-123", "dummy.jpeg", conversationId)
        givenAuditConnector()
        val request = testRequest
        val result = await(connector.transferFile(request, correlationId))
        result.success shouldBe true
        result.httpStatus shouldBe 202
        verifyFileTransferHasHappened(times = 1)
      }

      "return failure if 403" in {
        givenFileTransferFailure(403)
        givenAuditConnector()
        val request = testRequest
        val result = await(connector.transferFile(request, correlationId))
        result.success shouldBe false
        result.httpStatus shouldBe 403
        verifyFileTransferHasHappened(times = 1)
      }

      "retry if 499 failure" in {
        givenFileTransferFailure(499)
        givenAuditConnector()
        val request = testRequest
        val result = await(connector.transferFile(request, correlationId))
        result.success shouldBe false
        result.httpStatus shouldBe 499
        verifyFileTransferHasHappened(times = 3)
      }

      "retry if 500 failure" in {
        givenFileTransferFailure(500)
        givenAuditConnector()
        val request = testRequest
        val result = await(connector.transferFile(request, correlationId))
        result.success shouldBe false
        result.httpStatus shouldBe 500
        verifyFileTransferHasHappened(times = 3)
      }

      "retry if 502 failure" in {
        givenFileTransferFailure(502)
        givenAuditConnector()
        val request = testRequest
        val result = await(connector.transferFile(request, correlationId))
        result.success shouldBe false
        result.httpStatus shouldBe 502
        verifyFileTransferHasHappened(times = 3)
      }
    }

    "transferMultipleFiles" should {
      "return list of file transfer results" in {
        givenMultiFileTransferSucceeds(
          "Risk-123",
          "Route1",
          conversationId,
          Seq(
            FileTransferData(
              upscanReference = "XYZ0123456789",
              downloadUrl = "/dummy.jpeg",
              checksum = "0" * 64,
              fileName = "dummy.jpeg",
              fileMimeType = "image/jpeg"
            ),
            FileTransferData(
              upscanReference = "XYZ0123456780",
              downloadUrl = "/foo.jpeg",
              checksum = "1" * 64,
              fileName = "foo.jpeg",
              fileMimeType = "image/jpeg"
            )
          )
        )
        givenAuditConnector()
        val request = testMultiFileRequest
        val resultOpt = await(connector.transferMultipleFiles(request, correlationId))
        resultOpt match {
          case Right(result) =>
            result.conversationId shouldBe conversationId
            result.applicationName shouldBe "Route1"
            result.caseReferenceNumber shouldBe "Risk-123"
            result.results.head.success shouldBe true
            result.results.head.httpStatus shouldBe 202
            result.results.head.upscanReference shouldBe "XYZ0123456789"

          case Left(_) =>
            fail()
        }

        verifyMultiFileTransferHasHappened(times = 1)
      }

      "do not retry if http status is 400" in {
        givenMultiFileTransferFails("Risk-123", "Route1", conversationId, 400)
        givenAuditConnector()
        val request = testMultiFileRequest
        val resultOpt = await(connector.transferMultipleFiles(request, correlationId))
        resultOpt shouldBe Left(400)
        verifyMultiFileTransferHasHappened(times = 1)
      }

      "retry if http status is 499" in {
        givenMultiFileTransferFails("Risk-123", "Route1", conversationId, 499)
        givenAuditConnector()
        val request = testMultiFileRequest
        val resultOpt = await(connector.transferMultipleFiles(request, correlationId))
        resultOpt shouldBe Left(499)
        verifyMultiFileTransferHasHappened(times = 3)
      }

      "retry if http status is 500" in {
        givenMultiFileTransferFails("Risk-123", "Route1", conversationId, 500)
        givenAuditConnector()
        val request = testMultiFileRequest
        val resultOpt = await(connector.transferMultipleFiles(request, correlationId))
        resultOpt shouldBe Left(500)
        verifyMultiFileTransferHasHappened(times = 3)
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
    .as[FileTransferRequest]

  val testMultiFileRequest = Json
    .parse(s"""{
              |"conversationId":"$conversationId",
              |"caseReferenceNumber":"Risk-123",
              |"applicationName":"Route1",
              |"files":[
              |     {
              |          "upscanReference":"XYZ0123456789",
              |          "downloadUrl":"/dummy.jpeg",
              |          "fileName":"dummy.jpeg",
              |          "fileMimeType":"image/jpeg",
              |          "checksum":"${"0" * 64}"
              |      },
              |      {
              |          "upscanReference":"XYZ0123456780",
              |          "downloadUrl":"/foo.jpeg",
              |          "fileName":"foo.jpeg",
              |          "fileMimeType":"image/jpeg",
              |          "checksum":"${"1" * 64}"
              |      }
              |]
              |}""".stripMargin)
    .as[MultiFileTransferRequest]

}
