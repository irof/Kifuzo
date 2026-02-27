package dev.irof.kifuzo.logic.service
import dev.irof.kifuzo.StubKifuRepository
import dev.irof.kifuzo.logic.handler.FileActionHandler
import dev.irof.kifuzo.logic.handler.ImportHandler
import dev.irof.kifuzo.logic.handler.SettingsHandler
import dev.irof.kifuzo.logic.io.readLinesWithEncoding
import dev.irof.kifuzo.logic.io.readTextWithEncoding
import dev.irof.kifuzo.logic.parser.HeaderParser
import dev.irof.kifuzo.logic.parser.KifuParseException
import dev.irof.kifuzo.logic.parser.convertCsaToKifu
import dev.irof.kifuzo.logic.parser.parseCsa
import dev.irof.kifuzo.logic.parser.parseHeader
import dev.irof.kifuzo.logic.parser.parseKifu
import dev.irof.kifuzo.logic.parser.scanKifuInfo
import dev.irof.kifuzo.logic.service.FileTreeManager
import dev.irof.kifuzo.logic.service.KifuRepository
import dev.irof.kifuzo.logic.service.KifuRepositoryImpl
import dev.irof.kifuzo.logic.service.KifuSessionBuilder
import java.nio.file.Files
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteIfExists
import kotlin.io.path.name
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KifuRepositoryTest {

    private var tempDir = createTempDirectory()
    private val repository = KifuRepositoryImpl()

    @BeforeTest
    fun setUp() {
        tempDir = createTempDirectory()
    }

    @AfterTest
    fun tearDown() {
        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun ディレクトリ走査の結果がディレクトリ優先かつ名前順であること() {
        // ディレクトリとファイルを作成
        // 期待される順序: ディレクトリが先 (名前順)、次にファイル (名前順)
        val dirB = tempDir.resolve("dirB").createDirectory()
        val dirA = tempDir.resolve("dirA").createDirectory()
        val fileB = tempDir.resolve("fileB.kifu").createFile()
        val fileA = tempDir.resolve("fileA.kifu").createFile()
        val fileUpper = tempDir.resolve("FILEC.kifu").createFile()

        val contents = repository.scanDirectory(tempDir)

        assertEquals(5, contents.size)
        // ディレクトリが先
        assertEquals("dirA", contents[0].name)
        assertEquals("dirB", contents[1].name)
        // ファイルが後 (大文字小文字を区別しない)
        assertEquals("fileA.kifu", contents[2].name)
        assertEquals("fileB.kifu", contents[3].name)
        assertEquals("FILEC.kifu", contents[4].name)
    }

    @Test
    fun 空のディレクトリ走査で空リストを返すこと() {
        val contents = repository.scanDirectory(tempDir)
        assertTrue(contents.isEmpty())
    }

    @Test
    fun 存在しないディレクトリ走査で空リストを返すこと() {
        val nonExistent = tempDir.resolve("non_existent")
        val contents = repository.scanDirectory(nonExistent)
        assertTrue(contents.isEmpty())
    }
}
