# AI-WORKFLOW

Operating rules for any AI agent (or human) changing code in this repository.
These rules are **binding**. A change that breaks one of them is not done.

---

## 1. Golden rules

1. **Read before you write.** Locate the existing owner of a concept before adding anything. Never introduce a second way to do something that already exists.
2. **Reuse, then extend, then create.** Creating a new class is the last resort and must be justified by a second caller.
3. **One concept, one home.** Every rule, constant, format, path or string lives in exactly one place.
4. **Delete what you replace.** No dead code, no unused imports, no orphan config keys, no "just in case" methods.
5. **Leave the tree compiling clean.** `javac -Xlint:all -Werror` must pass with zero output.

---

## 2. Comment rules (strict)

- **Single-line `//` only.** No Javadoc (`/** */`), no block comments (`/* */`), no banner or separator comments.
- **One comment above a class**, stating what it owns and why it exists. Every class has exactly one.
- **Comments explain *why*, never *what*.** If the comment restates the code, delete it.
- **Comment only the non-obvious**: an invariant, an ordering constraint, a workaround, a threading rule, a fallback.
- **No comments on getters, setters, trivial delegation or self-describing methods.**
- **No `TODO`, `FIXME`, author tags, dates, changelogs or commented-out code.**
- Comments are written in **English**, sentence case, and fit on one line.

```java
// Good - explains a constraint the reader cannot infer.
// Both pipes must be drained concurrently or the child blocks on a full buffer.

// Bad - restates the code.
// Loops over the list and adds each item.
```

---

## 3. Reuse map — check here first

Before writing any of these, use the existing owner.

| Need | Use | Never |
|------|-----|-------|
| Read a bundled file | `ResourceLocator.readString` / `openStream` / `find` | Hand-rolled classpath or `File` lookups |
| Any user-visible text | `I18n.get(key, args)` + all three bundles | String literals in Java code |
| A setting | `AppSettings` constant + key in `java_config.ini` | Ad-hoc `System.getProperty` or magic values |
| Change a setting | `AppSettings.set` (memory only) | `AppSettings.save()` outside the Save button |
| A file extension, CLI flag or export label | `PlantUmlFormat` | Extension string literals, parallel format maps |
| Split, strip or swap a file extension | `FileNames` | Inline `lastIndexOf('.')` |
| Run an external JAR | `JarUtils.runJar` | Raw `ProcessBuilder` |
| Work off the EDT | `Background.run` | `new Thread`, `SwingWorker` |
| Dialog, clipboard, toolbar, menu item, look and feel, font | `SwingUtils` | Direct `JOptionPane` / `Toolkit` / `UIManager` calls |
| A scratch file for rendering | `PlantUmlRenderer` | A temp directory of your own |

---

## 4. Architecture rules

- Package dependency direction is strictly **`ui` → `service` → `util`**. Never import upwards.
- `service` and `util` must stay **headless**: no Swing imports, no dialogs, no EDT assumptions.
- `ui` holds **no business logic** — it collects input, delegates to `service`, and renders the result.
- All classes are `final` unless designed for extension. All utility classes are `final` with a private constructor.
- Prefer `record` for immutable data carriers. Prefer `enum` for closed sets of options.
- Constants are `private static final` and named; no magic numbers or repeated string literals.

---

## 5. Tree-shaking checklist

Run this on every touched file:

- [ ] No two methods do the same thing — merge them.
- [ ] No two constants hold the same value for the same reason — merge them.
- [ ] No parallel data structures keyed the same way — merge into one record-valued map.
- [ ] Every `private` member has a caller.
- [ ] Every `public` member has a caller outside its class.
- [ ] Every import is used.
- [ ] Every i18n key is referenced; every referenced key exists in **all** bundles.
- [ ] Every `java_config.ini` key is read by the app or a launcher, and every key read exists in the file.

---

## 6. Internationalization rules

- Bundles live in `plantuml-gui-java/src/main/resources/i18n/messages_<lang>_<COUNTRY>.properties`.
- Files are **UTF-8** with real accented characters. Never commit escaped or double-encoded text.
- `messages_en_US.properties` is the reference. **All bundles must contain exactly the same key set.**
- Keys are dot-namespaced by area (`menu.`, `export.`, `config.`, `console.`, `preview.`, `archimate.`).
- Placeholders use `MessageFormat` (`{0}`, `{1}`). Avoid literal single quotes in translated values.

---

## 7. Configuration rules

- Adding a setting means: a constant in `AppSettings`, a key in `src/main/resources/java_config.ini`, and a row in the README table.
- The bundled INI is the template — it defines both defaults and key order, and its comments are preserved on write.
- **Settings never persist implicitly.** `AppSettings.set` updates memory and marks the state dirty; only `AppSettings.save()` touches the file, and only the **Save** button calls it. A menu, toggle or dialog that changes a setting must not save.
- Writing must round-trip: saving unmodified values reproduces the file byte for byte, and a single edit changes exactly one line.
- Keep `java_config.ini` at the repository root in sync with the bundled template.
- Never read a key that is not in the template.
- Restoring defaults means **producing the bundled template verbatim** — nothing else. It is implemented exactly once per launcher (`restore_config` / `:restore_config`) and once in the app (`AppSettings.resetToDefaults`). Reuse those; never inline a `cp`/`copy`.
- Anything that restores or discards config must reload the settings it derived from it, so the running process reflects the new values.

---

## 8. Launcher rules

- `launcher_java_unix.sh` and `launcher_java_windows.bat` are **feature-mirrors**. A menu option added to one must be added to the other, with the same number, wording and confirmation prompt.
- Launchers read every path and name from `java_config.ini`; no hard-coded project layout beyond the fallback defaults.
- Every destructive action asks for confirmation and prints exactly what it will touch beforehand.
- Verify the shell script with `bash -n`, and check the batch file for undefined labels and missing `exit /b`.

---

## 9. Definition of done

A change is complete only when **all** of the following hold:

1. `javac -Xlint:all -Werror` produces no output.
2. The app starts and renders the default sample.
3. Every new user-visible string exists in `en_US`, `pt_BR` and `es_ES`, and no bundle has an unmatched key.
4. Nothing was duplicated: the reuse map in §3 was checked.
5. Comments follow §2 exactly — one single-line comment per class, and none that restate code.
6. Any code, key or file the change made redundant has been deleted.
7. Both launchers still offer the same options, and `bash -n launcher_java_unix.sh` passes.
8. The README reflects any change to behaviour, configuration, menus or layout.

---

## 10. Verification commands

```bash
cd plantuml-gui-java

# Compile the whole tree, warnings are errors.
find src/main/java -name "*.java" > /tmp/sources.txt
javac -Xlint:all -Werror -d /tmp/build @/tmp/sources.txt

# Run from the project directory so resources resolve.
java -cp /tmp/build:src/main/resources com.diosaraiva.plantumlgui.Main

# Confirm every bundle carries the same keys.
for f in src/main/resources/i18n/*.properties; do
  echo "$f: $(grep -c '^[a-z].*=' "$f") keys"
done

# Launcher syntax, and confirm the active config matches the template after a reset.
cd ..
bash -n launcher_java_unix.sh
diff java_config.ini plantuml-gui-java/src/main/resources/java_config.ini
```
