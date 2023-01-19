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

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.traderservices.models.TraderServicesUpdateCaseRequest

/** An API to create specified case in the PEGA system automatically. Based on spec "CPR02-1.0.0-EIS API
  * Specification-Update Case from MDTP"
  *
  * @param AcknowledgementReference
  *   Unique id created at source after a form is saved Unique ID throughout the journey of a message-stored in CSG data
  *   records, may be passed to Decision Service, CSG records can be searched using this field etc.
  * @param ApplicationType
  *   Its key value to create the case for respective process.
  * @param OriginatingSystem
  *   “Digital” for all requests originating in Digital
  */
case class PegaUpdateCaseRequest(
  AcknowledgementReference: String,
  ApplicationType: String,
  OriginatingSystem: String,
  Content: PegaUpdateCaseRequest.Content
)

object PegaUpdateCaseRequest {

  /** @param RequestType
    *   This field is used to specify the request type. This field will have following values: "Additional Information",
    *   "Query Response"
    * @param CaseID
    *   This field is used to hold the Case ID to be updated. No validation required against this field
    * @param Description
    *   This field is used to hold the query response description or the additional information notes
    */
  case class Content(
    RequestType: String,
    CaseID: String,
    Description: String
  )

  object Content {
    implicit val formats: Format[Content] = Json.format[Content]

    def from(request: TraderServicesUpdateCaseRequest): Content =
      Content(
        CaseID = request.caseReferenceNumber,
        RequestType = "Additional Information",
        Description = request.responseText
          .getOrElse(
            s"The user has attached the following file(s): ${request.uploadedFiles.map(_.fileName.replaceAll(s"[^\\p{ASCII}]", "?")).mkString(", ")}."
          )
      )
  }

  implicit val formats: Format[PegaUpdateCaseRequest] = Json.format[PegaUpdateCaseRequest]

}
