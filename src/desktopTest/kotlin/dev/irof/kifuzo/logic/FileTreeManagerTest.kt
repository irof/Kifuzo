package dev.irof.kifuzo.logic

import dev.irof.kifuzo.StubKifuRepository
import dev.irof.kifuzo.models.FileSortOption
import dev.irof.kifuzo.models.FileTreeNode
import dev.irof.kifuzo.models.KifuInfo
import dev.irof.kifuzo.models.ShogiBoardState
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileTreeManagerTest {

    private val mockRepository = object : StubKifuRepository() {
        override fun scanDirectory(directory: Path, sortOption: FileSortOption): List<Path> = when (directory.toString()) {
            "/root" -> listOf(Paths.get("/root/dir1"), Paths.get("/root/file1.kifu"))
            "/root/dir1" -> listOf(Paths.get("/root/dir1/file2.kifu"))
            else -> emptyList()
        }
    }

    private val manager = FileTreeManager(mockRepository)

    @Test
    fun ディレクトリノードを展開できること() {
        val rootNode = FileTreeNode(Paths.get("/root"), 0, true, isExpanded = false)
        val initialNodes = listOf(rootNode)

        // 展開する
        val expandedNodes = manager.toggleNode(rootNode, initialNodes)

        assertEquals(3, expandedNodes.size)
        assertTrue(expandedNodes[0].isExpanded)
        assertEquals("/root/dir1", expandedNodes[1].path.toString())
        assertEquals(1, expandedNodes[1].level)
        assertEquals("/root/file1.kifu", expandedNodes[2].path.toString())
        assertEquals(1, expandedNodes[2].level)
    }

    @Test
    fun ディレクトリノードを閉じることができること() {
        val rootPath = Paths.get("/root")
        val dir1Path = Paths.get("/root/dir1")
        val file1Path = Paths.get("/root/file1.kifu")
        val file2Path = Paths.get("/root/dir1/file2.kifu")

        val initialNodes = listOf(
            FileTreeNode(rootPath, 0, true, isExpanded = true),
            FileTreeNode(dir1Path, 1, true, isExpanded = true),
            FileTreeNode(file2Path, 2, false, isExpanded = false),
            FileTreeNode(file1Path, 1, false, isExpanded = false),
        )

        // dir1 を閉じる
        val collapsedNodes = manager.toggleNode(initialNodes[1], initialNodes)

        assertEquals(3, collapsedNodes.size)
        assertFalse(collapsedNodes[1].isExpanded)
        assertEquals(rootPath, collapsedNodes[0].path)
        assertEquals(dir1Path, collapsedNodes[1].path)
        assertEquals(file1Path, collapsedNodes[2].path)
    }

    @Test
    fun ファイルノードをクリックしても何も起きないこと() {
        val filePath = Paths.get("/root/file1.kifu")
        val fileNode = FileTreeNode(filePath, 1, false, isExpanded = false)
        val initialNodes = listOf(fileNode)

        val resultNodes = manager.toggleNode(fileNode, initialNodes)

        assertEquals(initialNodes, resultNodes)
    }
}
