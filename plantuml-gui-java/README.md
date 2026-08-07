# PlantUML GUI JAVA

A dependency-free Java Swing desktop app for PlantUML editing, live preview and export, plus ArchiMate Model Exchange XML generation.

It renders diagrams by invoking a bundled PlantUML JAR as a subprocess, so the only runtime requirement is a Java installation.

---

## Features

- **Diagram editor** with a monospaced code area, line numbers, and full undo/redo history.
- **Sample gallery** — a browsable list of ready-to-use PlantUML samples (Activity, Class, Sequence, State, C4, ArchiMate, Gantt, Mindmap, WBS, JSON/YAML, and more) that load straight into the editor.
- **Live preview** that re-renders automatically as you type, with a manual *Preview* button and a character/line counter.
- **Zoomable image preview** — zoom in/out, fit-to-window, reset to 100%, and Ctrl/⌘ + mouse-wheel zoom.
- **Integrated console** that shows PlantUML compilation output and refreshes on every preview.
- **Export** to PNG, SVG, PUML, or **ArchiMate Model Exchange XML**.
- **Copy to clipboard** for both the rendered image and the diagram code.
- **Configurable PlantUML JAR** — point the app at any PlantUML JAR from the Config tab.
- **Look & feel and layout options** — theme, UI font, window resolution presets.
- **Internationalized UI** — English (US), Português (BR), Español (ES).
- **Metal cross-platform theme by default**, dependency-free, runs on Java 11+.

---

## Getting Started

### Requirements

- **Java 11 or newer** on your `PATH` (the `java` command is used to run the PlantUML JAR).

### Run

From the project directory:

```bash
./gradlew run
```

or launch the built application and run the `com.diosaraiva.plantumlgui.Main` class with the project directory as the working directory (the app reads samples, the bundled JAR, and translations relative to it).

---

## User Manual

### Window layout

The main window is split into three areas:

1. **Input (left)** — a tabbed pane:
   - **Code** — the diagram editor (default tab).
   - **Samples** — a list of built-in examples; selecting one loads it into the *Code* tab.
   - Below the tabs: the **Auto Preview** toggle, a character/line counter, and the **Preview** button.
2. **Output (right)** — a tabbed pane:
   - **Preview** — the rendered image with a zoom toolbar.
   - **Console** — the PlantUML compilation output.
3. **Footer (bottom)** — a tabbed pane:
   - **Export** — target file, format, and export/copy actions.
   - **Config** — the PlantUML JAR path.

### Editing and previewing

- Type PlantUML in the **Code** tab. With **Auto Preview** enabled, the **Preview** tab updates automatically after a short pause.
- Disable **Auto Preview** to render only when you click **Preview**.
- Every render also refreshes the **Console** tab with the compiler output; use its **Refresh** button to re-run and **Clean** to clear it.

### Loading a sample

Open the **Samples** tab and select an entry. Its content loads into the **Code** tab, replacing the current text (undo history is reset).

### Zooming the preview

In the **Preview** tab toolbar:

- **+ / −** zoom in and out.
- **Fit** scales the diagram to fit the window.
- **1:1** resets to 100%.
- Hold **Ctrl** (or **⌘**) and scroll the mouse wheel to zoom.

### Exporting

1. Open the **Export** tab.
2. Set the **Target File** (type a path or use **Browse…**).
3. Choose the **Format**: PNG, SVG, PUML, or ArchiMate Exchange (.xml).
4. Click **Export File**. On success a confirmation dialog shows the output path.
5. Use **Copy to Clipboard** to copy the rendered image.

Exporting to **ArchiMate Exchange (.xml)** converts the PlantUML source into an ArchiMate Model Exchange file; conversion details and any warnings appear in the **Console** tab.

### Using a custom PlantUML JAR

1. Open the **Config** tab.
2. The **PlantUML JAR Path** field shows the bundled JAR by default.
3. Enter a path or use **Browse…** to select a different `.jar`.
4. The choice is saved and used for all subsequent previews and exports. Clearing it (or matching the bundled path) reverts to the bundled JAR.

### Menus

- **File**
  - **Open** — load a `.puml` file into the editor.
  - **Quit** — exit the app.
- **Edit**
  - **Undo** / **Redo** — editor history.
  - **Copy** — copy the selected code (or all code).
  - **Copy Image** — copy the rendered diagram image.
  - **Paste** — paste into the editor.
  - **Select All** — select all code in the editor.
- **Settings**
  - **Theme** — switch the Swing look and feel.
  - **Font** — change the UI font family.
  - **Window** — pick a preset window resolution.
  - **Language** — English (US), Português (BR), Español (ES).
- **Help**
  - **About** — version and project information.

### Keyboard shortcuts

`⌘`/`Ctrl` is the platform menu-shortcut key.

| Action       | Shortcut                          |
|--------------|-----------------------------------|
| Undo         | `⌘`/`Ctrl` + `Z`                  |
| Redo         | `⌘`/`Ctrl` + `Shift` + `Z` or `Ctrl` + `Y` |
| Copy         | `⌘`/`Ctrl` + `C`                  |
| Copy Image   | `⌘`/`Ctrl` + `Shift` + `C`        |
| Paste        | `⌘`/`Ctrl` + `V`                  |
| Select All   | `⌘`/`Ctrl` + `A`                  |

---

## Troubleshooting

- **Nothing renders / “Preview error”** — ensure `java` is on your `PATH`, and that the **Config** tab points to a valid PlantUML JAR. Check the **Console** tab for the compiler output.
- **Translations or samples missing** — run the app with the project directory as the working directory so it can locate `src/main/resources`.