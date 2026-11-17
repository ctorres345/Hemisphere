package io.github.ctorres345.hemisphere.core.model

import org.junit.Assert.assertTrue
import org.junit.Test

/** Sample for a pure Kotlin/JVM module. Replace it with the first real `core:model` test. */
class SampleTest {
    @Test
    fun `JVM unit tests run`() {
        assertTrue(Runtime.version().feature() >= JAVA_17)
    }

    private companion object {
        const val JAVA_17 = 17
    }
}
