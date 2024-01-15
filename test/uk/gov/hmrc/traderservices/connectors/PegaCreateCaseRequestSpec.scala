/*
 * Copyright 2024 HM Revenue & Customs
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

import uk.gov.hmrc.traderservices.support.UnitSpec
import uk.gov.hmrc.traderservices.models._
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.LocalTime

class PegaCreateCaseRequestSpec extends UnitSpec {

  "PegaCreateCaseRequest.Content" should {
    for {
      requestType   <- ImportRequestType.values
      routeType     <- ImportRouteType.values
      priorityGoods <- ImportPriorityGoods.values.map(Option.apply) ++ Set(None)
      hasALVS       <- Set(true, false)
      freightType   <- ImportFreightType.values
    } s"build Content from create import case request having [$requestType,$routeType,$priorityGoods,$hasALVS,$freightType]" in {
      PegaCreateCaseRequest.Content
        .from(serviceCreateImportCaseRequest(requestType, routeType, priorityGoods, hasALVS, freightType))
        .shouldBe(
          PegaCreateCaseRequest.Content(
            EntryType = "Import",
            RequestType = PegaCreateCaseRequest.importRequestTypeToPegaValue(requestType),
            EntryProcessingUnit = "002",
            Route = PegaCreateCaseRequest.importRouteTypeToPegaValue(routeType),
            EntryNumber = "223456A",
            VesselName = Some("Vessel Name"),
            EntryDate = "20200902",
            VesselEstimatedDate = Some("20201029"),
            VesselEstimatedTime = Some("234500"),
            FreightOption = PegaCreateCaseRequest.importFreightTypeToPegaValue(freightType),
            EORI = Some("GB123456789012345"),
            TelephoneNumber = Some("07123456789"),
            TraderName = Some("Full Name"),
            EmailAddress = "sampelname@gmail.com",
            Priority = priorityGoods.map(PegaCreateCaseRequest.importPriorityGoodsToPegaValue),
            IsALVS = if (hasALVS) Some("true") else Some("false")
          )
        )
    }

    for {
      requestType   <- ExportRequestType.values
      routeType     <- ExportRouteType.values
      priorityGoods <- ExportPriorityGoods.values.map(Option.apply) ++ Set(None)
      freightType   <- ExportFreightType.values
    } s"build Content from create export case request having [$requestType,$routeType,$priorityGoods,$freightType]" in {
      PegaCreateCaseRequest.Content
        .from(serviceCreateExportCaseRequest(requestType, routeType, priorityGoods, freightType))
        .shouldBe(
          PegaCreateCaseRequest.Content(
            EntryType = "Export",
            RequestType = PegaCreateCaseRequest.exportRequestTypeToPegaValue(requestType),
            EntryProcessingUnit = "002",
            Route = PegaCreateCaseRequest.exportRouteTypeToPegaValue(routeType),
            EntryNumber = "A23456A",
            VesselName = Some("Vessel Name"),
            EntryDate = "20200902",
            VesselEstimatedDate = Some("20201029"),
            VesselEstimatedTime = Some("234500"),
            FreightOption = PegaCreateCaseRequest.exportFreightTypeToPegaValue(freightType),
            EORI = Some("GB123456789012345"),
            TelephoneNumber = Some("07123456789"),
            TraderName = Some("Full Name"),
            EmailAddress = "sampelname@gmail.com",
            Priority = priorityGoods.map(PegaCreateCaseRequest.exportPriorityGoodsToPegaValue),
            IsALVS = None
          )
        )
    }
  }

  def serviceCreateImportCaseRequest(
    requestType: ImportRequestType,
    routeType: ImportRouteType,
    priorityGoods: Option[ImportPriorityGoods],
    hasALVS: Boolean,
    freightType: ImportFreightType
  ) =
    TraderServicesCreateCaseRequest(
      EntryDetails(EPU(2), EntryNumber("223456A"), LocalDate.parse("2020-09-02")),
      ImportQuestions(
        requestType,
        routeType,
        priorityGoods,
        hasALVS,
        freightType,
        vesselDetails = Some(
          VesselDetails(
            vesselName = Some("Vessel Name"),
            dateOfArrival = Some(LocalDate.of(2020, 10, 29)),
            timeOfArrival = Some(LocalTime.of(23, 45, 0))
          )
        ),
        contactInfo = ContactInfo(
          contactName = Some("Full Name"),
          contactNumber = Some("07123456789"),
          contactEmail = "sampelname@gmail.com"
        )
      ),
      Seq(
        UploadedFile(
          "ref-123",
          downloadUrl = "/bucket/test⫐1.jpeg",
          uploadTimestamp = ZonedDateTime.of(2020, 10, 10, 10, 10, 10, 0, ZoneId.of("UTC")),
          checksum = "f55a741917d512ab4c547ea97bdfdd8df72bed5fe51b6a248e0a5a0ae58061c8",
          fileName = "test⫐1.jpeg",
          fileMimeType = "image/jpeg"
        ),
        UploadedFile(
          "ref-789",
          downloadUrl = "/bucket/app.routes",
          uploadTimestamp = ZonedDateTime.of(2020, 10, 10, 10, 20, 20, 0, ZoneId.of("UTC")),
          checksum = "f1198e91e6fe05ccf6788b2f871f0fa90e9fab98252e81ca20238cf26119e616",
          fileName = "app.routes",
          fileMimeType = "application/routes"
        )
      ),
      eori = Some("GB123456789012345")
    )

  def serviceCreateExportCaseRequest(
    requestType: ExportRequestType,
    routeType: ExportRouteType,
    priorityGoods: Option[ExportPriorityGoods],
    freightType: ExportFreightType
  ) =
    TraderServicesCreateCaseRequest(
      EntryDetails(EPU(2), EntryNumber("A23456A"), LocalDate.parse("2020-09-02")),
      ExportQuestions(
        requestType,
        routeType,
        priorityGoods,
        freightType,
        vesselDetails = Some(
          VesselDetails(
            vesselName = Some("Vessel Name"),
            dateOfArrival = Some(LocalDate.of(2020, 10, 29)),
            timeOfArrival = Some(LocalTime.of(23, 45, 0))
          )
        ),
        contactInfo = ContactInfo(
          contactName = Some("Full Name"),
          contactNumber = Some("07123456789"),
          contactEmail = "sampelname@gmail.com"
        )
      ),
      Seq(
        UploadedFile(
          "ref-123",
          downloadUrl = "/bucket/test⫐1.jpeg",
          uploadTimestamp = ZonedDateTime.of(2020, 10, 10, 10, 10, 10, 0, ZoneId.of("UTC")),
          checksum = "f55a741917d512ab4c547ea97bdfdd8df72bed5fe51b6a248e0a5a0ae58061c8",
          fileName = "test⫐1.jpeg",
          fileMimeType = "image/jpeg"
        ),
        UploadedFile(
          "ref-789",
          downloadUrl = "/bucket/app.routes",
          uploadTimestamp = ZonedDateTime.of(2020, 10, 10, 10, 20, 20, 0, ZoneId.of("UTC")),
          checksum = "f1198e91e6fe05ccf6788b2f871f0fa90e9fab98252e81ca20238cf26119e616",
          fileName = "app.routes",
          fileMimeType = "application/routes"
        )
      ),
      eori = Some("GB123456789012345")
    )

}
