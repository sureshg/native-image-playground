package dev.suresh.aot

import dev.suresh.model.*
import org.graalvm.nativeimage.hosted.*

class RuntimeReflectonFeature : Feature {
  override fun beforeAnalysis(access: Feature.BeforeAnalysisAccess?) {
    RuntimeReflection.register(JVersion::class.java, KtVersion::class.java)
  }
}
