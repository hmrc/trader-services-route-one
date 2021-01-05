/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.traderservices.repository

import javax.inject.{Inject, Singleton}

import play.api.libs.json.Format
import play.api.libs.json.Json.{format, toJsFieldJsValueWrapper}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.collection.Seq
import scala.concurrent.{ExecutionContext, Future}

case class TraderServicesEntity(id: String, dummy: String)

object TraderServicesEntity extends ReactiveMongoFormats {
  implicit val formats: Format[TraderServicesEntity] =
    format[TraderServicesEntity]
}

//@Singleton set this annotation when the repository is back in use
class TraderServicesRepository @Inject() (mongoComponent: ReactiveMongoComponent)
    extends ReactiveRepository[TraderServicesEntity, BSONObjectID](
      "trader-services",
      mongoComponent.mongoConnector.db,
      TraderServicesEntity.formats,
      ReactiveMongoFormats.objectIdFormats
    ) with StrictlyEnsureIndexes[TraderServicesEntity, BSONObjectID] {

  def findBy(
    id: String
  )(implicit ec: ExecutionContext): Future[List[TraderServicesEntity]] =
    find(
      Seq("id" -> Some(id)).map(option => option._1 -> toJsFieldJsValueWrapper(option._2.get)): _*
    )

  override def indexes =
    Seq(
      Index(Seq("id" -> Ascending), Some("TraderServices"), unique = true)
    )

  def createEntity(id: String, dummy: String)(implicit ec: ExecutionContext): Future[Unit] =
    insert(TraderServicesEntity(id, dummy)).map(_ => ())

  def delete(id: String)(implicit ec: ExecutionContext): Future[WriteResult] =
    remove("id" -> id)

}
