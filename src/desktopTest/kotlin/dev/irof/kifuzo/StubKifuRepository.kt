package dev.irof.kifuzo

import dev.irof.kifuzo.logic.service.FileTreeManager
import dev.irof.kifuzo.logic.service.KifuRepository
import dev.irof.kifuzo.logic.service.KifuRepositoryImpl
import dev.irof.kifuzo.logic.service.KifuSessionBuilder
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
    var convertCsaAction: () -> Unit = {}
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

    override fun parseManually(lines: List<String>, state: ShogiBoardState) {
        lastMethodCalled = "parseManuallyLines"
        parseAction(state)
    }

    override fun scanKifuInfo(lines: List<String>): KifuInfo = KifuInfo(java.nio.file.Paths.get("stub"))

    override fun convertCsa(path: Path): Path {
        convertCsaAction()
        return path
    }
    override fun updateResult(path: Path, result: String) { /* Stub */ }
    override fun updateHeader(path: Path, event: String, startTime: String) { /* Stub */ }
    override fun generateProposedName(path: Path, info: KifuInfo, template: String): String? = proposedName ?: path.fileName.toString()
    override fun generateProposedNameForPasted(info: KifuInfo, template: String): String? = proposedName
    override fun renameFileTo(path: Path, newName: String): Path? = renameResult
    override fun importQuestFiles(sourceDir: Path, targetDir: Path): Int = importResult
}

/**
 * テスト用のメモリ内設定保持クラス。
 */
class InMemoryAppSettings : dev.irof.kifuzo.models.AppSettings {
    override var myNameRegex: String = ""
    override var windowX: Float? = null
    override var windowY: Float? = null
    override var windowWidth: Float = 800f
    override var windowHeight: Float = 750f
    override var sidebarWidth: Float = 250f
    override var importSourceDir: String = ""
    override var lastRootDir: String = ""
    override var filenameTemplate: String = "{開始日の年月日}_{開始日の時分秒}_{棋戦名}_{先手}_{後手}"
    override var fileSortOption: dev.irof.kifuzo.models.FileSortOption = dev.irof.kifuzo.models.FileSortOption.NAME

    override fun saveWindowState(windowState: androidx.compose.ui.window.WindowState) { /* Stub */ }
    override fun getAllSettings(): Map<String, String> = emptyMap()
    override fun removeSetting(key: String) { /* Stub */ }
    override fun putSetting(key: String, value: String) { /* Stub */ }
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
