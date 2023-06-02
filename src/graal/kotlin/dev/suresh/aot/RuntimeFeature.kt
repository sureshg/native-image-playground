package dev.suresh.aot

import dev.suresh.config.BuildEnv
import dev.suresh.model.JVersion
import dev.suresh.model.KtVersion
import io.github.classgraph.ClassGraph
import org.graalvm.nativeimage.hosted.*

class RuntimeFeature : Feature {
  override fun beforeAnalysis(access: Feature.BeforeAnalysisAccess?) {
    RuntimeReflection.register(JVersion::class.java, KtVersion::class.java)
    RuntimeClassInitialization.initializeAtBuildTime(BuildEnv::class.java)
    val module = javaClass.classLoader.unnamedModule
    println("Registering static resources...")
    ClassGraph().acceptPaths("/static").scan().use {
      it.allResources.paths.forEachIndexed { idx, resource ->
        println("${idx+1}: $resource")
        RuntimeResourceAccess.addResource(module, resource)
      }
    }
  }

  override fun afterImageWrite(access: Feature.AfterImageWriteAccess) {
    // access.imagePath.toFile()
  }
}
