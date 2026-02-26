package dev.irof.kifuzo

import dev.irof.kifuzo.logic.KifuRepository
import dev.irof.kifuzo.models.BoardPiece
import dev.irof.kifuzo.models.BoardSnapshot
import dev.irof.kifuzo.models.Evaluation
import dev.irof.kifuzo.models.FileSortOption
import dev.irof.kifuzo.models.KifuInfo
import dev.irof.kifuzo.models.KifuSession
import dev.irof.kifuzo.models.Move
import dev.irof.kifuzo.models.Piece
import dev.irof.kifuzo.models.PieceColor
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
    var parseAction: (ShogiBoardState) -> Unit = {}
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
    override fun updateResult(path: Path, result: String) { /* Stub */ }
    override fun updateHeader(path: Path, event: String, startTime: String) { /* Stub */ }
    override fun generateProposedName(path: Path, template: String): String? = proposedName ?: path.fileName.toString()
    override fun renameFileTo(path: Path, newName: String): Path? = renameResult
    override fun renameKifuFile(path: Path, template: String): Path? = renameResult
    override fun importQuestFiles(sourceDir: Path, targetDir: Path): Int = importResult
}

/**
 * 盤面の特定の座標にある駒を検証します。
 */
fun BoardSnapshot.assertAt(file: Int, rank: Int, piece: Piece, color: PieceColor) {
    val actual = at(file, rank)
    val expected = BoardPiece(piece, color)
    assertEquals(expected, actual, "At $file$rank: expected $expected but was $actual")
}

/**
 * 指定された座標が空であることを検証します。
 */
fun BoardSnapshot.assertEmptyAt(file: Int, rank: Int) {
    val actual = at(file, rank)
    assertEquals(null, actual, "At $file$rank: expected empty but was $actual")
}

/**
 * 持駒の数を検証します。
 */
fun BoardSnapshot.assertMochigomaCount(color: PieceColor, piece: Piece, expectedCount: Int) {
    val mochi = if (color == PieceColor.Black) senteMochigoma else goteMochigoma
    val actualCount = mochi.count { it == piece }
    assertEquals(expectedCount, actualCount, "$color's $piece count: expected $expectedCount but was $actualCount")
}

/**
 * KifuSession の総手数を検証します。
 */
fun KifuSession.assertMaxStep(expected: Int) {
    assertEquals(expected, maxStep, "Max step: expected $expected but was $maxStep")
}

/**
 * Move の情報を検証します。
 */
fun Move.assertMove(expectedText: String, expectedEval: Evaluation = Evaluation.Unknown) {
    assertEquals(expectedText, moveText, "Move text mismatch at step $step")
    if (expectedEval != Evaluation.Unknown) {
        assertEquals(expectedEval, evaluation, "Evaluation mismatch at step $step")
    }
}
