package no.liflig.snapshot

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import java.io.File
import kotlin.test.assertEquals

private const val REGENERATE_SNAPSHOTS = "REGENERATE_SNAPSHOTS"

/**
 * Check that we are running using the expected working directory.
 *
 * In IntelliJ if using a multi-module Maven project, the working directory
 * will be the workspace directory and not each module directory. This must
 * be changed in test configuration in IntelliJ.
 */
private fun checkExpectedWorkingDirectory() {
  check(File("src/test/resources").exists()) {
    "The tests are likely running with wrong working directory. src/test/resources was not found"
  }
}

private fun shouldRegenerate(): Boolean =
  System.getProperty(REGENERATE_SNAPSHOTS)?.toBoolean() ?: false

private fun createDiff(
  original: List<String>,
  new: List<String>
): String {
  val patch = DiffUtils.diff(original, new)
  val diff = UnifiedDiffUtils.generateUnifiedDiff(null, null, original, patch, 10)
  return diff.drop(3).joinToString("\n")
}

/**
 * Find and read previous snapshot and compare the new and old values
 * to match exactly.
 *
 * The snapshot named [name] will be stored in src/test/resources/__snapshots__
 * and might contain a subdirectory, e.g. "subdir/test.json".
 *
 * The [getExtra] parameter is for local library usage.
 */
fun verifyStringSnapshot(
  name: String,
  value: String,
  getExtra: ((previous: String, current: String) -> String?)? = null
) {
  checkExpectedWorkingDirectory()

  val resource = File("src/test/resources/__snapshots__", name)
  resource.parentFile.mkdirs()

  val snapshotExists = resource.exists()

  if (!snapshotExists || shouldRegenerate()) {
    if (snapshotExists) {
      val existingValue = resource.readText()
      if (existingValue == value) {
        // Existing snapshot OK.
        return
      }

      println("[INFO] Snapshot for [$name] does not match, regenerating")
    } else {
      println("[INFO] Created initial snapshot for [$name]")
    }

    resource.writeText(value)
    return
  }

  val existingValue = resource.readText()
  try {
    assertEquals(existingValue, value)
  } catch (e: AssertionError) {
    val diff = createDiff(existingValue.lines(), value.lines())

    val extra = getExtra?.invoke(existingValue, value)?.let {
      "$it\n\n"
    } ?: ""

    System.err.println(
      """
#####################################################################

Snapshot [$name] failed - recreate by setting system property $REGENERATE_SNAPSHOTS to true
Example: mvn test -DREGENERATE_SNAPSHOTS=true

${extra}Diff:

$diff

#####################################################################
      """.trimIndent()
    )
    throw AssertionError("Snapshot [$name] doesn't match")
  }
}
