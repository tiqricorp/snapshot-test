package no.liflig.snapshot

import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Test
import java.time.Instant

class Test {

  @Test
  fun testStringSnapshot() {
    verifyStringSnapshot("String.txt", "Some text")
  }

  @Test
  fun testJsonSnapshot() {
    verifyJsonSnapshot(
      "Json.json",
      buildJsonObject {
        put(
          "a",
          buildJsonArray {
            add(1)
            add(2)
          }
        )
      }
    )
  }

  @Test
  fun testJsonWithIgnoredPathSnapshot() {
    val element = buildJsonObject {
      put(
        "a",
        buildJsonObject {
          put("name", "dev developersen")
          put("timestamp", Instant.now().toString())
        }
      )
      put("b", 1234)
    }
    verifyJsonSnapshot(
      "JsonWithIgnoredPath.json",
      element,
      listOf("a.timestamp")
    )
  }
}
