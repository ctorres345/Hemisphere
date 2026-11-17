package io.github.ctorres345.hemisphere.core.fs

import javax.inject.Qualifier

/** The dispatcher for blocking file I/O (architecture §9). Tests swap it for a test dispatcher. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** The dispatcher for CPU work such as sorting a large listing (architecture §9). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
