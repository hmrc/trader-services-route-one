/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.traderservices.services

import javax.inject.Inject

import com.google.inject.Singleton
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import scala.concurrent.ExecutionContext

import scala.concurrent.Future
import scala.util.Try
import uk.gov.hmrc.traderservices.models.TraderServicesCreateCaseRequest
import uk.gov.hmrc.traderservices.models.TraderServicesUpdateCaseRequest
import uk.gov.hmrc.traderservices.connectors.TraderServicesCaseResponse
import play.api.libs.json._

object TraderServicesAuditEvent extends Enumeration {
  type TraderServicesAuditEvent = Value
  val CreateCase, UpdateCase = Value
}

@Singleton
class AuditService @Inject() (val auditConnector: AuditConnector) {

  import TraderServicesAuditEvent._

  final def auditCreateCaseEvent(createRequest: TraderServicesCreateCaseRequest)(
    createResponse: TraderServicesCaseResponse
  )(implicit hc: HeaderCarrier, request: Request[Any], ec: ExecutionContext): Future[Unit] = {
    val details: Seq[(String, Any)] =
      pegaResponseToDetails(createResponse, true) ++ AuditService.entityToDetails(createRequest) ++ Seq(
        "numberOfFilesUploaded" -> createRequest.uploadedFiles.size
      )
    auditEvent(CreateCase, "create-case", details)
  }

  final def auditCreateCaseErrorEvent(
    createResponse: TraderServicesCaseResponse
  )(implicit hc: HeaderCarrier, request: Request[Any], ec: ExecutionContext): Future[Unit] = {
    val details: Seq[(String, Any)] = pegaResponseToDetails(createResponse, true)
    auditEvent(CreateCase, "create-case", details)
  }

  final def auditUpdateCaseEvent(updateRequest: TraderServicesUpdateCaseRequest)(
    updateResponse: TraderServicesCaseResponse
  )(implicit hc: HeaderCarrier, request: Request[Any], ec: ExecutionContext): Future[Unit] = {
    val details: Seq[(String, Any)] =
      pegaResponseToDetails(updateResponse, false) ++ AuditService.entityToDetails(updateRequest) ++ Seq(
        "numberOfFilesUploaded" -> updateRequest.uploadedFiles.size
      )
    auditEvent(UpdateCase, "update-case", details)
  }

  final def auditUpdateCaseErrorEvent(
    updateResponse: TraderServicesCaseResponse
  )(implicit hc: HeaderCarrier, request: Request[Any], ec: ExecutionContext): Future[Unit] = {
    val details: Seq[(String, Any)] = pegaResponseToDetails(updateResponse, false)
    auditEvent(UpdateCase, "update-case", details)
  }

  private def pegaResponseToDetails(
    caseResponse: TraderServicesCaseResponse,
    reportDuplicate: Boolean
  ): Seq[(String, Any)] =
    Seq(
      "success" -> caseResponse.isSuccess
    ) ++
      (if (caseResponse.isSuccess)
         Seq(
           "caseReferenceNumber" -> caseResponse.result.get
         )
       else
         (if (reportDuplicate)
            Seq(
              "duplicate" -> caseResponse.isDuplicate
            )
          else Seq.empty) ++ caseResponse.error.map(e => Seq("errorCode" -> e.errorCode)).getOrElse(Seq.empty) ++
           caseResponse.error
             .flatMap(_.errorMessage)
             .map(m => Seq("errorMessage" -> m))
             .getOrElse(Seq.empty))

  private def auditEvent(
    event: TraderServicesAuditEvent,
    transactionName: String,
    details: Seq[(String, Any)]
  )(implicit hc: HeaderCarrier, request: Request[Any], ec: ExecutionContext): Future[Unit] =
    send(createEvent(event, transactionName, details: _*))

  private def createEvent(
    event: TraderServicesAuditEvent,
    transactionName: String,
    details: (String, Any)*
  )(implicit hc: HeaderCarrier, request: Request[Any], ec: ExecutionContext): DataEvent = {

    val detail = hc.toAuditDetails(details.map(pair => pair._1 -> pair._2.toString): _*)
    val tags = hc.toAuditTags(transactionName, request.path)
    DataEvent(
      auditSource = "trader-services-route-one",
      auditType = event.toString,
      tags = tags,
      detail = detail
    )
  }

  private def send(
    events: DataEvent*
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    Future {
      events.foreach { event =>
        Try(auditConnector.sendEvent(event))
      }
    }

}

object AuditService {

  /** Represent an entity as a sequence of key to value mappings. */
  def entityToDetails[T: Writes](enity: T): Seq[(String, Any)] =
    detailsFromJson(implicitly[Writes[T]].writes(enity))

  /** Flatten JSON structure as a sequence of key to value mappings. */
  def detailsFromJson(value: JsValue): Seq[(String, Any)] =
    value match {

      case JsNull          => Seq.empty
      case JsFalse         => Seq("" -> false)
      case JsTrue          => Seq("" -> true)
      case JsNumber(value) => Seq("" -> value)
      case JsString(value) => Seq("" -> value)

      case o: JsObject =>
        o.fields.flatMap {
          case (k, v) =>
            detailsFromJson(v).map {
              case (m, n) if m.isEmpty => k        -> n
              case (m, n)              => s"$k.$m" -> n
            }
        }

      case JsArray(values) =>
        values
          .map(detailsFromJson)
          .zipWithIndex
          .map {
            case (s, i) =>
              s.map {
                case (m, n) if m.isEmpty => s"$i"    -> n
                case (m, n)              => s"$i.$m" -> n
              }
          }
          .flatten
    }

}
