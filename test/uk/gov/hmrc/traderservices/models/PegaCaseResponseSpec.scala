package uk.gov.hmrc.traderservices.models

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.traderservices.connectors.PegaCaseError.ErrorDetail
import uk.gov.hmrc.traderservices.connectors.{PegaCaseError, PegaCaseResponse, PegaCaseSuccess}

class PegaCaseResponseSpec extends AnyFreeSpec with Matchers {
  "PegaCaseError" - {
    "deserialize json to PegaCaseError" in {
      val json: JsValue =
        Json.parse("""
                     |{"errorDetail":{"errorMessage":"999 : PC12010081330XGBNZJO04"}}
            """.stripMargin)

      json.as[PegaCaseResponse] mustBe PegaCaseError(
        ErrorDetail(None, None, None, Some("999 : PC12010081330XGBNZJO04"), None, None)
      )
    }

    "serialise PegaCaseError to json" in {
      val data = PegaCaseError(ErrorDetail(None, None, None, Some("999 : PC12010081330XGBNZJO04"), None, None))
      val json: JsValue =
        Json.parse("""
                     |{"errorDetail":{"errorMessage":"999 : PC12010081330XGBNZJO04"}}
            """.stripMargin)
      Json.toJson(data) mustBe json

    }

    "deserialize json to PegaCaseSuccess" in {
      val data =
        PegaCaseSuccess("PC12010081330XGBNZJO04", "2024-08-14T15:47:11.284616Z", "Success", "Case Created Successfully")
      val json: JsValue =
        Json.parse(
          """
            |{"CaseID":"PC12010081330XGBNZJO04","ProcessingDate":"2024-08-14T15:47:11.284616Z","Status":"Success","StatusText":"Case Created Successfully"}
            """.stripMargin
        )

      Json.toJson(data) mustBe json

      json.as[PegaCaseResponse] mustBe PegaCaseSuccess(
        "PC12010081330XGBNZJO04",
        "2024-08-14T15:47:11.284616Z",
        "Success",
        "Case Created Successfully"
      )
    }

    "serialise PegaCaseSuccess to json" in {
      val data =
        PegaCaseSuccess("PC12010081330XGBNZJO04", "2024-08-14T15:47:11.284616Z", "Success", "Case Created Successfully")
      val json: JsValue =
        Json.parse(
          """
            |{"CaseID":"PC12010081330XGBNZJO04","ProcessingDate":"2024-08-14T15:47:11.284616Z","Status":"Success","StatusText":"Case Created Successfully"}
            """.stripMargin
        )

      Json.toJson(data) mustBe json
    }
  }

}
