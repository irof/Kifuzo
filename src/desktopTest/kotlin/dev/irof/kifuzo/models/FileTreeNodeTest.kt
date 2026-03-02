package dev.irof.kifuzo.models

import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals

class FileTreeNodeTest {
    @Test
    fun FileTreeNodeのプロパティが正しく取得できること() {
        val path = Paths.get("/tmp/test.kifu")
        val node = FileTreeNode(path, 0, false)
        assertEquals("test.kifu", node.name)

        val dirNode = FileTreeNode(Paths.get("/tmp"), 0, true)
        assertEquals("tmp", dirNode.name)
    }

    @Test
    fun ルートディレクトリの場合に名前がスラッシュになること() {
        val node = FileTreeNode(Paths.get("/"), 0, true)
        assertEquals("/", node.name)
    }
}
