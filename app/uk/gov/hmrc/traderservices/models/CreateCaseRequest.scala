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

package uk.gov.hmrc.traderservices.models

import play.api.libs.json.{Format, Json}

case class CreateCase(
  EntryType: String,
  RequestType: String,
  EntryNumber: String,
  Route: String,
  EntryProcessingUnit: String,
  EntryDate: String,
  FreightOption: String,
  Priority: Option[String],
  VesselName: Option[String],
  VesselEstimatedDate: Option[String],
  VesselEstimatedTime: Option[String],
  MUCR: Option[String],
  IsALVS: String,
  EORI: String,
  TelephoneNumber: String,
  EmailAddress: String
)

object CreateCase {
  implicit val formats: Format[CreateCase] = Json.format[CreateCase]
}

case class CreateCaseRequest(
  AcknowledgementReference: String,
  ApplicationType: String = "Route1",
  OriginatingSystem: String = "Digital",
  Content: CreateCase
)

object CreateCaseRequest {
  implicit val formats: Format[CreateCaseRequest] = Json.format[CreateCaseRequest]
}
