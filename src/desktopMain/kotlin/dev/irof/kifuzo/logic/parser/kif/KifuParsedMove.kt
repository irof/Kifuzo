package dev.irof.kifuzo.logic.parser.kif
import dev.irof.kifuzo.logic.io.readLinesWithEncoding
import dev.irof.kifuzo.logic.io.readTextWithEncoding
import dev.irof.kifuzo.logic.parser.HeaderParser
import dev.irof.kifuzo.logic.parser.KifuHeader
import dev.irof.kifuzo.logic.parser.KifuParseException
import dev.irof.kifuzo.logic.parser.parseHeader
import dev.irof.kifuzo.logic.service.FileTreeManager
import dev.irof.kifuzo.logic.service.KifuRepository
import dev.irof.kifuzo.logic.service.KifuRepositoryImpl
import dev.irof.kifuzo.logic.service.KifuSessionBuilder
import dev.irof.kifuzo.models.Piece
import dev.irof.kifuzo.models.ShogiConstants
import dev.irof.kifuzo.models.Square

private val moveRegex = Regex("""^\s*(?<moveNum>\d+)\s+(?<toPos>[^\s(]{2}|同\s*)(?<pieceName>[^\s(]+)\((?<fromPos>[1-9]{2})\)\s*(?:\(\s*(?<timeMin>\d+)\s*:\s*(?<timeSec>\d+)\s*/.*?\))?.*""")
private val dropRegex = Regex("""^\s*(?<moveNum>\d+)\s+(?<toPos>[^\s(]{2})(?<pieceName>[^\s(]+?)打\s*(?:\(\s*(?<timeMin>\d+)\s*:\s*(?<timeSec>\d+)\s*/.*?\))?.*""")
private val resultRegex = Regex("""^\s*(\d+)\s+(${dev.irof.kifuzo.models.GameResult.ALL_KEYWORDS.joinToString("|")}).*""")

private const val KANJI_NUMS = "一二三四五六七八九"
private const val ZEN_NUMS = "１２３４５６７８９"
private const val HALF_NUMS = "123456789"

sealed class KifuParsedMove {
    abstract val moveNum: Int
    abstract val consumptionSeconds: Int?

    data class Move(
        override val moveNum: Int,
        val to: Square,
        val from: Square,
        val isPromote: Boolean,
        override val consumptionSeconds: Int? = null,
    ) : KifuParsedMove()

    data class Drop(
        override val moveNum: Int,
        val to: Square,
        val piece: Piece,
        override val consumptionSeconds: Int? = null,
    ) : KifuParsedMove()

    data class Result(
        override val moveNum: Int,
        val result: String,
    ) : KifuParsedMove() {
        override val consumptionSeconds: Int? = null
    }
}

fun parseMove(line: String, lastTo: Square?): KifuParsedMove? {
    if (!Regex("""^\s*\d+\s+.*""").matches(line)) return null

    return parseNormalMove(line, lastTo)
        ?: parseDropMove(line)
        ?: parseResultMove(line)
}

private fun parseNormalMove(line: String, lastTo: Square?): KifuParsedMove.Move? {
    val match = moveRegex.find(line) ?: return null
    val groups = match.groups
    val moveNum = groups["moveNum"]?.value?.toIntOrNull()
    val toPosStr = groups["toPos"]?.value?.trim() ?: ""
    val pieceName = groups["pieceName"]?.value?.trim() ?: ""
    val fromPosStr = groups["fromPos"]?.value ?: ""

    return moveNum?.let {
        KifuParsedMove.Move(
            moveNum = it,
            to = getToSquare(toPosStr, lastTo),
            from = getFromSquare(fromPosStr),
            isPromote = pieceName.contains("成") || pieceName in listOf("竜", "馬", "龍", "圭", "杏", "全"),
            consumptionSeconds = parseTime(match),
        )
    }
}

private fun validateToPosStr(toPosStr: String) {
    if (toPosStr.length < 2) {
        throw KifuParseException("指し手の座標が読み取れません: $toPosStr")
    }
}

private fun getToSquare(toPosStr: String, lastTo: Square?): Square {
    if (toPosStr.startsWith("同")) {
        return lastTo ?: throw KifuParseException("同の移動先が不明です")
    }
    validateToPosStr(toPosStr)
    val file = decodeX(toPosStr[0])
    val rank = decodeY(toPosStr[1])
    if (file == -1 || rank == -1) throw KifuParseException("不正な座標です: $toPosStr")
    return Square(file, rank)
}

private fun getFromSquare(fromPosStr: String): Square {
    if (fromPosStr.length < 2) {
        throw KifuParseException("移動元座標が読み取れません: $fromPosStr")
    }
    val file = fromPosStr[0] - '0'
    val rank = fromPosStr[1] - '0'
    if (file !in 1..9 || rank !in 1..9) throw KifuParseException("不正な移動元座標です: $fromPosStr")
    return Square(file, rank)
}

private fun parseDropMove(line: String): KifuParsedMove.Drop? {
    val match = dropRegex.find(line) ?: return null
    val groups = match.groups
    val moveNum = groups["moveNum"]?.value?.toIntOrNull()
    val toPosStr = groups["toPos"]?.value ?: ""
    val pieceSym = groups["pieceName"]?.value?.substring(0, 1) ?: ""

    return moveNum?.let {
        val file = decodeX(toPosStr[0])
        val rank = decodeY(toPosStr[1])
        if (file == -1 || rank == -1) throw KifuParseException("不正な座標です: $toPosStr")

        KifuParsedMove.Drop(
            moveNum = it,
            to = Square(file, rank),
            piece = findPieceForDrop(pieceSym),
            consumptionSeconds = parseTime(match),
        )
    }
}

private fun findPieceForDrop(symbol: String): Piece = Piece.entries.find {
    it.symbol == symbol ||
        (symbol == "王" && it == Piece.OU) ||
        (symbol == "玉" && it == Piece.OU) ||
        (symbol == "竜" && it == Piece.RY) ||
        (symbol == "龍" && it == Piece.RY) ||
        (symbol == "馬" && it == Piece.UM)
} ?: throw KifuParseException("不明な駒種: $symbol")

private fun parseTime(match: MatchResult): Int? {
    val mins = match.groups["timeMin"]?.value?.trim()?.toIntOrNull()
    val secs = match.groups["timeSec"]?.value?.trim()?.toIntOrNull()

    return if (mins != null && secs != null) {
        mins * ShogiConstants.SECONDS_IN_MINUTE + secs
    } else {
        null
    }
}

private fun parseResultMove(line: String): KifuParsedMove.Result? {
    val match = resultRegex.find(line) ?: return null
    val moveNum = match.groupValues[1].toInt()
    val result = match.groupValues[2]
    return KifuParsedMove.Result(moveNum, result)
}

private fun decodeX(c: Char): Int = when {
    ZEN_NUMS.indexOf(c) != -1 -> ZEN_NUMS.indexOf(c) + 1
    HALF_NUMS.indexOf(c) != -1 -> HALF_NUMS.indexOf(c) + 1
    else -> -1
}

private fun decodeY(c: Char): Int = when {
    KANJI_NUMS.indexOf(c) != -1 -> KANJI_NUMS.indexOf(c) + 1
    ZEN_NUMS.indexOf(c) != -1 -> ZEN_NUMS.indexOf(c) + 1
    HALF_NUMS.indexOf(c) != -1 -> HALF_NUMS.indexOf(c) + 1
    else -> -1
}
