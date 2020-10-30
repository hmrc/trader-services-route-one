package uk.gov.hmrc.traderservices.models

trait CaseRequestContent

case class CaseRequest
(
  AcknowledgementReference: String,
  ApplicationType: String = "Route1",
  OriginatingSystem: String = "Digital",
  Content: CaseRequestContent
)


