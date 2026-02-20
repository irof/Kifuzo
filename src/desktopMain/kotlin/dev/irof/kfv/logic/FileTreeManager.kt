package dev.irof.kfv.logic

import dev.irof.kfv.models.FileTreeNode
import java.nio.file.Path
import kotlin.io.path.isDirectory

class FileTreeManager(
    private val repository: KifuRepository,
) {
    /**
     * ルートディレクトリから現在の展開状態に基づいてファイルツリーを構築します。
     */
    fun buildTree(root: Path, currentNodes: List<FileTreeNode>): List<FileTreeNode> {
        val expandedPaths = currentNodes.filter { it.isExpanded }.map { it.path }.toSet()
        val newNodes = mutableListOf<FileTreeNode>()

        fun traverse(dir: Path, level: Int) {
            val contents = repository.scanDirectory(dir)
            contents.forEach { path ->
                val isDir = path.isDirectory()
                val isExpanded = expandedPaths.contains(path)
                val node = FileTreeNode(path, level, isDir, isExpanded)
                newNodes.add(node)

                if (isDir && isExpanded) {
                    traverse(path, level + 1)
                }
            }
        }

        traverse(root, 0)
        return newNodes
    }

    /**
     * 指定されたノードの開閉状態を切り替え、新しいツリーリストを返します。
     */
    fun toggleNode(node: FileTreeNode, currentNodes: List<FileTreeNode>): List<FileTreeNode> {
        if (!node.isDirectory) return currentNodes

        val newNodes = currentNodes.toMutableList()
        val index = newNodes.indexOfFirst { it.path == node.path }
        if (index == -1) return currentNodes

        if (node.isExpanded) {
            // 閉じる: 配下のノードを削除
            newNodes[index] = node.copy(isExpanded = false)
            var i = index + 1
            while (i < newNodes.size && newNodes[i].level > node.level) {
                newNodes.removeAt(i)
            }
        } else {
            // 展開: 子ノードを追加
            newNodes[index] = node.copy(isExpanded = true)
            val children = repository.scanDirectory(node.path)
            val childNodes = children.map { FileTreeNode(it, node.level + 1, it.isDirectory()) }
            newNodes.addAll(index + 1, childNodes)
        }
        return newNodes
    }
}
