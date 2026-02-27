package dev.irof.kifuzo.logic.parser
import dev.irof.kifuzo.logic.io.*
import dev.irof.kifuzo.logic.service.*
import dev.irof.kifuzo.models.Piece
import dev.irof.kifuzo.models.ShogiConstants
import dev.irof.kifuzo.models.Square

private val moveRegex = Regex("""^\s*(?<moveNum>\d+)\s+(?<toPos>[^\s(]{2}|同\s*)(?<pieceName>[^\s(]+)\((?<fromPos>[1-9]{2})\)\s*(?:\(\s*(?<timeMin>\d+)\s*:\s*(?<timeSec>\d+)\s*/.*?\))?.*""")
private val dropRegex = Regex("""^\s*(?<moveNum>\d+)\s+(?<toPos>[^\s(]{2})(?<pieceName>[^\s(]+?)打\s*(?:\(\s*(?<timeMin>\d+)\s*:\s*(?<timeSec>\d+)\s*/.*?\))?.*""")
private val resultRegex = Regex("""^\s*(\d+)\s+(${dev.irof.kifuzo.models.GameResult.ALL_KEYWORDS.joinToString("|")}).*""")

internal sealed class KifuParsedMove {
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

internal fun parseMove(line: String, lastTo: Square?): KifuParsedMove? {
    if (!Regex("""^\s*\d+\s+.*""").matches(line)) return null

    return parseNormalMove(line, lastTo)
        ?: parseDropMove(line)
        ?: parseResultMove(line)
}

private fun parseNormalMove(line: String, lastTo: Square?): KifuParsedMove.Move? {
    val match = moveRegex.find(line) ?: return null
    return match.groups["moveNum"]?.value?.toIntOrNull()?.let { moveNum ->
        val toPosStr = match.groups["toPos"]?.value?.trim() ?: ""
        val pieceName = match.groups["pieceName"]?.value?.trim() ?: ""
        val fromPosStr = match.groups["fromPos"]?.value ?: ""

        val toSquare = if (toPosStr.startsWith("同")) {
            lastTo ?: throw KifuParseException("同の移動先が不明です")
        } else {
            Square(decodeX(toPosStr[0]), decodeY(toPosStr[1]))
        }

        KifuParsedMove.Move(
            moveNum = moveNum,
            to = toSquare,
            from = Square(fromPosStr[0] - '0', fromPosStr[1] - '0'),
            isPromote = pieceName.contains("成") || pieceName in listOf("竜", "馬", "龍", "圭", "杏", "全"),
            consumptionSeconds = parseTime(match),
        )
    }
}

private fun parseDropMove(line: String): KifuParsedMove.Drop? {
    val match = dropRegex.find(line) ?: return null
    return match.groups["moveNum"]?.value?.toIntOrNull()?.let { moveNum ->
        val toPosStr = match.groups["toPos"]?.value ?: ""
        val pieceSym = match.groups["pieceName"]?.value?.substring(0, 1) ?: ""

        KifuParsedMove.Drop(
            moveNum = moveNum,
            to = Square(decodeX(toPosStr[0]), decodeY(toPosStr[1])),
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

private fun decodeX(c: Char): Int {
    val idx = "１２３４５６７８９123456789".indexOf(c)
    return if (idx == -1) -1 else (idx % ShogiConstants.BOARD_SIZE) + 1
}

private fun decodeY(c: Char): Int {
    val idx = "一二三四五六七八九１２３４５６７８９123456789".indexOf(c)
    return if (idx == -1) -1 else (idx % ShogiConstants.BOARD_SIZE) + 1
}
