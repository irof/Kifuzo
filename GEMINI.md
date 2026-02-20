# KifuManager - Shogi Game Record Manager

KifuManager is a macOS GUI application built with **Compose Multiplatform** and **Kotlin**. It is designed to manage, view, and convert Shogi game records (Kifu) from local files.

## Project Overview

- **Main Technology:** Kotlin 2.3.10, Compose Multiplatform 1.7.0.
- **Platform:** macOS (Desktop).
- **Purpose:** Provide a modern interface for navigating Shogi game record folders, viewing the final board state of a match, and converting between record formats.

## Key Features

- **File Browser:** A dual-pane interface with a file navigator on the left and a preview/board view on the right. Supports directory navigation (double-click to enter, `..` to go back).
- **Board Display:** Renders a 9x9 Shogi board showing the **final state** (endgame) of a `.kifu` record.
    - Supports piece rotation (opponent pieces face down).
    - Highlights sente (black) and gote (red) pieces.
- **Kifu Parser:** A robust parser for `.kifu` (UTF-8) files.
    - Handles full-width digits and Kanji notation (e.g., `７六歩`, `同　`).
    - Skips comments, branch/variation sections (`変化`), and game results (e.g., `投了`, `切れ負け`).
- **Senkei Detection:** Automatically detects opening patterns (Senkei) like "Yagura", "Gokigen Nakabisha", etc.
- **Format Conversion:** Converts `.csa` files to standard `.kifu` (UTF-8) format with proper notation.
- **Import Utility:** Imports Shogi Quest game records from a user-specified directory to the **current root directory**.
    - Detects `.txt` files containing CSA formatted records.
    - Automatically renames files to `{YYYYMMDD}-{Sente}-{Gote}.csa` based on game metadata and file timestamps.
    - Remembers the last used import source directory in application settings.
- **Clipboard Integration:** Buttons to copy kifu text or error logs for easy sharing.

## Building and Running

The project uses **Gradle** as the build tool.

### Prerequisites
- **JDK:** 21 or higher (optimized for JDK 25 with Kotlin 2.3.10).

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
- **Run tests:**
  ```bash
  ./gradlew test
  ```
- **Clean build artifacts:**
  ```bash
  ./gradlew clean
  ```

## Project Structure

- `src/desktopMain/kotlin/dev/irof/kfv/`: Root package.
    - `Main.kt`: Application entry point.
    - `logic/`: Business logic (parsers, converters, IO, repository, senkei detection).
    - `models/`: Domain models (Piece, BoardState) and app configuration.
    - `ui/`: Compose UI components, themes, and dialogs.
    - `viewmodel/`: State management using the ViewModel pattern (UI State & Actions).
    - `utils/`: Common utilities (Strings, Clipboard).
- `src/desktopTest/kotlin/dev/irof/kfv/`: Unit tests for logic and models.
- `build.gradle.kts`: Project dependencies and Compose Multiplatform configuration.
- `gradle.properties`: Build environment settings.

## Development Conventions

- **Language:** Comments and documentation in the code must be written in **Japanese**.
- **Workflow:** Commit changes autonomously after finishing each task (feature addition, bug fix, etc.) without waiting for explicit user confirmation. **Before committing, you MUST run `./gradlew verify` and ensure it passes. Additionally, if a change affects the content of `GEMINI.md` (e.g., new features, structural changes, or updated conventions), you MUST update `GEMINI.md` accordingly in the same task.**
- **UI Framework:** Jetpack Compose (Desktop).
- **State Management:** Uses a **ViewModel** pattern with `UiState` and `Action` objects for predictable state transitions.
- **Parsing:** Primarily uses Regex and line-by-line processing for game records.
- **Encoding:** Standardizes on **UTF-8** for all file operations.
- **Naming:** Uses standard Kotlin/JVM naming conventions.
