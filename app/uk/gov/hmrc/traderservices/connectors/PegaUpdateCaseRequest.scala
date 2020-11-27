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
import uk.gov.hmrc.traderservices.models.Validator
import uk.gov.hmrc.traderservices.models.TraderServicesUpdateCaseRequest

/**
  * An API to create specified case in the PEGA system automatically.
  * Based on spec "CPR02-1.0.0-EIS API Specification-Update Case from MDTP"
  *
  * @param AcknowledgementReference Unique id created at source after a form is saved Unique ID throughout the journey of a message-stored in CSG data records, may be passed to Decision Service, CSG records can be searched using this field etc.
  * @param ApplicationType Its key value to create the case for respective process.
  * @param OriginatingSystem “Digital” for all requests originating in Digital
  */
case class PegaUpdateCaseRequest(
  AcknowledgementReference: String,
  ApplicationType: String,
  OriginatingSystem: String,
  Content: PegaUpdateCaseRequest.Content
)

object PegaUpdateCaseRequest {

  import Validator._
  import CommonValues._

  /**
    * @param RequestType This field is used to specify the request type. This field will have following values: "Additional Information", "Query Response"
    * @param CaseID This field is used to hold the Case ID to be updated. No validation required against this field
    * @param Description This field is used to hold the query response description or the additional information notes
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
            s"The user has attached the following file(s): ${request.fileUploads.map(_.fileName).mkString(", ")}."
          )
      )

    val RequestTypeValidator: Validate[String] =
      check(
        _.isOneOf(RequestTypeEnum),
        s""""Invalid RequestType, should be one of [${RequestTypeEnum.mkString(", ")}]"""
      )

    val CaseIDValidator: Validate[String] =
      check(
        _.lengthMinMaxInclusive(1, 32),
        s""""Invalid CaseID, should be between 1 and 32 (inclusive) character long"""
      )

    val DescriptionValidator: Validate[String] =
      check(
        _.lengthMinMaxInclusive(1, 1024),
        s""""Invalid Description, should be between 1 and 1024 (inclusive) character long"""
      )

    val validate: Validate[Content] = Validator(
      checkProperty(_.RequestType, RequestTypeValidator),
      checkProperty(_.CaseID, CaseIDValidator),
      checkProperty(_.Description, DescriptionValidator)
    )
  }

  object CommonValues {
    val OriginatingSystemEnum = Seq("Digital")
    val RequestTypeEnum = Seq("Additional Information", "Query Response")
    val ApplicationTypeEnum = Seq("Route1")
  }

  implicit val formats: Format[PegaUpdateCaseRequest] = Json.format[PegaUpdateCaseRequest]

  val AcknowledgementReferenceValidator: Validate[String] = check(
    _.lengthMinMaxInclusive(1, 32),
    s""""Invalid length of AcknowledgementReference, should be between 1 and 32 inclusive"""
  )

  val ApplicationTypeValidator: Validate[String] = check(
    _ == "Route1",
    s""""Invalid ApplicationType, should be "Route1""""
  )

  val OriginatingSystemValidator: Validate[String] = check(
    _ == "Digital",
    s""""Invalid OriginatingSystem, should be "Digital""""
  )

  implicit val validate: Validate[PegaUpdateCaseRequest] = Validator(
    checkProperty(_.AcknowledgementReference, AcknowledgementReferenceValidator),
    checkProperty(_.ApplicationType, ApplicationTypeValidator),
    checkProperty(_.OriginatingSystem, OriginatingSystemValidator),
    checkProperty(_.Content, Content.validate)
  )

}
