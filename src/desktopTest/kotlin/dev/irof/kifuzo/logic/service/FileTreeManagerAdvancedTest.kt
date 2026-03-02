package dev.irof.kifuzo.logic.service

import dev.irof.kifuzo.models.FileFilter
import dev.irof.kifuzo.models.FileSortOption
import dev.irof.kifuzo.models.FileTreeNode
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

    @Test
    fun buildTreeが指定したパスを展開してツリーを構築すること() {
        val root = createTempDirectory("kifuzo-test-tree")
        try {
            val dir1 = (root.resolve("dir1")).createDirectory()
            val file1 = (root.resolve("file1.kifu")).createFile()
            val file2 = (dir1.resolve("file2.csa")).createFile()

            val manager = FileTreeManager(object : dev.irof.kifuzo.StubKifuRepository() {
                override fun scanDirectory(directory: java.nio.file.Path, sortOption: dev.irof.kifuzo.models.FileSortOption): List<java.nio.file.Path> = when (directory) {
                    root -> listOf(dir1, file1)
                    dir1 -> listOf(file2)
                    else -> emptyList()
                }
            })

            // dir1 を展開状態にする
            val initialNodes = listOf(FileTreeNode(dir1, 0, true, isExpanded = true))
            val nodes = manager.buildTree(root, initialNodes)

            assertEquals(3, nodes.size)
            assertEquals(dir1, nodes[0].path)
            assertTrue(nodes[0].isExpanded)
            assertEquals(file2, nodes[1].path) // 子
            assertEquals(1, nodes[1].level)
            assertEquals(file1, nodes[2].path) // 兄弟
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun buildTreeで権限エラーが多発した場合に例外を投げること() {
        val root = createTempDirectory("kifuzo-error-root")
        try {
            val dir1 = root.resolve("dir1").createDirectory()
            val dir2 = root.resolve("dir2").createDirectory()
            val dir3 = root.resolve("dir3").createDirectory()

            val manager = FileTreeManager(object : dev.irof.kifuzo.StubKifuRepository() {
                override fun scanDirectory(directory: java.nio.file.Path, sortOption: dev.irof.kifuzo.models.FileSortOption): List<java.nio.file.Path> = when (directory) {
                    root -> listOf(dir1, dir2, dir3)
                    else -> throw java.io.IOException("Permission denied")
                }
            })

            // 3つのディレクトリをすべて展開状態に設定
            val initialNodes = listOf(
                FileTreeNode(dir1, 0, true, isExpanded = true),
                FileTreeNode(dir2, 0, true, isExpanded = true),
                FileTreeNode(dir3, 0, true, isExpanded = true),
            )

            kotlin.test.assertFailsWith<dev.irof.kifuzo.logic.parser.TooManyErrorsException> {
                manager.buildTree(root, initialNodes)
            }
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun buildFlatListが隠しディレクトリをスキップすること() {
        val root = createTempDirectory("kifuzo-test-hidden-dir")
        try {
            val hiddenDir = root.resolve(".hidden").createDirectory()
            hiddenDir.resolve("file.kifu").createFile()
            val normalDir = root.resolve("normal").createDirectory()
            val normalFile = normalDir.resolve("file.kifu").createFile()

            val manager = FileTreeManager(object : dev.irof.kifuzo.StubKifuRepository() {})
            val nodes = manager.buildFlatList(root)

            assertEquals(1, nodes.size)
            assertEquals(normalFile, nodes[0].path)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun toggleNodeが隠しファイルを無視すること() {
        val root = createTempDirectory("kifuzo-test-toggle-hidden")
        try {
            val dir = root.resolve("dir").createDirectory()
            dir.resolve(".hidden.kifu").createFile()
            dir.resolve("visible.kifu").createFile()

            val manager = FileTreeManager(object : dev.irof.kifuzo.StubKifuRepository() {
                override fun scanDirectory(directory: java.nio.file.Path, sortOption: dev.irof.kifuzo.models.FileSortOption): List<java.nio.file.Path> = when (directory) {
                    dir -> listOf(dir.resolve(".hidden.kifu"), dir.resolve("visible.kifu"))
                    else -> emptyList()
                }
            })

            val node = FileTreeNode(dir, 0, true, isExpanded = false)
            val result = manager.toggleNode(node, listOf(node))

            // 展開後のリストには visible.kifu だけが含まれるはず
            assertEquals(2, result.size)
            assertTrue(result.any { it.name == "visible.kifu" })
            assertTrue(result.none { it.name.startsWith(".") })
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}
