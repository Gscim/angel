/*
 * Tencent is pleased to support the open source community by making Angel available.
 *
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/Apache-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package com.tencent.angel.spark.examples.cluster

import com.tencent.angel.spark.context.PSContext
import com.tencent.angel.spark.ml.core.ArgsUtil
import com.tencent.angel.graph.embedding.metapath2vec.MetaPath2Vec
import com.tencent.angel.graph.utils.GraphIO
import org.apache.spark.storage.StorageLevel
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.mutable

object MetaPath2VecExample {
  def main(args: Array[String]): Unit = {

    val params = ArgsUtil.parse(args)
    val mode = params.getOrElse("mode", "local")
    val sc = start(mode)

    val input = params.getOrElse("input", null)
    val partitionNum = params.getOrElse("partitionNum", "1").toInt
    val storageLevel = StorageLevel.fromString(params.getOrElse("storageLevel", "MEMORY_ONLY"))
    val batchSize = params.getOrElse("batchSize", "10000").toInt
    val pullBatchSize = params.getOrElse("pullBatchSize", "1000").toInt
    val output = params.getOrElse("output", null)
    val srcIndex = params.getOrElse("src", "0").toInt
    val dstIndex = params.getOrElse("dst", "1").toInt
    val isWeighted = params.getOrElse("isWeighted", "true").toBoolean
    val weightIndex = params.getOrElse("weightIndex", "2").toInt
    val psPartitionNum = params.getOrElse("psPartitionNum",
      sc.getConf.get("spark.ps.instances", "1")).toInt
    val useBalancePartition = params.getOrElse("useBalancePartition", "false").toBoolean

    val cpDir = params.get("cpDir").filter(_.nonEmpty).orElse(GraphIO.defaultCheckpointDir)
      .getOrElse(throw new Exception("checkpoint dir not provided"))
    sc.setCheckpointDir(cpDir)

    val metaPath = params.getOrElse("metaPath", "0-1-2-1-0") // should be symmetrical, eg: 0-0, 0-1-0, 0-1-2-1-0
    val nodeTypePath = params.getOrElse("nodeTypePath", null)
    val walkLength = params.getOrElse("walkLength", "5").toInt
    val needReplicateEdge = params.getOrElse("needReplicateEdge", "true").toBoolean
    val numWalks = params.getOrElse("numWalks", "1").toInt

    val sep = params.getOrElse("sep",  "tab") match {
      case "space" => " "
      case "comma" => ","
      case "tab" => "\t"
    }

    val metaPath2Vec = new MetaPath2Vec()
      .setPartitionNum(partitionNum)
      .setStorageLevel(storageLevel)
      .setPSPartitionNum(psPartitionNum)
      .setUseBalancePartition(useBalancePartition)
      .setBatchSize(batchSize)
      .setIsWeighted(isWeighted)
      .setWalkLength(walkLength)
      .setPullBatchSize(pullBatchSize)
      .setNeedReplicaEdge(needReplicateEdge)
      .setEpochNum(numWalks)

    // read and set metaPath
    val meta = metaPath.trim.split("-").map(_.toInt)
    assert(meta.length > 1, s"metaPath should be symmetrical, eg. 0-0 or 0-1-2-1-0")
    //    val metaMap = new mutable.HashMap[Int, Int]()
    val metaMap = mutable.Set[(Int, Int)]()
    for (index <- meta.indices) {
      if (index < meta.length - 1)
        metaMap += ((meta(index), meta(index + 1)))
    }
    metaPath2Vec.setMetaPath(metaMap)

    // read and set nodeType
    if (nodeTypePath != null) {
      val nodeAttrs = GraphIO.load(nodeTypePath, isWeighted = false, sep = sep)
        .select("src", "dst").rdd
        .map(row => (row.getLong(0), row.getLong(1).toInt))
        .distinct(partitionNum)
      metaPath2Vec.setNodeAttr(nodeAttrs)
    }

    val df = GraphIO.load(input, isWeighted = isWeighted, srcIndex, dstIndex, weightIndex, sep = sep)
    val mapping = metaPath2Vec.transform(df)
    GraphIO.save(mapping, output)
    stop()
  }

  def start(mode: String): SparkContext = {
    val conf = new SparkConf()

    conf.setMaster(mode)
    conf.setAppName("metaPath2Vec")
    val sc = new SparkContext(conf)
    //PSContext.getOrCreate(sc)
    sc
  }

  def stop(): Unit = {
    PSContext.stop()
    SparkContext.getOrCreate().stop()
  }
}
