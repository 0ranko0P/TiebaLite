package com.huanchengfly.tieba.post.utils

import androidx.annotation.IntDef
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import com.huanchengfly.tieba.post.api.booleanToString
import com.huanchengfly.tieba.post.models.database.BlockForum
import com.huanchengfly.tieba.post.models.database.BlockKeyword
import com.huanchengfly.tieba.post.models.database.BlockUser
import com.huanchengfly.tieba.post.models.database.dao.BlockDao
import com.huanchengfly.tieba.post.models.database.dao.KeywordCSV
import com.huanchengfly.tieba.post.models.database.dao.TransactionRunner
import com.huanchengfly.tieba.post.models.database.dao.UserCSV
import com.huanchengfly.tieba.post.ui.models.settings.BlockBackupMetadata
import com.huanchengfly.tieba.post.utils.RestoreOption.Companion.EXCLUDE_FORUM
import com.huanchengfly.tieba.post.utils.RestoreOption.Companion.EXCLUDE_KEYWORD
import com.huanchengfly.tieba.post.utils.RestoreOption.Companion.EXCLUDE_USER
import com.huanchengfly.tieba.post.utils.StringUtil.normalized
import de.siegmar.fastcsv.reader.CsvParseException
import de.siegmar.fastcsv.reader.CsvReader
import de.siegmar.fastcsv.writer.CsvWriter
import de.siegmar.fastcsv.writer.LineDelimiter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import okhttp3.internal.closeQuietly
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import java.io.Writer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.time.Clock

@IntDef(EXCLUDE_FORUM, EXCLUDE_KEYWORD, EXCLUDE_USER)
@Retention(AnnotationRetention.SOURCE)
annotation class RestoreOption {
    companion object {
        /** 恢复选项: 排除吧规则 */
        const val EXCLUDE_FORUM: Int = 2

        /** 恢复选项: 排除关键字规则 */
        const val EXCLUDE_KEYWORD: Int = EXCLUDE_FORUM shl 1

        /** 恢复选项: 排除用户规则 */
        const val EXCLUDE_USER: Int = EXCLUDE_FORUM shl 2
    }
}

object BlockRuleBackupUtil {

    /**
     * Zip Entry: [BlockBackupMetadata]
     * */
    private const val ENTRY_NAME_METADATA = "META-INF/metadata"

    /**
     * Zip Entry: block_forum keyword column backup
     * */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    const val ENTRY_NAME_FORUM = "forum.txt"

    /**
     * Zip Entry: block_keyword table csv backup
     * */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    const val ENTRY_NAME_KEYWORD = "keywords.csv"

    /**
     * Zip Entry: block_user table csv backup
     * */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    const val ENTRY_NAME_USER = "users.csv"

    /**
     * Block rule backup version 1
     *
     * Backup Structure
     *   ├── META-INF
     *   │      └── metadata
     *   ├── forum.txt
     *   ├── keywords.csv
     *   └── users.csv
     *
     * @since 4.0.0-beta.4.4
     * */
    private const val DEFAULT_BACKUP_VERSION = 1

    @WorkerThread
    @Throws(IOException::class, SerializationException::class)
    fun readMetadata(input: InputStream): BlockBackupMetadata = ZipInputStream(input).use { zipIn ->
        var entry: ZipEntry? = zipIn.nextEntry
        while (entry != null) {
            if (entry.name == ENTRY_NAME_METADATA) {
                return Json.decodeFromStream(zipIn)
            }
            zipIn.closeEntry()
            entry = zipIn.nextEntry
        }
        throw FileNotFoundException("Metadata not found")
    }

    @Throws(IOException::class)
    suspend fun backup(
        dao: BlockDao,
        transaction: TransactionRunner,
        timestamp: Long,
        output: OutputStream
    ): Int = withContext(Dispatchers.IO) {
        val (forums, keywords, users) = transaction {
            Triple(dao.getForums(), dao.getAllKeywords(), dao.getAllUsers())
        }
        val totalRules = forums.size + keywords.size + users.size
        if (totalRules == 0) {
            throw IllegalStateException("备份规则为空")
        }

        val metadata = BlockBackupMetadata(
            version = DEFAULT_BACKUP_VERSION,
            timestamp = if (timestamp <= 0) Clock.System.now().toEpochMilliseconds() else timestamp,
            forumRuleCount = forums.size,
            keywordRuleCount = keywords.size,
            userRuleCount = users.size,
        )

        var zipOut: ZipOutputStream? = null
        var writer: BufferedWriter? = null
        try {
            zipOut = ZipOutputStream(output)
            writer = zipOut.bufferedWriter()
            // Write metadata
            zipOut.putNextEntry(ZipEntry(ENTRY_NAME_METADATA))
            Json.encodeToStream(metadata, zipOut)
            zipOut.closeEntry()

            // Write block rules
            writer.writeListToZipEntry(forums, ENTRY_NAME_FORUM, zipOut, ::writeForum)
            writer.writeListToZipEntry(keywords, ENTRY_NAME_KEYWORD, zipOut, ::writeKeyword)
            writer.writeListToZipEntry(users, ENTRY_NAME_USER, zipOut, ::writeUser)
        } finally {
            writer?.closeQuietly()
            zipOut?.closeQuietly()
        }
        totalRules
    }

    private fun <T> Writer.writeListToZipEntry(
        list: List<T>,
        name: String,
        zipOut: ZipOutputStream,
        onWrite: (List<T>, Writer) -> Unit,
    ) {
        if (list.isNotEmpty()) {
            zipOut.putNextEntry(ZipEntry(name))
            onWrite(list, this)
            zipOut.closeEntry()
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    @Throws(IOException::class)
    fun writeForum(forums: List<String>, writer: Writer) {
        for (i in forums.indices) {
            writer.append(forums[i])
            if (i < forums.lastIndex) {
                writer.appendLine()
            }
        }
        writer.flush()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    @Throws(IOException::class)
    fun writeKeyword(keywords: List<KeywordCSV>, writer: Writer) {
        CsvWriter.builder()
            .lineDelimiter(LineDelimiter.LF)
            .build(writer)
            .writeRecord("keyword", "isRegex", "whitelisted")
            .apply {
                for (k in keywords) {
                    writeRecord(k.keyword, k.isRegex.booleanToString(), k.whitelisted.booleanToString())
                }
            }
            .flush()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    @Throws(IOException::class)
    fun writeUser(users: List<UserCSV>, writer: Writer) {
        CsvWriter.builder()
            .lineDelimiter(LineDelimiter.LF)
            .build(writer)
            .writeRecord("uid", "name", "whitelisted")
            .apply {
                for (u in users) {
                    writeRecord(u.uid.toString(), u.name ?: "", u.whitelisted.booleanToString())
                }
            }
            .flush()
    }

    /**
     * Restore block rules from backup
     *
     * @param dao Block rule DAO
     * @param transaction Database transaction runner
     * @param input backup data InputStream
     * @param restoreOption flags controlling how data is restored, see [RestoreOption]
     * */
    @Throws(IOException::class)
    suspend fun restore(
        dao: BlockDao,
        transaction: TransactionRunner,
        input: InputStream,
        restoreOption: Int = 0,
    ): Unit = withContext(Dispatchers.IO) {
        var forums: List<String>? = null
        var keywords: List<KeywordCSV>? = null
        var users: List<UserCSV>? = null

        var zipIn: ZipInputStream? = null
        var reader: BufferedReader? = null
        try {
            zipIn = ZipInputStream(input)
            reader = zipIn.bufferedReader()
            var entry: ZipEntry? = zipIn.nextEntry
            while (entry != null) {
                when (entry.name) {
                    ENTRY_NAME_FORUM -> if (restoreOption and EXCLUDE_FORUM != EXCLUDE_FORUM) {
                        forums = readForum(reader)
                    }

                    ENTRY_NAME_KEYWORD -> if (restoreOption and EXCLUDE_KEYWORD != EXCLUDE_KEYWORD) {
                        keywords = readKeyword(reader)
                    }

                    ENTRY_NAME_USER -> if (restoreOption and EXCLUDE_USER != EXCLUDE_USER) {
                        users = readUser(reader)
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        } finally {
            reader?.closeQuietly()
            zipIn?.closeQuietly()
        }
        // Empty backup or user excluded all data
        if (forums.isNullOrEmpty() && keywords.isNullOrEmpty() && users.isNullOrEmpty()) {
            throw IllegalStateException("Empty restore list, abort!")
        }

        // Restore transaction begin
        transaction {
            // Restore forum blacklist
            if (!forums.isNullOrEmpty()) {
                dao.deleteAllForum()
                dao.insertForums(forums = forums.mapToEntity { name -> BlockForum(name) })
            }

            // Restore keyword block rules
            if (!keywords.isNullOrEmpty()) {
                dao.deleteAllKeyword()
                var id = 0L // PrimaryKey auto generation is off, manage id manually
                dao.insertKeywords(keywords = keywords.mapToEntity { (keyword, isRegex, whitelisted) ->
                    BlockKeyword(id++, keyword, isRegex, whitelisted)
                })
            }

            // Restore user block rules
            if (!users.isNullOrEmpty()) {
                dao.deleteAllUser()
                dao.insertUsers(blockUsers = users.mapToEntity { (uid, name, whitelisted) ->
                    BlockUser(uid, name, whitelisted)
                })
            }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    @Throws(IOException::class)
    fun readForum(reader: BufferedReader): List<String> {
        val rec = mutableListOf<String>()
        for (line in reader.lineSequence()) {
            val forum = line.normalized().trim()
            if (forum.isNotEmpty()) {
                rec.add(forum)
            }
        }
        return rec
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    @Throws(IOException::class, CsvParseException::class)
    fun readKeyword(reader: Reader): List<KeywordCSV> {
        return CsvReader.builder()
            .ofNamedCsvRecord(reader)
            .map { record -> // keyword, isRegex: Int, whitelisted: Int
                try {
                    val keyword = record.getField(0).trim().normalized()
                    if (keyword.isEmpty()) {
                        throw IllegalArgumentException("Empty keyword")
                    }
                    KeywordCSV(
                        keyword = keyword,
                        isRegex = record.getField("isRegex")?.ofBooleanInt()!!,
                        whitelisted = record.getField("whitelisted")?.ofBooleanInt()!!
                    )
                } catch (e: Throwable) {
                    throw CsvParseException("Read keyword failed at: ${record.startingLineNumber}", e)
                }
            }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    @Throws(IOException::class, CsvParseException::class)
    fun readUser(reader: Reader): List<UserCSV> {
        return CsvReader.builder()
            .ofNamedCsvRecord(reader)
            .map { record -> // uid, name, whitelisted: Int
                try {
                    val uid = record.getField(0).toLong()
                    if (uid <= 0) {
                        throw IllegalArgumentException("Invalid uid ${record.getField(0)}")
                    }
                    UserCSV(
                        uid = uid,
                        name = record.getField("name").normalized().trim().takeUnless { it.isEmpty() },
                        whitelisted = record.getField("whitelisted")?.ofBooleanInt()!!
                    )
                } catch (e: Throwable) {
                    throw CsvParseException("Read user failed at: ${record.startingLineNumber}", e)
                }
            }
    }

    fun getBackupFileName(date: Date = Date()): String {
        val now = SimpleDateFormat("yy-MM-dd_HH-mm-ss", Locale.ENGLISH).format(date)
        return "TiebaLite_${now}.tbrules"
    }

    private fun String.ofBooleanInt(): Boolean {
        return when (toIntOrNull()) {
            0 -> false
            1 -> true
            else -> throw NumberFormatException("Invalid boolean bits $this")
        }
    }

    // Convert POJO to Database Entity for insertion
    @Suppress("UNCHECKED_CAST")
    private inline fun <T, reified E> List<T>.mapToEntity(transform: (T) -> E): Array<E> {
        require(isNotEmpty())
        // Map to typed array: map(transform = transform).toTypedArray()
        val typedArray = arrayOfNulls<E>(size)
        for (i in indices) {
            typedArray[i] = transform(this[i])
        }
        return typedArray as Array<E>
    }
}