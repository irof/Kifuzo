package dev.irof.kfv.models

enum class PieceColor {
    Black, // 先手 (▲)
    White, // 後手 (△)
    ;

    fun toSymbol(): String = if (this == Black) "▲" else "△"
}

data class Square(val file: Int, val rank: Int) {
    // file: 1-9 (筋), rank: 1-9 (段)

    init {
        require(file in 1..9 && rank in 1..9) { "Invalid square: $file$rank" }
    }

    // 配列インデックスへの変換 (x: 0-8, y: 0-8)
    // 配列は [y][x] で、x=0 が 9筋, x=8 が 1筋 となっている（ShogiBoardViewの実装に合わせる）
    // 1筋(file=1) -> x=8, 9筋(file=9) -> x=0
    val xIndex: Int get() = 9 - file
    val yIndex: Int get() = rank - 1

    companion object {
        fun fromIndex(x: Int, y: Int): Square = Square(9 - x, y + 1)
    }
}
