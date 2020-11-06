/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.traderservices.controllers

import play.api.libs.json.Reads
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsError
import scala.util.Success
import scala.util.Failure
import scala.util.Try
import scala.concurrent.Future
import play.api.mvc.Result
import scala.concurrent.ExecutionContext
import play.api.mvc.Request
import uk.gov.hmrc.traderservices.models.Validator
import cats.data.Validated.Invalid
import cats.data.Validated.Valid
import play.api.libs.json.Json

trait ControllerHelper {

  type HandleError = (String, String) => Future[Result]

  protected def withPayload[T](
    f: T => Future[Result]
  )(
    handleError: HandleError
  )(implicit
    request: Request[String],
    reads: Reads[T],
    validate: Validator.Validator[T],
    ec: ExecutionContext
  ): Future[Result] =
    Try(Json.parse(request.body).validate[T]) match {

      case Success(JsSuccess(payload, _)) =>
        validate(payload) match {

          case Valid(a) =>
            f(payload)

          case Invalid(errs) =>
            handleError(
              "ERROR_VALIDATION",
              s"Invalid payload: Validation failed due to ${errs.mkString(", and ")}."
            )
        }

      case Success(JsError(errs)) =>
        handleError(
          "ERROR_JSON",
          s"Invalid payload: Parsing failed due to ${errs
            .map {
              case (path, errors) =>
                s"at path $path with ${errors.map(e => e.messages.mkString(", ")).mkString(", ")}"
            }
            .mkString(", and ")}."
        )

      case Failure(e) =>
        handleError("ERROR_UNKNOWN", s"Could not parse payload due to ${e.getMessage}.")
    }

}
