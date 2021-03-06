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
package com.tencent.angel.kubernetesmanager.deploy.features

import com.tencent.angel.kubernetesmanager.deploy.config.{AngelPod, KubernetesConf, KubernetesRoleSpecificConf}
import io.fabric8.kubernetes.api.model.{ContainerBuilder, EnvVarBuilder, HasMetadata}

import scala.collection.JavaConverters._

private[angel] class EnvSecretsFeatureStep(
                                            kubernetesConf: KubernetesConf[_ <: KubernetesRoleSpecificConf])
  extends KubernetesFeatureConfigStep {
  override def configurePod(pod: AngelPod): AngelPod = {
    val addedEnvSecrets = kubernetesConf
      .roleSecretEnvNamesToKeyRefs
      .map { case (envName, keyRef) =>
        // Keyref parts
        val keyRefParts = keyRef.split(":")
        require(keyRefParts.size == 2, "SecretKeyRef must be in the form name:key.")
        val name = keyRefParts(0)
        val key = keyRefParts(1)
        new EnvVarBuilder()
          .withName(envName)
          .withNewValueFrom()
          .withNewSecretKeyRef()
          .withKey(key)
          .withName(name)
          .endSecretKeyRef()
          .endValueFrom()
          .build()
      }

    val containerWithEnvVars = new ContainerBuilder(pod.container)
      .addAllToEnv(addedEnvSecrets.toSeq.asJava)
      .build()
    AngelPod(pod.pod, containerWithEnvVars)
  }

  override def getAdditionalPodSystemProperties(): Map[String, String] = Map.empty

  override def getAdditionalKubernetesResources(): Seq[HasMetadata] = Seq.empty
}
