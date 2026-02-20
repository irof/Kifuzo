package dev.irof.kfv.models

enum class Piece(val symbol: String) {
    FU("歩"),
    KY("香"),
    KE("桂"),
    GI("銀"),
    KI("金"),
    KA("角"),
    HI("飛"),
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
    fun isPromoted(): Boolean = this in listOf(TO, NY, NK, NG, UM, RY)

    companion object {
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
                val piece = entries.find {
                    it.symbol == pieceName ||
                        (pieceName == "王" && it == OU) ||
                        (pieceName == "玉" && it == OU) ||
                        (pieceName == "竜" && it == RY) ||
                        (pieceName == "龍" && it == RY) ||
                        (pieceName == "馬" && it == UM)
                }
                if (piece != null) repeat(count) { list.add(piece) }
            }
            return list
        }
    }
}
