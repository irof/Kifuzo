package dev.irof.kfv.models

import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.div

object AppConfig {
    val USER_HOME: String = System.getProperty("user.home")
    val USER_HOME_PATH: Path = Path(USER_HOME)
    val KIFU_ROOT: Path = USER_HOME_PATH / "Kifu"
}
