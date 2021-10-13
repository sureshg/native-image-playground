package dev.suresh.service

import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.PROPERTY

@Retention(SOURCE)
@Target(PROPERTY, CLASS)
annotation class Redacted
