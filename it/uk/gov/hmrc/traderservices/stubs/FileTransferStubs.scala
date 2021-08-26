package uk.gov.hmrc.traderservices.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.traderservices.support.WireMockSupport
import uk.gov.hmrc.traderservices.models.FileTransferData
import java.util.UUID

trait FileTransferStubs {
  me: WireMockSupport =>

  def givenFileTransferSucceeds(
    caseReferenceNumber: String,
    fileName: String,
    conversationId: String
  ): Unit =
    stubFor(
      post(urlPathEqualTo("/transfer-file"))
        .withRequestBody(
          equalToJson(
            s"""{
               |   "caseReferenceNumber":"$caseReferenceNumber",
               |   "fileName":"$fileName",
               |   "conversationId":"$conversationId"
               |}""".stripMargin,
            true,
            true
          )
        )
        .willReturn(
          aResponse()
            .withStatus(202)
        )
    )

  def givenFileTransferFailure(status: Int): Unit =
    stubFor(
      post(urlPathEqualTo("/transfer-file"))
        .willReturn(
          aResponse()
            .withStatus(status)
        )
    )

  def verifyFileTransferHasHappened(times: Int = 1) =
    verify(times, postRequestedFor(urlPathEqualTo("/transfer-file")))

  def verifyFileTransferDidNotHappen() =
    verify(0, postRequestedFor(urlPathEqualTo("/transfer-file")))

  def givenMultiFileTransferSucceeds(
    caseReferenceNumber: String,
    applicationName: String,
    conversationId: String,
    files: Seq[FileTransferData]
  ): Unit =
    stubFor(
      post(urlPathEqualTo("/transfer-multiple-files"))
        .withRequestBody(
          equalToJson(
            s"""{
               |   "caseReferenceNumber":"$caseReferenceNumber",
               |   "applicationName":"$applicationName",
               |   "conversationId":"$conversationId",
               |   "files": [ ${files
              .map(file => s"""{
               |          "upscanReference":"${file.upscanReference}",
               |          "downloadUrl":"${file.downloadUrl}",
               |          "fileName":"${file.fileName}",
               |          "fileMimeType":"${file.fileMimeType}"
               |          ${file.fileSize.map(s => s""", "fileSize":$s""").getOrElse("")},
               |          "checksum":"${file.checksum}"
               |      }""".stripMargin)
              .mkString(",")}
               |   ]
               |}""".stripMargin,
            true,
            true
          )
        )
        .willReturn(
          aResponse()
            .withStatus(201)
            .withBody(s"""{
                         |   "caseReferenceNumber":"$caseReferenceNumber",
                         |   "applicationName":"$applicationName",
                         |   "conversationId":"$conversationId",
                         |   "totalDurationMillis": 2222,
                         |   "results": [ ${files
              .map(file => s"""{
                         |          "upscanReference":"${file.upscanReference}",
                         |          "fileName":"${file.fileName}",
                         |          "fileMimeType":"${file.fileMimeType}"
                         |          ${file.fileSize.map(s => s""", "fileSize":$s""").getOrElse("")},
                         |          "checksum":"${file.checksum}",
                         |          "fileSize": 1024,
                         |          "success": true,
                         |          "httpStatus": 202,
                         |          "transferredAt": "2021-07-17T12:18:09",
                         |          "durationMillis": 1234,
                         |          "correlationId": "${UUID.randomUUID}"
                         |      }""".stripMargin)
              .mkString(",")}
                         |   ]
                         |}""".stripMargin)
        )
    )

  def givenMultiFileTransferFails(
    caseReferenceNumber: String,
    applicationName: String,
    conversationId: String,
    status: Int
  ): Unit =
    stubFor(
      post(urlPathEqualTo("/transfer-multiple-files"))
        .withRequestBody(
          equalToJson(
            s"""{
               |   "caseReferenceNumber":"$caseReferenceNumber",
               |   "applicationName":"$applicationName",
               |   "conversationId":"$conversationId",
               |   "files": [
               |      {
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
               |   ]
               |}""".stripMargin,
            true,
            true
          )
        )
        .willReturn(
          aResponse()
            .withStatus(status)
        )
    )

  def verifyMultiFileTransferHasHappened(times: Int = 1) =
    verify(times, postRequestedFor(urlPathEqualTo("/transfer-multiple-files")))

  def verifyMultiFileTransferDidNotHappen() =
    verify(0, postRequestedFor(urlPathEqualTo("/transfer-multiple-files")))

}
