package dev.irof.kifuzo.logic.service

import dev.irof.kifuzo.logic.parser.KifuFormat
import dev.irof.kifuzo.models.FileSortOption
import dev.irof.kifuzo.models.KifuInfo
import java.nio.file.Files
import java.time.Instant
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KifuFileServiceTest {

    private val service = KifuFileServiceImpl()

    @Test
    fun scanDirectoryが正しくソートされること() {
        val root = createTempDirectory("kifuzo-scan-test")
        try {
            val fileB = root.resolve("b.kifu").createFile()
            val fileA = root.resolve("a.kifu").createFile()
            val dir = root.resolve("dir").createDirectory()

            // 名前順
            val byName = service.scanDirectory(root, FileSortOption.NAME)
            // ディレクトリが先、その後にファイルの名前順
            assertEquals("dir", byName[0].fileName.toString())
            assertEquals("a.kifu", byName[1].fileName.toString())
            assertEquals("b.kifu", byName[2].fileName.toString())

            // 更新日時順 (fileAの方が新しい)
            Files.setLastModifiedTime(fileB, java.nio.file.attribute.FileTime.from(Instant.now().minusSeconds(100)))
            val byTime = service.scanDirectory(root, FileSortOption.LAST_MODIFIED)
            assertEquals("dir", byTime[0].fileName.toString())
            assertEquals("a.kifu", byTime[1].fileName.toString())
            assertEquals("b.kifu", byTime[2].fileName.toString())
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun generateProposedNameが正しく動作すること() {
        val info = KifuInfo(
            path = java.nio.file.Paths.get("test.kifu"),
            event = "棋戦名",
            senteName = "先手",
            goteName = "後手",
            startTime = "2026/03/02 12:34:56",
            format = KifuFormat.KIF,
        )
        val template = "{開始日の年月日}_{開始日の時分秒}_{棋戦名}_{先手}_{後手}"

        val proposed = service.generateProposedNameForPasted(info, template)
        assertEquals("20260302_123456_棋戦名_先手_後手.kifu", proposed)
    }

    @Test
    fun 特殊文字を含む名前がサニタイズされること() {
        val info = KifuInfo(
            path = java.nio.file.Paths.get("test.kifu"),
            event = "棋戦/名",
            senteName = "先手:A",
            goteName = "後手*B",
            startTime = "2026/03/02 12:34:56",
            format = KifuFormat.KIF,
        )
        val template = "{棋戦名}_{先手}_{後手}"

        val proposed = service.generateProposedNameForPasted(info, template)
        assertEquals("棋戦_名_先手_A_後手_B.kifu", proposed)
    }
}
