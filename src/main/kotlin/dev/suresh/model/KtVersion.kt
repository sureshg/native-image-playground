package dev.suresh.model

data class KtVersion(
    val name: String = "Kotlin",
    val version: String = KotlinVersion.CURRENT.toString()
)
