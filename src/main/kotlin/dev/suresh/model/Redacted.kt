package dev.suresh.model

import dev.zacsweers.redacted.annotations.Redacted

@Redacted data class Secret(val value: String)

data class Creds(val user: String, @Redacted val password: String)
