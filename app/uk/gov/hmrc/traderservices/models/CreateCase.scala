package uk.gov.hmrc.traderservices.models

import play.api.libs.json.{Format, Json}

case class CreateCase
(
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
  IsALVS: Option[String],
  EORI: String,
  TelephoneNumber: String,
  EmailAddress: String
) extends CaseRequestContent

object CreateCase {
  implicit val formats: Format[CreateCase] = Json.format[CreateCase]
}
