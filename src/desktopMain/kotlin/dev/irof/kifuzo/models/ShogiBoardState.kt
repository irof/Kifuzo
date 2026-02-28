package dev.irof.kifuzo.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 盤面の表示状態を管理するクラス。
 * 複数の関連する状態を一括で更新することで、不整合の発生を防ぎます。
 */
class ShogiBoardState {
    private data class State(
        val session: KifuSession,
        val currentInitialSnapshot: BoardSnapshot,
        val currentMoves: List<Move>,
        val currentStep: Int,
    )

    private var state by mutableStateOf(
        KifuSession().let {
            State(it, it.initialSnapshot, it.moves, it.initialStep)
        },
    )

    // 外部公開用プロパティ（常に現在の state から取得）
    val session: KifuSession get() = state.session
    val currentInitialSnapshot: BoardSnapshot get() = state.currentInitialSnapshot
    val currentMoves: List<Move> get() = state.currentMoves

    var currentStep: Int
        get() = state.currentStep
        set(value) {
            state = state.copy(currentStep = value.coerceIn(0, state.currentMoves.size))
        }

    val currentHistory: List<BoardSnapshot>
        get() = listOf(state.currentInitialSnapshot) + state.currentMoves.map { it.resultSnapshot }

    val currentBoard: BoardSnapshot
        get() = if (state.currentStep == 0) {
            state.currentInitialSnapshot
        } else {
            state.currentMoves[state.currentStep - 1].resultSnapshot
        }

    /**
     * 新しい対局データをセットします。
     */
    fun updateSession(newSession: KifuSession) {
        state = State(
            session = newSession,
            currentInitialSnapshot = newSession.initialSnapshot,
            currentMoves = newSession.moves,
            currentStep = newSession.initialStep,
        )
    }

    /**
     * 手順を切り替えます（変化手順の選択など）。
     */
    fun switchHistory(newMoves: List<Move>) {
        if (newMoves.isEmpty()) return
        state = state.copy(
            currentInitialSnapshot = newMoves[0].resultSnapshot, // 分岐元
            currentMoves = newMoves.drop(1),
            currentStep = 0,
        )
    }

    /**
     * 本譜に戻ります。
     */
    fun resetToMainHistory() {
        state = state.copy(
            currentInitialSnapshot = state.session.initialSnapshot,
            currentMoves = state.session.moves,
            currentStep = state.session.coerceStep(state.currentStep),
        )
    }

    /**
     * すべての状態を初期化します。
     */
    fun clear() {
        updateSession(KifuSession())
    }
}
