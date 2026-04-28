/*
 * Copyright 2026 HM Revenue & Customs
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
import cats.Semigroup
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}

// $COVERAGE-OFF$
object Validator {

  import Implicits._

  type Validate[T] = T => Validated[List[String], Unit]

  def apply[T](constraints: Validate[T]*): Validate[T] =
    (entity: T) =>
      constraints
        .foldLeft[Validated[List[String], Unit]](Valid(()))((v, fx) =>
          v.combine(
            fx(entity).leftMap(le => le.map(e => if (e.contains(" in ")) e else s"$e in $entity"))
          )
        )

  def always[T]: Validate[T] = (entity: T) => Valid(entity)

  private type SimpleValidator[T] = T => Validated[String, Unit]

  def check[T](test: T => Boolean, error: String): Validate[T] =
    (entity: T) => Validated.cond(test(entity), (), error :: Nil)

  def checkProperty[T, E](element: T => E, validator: Validate[E]): Validate[T] =
    (entity: T) => validator(element(entity))

  def checkIfSome[T, E](
    element: T => Option[E],
    validator: Validate[E],
    isValidIfNone: Boolean = true
  ): Validate[T] =
    (entity: T) =>
      element(entity)
        .map(validator)
        .getOrElse(
          if (isValidIfNone) Valid(()) else Invalid(List("Some value expected but got None"))
        )

  def checkEach[T, E](elements: T => Seq[E], validator: Validate[E]): Validate[T] =
    (entity: T) => {
      val vs = elements(entity).map(validator)
      if (vs.nonEmpty)
        vs.reduce(_.combine(_))
      else Valid(())
    }

  implicit class StringMatchers(val value: String) extends AnyVal {
    def lengthMinMaxInclusive(min: Int, max: Int): Boolean =
      value != null && value.length >= min && value.length <= max
  }

  implicit class IntMatchers(val value: Int) extends AnyVal {
    def inRange(min: Int, max: Int, multipleOf: Option[Int] = None): Boolean =
      value <= max && value >= min && multipleOf.forall(a => (value % a).abs < 0.0001)
  }

  object Implicits {

    implicit val listSemigroup: Semigroup[List[String]] = Semigroup.instance(_ ++ _)
    implicit val unitSemigroup: Semigroup[Unit] = Semigroup.instance((_, _) => ())
    implicit val stringSemigroup: Semigroup[String] = Semigroup.instance(_ + _)
  }
}
