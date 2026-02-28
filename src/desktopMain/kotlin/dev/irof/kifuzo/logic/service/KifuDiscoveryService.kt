package dev.irof.kifuzo.logic.service

import dev.irof.kifuzo.models.FileFilter
import dev.irof.kifuzo.models.FileSortOption
import dev.irof.kifuzo.models.FileTreeNode
import dev.irof.kifuzo.models.FileViewMode
import dev.irof.kifuzo.models.KifuInfo
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

private val logger = KotlinLogging.logger {}

/**
 * 棋譜ファイルのスキャンや一覧構築に関連する責務を持つサービス。
 */
class KifuDiscoveryService(
    private val repository: KifuRepository,
    private val fileTreeManager: FileTreeManager,
) {
    /**
     * 現在のモードに応じたファイルリストを構築します。
     */
    fun buildFileList(
        root: Path,
        mode: FileViewMode,
        currentNodes: List<FileTreeNode>,
        filters: Set<FileFilter>,
        sortOption: FileSortOption,
    ): List<FileTreeNode> = if (mode == FileViewMode.HIERARCHY) {
        fileTreeManager.buildTree(root, currentNodes, filters, sortOption)
    } else {
        fileTreeManager.buildFlatList(root, filters, sortOption)
    }

    /**
     * 表示中のディレクトリから棋譜情報をスキャンします。
     */
    fun scanVisibleKifuInfos(
        root: Path,
        viewMode: FileViewMode,
        treeNodes: List<FileTreeNode>,
    ): Map<Path, KifuInfo> {
        val targetDirs = getScanTargetDirs(root, viewMode, treeNodes)
        val allKifuFiles = mutableListOf<Path>()

        targetDirs.forEach { dir ->
            scanDirectoryForKifuFiles(dir, allKifuFiles)
        }

        return repository.getKifuInfos(allKifuFiles)
    }

    private fun getScanTargetDirs(root: Path, viewMode: FileViewMode, treeNodes: List<FileTreeNode>): List<Path> = if (viewMode == FileViewMode.HIERARCHY) {
        listOf(root) + treeNodes.filter { it.isDirectory && it.isExpanded }.map { it.path }
    } else {
        listOf(root)
    }

    private fun scanDirectoryForKifuFiles(dir: Path, result: MutableList<Path>) {
        try {
            Files.list(dir).use { stream ->
                stream.filter { isKifuFile(it) }.forEach { result.add(it) }
            }
        } catch (e: IOException) {
            logger.warn(e) { "Failed to list directory: $dir" }
        } catch (e: SecurityException) {
            logger.warn(e) { "Permission denied listing directory: $dir" }
        }
    }

    private fun isKifuFile(path: Path): Boolean = try {
        !path.name.startsWith(".") && path.isRegularFile() && (path.extension.lowercase() in listOf("kifu", "kif", "csa"))
    } catch (e: IOException) {
        logger.debug(e) { "Failed to check if kifu file (IO): $path" }
        false
    } catch (e: SecurityException) {
        logger.debug(e) { "Failed to check if kifu file (Security): $path" }
        false
    }
}
