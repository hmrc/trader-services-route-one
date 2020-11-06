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

package uk.gov.hmrc.traderservices.connectors

import java.time.format.DateTimeFormatter

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.{HeaderCarrier, HttpPost}
import uk.gov.hmrc.traderservices.models._
import uk.gov.hmrc.traderservices.wiring.AppConfig

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CreateCaseConnector @Inject() (val config: AppConfig, val http: HttpPost) {

  val entryDateFormat = DateTimeFormatter.ofPattern("yyyyMMdd")

  def processCreateCaseRequest(createImportCaseRequest: TraderServicesCreateCaseRequest, eori: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[CaseResponse] = {
    val answers = createImportCaseRequest.questionsAnswers
    val declarationDetails = createImportCaseRequest.declarationDetails
    val createCase = buildCreateCase(eori, answers, declarationDetails)
    val json =
      CreateCaseRequest.formats.writes(
        CreateCaseRequest(
          AcknowledgementReference = "1234", //TODO: How will we set this?
          Content = createCase
        )
      )
    http.POST(config.caseBaseUrl + config.createCaseUrl, json) map {
      _.json match {
        case bdy =>
          val result = bdy \ "errorDetail"
          if (result.isDefined) CreateCaseError.formats.reads(result.get).get
          else CreateCaseSuccess.formats.reads(bdy).get
      }
    }
  }

  private def buildCreateCase(eori: String, answers: QuestionsAnswers, declarationDetails: DeclarationDetails) =
    answers match {
      case ImportQuestions(
            requestType,
            routeType,
            _,
            priorityGoods,
            hasALVS,
            freightType,
            vesselDetails,
            contactInfo
          ) =>
        CreateCase(
          EntryType = "Import",
          RequestType = requestType.get.toString,
          EntryNumber = declarationDetails.entryNumber.value,
          Route = routeType.get.toString,
          EntryProcessingUnit = declarationDetails.epu.value.toString,
          EntryDate = entryDateFormat.format(declarationDetails.entryDate),
          FreightOption = freightType.get.toString,
          Priority = priorityGoods.map(pg => pg.toString),
          VesselName = vesselDetails.flatMap(vd => vd.vesselName),
          VesselEstimatedDate = vesselDetails.flatMap(vd => vd.dateOfArrival.map(_.toString)),
          VesselEstimatedTime = vesselDetails.flatMap(vd => vd.timeOfArrival.map(_.toString)),
          IsALVS = hasALVS.get.toString,
          EORI = eori,
          TelephoneNumber = contactInfo.get.contactNumber.get,
          EmailAddress = contactInfo.get.contactEmail,
          MUCR = None
        )
      case ExportQuestions(requestType, routeType, _, priorityGoods, freightType, vesselDetails, contactInfo) =>
        CreateCase(
          EntryType = "Import",
          RequestType = requestType.get.toString,
          EntryNumber = declarationDetails.entryNumber.value,
          Route = routeType.get.toString,
          EntryProcessingUnit = declarationDetails.epu.value.toString,
          EntryDate = entryDateFormat.format(declarationDetails.entryDate),
          FreightOption = freightType.get.toString,
          Priority = priorityGoods.map(pg => pg.toString),
          VesselName = vesselDetails.flatMap(vd => vd.vesselName),
          VesselEstimatedDate = vesselDetails.flatMap(vd => vd.dateOfArrival.map(_.toString)),
          VesselEstimatedTime = vesselDetails.flatMap(vd => vd.timeOfArrival.map(_.toString)),
          IsALVS = "false",
          EORI = eori,
          TelephoneNumber = contactInfo.get.contactNumber.get,
          EmailAddress = contactInfo.get.contactEmail,
          MUCR = None
        )
    }
}
