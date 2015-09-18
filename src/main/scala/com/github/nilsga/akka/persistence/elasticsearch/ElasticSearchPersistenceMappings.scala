package com.github.nilsga.akka.persistence.elasticsearch

import java.util.concurrent.Executors

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType.{LongType, StringType}
import com.sksamuel.elastic4s.mappings.{DynamicMapping, TypedFieldDefinition}

import scala.concurrent.{ExecutionContext, Future}

object ElasticSearchPersistenceMappings {

  private def ensureIndexAndMappingExists(mappingType: String, mapping: Seq[TypedFieldDefinition])(implicit extension : ElasticSearchPersistenceExtensionImpl) : Future[Unit] = {
    val client = extension.client
    val persistenceIndex = extension.config.index
    implicit val ec = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())
    client.execute(index exists persistenceIndex).flatMap(_.isExists match {
      case true =>
        val putMapping = put mapping persistenceIndex / mappingType dynamic(DynamicMapping.Strict) as mapping
        client.execute(putMapping).map(_ => Unit)
      case false =>
        val putMapping = put mapping persistenceIndex / mappingType dynamic(DynamicMapping.Strict) as mapping
        client.execute(create index persistenceIndex).flatMap(resp => client.execute(putMapping).map(_ => Unit))
    })
  }

  def ensureJournalMappingExists()(implicit extension : ElasticSearchPersistenceExtensionImpl) : Future[Unit] = {
    ensureIndexAndMappingExists(extension.config.journalType, Seq(
      field name "persistenceId" withType StringType index NotAnalyzed,
      field name "sequenceNumber" withType LongType,
      field name "message" withType StringType index NotAnalyzed
    ))
  }

  def ensureSnapshotMappingExists()(implicit extension : ElasticSearchPersistenceExtensionImpl) : Future[Unit] = {
    ensureIndexAndMappingExists(extension.config.snapshotType, Seq(
      field name "persistenceId" withType StringType index NotAnalyzed,
      field name "sequenceNumber" withType LongType,
      field name "timestamp" withType LongType,
      field name "snapshot" withType StringType index NotAnalyzed
    ))
  }

}
