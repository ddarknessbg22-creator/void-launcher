package com.voidlauncher

import com.voidlauncher.model.VersionDetails
import com.voidlauncher.util.voidJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionJsonTest {
    private val json = """
    {
      "id": "1.21.11",
      "type": "release",
      "assets": "26",
      "assetIndex": {"url": "https://example.com/index.json", "sha1": "aaa", "size": 100},
      "mainClass": "net.minecraft.client.main.Main",
      "javaVersion": {"component": "java-runtime-delta", "majorVersion": 21},
      "arguments": {
        "game": [
          "--username", "${'$'}{auth_player_name}",
          {"rules": [{"action": "allow", "os": {"name": "osx"}}], "value": ["-Xstuff", "1"]},
          {"rules": [{"action": "allow"}], "value": "--versionName"}
        ],
        "jvm": ["-Xmx2G"]
      },
      "libraries": [
        {"name": "org.ow2.asm:asm:9", "downloads": {"artifact": {
          "path": "org/ow2/asm/asm/9/asm-9.jar",
          "url": "https://example.com/asm-9.jar",
          "sha1": "abc123", "size": 10
        }}},
        {"name": "skipped:osx", "rules": [{"action": "allow", "os": {"name": "osx"}}]}
      ],
      "downloads": {"client": {
        "url": "https://example.com/client.jar", "sha1": "deadbeef", "size": 100
      }}
    }
    """.trimIndent()

    @Test
    fun parsesMixedArgumentTypesWithoutCrashing() {
        val details = voidJson.decodeFromString<VersionDetails>(json)
        assertEquals("1.21.11", details.id)
        assertEquals("net.minecraft.client.main.Main", details.mainClass)
        assertEquals(21, details.javaVersion?.majorVersion)
        assertEquals(4, details.arguments?.game?.size)
        assertTrue(details.arguments?.game?.any { it.toString().contains("rules") } == true)
        assertEquals(2, details.libraries.size)
    }

    @Test
    fun parsesMojangAssetIndexFieldName() {
        val details = voidJson.decodeFromString<VersionDetails>(json)
        assertEquals("https://example.com/index.json", details.assetsIndex?.url)
    }

    @Test
    fun ignoresUnknownFutureFields() {
        val extended = json.replace("\"assets\": \"26\"", "\"assets\": \"26\", \"futureField\": {\"x\": 1}")
        val details = voidJson.decodeFromString<VersionDetails>(extended)
        assertEquals("26", details.assets)
    }
}
