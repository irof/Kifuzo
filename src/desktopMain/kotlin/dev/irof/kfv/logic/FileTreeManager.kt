package dev.irof.kfv.logic

import dev.irof.kfv.models.FileTreeNode
import dev.irof.kfv.viewmodel.FileFilter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

class FileTreeManager(
    private val repository: KifuRepository,
) {
    /**
     * ルートディレクトリから現在の展開状態に基づいてファイルツリーを構築します。
     */
    fun buildTree(
        root: Path,
        currentNodes: List<FileTreeNode>,
        filter: FileFilter = FileFilter.ALL,
    ): List<FileTreeNode> {
        val expandedPaths = currentNodes.filter { it.isExpanded }.map { it.path }.toSet()
        val newNodes = mutableListOf<FileTreeNode>()
        val now = Instant.now()
        val twentyFourHoursAgo = now.minus(24, ChronoUnit.HOURS)

        fun traverse(dir: Path, level: Int) {
            val contents = repository.scanDirectory(dir)
            contents.forEach { path ->
                val isDir = path.isDirectory()
                val isExpanded = expandedPaths.contains(path)

                // フィルタの適用
                val matchesFilter = when (filter) {
                    FileFilter.ALL -> true
                    FileFilter.KIFU_ONLY -> isDir || isKifuFile(path)
                    FileFilter.RECENT -> isDir || isRecentFile(path, twentyFourHoursAgo)
                }

                if (matchesFilter) {
                    val node = FileTreeNode(path, level, isDir, isExpanded)
                    newNodes.add(node)

                    if (isDir && isExpanded) {
                        traverse(path, level + 1)
                    }
                }
            }
        }

        traverse(root, 0)
        return newNodes
    }

    /**
     * 全ファイルを再帰的にスキャンし、フラットなリストを構築します。
     */
    fun buildFlatList(
        root: Path,
        filter: FileFilter = FileFilter.ALL,
    ): List<FileTreeNode> {
        val newNodes = mutableListOf<FileTreeNode>()
        val now = Instant.now()
        val twentyFourHoursAgo = now.minus(24, ChronoUnit.HOURS)

        Files.walk(root).filter { it.isRegularFile() }.forEach { path ->
            val matchesFilter = when (filter) {
                FileFilter.ALL -> true
                FileFilter.KIFU_ONLY -> isKifuFile(path)
                FileFilter.RECENT -> isRecentFile(path, twentyFourHoursAgo)
            }

            if (matchesFilter) {
                newNodes.add(FileTreeNode(path, 0, false, false))
            }
        }

        return newNodes.sortedByDescending {
            try {
                Files.readAttributes(it.path, BasicFileAttributes::class.java).lastModifiedTime().toInstant()
            } catch (e: Exception) {
                Instant.MIN
            }
        }
    }

    private fun isKifuFile(path: Path): Boolean {
        val ext = path.extension.lowercase()
        return ext == "kifu" || ext == "kif" || ext == "csa"
    }

    private fun isRecentFile(path: Path, since: Instant): Boolean = try {
        val attrs = Files.readAttributes(path, BasicFileAttributes::class.java)
        attrs.lastModifiedTime().toInstant().isAfter(since)
    } catch (e: Exception) {
        false
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
