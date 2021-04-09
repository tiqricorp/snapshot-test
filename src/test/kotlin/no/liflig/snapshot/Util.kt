package no.liflig.snapshot

import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets

/**
 * Assert a negative test and capture output written to stderr.
 *
 * This will disable any regeneration of snapshots during execution and
 * recover previous configuration afterwards. This is needed since we
 * do not want these tests to regenerate snapshots even when the tests
 * itself is run with the flag.
 */
fun assertNegative(block: () -> Unit): String =
  withRegenerateDisabled {
    val out = ByteArrayOutputStream()
    System.setErr(PrintStream(out))

    try {
      assertThrows<AssertionError> {
        block()
      }
    } finally {
      System.setErr(PrintStream(FileOutputStream(FileDescriptor.err)))
    }

    out.toString(StandardCharsets.UTF_8)
  }

fun <T> withRegenerateDisabled(block: () -> T): T =
  withSystemProperty(REGENERATE_SNAPSHOTS, "false") {
    withSystemProperty(REGENERATE_FAILED_SNAPSHOTS, "false") {
      block()
    }
  }

fun <T> withRegenerateOnlyAll(block: () -> T): T =
  withSystemProperty(REGENERATE_SNAPSHOTS, "true") {
    withSystemProperty(REGENERATE_FAILED_SNAPSHOTS, null) {
      block()
    }
  }

fun <T> withRegenerateOnlyFailure(block: () -> T): T =
  withSystemProperty(REGENERATE_SNAPSHOTS, null) {
    withSystemProperty(REGENERATE_FAILED_SNAPSHOTS, "true") {
      block()
    }
  }

private fun <T> withSystemProperty(name: String, overriddenValue: String?, block: () -> T): T {
  val originalValue = System.getProperty(name)
  setSystemProperty(name, overriddenValue)
  try {
    return block()
  } finally {
    setSystemProperty(name, originalValue)
  }
}

/**
 * Set a specific system property. For null values the system property is removed instead.
 */
private fun setSystemProperty(name: String, value: String?) {
  if (value == null) {
    System.clearProperty(name)
  } else {
    System.setProperty(name, value)
  }
}

fun <T> File.deleteBeforeAndAfter(block: () -> T): T =
  try {
    delete()
    block()
  } finally {
    delete()
  }
