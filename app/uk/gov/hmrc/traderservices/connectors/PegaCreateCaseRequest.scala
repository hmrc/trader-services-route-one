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

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.traderservices.models._
import java.time.format.DateTimeFormatter

/**
  * Create specified case in the PEGA system.
  * Based on spec "CPR01-1.0.0-EIS API Specification-Create Case from MDTP"
  *
  * @param AcknowledgementReference Unique id created at source after a form is saved Unique ID throughout the journey of a message-stored in CSG data records, may be passed to Decision Service, CSG records can be searched using this field etc.
  * @param ApplicationType Its key value to create the case for respective process.
  * @param OriginatingSystem “Digital” for all requests originating in Digital
  */
case class PegaCreateCaseRequest(
  AcknowledgementReference: String,
  ApplicationType: String,
  OriginatingSystem: String,
  Content: PegaCreateCaseRequestContent
)

object PegaCreateCaseRequest {
  implicit val formats: Format[PegaCreateCaseRequest] = Json.format[PegaCreateCaseRequest]
}

/**
  * @param EntryType specify whether it is Import or Export.
  * @param RequestType specify the request type.
  * @param EntryNumber CHIEF entry number value.
  * @param Route route value for this application.
  * @param EntryProcessingUnit 3 digit entry processing unit.
  * @param EntryDate CHIEF entry date in YYYYMMDD format.
  * @param FreightOption freight option value.
  * @param EmailAddress trader email address.
  * @param Priority value of priority.
  * @param VesselName the Vessel Name, this field is mandatory when Route has a value of Hold and Freight Option has a value of Meritime; not needed otherwise.
  * @param VesselEstimatedDate the Vessel Estimated Date in YYYYMMDD format.
  * @param VesselEstimatedTime the Vessel Estimated Time in HHMMSS format.
  * @param MUCR the Master Unique Consignment Reference.
  * @param IsALVS This field is used hold the Boolean value of IsALVS.
  * @param TraderName This field is used hold the TraderName when supplied.
  * @param EORI This field is used to hold the EORI value entered by the user.
  * @param TelephoneNumber This field is used to hold the trader telephone number.
  */
case class PegaCreateCaseRequestContent(
  EntryType: String,
  RequestType: String,
  EntryNumber: String,
  Route: String,
  EntryProcessingUnit: String,
  EntryDate: String,
  FreightOption: String,
  EmailAddress: String,
  Priority: Option[String] = None,
  VesselName: Option[String] = None,
  VesselEstimatedDate: Option[String] = None,
  VesselEstimatedTime: Option[String] = None,
  MUCR: Option[String] = None,
  IsALVS: Option[String] = None,
  TraderName: Option[String] = None,
  EORI: Option[String] = None,
  TelephoneNumber: Option[String] = None
)

object PegaCreateCaseRequestContent {
  implicit val formats: Format[PegaCreateCaseRequestContent] = Json.format[PegaCreateCaseRequestContent]

  val dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd")
  val timeFormat = DateTimeFormatter.ofPattern("HHmmss")

  def from(request: TraderServicesCreateCaseRequest): PegaCreateCaseRequestContent =
    request.questionsAnswers match {
      case ImportQuestions(
            requestType,
            routeType,
            priorityGoods,
            hasALVS,
            freightType,
            vesselDetails,
            contactInfo
          ) =>
        PegaCreateCaseRequestContent(
          EntryType = "Import",
          RequestType = requestType match {
            case ImportRequestType.New          => "New"
            case ImportRequestType.Cancellation => "Cancellation"
          },
          EntryNumber = request.declarationDetails.entryNumber.value,
          Route = routeType match {
            case ImportRouteType.Route1    => "Route 1"
            case ImportRouteType.Route1Cap => "Route 1 CAP"
            case ImportRouteType.Route2    => "Route 2"
            case ImportRouteType.Route3    => "Route 3"
            case ImportRouteType.Route6    => "Route 6"
            case ImportRouteType.Hold      => "Hold"
          },
          EntryProcessingUnit = f"${request.declarationDetails.epu.value}%03d",
          EntryDate = dateFormat.format(request.declarationDetails.entryDate),
          FreightOption = freightType match {
            case ImportFreightType.Maritime => "Maritime"
            case ImportFreightType.Air      => "Air"
            case ImportFreightType.RORO     => "Road, rail or roll-on, roll-off (RORO)"
          },
          Priority = priorityGoods.map {
            case ImportPriorityGoods.ClassADrugs           => "Class A drugs"
            case ImportPriorityGoods.ExplosivesOrFireworks => "Explosives/Fireworks"
            case ImportPriorityGoods.HighValueArt          => "High value art"
            case ImportPriorityGoods.LiveAnimals           => "Live animals"
            case ImportPriorityGoods.HumanRemains          => "Human remains"
          },
          VesselName = vesselDetails.flatMap(_.vesselName),
          VesselEstimatedDate = vesselDetails.flatMap(_.dateOfArrival.map(dateFormat.format)),
          VesselEstimatedTime = vesselDetails.flatMap(_.timeOfArrival.map(timeFormat.format)),
          IsALVS = Some(if (hasALVS) "true" else "false"),
          EORI = Some(request.eori),
          TelephoneNumber = contactInfo.contactNumber,
          EmailAddress = contactInfo.contactEmail,
          MUCR = None,
          TraderName = Some(contactInfo.contactName)
        )
      case ExportQuestions(
            requestType,
            routeType,
            priorityGoods,
            freightType,
            vesselDetails,
            contactInfo
          ) =>
        PegaCreateCaseRequestContent(
          EntryType = "Export",
          RequestType = requestType match {
            case ExportRequestType.New                => "New"
            case ExportRequestType.Cancellation       => "Cancellation"
            case ExportRequestType.C1601              => "C1601"
            case ExportRequestType.C1602              => "C1602"
            case ExportRequestType.C1603              => "C1603"
            case ExportRequestType.WithdrawalOrReturn => "Withdrawal or return of goods"
          },
          EntryNumber = request.declarationDetails.entryNumber.value,
          Route = routeType match {
            case ExportRouteType.Route1    => "Route 1"
            case ExportRouteType.Route1Cap => "Route 1 CAP"
            case ExportRouteType.Route2    => "Route 2"
            case ExportRouteType.Route3    => "Route 3"
            case ExportRouteType.Route6    => "Route 6"
            case ExportRouteType.Hold      => "Hold"
          },
          EntryProcessingUnit = f"${request.declarationDetails.epu.value}%03d",
          EntryDate = dateFormat.format(request.declarationDetails.entryDate),
          FreightOption = freightType match {
            case ExportFreightType.Maritime => "Maritime"
            case ExportFreightType.Air      => "Air"
            case ExportFreightType.RORO     => "Road, rail or roll-on, roll-off (RORO)"
          },
          Priority = priorityGoods.map {
            case ExportPriorityGoods.ClassADrugs           => "Class A drugs"
            case ExportPriorityGoods.ExplosivesOrFireworks => "Explosives/Fireworks"
            case ExportPriorityGoods.HighValueArt          => "High value art"
            case ExportPriorityGoods.LiveAnimals           => "Live animals"
            case ExportPriorityGoods.HumanRemains          => "Human remains"
          },
          VesselName = vesselDetails.flatMap(_.vesselName),
          VesselEstimatedDate = vesselDetails.flatMap(_.dateOfArrival.map(dateFormat.format)),
          VesselEstimatedTime = vesselDetails.flatMap(_.timeOfArrival.map(timeFormat.format)),
          IsALVS = None,
          EORI = Some(request.eori),
          TelephoneNumber = contactInfo.contactNumber,
          EmailAddress = contactInfo.contactEmail,
          MUCR = None,
          TraderName = Some(contactInfo.contactName)
        )
    }
}
