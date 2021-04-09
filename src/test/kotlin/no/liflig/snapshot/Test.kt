@file:Suppress("ClassName")

package no.liflig.snapshot

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.time.Instant
import kotlin.test.assertEquals

class Test {

  @Test
  fun testStringSnapshot() {
    val snapshotName = "String.txt"

    val valueToSnapshot = "Some text"
    val valueUnexpected = "Some other text"

    verifyStringSnapshot(snapshotName, valueToSnapshot)

    val errorMessage = assertNegative {
      verifyStringSnapshot(snapshotName, valueUnexpected)
    }

    val expectedErrorMessage = """
      #####################################################################

      Snapshot [String.txt] failed - recreate all snapshots by setting system property REGENERATE_SNAPSHOTS to true
      Example: mvn test -DREGENERATE_SNAPSHOTS=true
      Only recreate failed snapshots by setting system property REGENERATE_FAILED_SNAPSHOTS to true instead
      Example: mvn test -DREGENERATE_FAILED_SNAPSHOTS=true

      Diff:

      -Some text
      +Some other text

      #####################################################################
    """.trimIndent() + '\n'

    assertEquals(expectedErrorMessage, errorMessage)
  }

  @Test
  fun testJsonSnapshotByJsonElement() {
    val snapshotName = "JsonByJsonElement.json"

    val valueToSnapshot = buildJsonObject {
      put(
        "a",
        buildJsonArray {
          add(1)
          add(2)
        }
      )
    }

    val valueUnexpected = buildJsonObject {
      put(
        "a",
        buildJsonArray {
          add(2)
          add(1)
        }
      )
    }

    val expectedErrorMessage = """
      #####################################################################

      Snapshot [JsonByJsonElement.json] failed - recreate all snapshots by setting system property REGENERATE_SNAPSHOTS to true
      Example: mvn test -DREGENERATE_SNAPSHOTS=true
      Only recreate failed snapshots by setting system property REGENERATE_FAILED_SNAPSHOTS to true instead
      Example: mvn test -DREGENERATE_FAILED_SNAPSHOTS=true

      Error(s):
      a[0]
      Expected: 1
           got: 2
       ; a[1]
      Expected: 2
           got: 1


      Diff:

       {
         "a": [
      -    1,
      -    2
      +    2,
      +    1
         ]
       }


      #####################################################################
    """.trimIndent() + '\n'

    verifyJsonSnapshot(snapshotName, valueToSnapshot)

    val errorMessage = assertNegative {
      verifyJsonSnapshot(snapshotName, valueUnexpected)
    }

    assertEquals(expectedErrorMessage, errorMessage)
  }

  @Test
  fun testJsonSnapshotByString() {
    val snapshotName = "JsonByString.json"

    val valueToSnapshot = """{"a": "hello"}"""

    val valueUnexpected = """{"a": "hello", "b": "world"}"""

    val expectedErrorMessage = """
      #####################################################################

      Snapshot [JsonByString.json] failed - recreate all snapshots by setting system property REGENERATE_SNAPSHOTS to true
      Example: mvn test -DREGENERATE_SNAPSHOTS=true
      Only recreate failed snapshots by setting system property REGENERATE_FAILED_SNAPSHOTS to true instead
      Example: mvn test -DREGENERATE_FAILED_SNAPSHOTS=true

      Error(s):

      Unexpected: b


      Diff:

       {
      -  "a": "hello"
      +  "a": "hello",
      +  "b": "world"
       }


      #####################################################################
    """.trimIndent() + '\n'

    verifyJsonSnapshot(snapshotName, valueToSnapshot)

    val errorMessage = assertNegative {
      verifyJsonSnapshot(snapshotName, valueUnexpected)
    }

    assertEquals(expectedErrorMessage, errorMessage)
  }

  @Test
  fun testJsonWithIgnoredPathSnapshot() {
    val snapshotName = "JsonWithIgnoredPath.json"

    val timestamp1 = Instant.parse("2021-04-08T14:30:00.123Z")
    val timestamp2 = Instant.parse("2021-03-02T14:30:00.456Z")

    val valueToSnapshot1 = buildJsonObject {
      put(
        "a",
        buildJsonObject {
          put("name", "dev developersen")
          put("timestamp", timestamp1.toString())
        }
      )
      put("b", 1234)
    }

    val valueToSnapshot2 = buildJsonObject {
      put(
        "a",
        buildJsonObject {
          put("name", "dev developersen")
          // Notice different timestamp!
          put("timestamp", timestamp2.toString())
        }
      )
      put("b", 1234)
    }

    val valueUnexpected = JsonObject(valueToSnapshot2 + buildJsonObject { put("b", 4321) })

    val ignoredPaths = listOf("a.timestamp")

    val expectedErrorMessage = """
      #####################################################################

      Snapshot [JsonWithIgnoredPath.json] failed - recreate all snapshots by setting system property REGENERATE_SNAPSHOTS to true
      Example: mvn test -DREGENERATE_SNAPSHOTS=true
      Only recreate failed snapshots by setting system property REGENERATE_FAILED_SNAPSHOTS to true instead
      Example: mvn test -DREGENERATE_FAILED_SNAPSHOTS=true

      Error(s):
      a.timestamp
      Expected: 2021-04-08T14:30:00.123Z
           got: 2021-03-02T14:30:00.456Z
       ; b
      Expected: 1234
           got: 4321


      Diff:

       {
         "a": {
           "name": "dev developersen",
      -    "timestamp": "2021-04-08T14:30:00.123Z"
      +    "timestamp": "2021-03-02T14:30:00.456Z"
         },
      -  "b": 1234
      +  "b": 4321
       }


      #####################################################################
    """.trimIndent() + '\n'

    // Setup a deterministic (stable) snapshot so it will not change on regeneration.
    verifyJsonSnapshot(snapshotName, valueToSnapshot1)

    // Now check that it still runs OK when having an ignored change.
    // Avoid changing files on disk.
    withRegenerateDisabled {
      verifyJsonSnapshot(snapshotName, valueToSnapshot2, ignoredPaths)
    }

    // Check that it can still fail.
    val errorMessage = assertNegative {
      verifyJsonSnapshot(snapshotName, valueUnexpected, ignoredPaths)
    }

    assertEquals(expectedErrorMessage, errorMessage)
  }

  @Nested
  inner class `without any flags set` {
    @Test
    fun `creates a file if not existing`() {
      val snapshotName = "Snapshot.txt"
      val snapshotPath = File("src/test/resources/__snapshots__", snapshotName)

      snapshotPath.deleteBeforeAndAfter {
        assertEquals(false, snapshotPath.exists())
        withRegenerateDisabled {
          verifyStringSnapshot(snapshotName, "hello world")
          assertEquals(true, snapshotPath.exists())
        }
      }
    }

    @Test
    fun `fails if the snapshot is not OK without modifying snapshot`() {
      val snapshotName = "Snapshot.txt"
      val snapshotPath = File("src/test/resources/__snapshots__", snapshotName)

      snapshotPath.deleteBeforeAndAfter {
        snapshotPath.writeText("hello world")

        assertThrows<AssertionError> {
          withRegenerateDisabled {
            verifyStringSnapshot(snapshotName, "another value")
          }
        }

        assertEquals("hello world", snapshotPath.readText())
      }
    }
  }

  @Nested
  inner class `with flag -DREGENERATE_SNAPSHOTS` {
    @Test
    fun `creates a file if not existing`() {
      val snapshotName = "Snapshot.txt"
      val snapshotPath = File("src/test/resources/__snapshots__", snapshotName)

      snapshotPath.deleteBeforeAndAfter {
        assertEquals(false, snapshotPath.exists())
        withRegenerateOnlyAll {
          verifyStringSnapshot(snapshotName, "hello world")
          assertEquals(true, snapshotPath.exists())
        }
      }
    }

    @Test
    fun `modifies an existing snapshot instead of throwing`() {
      val snapshotName = "Snapshot.txt"
      val snapshotPath = File("src/test/resources/__snapshots__", snapshotName)

      snapshotPath.deleteBeforeAndAfter {
        snapshotPath.writeText("hello world")

        withRegenerateOnlyAll {
          verifyStringSnapshot(snapshotName, "another value")
        }

        assertEquals("another value", snapshotPath.readText())
      }
    }
  }

  @Nested
  inner class `with flag -DREGENERATE_FAILED_SNAPSHOTS` {
    @Test
    fun `creates a file if not existing`() {
      val snapshotName = "Snapshot.txt"
      val snapshotPath = File("src/test/resources/__snapshots__", snapshotName)

      snapshotPath.deleteBeforeAndAfter {
        assertEquals(false, snapshotPath.exists())
        withRegenerateOnlyFailure {
          verifyStringSnapshot(snapshotName, "hello world")
          assertEquals(true, snapshotPath.exists())
        }
      }
    }

    @Test
    fun `modifies an existing snapshot instead of throwing`() {
      val snapshotName = "Snapshot.txt"
      val snapshotPath = File("src/test/resources/__snapshots__", snapshotName)

      snapshotPath.deleteBeforeAndAfter {
        snapshotPath.writeText("hello world")

        withRegenerateOnlyFailure {
          verifyStringSnapshot(snapshotName, "another value")
        }

        assertEquals("another value", snapshotPath.readText())
      }
    }

    @Test
    fun `does not modify or fail an existing snapshot if the diff is ignored`() {
      val snapshotName = "Snapshot.json"
      val snapshotPath = File("src/test/resources/__snapshots__", snapshotName)

      val value1 = """
        {
          "name": "hello world",
          "version": "1"
        }
      """.trimIndent() + '\n'

      val value2 = """
        {
          "name": "hello world",
          "version": "2"
        }
      """.trimIndent() + '\n'

      val ignoredPaths = listOf("version")

      snapshotPath.deleteBeforeAndAfter {
        snapshotPath.writeText(value1)

        withRegenerateOnlyFailure {
          verifyJsonSnapshot(snapshotName, value2, ignoredPaths)
        }

        assertEquals(value1, snapshotPath.readText())
      }
    }
  }
}
