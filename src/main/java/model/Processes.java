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

    public Processes () {

    }

    public static void main(String[] args) {
        Processes processes = new Processes();
        System.out.println("Number of process: " + processes.getTotalProcesses());
        System.out.println("Number of threads: " + processes.getTotalThreads());
        processes.printProcessesInformation();
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

    public List<Path> getAllProcessesPath() {
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

    public String getProcessUser(String process) {
        Path processStatus = Paths.get(process).resolve("status");
        List<String> uidInfo = new ArrayList<>();

        try (Stream<String> readLines = Files.lines(processStatus)){
            uidInfo = readLines
                    .filter(line -> line.startsWith("Uid:"))
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return getUser(uidInfo.getFirst().split("\\s+")[1]);
    }

    private String getUser (String uid) {
        List<String> uidInfo = new ArrayList<>();

        try (Stream<String> passwd = Files.lines(ETC_PASSWD)) {
            uidInfo = passwd
                    .filter(uidLine -> uidLine.contains(uid))
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return uidInfo.getFirst().split(":")[0];
    }

    public void printProcessesInformation() {

        for (Path processPath : getAllProcessesPath()) {
            System.out.println("Process: " + getProcessName(processPath)
                    + "\n\tID: " + getProcessID(processPath)
                    + "\n\tParent ID: " + getParentProcessID(processPath)
                    + "\n\tUser: " + getProcessUser(processPath.toString())
                    + "\n\tThreads: " + getProcessNumberOfThreads(processPath.toString())
                    + "\n\tState: " + getProcessState(processPath)
                    + "\n\tPriority: " + getProcessPriority(processPath)
                    + "\n\tNice Value: " + getProcessNiceValue(processPath)
                    + "\n\tVirtual Memory Allocated: " + getProcessVirtualMemoryAllocated(processPath) + " KB"
                    + "\n\tVirtual Memory Peak: " + getProcessVirtualMemoryPeak(processPath) + " KB"
                    + "\n\tPhysical Memory Usage: " + getProcessPhysicalMemoryUsage(processPath) + " KB"
                    + "\n\tExecutable Code Pages: " + getProcessExecCodePages(processPath) + " KB"
                    + "\n\tLibrary Code Pages: " + getProcessLibCodePages(processPath) + " KB"
                    + "\n\tHeap Pages: " + getProcessHeapPages(processPath) + " KB"
                    + "\n\tStack Pages: " + getProcessStackPages(processPath) + " KB"
                    + "\n\tPercentage of RAM Usage: " + getProcessMemoryPercentage(processPath) + " %"
                    + "\n\tPercentage of CPU Usage: " + getProcessCpuUsage(processPath) + " %"
                    + "\n\tProcess Commandline: " + getProcessCommandLine(processPath) + "\n");
        }
    }

    public String getProcessID (Path process) {
        Optional<String> optionalProcessInfo;

        try (Stream<String> processInfo = Files.lines(process.resolve("status"))) {
            optionalProcessInfo = processInfo
                    .filter(content -> content
                            .startsWith("Pid:")).findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(optionalProcessInfo.isPresent()) {
            return optionalProcessInfo.get().split(":")[1].trim();
        } else {
            return "NULL";
        }
    }

    public String getParentProcessID (Path process) {
        Optional<String> optionalProcessInfo;

        try (Stream<String> processInfo = Files.lines(process.resolve("status"))) {
            optionalProcessInfo = processInfo
                    .filter(content -> content
                            .startsWith("PPid:")).findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(optionalProcessInfo.isPresent()) {
            return optionalProcessInfo.get().split(":")[1].trim();
        } else {
            return "NULL";
        }
    }

    public String getProcessName (Path process) {
        Optional<String> optionalProcessInfo;

        try (Stream<String> processInfo = Files.lines(process.resolve("comm"))) {
            optionalProcessInfo = processInfo
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(optionalProcessInfo.isPresent()) {
            return optionalProcessInfo.get().trim();
        } else {
            return "NULL";
        }
    }

    public String getProcessNumberOfThreads(String process) {
        Path processPath = Paths.get(process).resolve("status");
        List<String> threadInfo = new ArrayList<>();

        try (Stream<String> readLines = Files.lines(processPath)) {
            threadInfo = readLines
                    .filter(line -> line.startsWith("Threads:"))
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return threadInfo.getFirst().split("\\s+")[1];
    }

    public String getProcessState(Path process) {
        Optional<String> optionalProcessInfo;

        try (Stream<String> processInfo = Files.lines(process.resolve("status"))) {
            optionalProcessInfo = processInfo
                    .filter(content -> content.startsWith("State:"))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(optionalProcessInfo.isPresent()) {
            return optionalProcessInfo.get().split(":")[1].trim();
        } else {
            return "NULL";
        }
    }

    public String getProcessVirtualMemoryAllocated (Path process) {
        Optional<String> optionalProcessInfo;

        try (Stream<String> processInfo = Files.lines(process.resolve("status"))) {
            optionalProcessInfo = processInfo
                    .filter(content -> content.startsWith("VmSize:"))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(optionalProcessInfo.isPresent()) {
            return optionalProcessInfo.get().split("\\s+")[1].trim();
        } else {
            return "NULL";
        }
    }

    public long getProcessVirtualMemoryPeak (Path process) {
        Optional<String> vmStk;

        try (Stream<String> processCodePages = Files.lines(process.resolve("status"))) {
            vmStk = processCodePages
                    .filter(content -> content.startsWith("VmPeak:"))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(vmStk.isPresent()) {
            return Long.parseLong(vmStk.get().trim().split("\\s+")[1]);
        } else {
            return 0;
        }
    }

    public String getProcessPhysicalMemoryUsage (Path process) {
        Optional<String> optionalProcessInfo;

        try (Stream<String> processInfo = Files.lines(process.resolve("status"))) {
            optionalProcessInfo = processInfo
                    .filter(content -> content.startsWith("VmRSS:"))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(optionalProcessInfo.isPresent()) {
            return optionalProcessInfo.get().split("\\s+")[1].trim();
        } else {
            return "NULL";
        }
    }

    public String getProcessPriority (Path process) {
        Optional<String[]> optionalProcessInfo;

        try (Stream<String> processInfo = Files.lines(process.resolve("stat"))) {
            optionalProcessInfo = processInfo
                    .map(content -> content.split(" "))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(optionalProcessInfo.isPresent()) {
            return optionalProcessInfo.get()[17];
        } else {
            return "NULL";
        }
    }

    public String getProcessNiceValue (Path process) {
        Optional<String[]> optionalProcessInfo;

        try (Stream<String> processInfo = Files.lines(process.resolve("stat"))) {
            optionalProcessInfo = processInfo
                    .map(content -> content.split(" "))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(optionalProcessInfo.isPresent()) {
            return optionalProcessInfo.get()[18];
        } else {
            return "NULL";
        }
    }

    public String getProcessCommandLine (Path process) {
        Optional<String> optionalProcessInfo;

        try (Stream<String> processInfo = Files.lines(process.resolve("cmdline"))) {
            optionalProcessInfo = processInfo
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(optionalProcessInfo.isPresent()) {
            return optionalProcessInfo.get().trim();
        } else {
            return "NULL";
        }
    }

    public double getProcessCpuUsage (Path process) {
        Optional<String[]> optionalProcessInfo;
        long cpuTimeProcessOlder = 0;
        long totalCpuTimeOlder = 0;
        long cpuTimeProcessNewer = 0;
        long totalCpuTimeNewer = 0;

        try (Stream<String> processInfo = Files.lines(process.resolve("stat"))) {
            optionalProcessInfo = processInfo
                    .map(content -> content.split(" "))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(optionalProcessInfo.isPresent()) {
            cpuTimeProcessOlder = Long.parseLong(optionalProcessInfo.get()[13]) + Long.parseLong(optionalProcessInfo.get()[14]);
            totalCpuTimeOlder = getTotalCpuUsage();

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            try (Stream<String> processInfo = Files.lines(process.resolve("stat"))) {
                optionalProcessInfo = processInfo
                        .map(content -> content.split(" "))
                        .findFirst();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if(optionalProcessInfo.isPresent()) {
                cpuTimeProcessNewer = Long.parseLong(optionalProcessInfo.get()[13]) + Long.parseLong(optionalProcessInfo.get()[14]);
                totalCpuTimeNewer = getTotalCpuUsage();

                return ((double) (cpuTimeProcessNewer - cpuTimeProcessOlder)) / (totalCpuTimeNewer - totalCpuTimeOlder) * 100;
            } else {
                return 0;
            }

        } else {
            return 0;
        }
    }

    private long getTotalCpuUsage () {
        String[] mainCpuInfo = getMainCpuInfo();
        long totalCpuUsage = 0;

        for (int j = 1; j < (mainCpuInfo.length - 2); j++) {
            totalCpuUsage += Long.parseLong(mainCpuInfo[j]);
        }

        return totalCpuUsage;
    }

    private String[] getMainCpuInfo() {
        Optional<String> optionalCpuInfo;
        String[] error = {};

        try (Stream<String> cpuInfo = Files.lines(PROC_STAT)) {
            optionalCpuInfo = cpuInfo
                    .filter(content -> content.startsWith("cpu"))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (optionalCpuInfo.isPresent()) {
            return optionalCpuInfo.get().trim().split("\\s+");
        } else {
            return error;
        }
    }

    public double getProcessMemoryPercentage(Path process) {
        Optional<String> totalRam;

        try (Stream<String> processRamUsage = Files.lines(PROC_MEMINFO)) {
            totalRam = processRamUsage
                    .filter(content -> content.startsWith("MemTotal:"))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(totalRam.isPresent()) {
            return (Double.parseDouble(getProcessPhysicalMemoryUsage(process)))/ (Double.parseDouble(totalRam.get().trim().split("\\s+")[1])) * 100.0;
        } else {
            return 0.0;
        }
    }

    public long getProcessExecCodePages(Path process) {
        Optional<String> vmExe;

        try (Stream<String> processCodePages = Files.lines(process.resolve("status"))) {
            vmExe = processCodePages
                    .filter(content -> content.startsWith("VmExe:"))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(vmExe.isPresent()) {
            return Long.parseLong(vmExe.get().trim().split("\\s+")[1]);
        } else {
            return 0;
        }
    }

    public long getProcessLibCodePages (Path process) {
        Optional<String> vmLib;

        try (Stream<String> processCodePages = Files.lines(process.resolve("status"))) {
            vmLib = processCodePages
                    .filter(content -> content.startsWith("VmLib:"))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(vmLib.isPresent()) {
            return Long.parseLong(vmLib.get().trim().split("\\s+")[1]);
        } else {
            return 0;
        }
    }

    public long getProcessHeapPages (Path process) {
        Optional<String> vmData;

        try (Stream<String> processCodePages = Files.lines(process.resolve("status"))) {
            vmData = processCodePages
                    .filter(content -> content.startsWith("VmData:"))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(vmData.isPresent()) {
            return Long.parseLong(vmData.get().trim().split("\\s+")[1]);
        } else {
            return 0;
        }
    }

    public long getProcessStackPages (Path process) {
        Optional<String> vmStk;

        try (Stream<String> processCodePages = Files.lines(process.resolve("status"))) {
            vmStk = processCodePages
                    .filter(content -> content.startsWith("VmStk:"))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(vmStk.isPresent()) {
            return Long.parseLong(vmStk.get().trim().split("\\s+")[1]);
        } else {
            return 0;
        }
    }
}