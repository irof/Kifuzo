package dev.irof.kifuzo.logic.service

import dev.irof.kifuzo.models.FileSortOption
import dev.irof.kifuzo.models.KifuInfo
import dev.irof.kifuzo.models.ShogiBoardState
import java.nio.file.Path

/**
 * 棋譜の検索・情報取得に関連する操作。
 */
interface KifuSearchRepository {
    fun scanDirectory(directory: Path, sortOption: FileSortOption = FileSortOption.NAME): List<Path>
}

/**
 * 棋譜のパース・解析に関連する操作。
 */
interface KifuParseRepository {
    fun parse(path: Path, state: ShogiBoardState)
    fun parseManually(path: Path, state: ShogiBoardState)
    fun parseManually(lines: List<String>, state: ShogiBoardState)
    fun scanKifuInfo(path: Path): KifuInfo
    fun scanKifuInfo(lines: List<String>): KifuInfo
}

/**
 * 棋譜の更新・変換に関連する操作。
 */
interface KifuUpdateRepository {
    fun convertCsa(path: Path): Path
    fun updateMetadata(path: Path, event: String? = null, startTime: String? = null, result: String? = null)
}

/**
 * ファイル名の生成や操作に関連する操作。
 */
interface KifuFileOperationRepository {
    fun generateProposedName(path: Path, info: KifuInfo, template: String): String?
    fun generateProposedNameForPasted(info: KifuInfo, template: String): String?
    fun renameFileTo(path: Path, newName: String): Path?
}

/**
 * 棋譜に関する全ての操作を提供する統合リポジトリ。
 */
interface KifuRepository :
    KifuSearchRepository,
    KifuParseRepository,
    KifuUpdateRepository,
    KifuFileOperationRepository

/**
 * KifuRepository の標準実装。
 */
class KifuRepositoryImpl(
    private val fileService: KifuFileService = KifuFileServiceImpl(),
    private val parseService: KifuParseService = KifuParseServiceImpl(),
) : KifuRepository {

    override fun scanDirectory(directory: Path, sortOption: FileSortOption): List<Path> = fileService.scanDirectory(directory, sortOption)

    override fun parse(path: Path, state: ShogiBoardState) = parseService.parse(path, state)

    override fun parseManually(path: Path, state: ShogiBoardState) = parseService.parseManually(path, state)

    override fun parseManually(lines: List<String>, state: ShogiBoardState) = parseService.parseManually(lines, state)

    override fun scanKifuInfo(path: Path): KifuInfo = parseService.scanInfo(path)

    override fun scanKifuInfo(lines: List<String>): KifuInfo = parseService.scanInfo(lines)

    override fun convertCsa(path: Path): Path = parseService.convertCsaToKifu(path)

    override fun updateMetadata(path: Path, event: String?, startTime: String?, result: String?) {
        event?.let { startTime?.let { st -> fileService.updateHeader(path, it, st) } }
        result?.let { fileService.updateResult(path, it) }
    }

    override fun generateProposedName(path: Path, info: KifuInfo, template: String): String? = fileService.generateProposedName(path, info, template)

    override fun generateProposedNameForPasted(info: KifuInfo, template: String): String? = fileService.generateProposedNameForPasted(info, template)

    override fun renameFileTo(path: Path, newName: String): Path? = fileService.renameFile(path, newName)
}
