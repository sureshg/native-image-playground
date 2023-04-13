package dev.suresh.aot

import dev.suresh.model.*
import org.graalvm.nativeimage.hosted.*

class RuntimeReflectionFeature : Feature {
  override fun beforeAnalysis(access: Feature.BeforeAnalysisAccess?) {
    RuntimeReflection.register(JVersion::class.java, KtVersion::class.java)
    RuntimeClassInitialization.initializeAtBuildTime(BuildEnv::class.java)
    // RuntimeResourceAccess.addResource(this::class.java.module, "message.txt")
  }
}
