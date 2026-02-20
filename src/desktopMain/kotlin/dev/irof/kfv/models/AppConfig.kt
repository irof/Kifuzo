package dev.irof.kfv.models

import java.io.File

object AppConfig {
    val USER_HOME: String = System.getProperty("user.home")
    val KIFU_ROOT = File(USER_HOME, "Kifu")
    val QUEST_CSA_DIR = File(KIFU_ROOT, "quest/csa")
}
