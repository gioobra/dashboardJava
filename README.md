# Task Manager Dashboard (JavaFX)

JavaFX desktop dashboard that mirrors a Linux task manager: live CPU usage, memory and swap breakdowns, and process inspection (including an advanced details view). Built for the Operating Systems course at UTFPR.

## Features
- Live CPU usage and idle tracking with progress bars plus history line chart (5s refresh)
- RAM and swap breakdown with labeled pie charts and percentage badges
- Process table with PID, user, CPU%, and RAM% sorted data refreshed periodically
- Secondary details window with per-process metadata (threads, state, priority, memory pages, command line)
- Clean JavaFX styling via `styles.css` and FXML-driven layouts for the main and details views

## Requirements
- JDK 21+
- Maven 3.9+
- Linux environment exposing `/proc` (CPU/memory/process sampling depends on it); not compatible with Windows or macOS without adaptation

## Getting started
1. Clone this repository.
2. Run the app with Maven:
	```bash
	mvn clean javafx:run
	```
3. (Optional) Package a runnable JAR:
	```bash
	mvn clean package
	```
	The resulting artifact requires JavaFX modules on the classpath; prefer `javafx:run` during development.

## Using the app
- Launch opens the main dashboard showing CPU, memory, swap, and a process table updated every 5 seconds.
- Click "Detalhes" to open the advanced process window with deeper per-process metrics and command lines.
- Closing the main window stops the scheduled updater gracefully.

## Project layout
- `src/main/java/app/MainApp.java` – JavaFX bootstrapper
- `src/main/java/controller/MainController.java` – main dashboard logic and periodic sampler
- `src/main/java/controller/DetailsController.java` – advanced process table
- `src/main/java/model/*` – `/proc` readers for CPU, memory, and process data
- `src/main/resources/view/*` – FXML views and `styles.css`

## Known limitations
- Targets Linux `/proc`; will not gather metrics on Windows/macOS without implementing platform-specific providers.
- CPU usage sampling waits 2 seconds between reads to produce deltas, so instantaneous spikes may be smoothed.

## License
MIT License. See LICENSE for details.
