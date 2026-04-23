package com.huanchengfly.tieba.post.utils

import com.huanchengfly.tieba.post.mockk.varargArray
import com.huanchengfly.tieba.post.models.database.BlockForum
import com.huanchengfly.tieba.post.models.database.BlockKeyword
import com.huanchengfly.tieba.post.models.database.BlockUser
import com.huanchengfly.tieba.post.models.database.dao.BlockDao
import com.huanchengfly.tieba.post.models.database.dao.KeywordCSV
import com.huanchengfly.tieba.post.models.database.dao.TransactionRunner
import com.huanchengfly.tieba.post.models.database.dao.TransactionRunnerDao
import com.huanchengfly.tieba.post.models.database.dao.UserCSV
import com.huanchengfly.tieba.post.utils.BlockRuleBackupUtil.ENTRY_NAME_FORUM
import com.huanchengfly.tieba.post.utils.BlockRuleBackupUtil.ENTRY_NAME_KEYWORD
import com.huanchengfly.tieba.post.utils.BlockRuleBackupUtil.ENTRY_NAME_USER
import de.siegmar.fastcsv.reader.CsvParseException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private val testForums = listOf(
    null.toString(),
    "坏:(",
    "comma,"
)

private val testForumsTxt = """
        null
        坏:(
        comma,
    """.trimIndent()

private val testKeywords = listOf(
    KeywordCSV("#A⏎#B", isRegex = false, whitelisted = true),
    KeywordCSV("Comma!,", isRegex = false, whitelisted = true),
    KeywordCSV("abc,{1,3}芔", isRegex = true, whitelisted = false),
    KeywordCSV("␤A␤B␤", isRegex = true, whitelisted = false),
)

private val testKeywordsCSV = """
        keyword,isRegex,whitelisted
        "#A⏎#B",0,1
        "Comma!,",0,1
        "abc,{1,3}芔",1,0
        ␤A␤B␤,1,0
    """.trimIndent()

private val testUser = listOf(
    UserCSV(uid = 1, name = ",", whitelisted = false),
    UserCSV(uid = 2, name = "\"", whitelisted = false),
    UserCSV(uid = 3, name = "\uD83D\uDE21", whitelisted = false),
    UserCSV(uid = 4, name = "\\d{3,}", whitelisted = false),
    UserCSV(uid = 5, name = "#", whitelisted = false),
    UserCSV(uid = Long.MAX_VALUE, name = null, whitelisted = false),
)

private val testUserCSV = """
        uid,name,whitelisted
        1,",",0
        2,""${'"'}${'"'},0
        3,😡,0
        4,"\d{3,}",0
        5,#,0
        9223372036854775807,,0
    """.trimIndent()

class BlockRuleBackupUtilTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @RelaxedMockK
    private lateinit var dao: BlockDao

    // Dummy Database TransactionRunner
    private val transaction: TransactionRunner = TransactionRunnerDao

    @Test
    fun `Test backup from dao`() = runTest {
        // Mock DAO and return test data
        coEvery { dao.getForums() } returns testForums
        coEvery { dao.getAllKeywords() } returns testKeywords
        coEvery { dao.getAllUsers() } returns testUser

        // Write to In-Memory Zip
        val zipBytes = ByteArrayOutputStream().use { out ->
            BlockRuleBackupUtil.backup(dao, transaction, timestamp = 0, output = out)
            out.toByteArray()
        }

        assertValidZip(
            input = ByteArrayInputStream(zipBytes),
            expectedForums = testForumsTxt,
            expectedKeywordsCsv = testKeywordsCSV,
            expectedUsersCsv = testUserCSV,
        )
    }

    @Test
    fun `writeForum should write expected txt data`() {
        val forumsBytes = ByteArrayOutputStream().use { out ->
            out.writer().use { writer ->
                BlockRuleBackupUtil.writeForum(testForums, writer)
            }
            out.toByteArray()
        }
        assertEquals(testForumsTxt, String(forumsBytes))
    }

    @Test
    fun `writeKeyword should write expected csv data`() {
        val keywordsBytes = ByteArrayOutputStream().use { out ->
            out.writer().use { writer ->
                BlockRuleBackupUtil.writeKeyword(testKeywords, writer)
            }
            out.toByteArray()
        }

        assertEquals(testKeywordsCSV, String(keywordsBytes).trim())
    }

    @Test
    fun `writeUser should write expected csv data`() {
        val usersBytes = ByteArrayOutputStream().use { out ->
            out.writer().use { writer ->
                BlockRuleBackupUtil.writeUser(testUser, writer)
            }
            out.toByteArray()
        }

        assertEquals(testUserCSV, String(usersBytes).trim())
    }

    @Test
    fun `Test restore all rules`() = runTest {
        // Prepare In-Memory Zip
        val zipBytes = genTestZip(testForumsTxt, testKeywordsCSV, testUserCSV)
        BlockRuleBackupUtil.restore(dao, transaction, ByteArrayInputStream(zipBytes), 0)

        // Prepare expected vararg Database entities
        val expectedForums: Array<BlockForum> = testForums.map { BlockForum(it) }.toTypedArray()
        val expectedKeywords: Array<BlockKeyword> = testKeywords.toDataBaseEntity()
        val expectedUsers: Array<BlockUser> = testUser.toTypedArray()

        coVerifyOrder {
            // Verify forum table cleared and restored
            dao.deleteAllForum()
            dao.insertForums(*varargArray(expectedForums))

            // Verify keyword table cleared and restored
            dao.deleteAllKeyword()
            dao.insertKeywords(*varargArray(expectedKeywords))

            // Verify user table cleared and restored
            dao.deleteAllUser()
            dao.insertUsers(*varargArray(expectedUsers))
        }
    }

    @Test
    fun `Test restore with option`() = runTest {
        // Exclude forums and keywords
        val restoreOption = RestoreOption.EXCLUDE_FORUM or RestoreOption.EXCLUDE_KEYWORD

        // Prepare In-Memory Zip
        val zipBytes = genTestZip(testForumsTxt, testKeywordsCSV, testUserCSV)
        BlockRuleBackupUtil.restore(dao, transaction, ByteArrayInputStream(zipBytes), restoreOption)

        coEvery { dao.insertForums(*anyVararg()) } answers {
            throw AssertionError("Unexpected forum insertion")
        }
        coEvery { dao.insertKeywords(*anyVararg()) } answers {
            throw AssertionError("Unexpected keyword insertion")
        }

        // Verify not called
        coVerify(exactly = 0) {
            dao.deleteAllForum()
            dao.insertForums(*anyVararg())

            dao.deleteAllKeyword()
            dao.insertKeywords(*anyVararg())
        }

        // Verify user table cleared and restored
        coVerifyOrder {
            val expectedUsers: Array<BlockUser> = testUser.toTypedArray()
            dao.deleteAllUser()
            dao.insertUsers(*varargArray(expectedUsers))
        }
    }

    @Test
    fun `readForum should restore expected forum list`() {
        val forums: List<String> = testForumsTxt.reader().buffered().use { reader ->
            BlockRuleBackupUtil.readForum(reader)
        }

        assertThat(forums, `is`(testForums))
    }

    @Test
    fun `readKeyword should restore expected KeywordCSV objects`() {
        val keywords: List<KeywordCSV> = testKeywordsCSV.reader().use { reader ->
            BlockRuleBackupUtil.readKeyword(reader)
        }

        assertThat(keywords, `is`(testKeywords))
    }

    @Test
    fun `readUser should restore expected UserCSV objects`() {
        val users: List<UserCSV> = testUserCSV.reader().use { reader ->
            BlockRuleBackupUtil.readUser(reader)
        }

        assertThat(users, `is`(testUser))
    }

    @Test(expected = CsvParseException::class)
    fun `readKeyword should throw exception`() {
        val badKeywordsCSV = "keyword,isRegex,whitelisted\n,0,1"
        BlockRuleBackupUtil.readKeyword(badKeywordsCSV.reader())
    }

    @Test(expected = CsvParseException::class)
    fun `readUser should throw exception`() {
        val badUserCSV = "uid,name,whitelisted\n-1,😡,0"
        BlockRuleBackupUtil.readUser(badUserCSV.reader())
    }

    private fun List<KeywordCSV>.toDataBaseEntity(): Array<BlockKeyword> {
        var id = 0L // Manage keyword ID manually
        return map { BlockKeyword(id++, it.keyword, it.isRegex, it.whitelisted) }.toTypedArray()
    }

    private fun assertValidZip(
        input: InputStream,
        expectedForums: String,
        expectedKeywordsCsv: String,
        expectedUsersCsv: String,
    ) {
        var forums: String? = null
        var keywords: String? = null
        var users: String? = null

        ZipInputStream(input).use { zipIn ->
            zipIn.bufferedReader().use { reader ->
                var entry: ZipEntry? = zipIn.nextEntry
                while (entry != null) {
                    when(entry.name) {
                        ENTRY_NAME_FORUM -> forums = reader.readText().trim()

                        ENTRY_NAME_KEYWORD -> keywords = reader.readText().trim()

                        ENTRY_NAME_USER -> users = reader.readText().trim()
                    }
                    assertFalse(entry.isDirectory)
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
        }

        assertNotNull("Expect $ENTRY_NAME_FORUM exists in zip", forums)
        assertNotNull("Expect $ENTRY_NAME_KEYWORD exists in zip", keywords)
        assertNotNull("Expect $ENTRY_NAME_USER exists in zip", users)

        assertEquals(expectedForums, forums)
        assertEquals(expectedKeywordsCsv, keywords)
        assertEquals(expectedUsersCsv, users)
    }

    private fun genTestZip(forumsText: String, keywordsCsv: String, usersCsv: String): ByteArray {
        val zipFiles = listOf(
            ENTRY_NAME_FORUM to forumsText,
            ENTRY_NAME_KEYWORD to keywordsCsv,
            ENTRY_NAME_USER to usersCsv,
        )

        return ByteArrayOutputStream().use { out ->
            ZipOutputStream(out).use { zipOut ->
                zipFiles.forEach { (name, content) ->
                    zipOut.putNextEntry(ZipEntry(name))
                    zipOut.write(content.toByteArray())
                    zipOut.closeEntry()
                }
            }
            out.toByteArray()
        }
    }
}