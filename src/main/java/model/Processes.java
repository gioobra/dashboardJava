package model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class Processes {

    public static final Path PROC = Paths.get("/proc");
    private List<Process> processes = new ArrayList<Process>();

    public Processes () {
        initializeProcesses();
    }

    private void initializeProcesses() {
        List<Path> procList = getAllProcessesPath();

        for (Path procPath : procList) {
            this.processes.add(new Process(procPath));
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public int getTotalProcesses() {
        return this.processes.size();
    }

    public int getTotalThreads() {
        int numberOfThreads = 0;
        List<Path> processesPath = getAllProcessesPath();

        for (Path processPath : processesPath) {
            numberOfThreads += threadCount(processPath);
        }

        return numberOfThreads;
    }

    private long threadCount(Path process) {
        long threads = 0;

        process = process.resolve("task");

        if(Files.notExists(process)) {
            return 0;
        }

        try (Stream<Path> taskDir = Files.list(process)) {
            threads = taskDir
                    .filter(path -> path.getFileName().toString().matches("\\d+"))
                    .count();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return threads;
    }

    private List<Path> getAllProcessesPath() {
        List<Path> AllProcessPath = new ArrayList<>();

        try (Stream<Path> process = Files.list(PROC)) {
            AllProcessPath = process
                    .filter(path -> path.getFileName().toString().matches("\\d+"))
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return AllProcessPath;
    }

    public List<Process> getAllProcesses () {
        return this.processes;
    }
}