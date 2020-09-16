package no.liflig.snapshot

import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import org.junit.Test

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
}
