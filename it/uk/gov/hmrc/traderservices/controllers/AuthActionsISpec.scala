package uk.gov.hmrc.traderservices.controllers

import play.api.mvc.Result
import play.api.mvc.Results._
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException, InsufficientEnrolments}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.traderservices.support.AppBaseISpec
import uk.gov.hmrc.traderservices.wiring.AppConfig

import scala.concurrent.Future

class AuthActionsISpec extends AppBaseISpec {

  object TestController extends AuthActions {

    override def authConnector: AuthConnector = app.injector.instanceOf[AuthConnector]

    override val appConfig: AppConfig = new AppConfig {
      override val appName: String = "dummy"
      override val authBaseUrl: String = "dummy"
      override val authorisedServiceName: String = "HMRC-XYZ"
      override val authorisedIdentifierKey: String = "XYZNumber"
    }

    implicit val hc = HeaderCarrier()
    implicit val request = FakeRequest().withSession(SessionKeys.authToken -> "Bearer XYZ")
    import scala.concurrent.ExecutionContext.Implicits.global

    def withAuthorisedAsTrader[A]: Result =
      await(super.withAuthorisedAsTrader { identifier =>
        Future.successful(Ok(identifier))
      })

  }

  "withAuthorisedAsAgent" should {

    "call body with arn when valid agent" in {
      stubForAuthAuthorise(
        "{}",
        s"""{
          |"authorisedEnrolments": [
          |  { "key":"HMRC-XYZ", "identifiers": [
          |    { "key":"XYZNumber", "value": "fooXyz" }
          |  ]}
          |]}""".stripMargin
      )
      val result = TestController.withAuthorisedAsTrader
      status(result) shouldBe 200
      bodyOf(result) shouldBe "fooXyz"
    }

    "throw an AutorisationException when user not logged in" in {
      givenUnauthorisedWith("MissingBearerToken")
      an[AuthorisationException] shouldBe thrownBy {
        TestController.withAuthorisedAsTrader
      }
    }

    "throw InsufficientEnrolments when agent not enrolled for service" in {
      stubForAuthAuthorise(
        "{}",
        s"""{
          |"authorisedEnrolments": [
          |  { "key":"HMRC-FOO", "identifiers": [
          |    { "key":"XYZNumber", "value": "fooXyz" }
          |  ]}
          |]}""".stripMargin
      )
      an[InsufficientEnrolments] shouldBe thrownBy {
        TestController.withAuthorisedAsTrader
      }
    }

    "throw InsufficientEnrolments when expected agent's identifier missing" in {
      stubForAuthAuthorise(
        "{}",
        s"""{
          |"authorisedEnrolments": [
          |  { "key":"HMRC-XYZ", "identifiers": [
          |    { "key":"BAR", "value": "fooArn" }
          |  ]}
          |]}""".stripMargin
      )
      an[InsufficientEnrolments] shouldBe thrownBy {
        TestController.withAuthorisedAsTrader
      }
    }
  }

}
