@file:Suppress("MagicNumber")

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

    companion object
}
