package dev.irof.kifuzo.logic.service

import dev.irof.kifuzo.models.FileFilter
import dev.irof.kifuzo.models.FileSortOption
import java.nio.file.Files
import java.time.Instant
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileTreeManagerAdvancedTest {

    @Test
    fun buildFlatListが再帰的にファイルを収集すること() {
        val root = createTempDirectory("kifuzo-test-flat")
        try {
            val dir1 = (root.resolve("dir1")).createDirectory()
            val file1 = (root.resolve("file1.kifu")).createFile()
            val file2 = (dir1.resolve("file2.csa")).createFile()
            val hidden = (root.resolve(".hidden")).createFile()

            val manager = FileTreeManager(object : dev.irof.kifuzo.StubKifuRepository() {})
            val nodes = manager.buildFlatList(root)

            // .hidden は除外される
            assertEquals(2, nodes.size)
            assertTrue(nodes.any { it.path == file1 })
            assertTrue(nodes.any { it.path == file2 })
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun RECENTフィルタが正しく動作すること() {
        val root = createTempDirectory("kifuzo-test-recent")
        try {
            val recentFile = (root.resolve("recent.kifu")).createFile()
            val oldFile = (root.resolve("old.kifu")).createFile()

            // oldFile の更新日時を古くする
            val oldTime = Instant.now().minusSeconds(100000) // 約28時間前
            Files.setLastModifiedTime(oldFile, java.nio.file.attribute.FileTime.from(oldTime))

            val manager = FileTreeManager(object : dev.irof.kifuzo.StubKifuRepository() {})
            val nodes = manager.buildFlatList(root, filters = setOf(FileFilter.RECENT))

            assertEquals(1, nodes.size)
            assertEquals(recentFile, nodes[0].path)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun buildFlatListがソートオプションに従うこと() {
        val root = createTempDirectory("kifuzo-test-sort")
        try {
            val fileB = (root.resolve("b.kifu")).createFile()
            val fileA = (root.resolve("a.kifu")).createFile()

            val manager = FileTreeManager(object : dev.irof.kifuzo.StubKifuRepository() {})

            val nodesByName = manager.buildFlatList(root, sortOption = FileSortOption.NAME)
            assertEquals("a.kifu", nodesByName[0].name)
            assertEquals("b.kifu", nodesByName[1].name)

            // 更新日時ソート (fileAの方が新しいはず)
            Files.setLastModifiedTime(fileB, java.nio.file.attribute.FileTime.from(Instant.now().minusSeconds(100)))
            val nodesByTime = manager.buildFlatList(root, sortOption = FileSortOption.LAST_MODIFIED)
            assertEquals("a.kifu", nodesByTime[0].name)
            assertEquals("b.kifu", nodesByTime[1].name)
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}
