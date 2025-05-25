package model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class Processes {

    public static final Path PROC = Paths.get("/proc");
    public static final Path ETC_PASSWD = Paths.get("/etc/passwd");
    public static final Path PROC_STAT = Paths.get("/proc/stat");
    public static final Path PROC_MEMINFO = Paths.get("/proc/meminfo");
    private List<Process> processes = new ArrayList<Process>();

    public Processes () {
        initializeProcesses();
    }

    public static void main(String[] args) {
        Processes processes = new Processes();
        System.out.println("Number of process: " + processes.getTotalProcesses());
        System.out.println("Number of threads: " + processes.getTotalThreads());
        //processes.printProcessesInformation();
    }

    public int getTotalProcesses() {
        return getAllProcessesPath().size();
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

//    public void printProcessesInformation() {
//        this.initializeProcesses();
//
//        for (Process proc : processes) {
//            System.out.println("Process: " + proc.getProcessName()
//                    + "\n\tID: " + proc.getProcessID()
//                    + "\n\tParent ID: " + proc.getParentProcessID()
//                    + "\n\tUser: " + proc.getProcessUser()
//                    + "\n\tThreads: " + proc.getProcessNumberOfThreads()
//                    + "\n\tState: " + proc.getProcessState()
//                    + "\n\tPriority: " + proc.getProcessPriority()
//                    + "\n\tNice Value: " + proc.getProcessNiceValue()
//                    + "\n\tVirtual Memory Allocated: " + proc.getProcessVirtualMemoryAllocated() + " KB"
//                    + "\n\tVirtual Memory Peak: " + proc.getProcessVirtualMemoryPeak() + " KB"
//                    + "\n\tPhysical Memory Usage: " + proc.getProcessPhysicalMemoryUsage() + " KB"
//                    + "\n\tExecutable Code Pages: " + proc.getProcessExecCodePages() + " KB"
//                    + "\n\tLibrary Code Pages: " + proc.getProcessLibCodePages() + " KB"
//                    + "\n\tHeap Pages: " + proc.getProcessHeapPages() + " KB"
//                    + "\n\tStack Pages: " + proc.getProcessStackPages() + " KB"
//                    + "\n\tPercentage of RAM Usage: " + proc.getProcessMemoryPercentage() + " %"
//                    + "\n\tPercentage of CPU Usage: " + proc.getProcessCpuUsage() + " %"
//                    + "\n\tProcess Commandline: " + proc.getProcessCommandLine() + "\n");
//        }
//    }

    private void initializeProcesses() {
        var procList = getAllProcessesPath();

        for (var procPath : procList) {
            this.processes.add(new Process(procPath));
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}