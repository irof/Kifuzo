package dev.irof.kifuzo.logic.service

import dev.irof.kifuzo.logic.parser.TooManyErrorsException
import dev.irof.kifuzo.models.AppConfig
import dev.irof.kifuzo.models.FileFilter
import dev.irof.kifuzo.models.FileSortOption
import dev.irof.kifuzo.models.FileTreeNode
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

private val logger = KotlinLogging.logger {}

class FileTreeManager(
    private val repository: KifuRepository,
) {
    companion object {
        private const val RECENT_FILE_HOURS = 24L
    }

    /**
     * ルートディレクトリから現在の展開状態に基づいてファイルツリーを構築します。
     */
    fun buildTree(
        root: Path,
        currentNodes: List<FileTreeNode>,
        filters: Set<FileFilter> = emptySet(),
        sortOption: FileSortOption = FileSortOption.NAME,
    ): List<FileTreeNode> {
        val expandedPaths = currentNodes.filter { it.isExpanded }.map { it.path }.toSet()
        val newNodes = mutableListOf<FileTreeNode>()
        val now = Instant.now()
        val twentyFourHoursAgo = now.minus(RECENT_FILE_HOURS, ChronoUnit.HOURS)
        val errorCount = java.util.concurrent.atomic.AtomicInteger(0)

        traverse(root, 0, expandedPaths, filters, sortOption, twentyFourHoursAgo, newNodes, errorCount)
        return newNodes
    }

    @Suppress("TooGenericExceptionCaught")
    private fun traverse(
        dir: Path,
        level: Int,
        expandedPaths: Set<Path>,
        filters: Set<FileFilter>,
        sortOption: FileSortOption,
        since: Instant,
        result: MutableList<FileTreeNode>,
        errorCount: java.util.concurrent.atomic.AtomicInteger,
    ) {
        val contents = try {
            repository.scanDirectory(dir, sortOption)
        } catch (e: IOException) {
            logger.warn(e) { "Failed to scan directory: $dir" }
            if (errorCount.incrementAndGet() >= AppConfig.MAX_PERMISSION_ERRORS) {
                throw TooManyErrorsException("権限エラーが多発したため、走査を中断しました。", e)
            }
            return
        }

        contents.forEach { path ->
            if (path.name.startsWith(".")) return@forEach
            val isDir = try {
                path.isDirectory()
            } catch (e: Exception) {
                logger.debug(e) { "Failed to check if directory: $path" }
                false
            }

            if (shouldShowFile(isDir, path, filters, since)) {
                val isExpanded = expandedPaths.contains(path)
                val node = FileTreeNode(path, level, isDir, isExpanded)
                result.add(node)

                if (isDir && isExpanded) {
                    traverse(path, level + 1, expandedPaths, filters, sortOption, since, result, errorCount)
                }
            }
        }
    }

    private fun shouldShowFile(isDir: Boolean, path: Path, filters: Set<FileFilter>, since: Instant): Boolean = isDir || filters.all { filter ->
        when (filter) {
            FileFilter.RECENT -> isRecentFile(path, since)
        }
    }

    /**
     * 全ファイルを再帰的にスキャンし、フラットなリストを構築します。
     */
    fun buildFlatList(
        root: Path,
        filters: Set<FileFilter> = emptySet(),
        sortOption: FileSortOption = FileSortOption.NAME,
    ): List<FileTreeNode> {
        val newNodes = mutableListOf<FileTreeNode>()
        val now = Instant.now()
        val twentyFourHoursAgo = now.minus(RECENT_FILE_HOURS, ChronoUnit.HOURS)
        val errorCount = java.util.concurrent.atomic.AtomicInteger(0)

        Files.walkFileTree(
            root,
            object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (file.name.startsWith(".")) return FileVisitResult.CONTINUE
                    if (shouldShowFile(false, file, filters, twentyFourHoursAgo)) {
                        newNodes.add(FileTreeNode(file, 0, false, false))
                    }
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
                    logger.warn(exc) { "Failed to visit file or directory: $file" }
                    if (errorCount.incrementAndGet() >= AppConfig.MAX_PERMISSION_ERRORS) {
                        throw TooManyErrorsException("権限エラーが多発したため、走査を中断しました。", exc)
                    }
                    return FileVisitResult.CONTINUE
                }

                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (dir != root && dir.name.startsWith(".")) return FileVisitResult.SKIP_SUBTREE
                    return FileVisitResult.CONTINUE
                }
            },
        )

        return when (sortOption) {
            FileSortOption.NAME -> newNodes.sortedBy { it.name.lowercase() }
            FileSortOption.LAST_MODIFIED -> newNodes.sortedByDescending {
                try {
                    Files.readAttributes(it.path, BasicFileAttributes::class.java).lastModifiedTime().toInstant()
                } catch (e: IOException) {
                    logger.debug(e) { "Failed to read attributes for ${it.path}" }
                    Instant.MIN
                }
            }
        }
    }

    private fun isRecentFile(path: Path, since: Instant): Boolean = try {
        val attrs = Files.readAttributes(path, BasicFileAttributes::class.java)
        attrs.lastModifiedTime().toInstant().isAfter(since)
    } catch (e: IOException) {
        logger.debug(e) { "Failed to check if file is recent: $path" }
        false
    }

    /**
     * 指定されたノードの開閉状態を切り替え、新しいツリーリストを返します。
     */
    @Suppress("TooGenericExceptionCaught")
    fun toggleNode(node: FileTreeNode, currentNodes: List<FileTreeNode>, sortOption: FileSortOption = FileSortOption.NAME): List<FileTreeNode> {
        val index = if (node.isDirectory) currentNodes.indexOfFirst { it.path == node.path } else -1
        if (index == -1) return currentNodes

        val newNodes = currentNodes.toMutableList()
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
            val children = repository.scanDirectory(node.path, sortOption)
            val childNodes = children.filter { !it.name.startsWith(".") }.map {
                val isDir = try {
                    it.isDirectory()
                } catch (e: Exception) {
                    logger.debug(e) { "Failed to check if directory: $it" }
                    false
                }
                FileTreeNode(it, node.level + 1, isDir)
            }
            newNodes.addAll(index + 1, childNodes)
        }
        return newNodes
    }
}
