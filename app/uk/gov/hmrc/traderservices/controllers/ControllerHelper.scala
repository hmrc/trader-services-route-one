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

  type HandleError = (String, String) => Result

  protected def withPayload[T](
    f: T => Future[Result]
  )(
    handleError: HandleError
  )(implicit
    request: Request[String],
    reads: Reads[T],
    validate: Validator.Validate[T],
    ec: ExecutionContext
  ): Future[Result] =
    Try(Json.parse(request.body).validate[T]) match {

      case Success(JsSuccess(payload, _)) =>
        validate(payload) match {

          case Valid(a) =>
            f(payload)

          case Invalid(errs) =>
            Future successful handleError(
              "ERROR_VALIDATION",
              s"Invalid payload: Validation failed due to ${errs.mkString(", and ")}."
            )
        }

      case Success(JsError(errs)) =>
        Future successful handleError(
          "ERROR_JSON",
          s"Invalid payload: Parsing failed due to ${errs
            .map {
              case (path, errors) =>
                s"at path $path with ${errors.map(e => e.messages.mkString(", ")).mkString(", ")}"
            }
            .mkString(", and ")}."
        )

      case Failure(e) =>
        Future successful handleError("ERROR_UNKNOWN", s"Could not parse payload due to ${e.getMessage}.")
    }

}
