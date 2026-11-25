package com.voidlauncher

import com.voidlauncher.data.AccountRepository
import com.voidlauncher.util.Net
import com.voidlauncher.util.offlineUuid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreLogicTest {

    @Test
    fun offlineUuidMatchesMinecraftAlgorithm() {
        // MD5("OfflinePlayer:Notch") with RFC-4122 v3 version/variant bits.
        assertEquals("b50ad385-829d-3141-a216-7e7d7539ba7f", offlineUuid("Notch"))
    }

    @Test
    fun offlineUuidIsDeterministicAndDistinct() {
        assertEquals(offlineUuid("Player_1"), offlineUuid("Player_1"))
        assertNotEquals(offlineUuid("Player_1"), offlineUuid("Player_2"))
        val uuid = offlineUuid("Player_1")
        assertEquals('3', uuid[14]) // version 3
        assertTrue(uuid[19] in listOf('8', '9', 'a', 'b')) // RFC variant
    }

    @Test
    fun usernameValidation() {
        assertNull(AccountRepository.validateUsername("Player_1"))
        assertNull(AccountRepository.validateUsername("abc"))
        assertTrue(AccountRepository.validateUsername("ab")!!.contains("at least"))
        assertTrue(AccountRepository.validateUsername("a".repeat(17))!!.contains("at most"))
        assertTrue(AccountRepository.validateUsername("bad-name")!!.contains("letters"))
    }

    @Test
    fun byteFormatting() {
        assertEquals("512 B", Net.formatBytes(512))
        assertEquals("1.0 KB", Net.formatBytes(1024))
        assertEquals("1.0 MB", Net.formatBytes(1024 * 1024))
    }
}
