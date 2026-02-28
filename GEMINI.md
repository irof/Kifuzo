# Kifuzo - Shogi Game Record Librarian

Kifuzo is a macOS GUI application built with **Compose Multiplatform** and **Kotlin**. It is designed to manage, view, and convert Shogi game records (Kifu) from local files.

## Project Overview

- **Main Technology:** Kotlin 2.3.10, Compose Multiplatform 1.7.0.
- **Platform:** macOS (Desktop).
- **Purpose:** Provide a modern interface for navigating Shogi game record folders, viewing the final board state of a match, and converting between record formats.

## Key Features

- **File Browser:** A dual-pane interface with a file navigator on the left and a preview/board view on the right. Supports directory navigation (double-click to enter, `..` to go back).
    - Remembers the last opened root directory across application restarts. Defaults to "no directory selected" on first run.
    - **Vertical Menu Bar:** A dedicated sidebar on the far left for quick access to "Sidebar Toggle", "Import", and "Settings".
- **Board Display:** Renders a 9x9 Shogi board showing the board state of a match.
    - Supports piece rotation (opponent pieces face down).
    - Features a board flip button (refresh icon) at the top-right corner for swapping views.
    - Features a move list panel toggle in the header for showing/hiding the "Tejun" list.
    - Highlights regular pieces in black and promoted pieces in red.
    - **Koma-dai (Piece Stands):** Displays captured pieces on dedicated stands located at the top-left (Gote) and bottom-right (Sente) of the board, styled to match the board's aesthetic. Player names are positioned opposite to their respective piece stands.
    - **Move List Variations:** Supports viewing alternate move sequences ("Henka") from .kifu files. Branching points are marked with an icon in the move list, allowing users to switch between the main line and variations. Features a "Reset to Main" button to easily return to the primary game record.
- **Analysis Tools:** 
    - **Evaluation Graph:** Visualizes the game's evaluation values. Features non-linear scaling (compressing values > 2000) for better readability and automatic inversion when the board is flipped. Uses distinct background colors for Sente (red) and Gote (blue) advantage regions.
    - **Kifu Meta Info:** Displays game metadata such as tournament/event name and start time below the evaluation graph. Supports both `.kifu` (e.g., `棋戦：`, `開始日時：`) and `.csa` (e.g., `$EVENT:`, `$START_TIME:`) formats.
    - **Significant Moves:** Automatically detects moves with large evaluation changes (500+ points) and marks them in the move list with "!" or "!!".
            - **Kifu Parser:** A robust parser for `.kifu` (UTF-8) files.
            - Handles full-width digits and Kanji notation (e.g., `７六歩`, `同　`).
                - Supports nested variation sections (`変化`) by branching the game state at specified steps.
                - Skips comments and game results (e.g., `投了`, `切れ負け`) when searching for next moves.
            - **Mid-game Setup:** Supports initial captured pieces (`持駒：`) and board layout (`|...|`) for starting from arbitrary positions.
            - **CSA Support:** Correctly parses initial captured pieces (`P+`, `P-`) and mid-game board setups (`P1`-`P9`).
            
        - **Format Conversion:** Converts `.csa` files to standard `.kifu` (UTF-8) format with proper notation.        - Supports "nari" (promotion) detection by tracking board state.
        - Supports `PI`, `P+`, `P-`, and `P1`-`P9` board setup lines for accurate conversion of mid-game records.
    
- **Import Utility:** Imports Shogi Quest game records from a user-specified directory to the **current root directory**.
    - Detects `.txt` files containing CSA formatted records.
    - Automatically renames files to `{YYYYMMDD}-{Sente}-{Gote}.csa` based on game metadata and file timestamps.
    - Remembers the last used import source directory in application settings.
- **Clipboard Integration:** Buttons to copy kifu text or error logs for easy sharing.
    - **Pasted Kifu Support:** Allows pasting game records directly from the clipboard. Automatically detects KIF or CSA format, parses the content, and provides a "Save As" dialog with a proposed filename based on the metadata. Supports saving to the current root directory.

## Building and Running

The project uses **Gradle** as the build tool.

### Prerequisites
- **JDK:** 25 or higher (optimized for Kotlin 2.3.10).

### Key Commands

- **Run the application:**
  ```bash
  ./gradlew run
  ```
- **Create a macOS distribution (.dmg):**
  ```bash
  ./gradlew packageDmg
  ```
- **Create a distributable package:**
  ```bash
  ./gradlew createDistributable
  ```
- **Run verification (Spotless & Test):**
  ```bash
  ./gradlew verify
  ```
- **Run unit tests only:**
  ```bash
  ./gradlew desktopTest
  ```
- **Clean build artifacts:**
  ```bash
  ./gradlew clean
  ```

## Project Structure

- `src/desktopMain/kotlin/dev/irof/kifuzo/`: Root package.
    - `Main.kt`: Application entry point.
    - `logic/`: Business logic (parsers, converters, IO, repository).
        - Includes action handlers (`FileActionHandler`, `ImportHandler`, `SettingsHandler`) to offload logic from the ViewModel.
    - `models/`: Domain models (Piece, BoardState) and app configuration.
    - `ui/`: Compose UI components, themes, and dialogs.
    - `viewmodel/`: State management using the ViewModel pattern (UI State & Actions).
    - `utils/`: Common utilities (Strings, Clipboard).
- `src/desktopTest/kotlin/dev/irof/kifuzo/`: Unit tests for logic and models.
- `build.gradle.kts`: Project dependencies and Compose Multiplatform configuration.
- `gradle.properties`: Build environment settings.

## Development Conventions

- **Compliance and Self-Correction:** You MUST strictly adhere to all conventions defined in this file. If the user points out a failure to follow these conventions, you MUST investigate the cause and update `GEMINI.md` in the same task to include stricter, more explicit rules to prevent similar occurrences.
- **Task Management:** ALWAYS check `TODO.md` at the start of a session. Prioritize resolving existing technical debt and temporary workarounds listed in `TODO.md` whenever you perform related changes. Update `TODO.md` autonomously as tasks are completed or new issues are identified.
- **Language:**
    - **GEMINI.md:** Must be written in **English**.
    - **Commit Messages:** Must follow **Conventional Commits** using Japanese for the description and English for the type (e.g., `feat: 機能の追加`). **Every commit MUST include a body that explains the technical rationale (why) and the changes made (what). Additionally, the user's request that triggered the change MUST be included as a quotation.**
    - **Commit Trailer:** Every commit MUST use the `--trailer` flag to include `Generated-by: Gemini`. This ensures the trailer is properly separated from the commit body by a blank line (e.g., `git commit -m "feat: ..." --trailer "Generated-by: Gemini"`).
    - **Code Comments/Documentation:** Must be written in **Japanese**.
- **Workflow:** Commit changes autonomously after finishing each task (feature addition, bug fix, etc.) without waiting for explicit user confirmation. **Before committing, you MUST run `./gradlew verify` and ensure it passes. Additionally, if a change affects the content of `GEMINI.md` (e.g., new features, structural changes, or updated conventions), you MUST update `GEMINI.md` accordingly in the same task.**
- **Refactoring:** When refactoring, always measure the impact using tools like `detekt` (Complexity). Include the "before" and "after" metrics in the commit message to show the quantitative improvement. Ensure that `./gradlew verify` passes after any refactoring.
- **Bug Fixing:** You MUST empirically reproduce the reported failure with a new test case or reproduction script before applying a fix. Fulfill the user's request thoroughly, including adding tests when fixing bugs to prevent regressions.
- **UI Framework:** Jetpack Compose (Desktop).
- **State Management:** Uses a **ViewModel** pattern with `UiState` and `Action` objects for predictable state transitions.
    - Large handler methods in the ViewModel are split into smaller logical units (e.g., `handleFileSelectAction`, `handleFileEditAction`, `handleUiAction`) to manage complexity and comply with static analysis (Detekt).
- **Error Handling:** Do not swallow exceptions silently. Use `KifuParseException` to propagate errors (line numbers, content) to the UI. For bulk processing, record error states (e.g., `isError` in `KifuInfo`) to ensure visibility for the user.
- **Parsing:** Primarily uses Regex and line-by-line processing for game records.
- **Encoding:** Standardizes on **UTF-8** for all file operations.
- **Naming:** Uses standard Kotlin/JVM naming conventions.
- **Testing:** Test methods are named in Japanese without backticks for better readability. Function naming rules are globally disabled for `@Test` annotated methods in `build.gradle.kts`. Multi-line strings (`"""..."""`) are preferred for defining game records and board states within tests. **Always use `./gradlew desktopTest` to run unit tests. Do NOT use the generic `./gradlew test` task, as it is not applicable to this multiplatform project.**
