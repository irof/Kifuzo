package dev.irof.kifuzo.logic.service
import dev.irof.kifuzo.logic.io.readLinesWithEncoding
import dev.irof.kifuzo.logic.io.readTextWithEncoding
import dev.irof.kifuzo.logic.parser.HeaderParser
import dev.irof.kifuzo.logic.parser.KifuParseException
import dev.irof.kifuzo.logic.parser.convertCsaToKifu
import dev.irof.kifuzo.logic.parser.parseCsa
import dev.irof.kifuzo.logic.parser.parseHeader
import dev.irof.kifuzo.logic.parser.parseKifu
import dev.irof.kifuzo.logic.parser.scanKifuInfo
import dev.irof.kifuzo.models.FileSortOption
import dev.irof.kifuzo.models.KifuInfo
import dev.irof.kifuzo.models.ShogiBoardState
import java.nio.file.Path

/**
 * 棋譜に関する全ての操作を提供する統合リポジトリ。
 * 内部的には個別のサービス（FileService, ParseService, ImportService）に処理を委譲します。
 */
interface KifuRepository {
    fun scanDirectory(directory: Path, sortOption: FileSortOption = FileSortOption.NAME): List<Path>
    fun getKifuInfos(files: List<Path>): Map<Path, KifuInfo>
    fun parse(path: Path, state: ShogiBoardState)
    fun parseManually(path: Path, state: ShogiBoardState)
    fun convertCsa(path: Path): Path
    fun updateResult(path: Path, result: String)
    fun updateHeader(path: Path, event: String, startTime: String)
    fun generateProposedName(path: Path, template: String): String?
    fun renameFileTo(path: Path, newName: String): Path?
    fun renameKifuFile(path: Path, template: String): Path?
    fun importQuestFiles(sourceDir: Path, targetDir: Path): Int
}

class KifuRepositoryImpl(
    private val fileService: KifuFileService = KifuFileServiceImpl(),
    private val parseService: KifuParseService = KifuParseServiceImpl(),
    private val importService: KifuImportService = KifuImportServiceImpl(),
) : KifuRepository {

    override fun scanDirectory(directory: Path, sortOption: FileSortOption): List<Path> = fileService.scanDirectory(directory, sortOption)

    override fun getKifuInfos(files: List<Path>): Map<Path, KifuInfo> = parseService.getKifuInfos(files)

    override fun parse(path: Path, state: ShogiBoardState) = parseService.parse(path, state)

    override fun parseManually(path: Path, state: ShogiBoardState) = parseService.parseManually(path, state)

    override fun convertCsa(path: Path): Path = parseService.convertCsaToKifu(path)

    override fun updateResult(path: Path, result: String) = fileService.updateResult(path, result)

    override fun updateHeader(path: Path, event: String, startTime: String) = fileService.updateHeader(path, event, startTime)

    override fun generateProposedName(path: Path, template: String): String? = fileService.generateProposedName(path, template)

    override fun renameFileTo(path: Path, newName: String): Path? = fileService.renameFile(path, newName)

    override fun renameKifuFile(path: Path, template: String): Path? {
        val proposedName = generateProposedName(path, template) ?: return null
        return renameFileTo(path, proposedName)
    }

    override fun importQuestFiles(sourceDir: Path, targetDir: Path): Int = importService.importQuestFiles(sourceDir, targetDir)
}
