package dev.irof.kifuzo.models

enum class Piece(val symbol: String, val mochigomaOrder: Int = 99) {
    FU("歩", 6),
    KY("香", 5),
    KE("桂", 4),
    GI("銀", 3),
    KI("金", 2),
    KA("角", 1),
    HI("飛", 0),
    OU("玉"),
    TO("と"),
    NY("杏"),
    NK("圭"),
    NG("全"),
    UM("馬"),
    RY("龍"),
    ;

    fun toBase(): Piece = when (this) {
        TO -> FU
        NY -> KY
        NK -> KE
        NG -> GI
        UM -> KA
        RY -> HI
        else -> this
    }

    fun promote(): Piece = when (this) {
        FU -> TO
        KY -> NY
        KE -> NK
        GI -> NG
        KA -> UM
        HI -> RY
        else -> this
    }

    fun isPromoted(): Boolean = this in listOf(TO, NY, NK, NG, UM, RY)

    companion object {
        /**
         * 一文字の駒名（例: "歩", "王", "竜"）を解析して Piece を返します。
         */
        fun findPieceBySymbol(symbol: String): Piece? {
            val s = symbol.trim()
            if (s.isEmpty()) return null
            return entries.find {
                it.symbol == s ||
                    (s == "王" && it == OU) ||
                    (s == "玉" && it == OU) ||
                    (s == "竜" && it == RY) ||
                    (s == "龍" && it == RY) ||
                    (s == "馬" && it == UM)
            }
        }

        /**
         * 持ち駒文字列（例: "飛二 角 銀三"）を解析して Piece のリストを返します。
         */
        fun parseMochigoma(text: String): List<Piece> {
            val t = text.trim()
            if (t == "なし" || t.isEmpty()) return emptyList()
            val list = mutableListOf<Piece>()
            val kanjiDigits = "一二三四五六七八九"
            t.split(Regex("""[\s　]+""")).forEach { part ->
                if (part.isEmpty()) return@forEach
                val pieceName = part.substring(0, 1)
                val countStr = part.substring(1)
                val count = if (countStr.isEmpty()) {
                    1
                } else {
                    val idx = kanjiDigits.indexOf(countStr)
                    if (idx != -1) idx + 1 else countStr.toIntOrNull() ?: 1
                }
                val piece = findPieceBySymbol(pieceName)
                if (piece != null) repeat(count) { list.add(piece) }
            }
            return list
        }
    }
}
