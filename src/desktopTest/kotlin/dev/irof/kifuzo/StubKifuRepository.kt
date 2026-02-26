package dev.irof.kifuzo

import dev.irof.kifuzo.logic.KifuRepository
import dev.irof.kifuzo.models.BoardSnapshot
import dev.irof.kifuzo.models.FileSortOption
import dev.irof.kifuzo.models.KifuInfo
import dev.irof.kifuzo.models.ShogiBoardState
import dev.irof.kifuzo.models.Square
import java.nio.file.Path
import kotlin.test.assertEquals

/**
 * テストで共通して使用するリポジトリのスタブ実装。
 */
open class StubKifuRepository : KifuRepository {
    var lastParsedPath: Path? = null
    var lastMethodCalled: String? = null
    var parseAction: (ShogiBoardState) -> Unit = {
        // No action by default
    }
    var renameResult: Path? = null
    var proposedName: String? = null
    var importResult: Int = 0

    override fun scanDirectory(directory: Path, sortOption: FileSortOption): List<Path> = emptyList()
    override fun getKifuInfos(files: List<Path>): Map<Path, KifuInfo> = emptyMap()
    override fun parse(path: Path, state: ShogiBoardState) {
        lastParsedPath = path
        lastMethodCalled = "parse"
        parseAction(state)
    }

    override fun parseManually(path: Path, state: ShogiBoardState) {
        lastParsedPath = path
        lastMethodCalled = "parseManually"
        parseAction(state)
    }
    override fun convertCsa(path: Path): Path = path
    override fun updateResult(path: Path, result: String) {
        // Stub implementation
    }
    override fun updateHeader(path: Path, event: String, startTime: String) {
        // Stub implementation
    }
    override fun generateProposedName(path: Path, template: String): String? = proposedName ?: path.fileName.toString()
    override fun renameFileTo(path: Path, newName: String): Path? = renameResult
    override fun renameKifuFile(path: Path, template: String): Path? = renameResult
    override fun importQuestFiles(sourceDir: Path, targetDir: Path): Int = importResult
}

/**
 * 盤面の特定の座標にある駒を検証します。
 */
fun BoardSnapshot.assertAt(file: Int, rank: Int, expected: dev.irof.kifuzo.models.BoardPiece?) {
    val square = Square(file, rank)
    val actual = cells[square.yIndex][square.xIndex]
    assertEquals(expected, actual, "At $file$rank: expected $expected but was $actual")
}
