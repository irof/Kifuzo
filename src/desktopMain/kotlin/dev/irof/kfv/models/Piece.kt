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
}
