# HomeHelper

HomeHelper is a small Java command-line application for managing household reminders and shopping items. I built it around a practical family use case: keeping daily reminders and the next shopping list in one local place.

The project is intentionally dependency-free. It focuses on Java fundamentals, separation of concerns, input validation, local persistence and automated tests.

## Features

- Add dated household reminders.
- Add dated shopping items with quantities.
- Show every task or only items due today and overdue.
- Mark tasks complete or remove them using a short ID.
- Keep data between runs in a local file.
- Preserve non-English text such as Turkish item names.
- Reject blank titles, invalid dates, invalid quantities and ambiguous IDs.
- Write data through a temporary file before replacing the saved file.

## Requirements

- JDK 21 or newer.
- No third-party libraries or package manager.

## Compile and run

### PowerShell

```powershell
New-Item -ItemType Directory -Force out | Out-Null
$sources = Get-ChildItem src/main/java,src/test/java -Recurse -Filter *.java | ForEach-Object FullName
javac -d out $sources
java -cp out com.yusufkara.homehelper.Main
```

### macOS or Linux

```bash
mkdir -p out
find src/main/java src/test/java -name "*.java" -print0 | xargs -0 javac -d out
java -cp out com.yusufkara.homehelper.Main
```

## Example session

```text
reminder add 2026-09-02 "Take medicine"
shopping add 2026-09-02 2 "Milk"
today
list
done a1b2c3d4
remove a1b2c3d4
exit
```

Task IDs are generated at runtime; use the eight-character ID printed by `list`.

## Run tests

After compiling:

```text
java -cp out com.yusufkara.homehelper.HomeTaskServiceTest
```

The self-contained test runner covers adding, ordering, due-date filtering, completing, removing, validation, quoted command parsing and a UTF-8 file round trip. GitHub Actions runs the same compilation and tests on every push and pull request.

## Project structure

```text
src/main/java/com/yusufkara/homehelper/
├── Main.java                         Command-line interface and parser
├── model/HomeTask.java               Validated immutable task model
├── model/TaskType.java               Reminder and shopping types
├── repository/TaskRepository.java    Persistence boundary
├── repository/FileTaskRepository.java Local file implementation
└── service/HomeTaskService.java      Application rules and task operations

src/test/java/com/yusufkara/homehelper/
└── HomeTaskServiceTest.java          Dependency-free automated tests
```

## Data and privacy

HomeHelper does not use a server, account, analytics or network connection. It stores tasks in `data/home-helper.db` on the current device. The `data/` directory is ignored by Git so personal reminder and shopping data are not committed accidentally.

## Current limitations

- The interface is terminal-based rather than graphical or mobile.
- Reminders are displayed when the program is opened; it does not send operating-system notifications.
- The local data file is not encrypted, so private information should not be entered on a shared device.
- This version is single-user and does not synchronize between devices.

## Possible next steps

- Add a JavaFX interface.
- Add recurring reminders.
- Add optional desktop notifications.
- Add import/export while keeping personal data local by default.

