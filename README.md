# PlantUML GUI

A dependency-free Java Swing desktop app for PlantUML editing, live preview and export, plus ArchiMate Model Exchange XML generation.

It renders diagrams by invoking a bundled PlantUML JAR as a subprocess, so the only runtime requirement is a Java installation.

---

## Repository layout

| Path | Purpose |
|------|---------|
| `plantuml-gui-java/` | The Java Swing application (source, resources, bundled PlantUML JAR) |
| `plantuml-gui-js/` | Placeholder for the future browser port |
| `java_config.ini` | **Active** configuration, shared by the launchers and the app |
| `launcher_java_unix.sh` | Menu-driven launcher for Linux / macOS |
| `launcher_java_windows.bat` | Menu-driven launcher for Windows |
| `AI-WORKFLOW.md` | Mandatory rules for any AI or human contributing code |

Application packages, all under `com.diosaraiva.plantumlgui`:

| Package | Responsibility |
|---------|----------------|
| `util` | Cross-cutting helpers: `ResourceLocator`, `FileNames`, `I18n`, `SwingUtils`, `JarUtils`, `Background`, `TextLineNumber` |
| `service` | Headless logic: `AppSettings`, `PlantUmlFormat`, `PlantUmlRenderer`, `SampleLoader`, `ArchimatePlantUmlConverter`, `ArchimateExchangeModel` |
| `ui` | Swing shell: `MainFrame`, `MenuBar`, `AboutDialog` |
| `ui.plantuml` | The working area: `PlantUmlPanel` plus its input, output and footer panels |

The dependency direction is strictly `ui` → `service` → `util`.

---

## Features

- **Diagram editor** with a monospaced code area, line numbers, and full undo/redo history.
- **Sample gallery** — ready-to-use PlantUML samples (Activity, Class, Sequence, State, C4, ArchiMate, Gantt, Mindmap, WBS, JSON/YAML and more) that load straight into the editor.
- **Live preview** that re-renders as you type, with a manual *Preview* button and a character/line counter.
- **Zoomable image preview** — zoom in/out, fit-to-window, reset to 100%, and `Ctrl`/`⌘` + mouse-wheel zoom.
- **Integrated console** showing PlantUML compilation output.
- **Export** to PNG, SVG, PUML or **ArchiMate Model Exchange XML**.
- **Copy to clipboard** for both the rendered image and the diagram code.
- **Configurable PlantUML JAR** — point the app at any PlantUML JAR from the Config tab.
- **Look and feel options** — theme, UI font and window resolution presets.
- **Internationalized UI** — English (US), Português (BR), Español (ES).

---

## Getting started

### Requirements

- **Java 21 or newer** on your `PATH`. The app uses virtual threads, records and pattern matching, and it shells out to `java` to run the PlantUML JAR.

### Run

From the repository root:

```bash
./launcher_java_unix.sh          # Linux / macOS
launcher_java_windows.bat        # Windows
```

Both present the same menu:

1. **Run without compiling** — source-code launcher, no `.class` files (needs Java 22+ for this multi-class project).
2. **Compile (if needed) and run** — skips `javac` when the compiled classes are up to date.
3. **Clean build artifacts + reset config** — after confirmation, removes the directories listed in `launcher.cleanDirs` plus any stray `.class` files, then restores `java_config.ini` to the bundled defaults.
4. **Restore default configuration** — after confirmation, overwrites `java_config.ini` with the bundled default. Same effect as the **Default** button in the app's Config tab.
5. **Exit**.

Options 3 and 4 reload the launcher settings immediately, so the menu reflects the restored values without restarting the script.

You can also run `com.diosaraiva.plantumlgui.Main` directly with `plantuml-gui-java/` as the working directory, so samples, translations and the bundled JAR resolve.

---

## Configuration (`java_config.ini`)

One INI file is shared by the shell script, the batch script and the app.

- **Active copy** — `java_config.ini` next to the launchers. Read at start-up, written only by **Save**.
- **Factory default** — `plantuml-gui-java/src/main/resources/java_config.ini`.
- Three ways to reset the active copy to that default, all producing a byte-identical file:
  - the **Default** button in the app's **Config** tab,
  - launcher option **4** (*Restore default configuration*),
  - launcher option **3** (*Clean build artifacts + reset config*).
- Missing active file? Both the launchers and the app recreate it from the bundled default.
- Override the location with `-Dplantumlgui.config=/path/to/java_config.ini`.

### Saving settings

Changing a **theme**, **font**, **window size**, **language** or the **Auto Preview** toggle applies
immediately but is **not written to disk**. Nothing persists until you press **Save** in the
**Config** tab.

While edits are pending, the **Config** tab is marked with an asterisk (`Config *`). From there you can:

- **Save** — write every pending setting to `java_config.ini`.
- **Discard** — drop the pending edits and reload the stored values (enabled only when there is something to discard).
- **Default** — overwrite the file with the bundled defaults and re-apply them.

Quitting with pending edits simply discards them.

| Key | Meaning |
|-----|---------|
| `launcher.projectDir`, `launcher.srcDir`, `launcher.resDir`, `launcher.outDir` | Paths used by both launchers |
| `launcher.mainClass`, `launcher.cleanDirs`, `launcher.javacRelease` | Main class, folders removed by *Clean*, optional `javac --release` |
| `app.language`, `app.theme`, `app.font`, `app.window.width`, `app.window.height` | UI settings, persisted by the **Settings** menu |
| `app.autoPreview`, `app.previewDelayMs` | Live-preview behaviour |
| `plantuml.jarPath` | Custom PlantUML JAR (empty = bundled JAR) |
| `plantuml.bundledJar`, `plantuml.includeDir`, `plantuml.jvmOptions` | Values used on every PlantUML invocation |
| `plantuml.outputDir` | Directory that relative export targets resolve against |
| `export.format`, `export.targetFile` | Export defaults; `export.format` is a `PlantUmlFormat` name: `PNG`, `SVG`, `PUML` or `ARCHIMATE` |

Previews are rendered into a managed scratch directory under the system temp folder, emptied at start-up and on exit. It is not configurable.

---

## User manual

### Window layout

1. **Input (left)** — tabs:
   - **Code** — the diagram editor (default tab).
   - **Samples** — built-in examples; selecting one loads it into *Code*.
   - Below: the **Auto Preview** toggle, a character/line counter and the **Preview** button.
2. **Output (right)** — tabs:
   - **Preview** — the rendered image with a zoom toolbar.
   - **Console** — the PlantUML compilation output.
3. **Footer (bottom)** — tabs:
   - **Export** — target file, format and export/copy actions.
   - **Config** — the PlantUML JAR path plus **Save**, **Discard** and **Default**. Shows `Config *` while settings are unsaved.

### Editing and previewing

- Type PlantUML in the **Code** tab. With **Auto Preview** on, the **Preview** tab updates after a short pause.
- Turn **Auto Preview** off to render only when you click **Preview**.
- Every render appends to the **Console** tab; use **Refresh** to re-run and **Clean** to clear it.

### Loading a sample

Open the **Samples** tab and pick an entry. It replaces the editor content and resets the undo history.

### Zooming the preview

- **+ / −** zoom in and out.
- **Fit** scales the diagram to the window.
- **1:1** resets to 100%.
- Hold `Ctrl` (or `⌘`) and scroll to zoom.

### Exporting

1. Open the **Export** tab.
2. Set the **Target File** (type a path or use **Browse…**).
3. Choose the **Format**: PNG, SVG, PUML or ArchiMate Exchange (.xml). Changing the format rewrites the target extension.
4. Click **Export File**. A confirmation dialog shows the output path.
5. Use **Copy to Clipboard** to copy the rendered image.

Exporting to **ArchiMate Exchange (.xml)** converts the PlantUML source into an ArchiMate Model Exchange file. Conversion details and warnings appear in the **Console** tab.

### Using a custom PlantUML JAR

1. Open the **Config** tab; the **PlantUML JAR Path** field shows the bundled JAR by default.
2. Enter a path or use **Browse…**.
3. Click **Save** to write the path (plus the current export target and format) to `java_config.ini`. Clearing it, or matching the bundled path, reverts to the bundled JAR.
4. Click **Default** to restore the bundled defaults and re-apply them immediately.

### Menus

- **File** — **Open** a `.puml` file, **Quit**.
- **Edit** — **Undo**, **Redo**, **Copy**, **Copy Image**, **Paste**, **Select All**.
- **Settings** — **Theme**, **Font**, **Window** size, **Language**. Each applies at once; use **Config > Save** to keep it.
- **Help** — **About**.

### Keyboard shortcuts

`⌘`/`Ctrl` is the platform menu-shortcut key.

| Action | Shortcut |
|--------|----------|
| Undo | `⌘`/`Ctrl` + `Z` |
| Redo | `⌘`/`Ctrl` + `Shift` + `Z`, or `Ctrl` + `Y` |
| Copy | `⌘`/`Ctrl` + `C` |
| Copy Image | `⌘`/`Ctrl` + `Shift` + `C` |
| Paste | `⌘`/`Ctrl` + `V` |
| Select All | `⌘`/`Ctrl` + `A` |

---

## Troubleshooting

- **Nothing renders / "Preview error"** — make sure `java` is on your `PATH` and the **Config** tab points to a valid PlantUML JAR. Check the **Console** tab for compiler output.
- **Translations or samples missing** — run with `plantuml-gui-java/` as the working directory so `src/main/resources` resolves. The launchers do this for you.
- **Menu labels show `!some.key!`** — that key is missing from the active bundle in `src/main/resources/i18n`.

---

## Contributing

Read **[AI-WORKFLOW.md](AI-WORKFLOW.md)** first. It defines the comment style, the reuse rules and the definition of done that every change in this repository must follow.