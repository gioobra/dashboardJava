package model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class Process {
    public static final Path ETC_PASSWD = Paths.get("/etc/passwd");
    public static final Path PROC_STAT = Paths.get("/proc/stat");
    public static final Path PROC_MEMINFO = Paths.get("/proc/meminfo");

    public Path BasePath;

    private long cpuTimeProcessInit = 0;
    private long totalCpuTimeInit = 0;

    public Process(Path BasePath) {
        this.BasePath = BasePath;

        Optional<String[]> optionalProcessInfo;
        try (Stream<String> processInfo = Files.lines(BasePath.resolve("stat"))) {
            optionalProcessInfo = processInfo
                    .map(content -> content.split(" "))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (optionalProcessInfo.isPresent()) {
            cpuTimeProcessInit = Long.parseLong(optionalProcessInfo.get()[13]) + Long.parseLong(optionalProcessInfo.get()[14]);
            totalCpuTimeInit = getTotalCpuUsage();
        }
    }

    public Path getBasePath() {
        return this.BasePath;
    }

    public String getProcessUser() {
        List<String> uidInfo = new ArrayList<>();

        try (Stream<String> readLines = Files.lines(BasePath.resolve("status"))){
            uidInfo = readLines
                    .filter(line -> line.startsWith("Uid:"))
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return getUser(uidInfo.getFirst().split("\\s+")[1]);
    }

    private String getUser(String uid) {
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

    public long getProcessID () {
        Optional<String> optionalProcessInfo;

        try (Stream<String> processInfo = Files.lines(BasePath.resolve("status"))) {
            optionalProcessInfo = processInfo
                            .filter(content -> content
                            .startsWith("Pid:")).findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(optionalProcessInfo.isPresent()) {
            return Long.parseLong(optionalProcessInfo.get().split(":")[1].trim());
        } else {
            return 0;
        }
    }

    public long getParentProcessID () {
        Optional<String> optionalProcessInfo;

        try (Stream<String> processInfo = Files.lines(BasePath.resolve("status"))) {
            optionalProcessInfo = processInfo
                    .filter(content -> content
                            .startsWith("PPid:")).findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(optionalProcessInfo.isPresent()) {
            return Long.parseLong(optionalProcessInfo.get().split(":")[1].trim());
        } else {
            return 0;
        }
    }

    public String getProcessName () {
        Optional<String> optionalProcessInfo;

        try (Stream<String> processInfo = Files.lines(BasePath.resolve("comm"))) {
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

    public long getProcessNumberOfThreads() {
        List<String> threadInfo = new ArrayList<>();

        try (Stream<String> readLines = Files.lines(BasePath.resolve("status"))) {
            threadInfo = readLines
                    .filter(line -> line.startsWith("Threads:"))
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return Long.parseLong(threadInfo.getFirst().split("\\s+")[1]);
    }

    public String getProcessState() {
        Optional<String> optionalProcessInfo;

        try (Stream<String> processInfo = Files.lines(BasePath.resolve("status"))) {
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

    public String getProcessVirtualMemoryAllocated() {
        Optional<String> optionalProcessInfo;

        try (Stream<String> processInfo = Files.lines(BasePath.resolve("status"))) {
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

    public long getProcessVirtualMemoryPeak() {
        Optional<String> vmStk;

        try (Stream<String> processCodePages = Files.lines(BasePath.resolve("status"))) {
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

    public String getProcessPhysicalMemoryUsage() {
        Optional<String> optionalProcessInfo;

        try (Stream<String> processInfo = Files.lines(BasePath.resolve("status"))) {
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

    public long getProcessPriority() {
        Optional<String[]> optionalProcessInfo;

        try (Stream<String> processInfo = Files.lines(BasePath.resolve("stat"))) {
            optionalProcessInfo = processInfo
                    .map(content -> content.split(" "))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(optionalProcessInfo.isPresent()) {
            return Long.parseLong(optionalProcessInfo.get()[17]);
        } else {
            return 0;
        }
    }

    public long getProcessNiceValue() {
        Optional<String[]> optionalProcessInfo;

        try (Stream<String> processInfo = Files.lines(BasePath.resolve("stat"))) {
            optionalProcessInfo = processInfo
                    .map(content -> content.split(" "))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(optionalProcessInfo.isPresent()) {
            return Long.parseLong(optionalProcessInfo.get()[18]);
        } else {
            return 0;
        }
    }

    public String getProcessCommandLine() {
        Optional<String> optionalProcessInfo;

        try (Stream<String> processInfo = Files.lines(BasePath.resolve("cmdline"))) {
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

    public double getProcessCpuUsage() {
        Optional<String[]> optionalProcessInfo;
        long cpuTimeProcessNewer = 0;
        long totalCpuTimeNewer = 0;

        try (Stream<String> processInfo = Files.lines(BasePath.resolve("stat"))) {
            optionalProcessInfo = processInfo
                    .map(content -> content.split(" "))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(optionalProcessInfo.isPresent()) {
            cpuTimeProcessNewer = Long.parseLong(optionalProcessInfo.get()[13]) + Long.parseLong(optionalProcessInfo.get()[14]);
            totalCpuTimeNewer = getTotalCpuUsage();

            return ((double) (cpuTimeProcessNewer - cpuTimeProcessInit)) / (totalCpuTimeNewer - totalCpuTimeInit) * 100;
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

    public double getProcessMemoryPercentage() {
        Optional<String> totalRam;

        try (Stream<String> processRamUsage = Files.lines(PROC_MEMINFO)) {
            totalRam = processRamUsage
                    .filter(content -> content.startsWith("MemTotal:"))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(totalRam.isPresent()) {
            return (Double.parseDouble(getProcessPhysicalMemoryUsage()))/ (Double.parseDouble(totalRam.get().trim().split("\\s+")[1])) * 100.0;
        } else {
            return 0.0;
        }
    }

    public long getProcessExecCodePages() {
        Optional<String> vmExe;

        try (Stream<String> processCodePages = Files.lines(BasePath.resolve("status"))) {
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

    public long getProcessLibCodePages () {
        Optional<String> vmLib;

        try (Stream<String> processCodePages = Files.lines(BasePath.resolve("status"))) {
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

    public long getProcessHeapPages () {
        Optional<String> vmData;

        try (Stream<String> processCodePages = Files.lines(BasePath.resolve("status"))) {
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

    public long getProcessStackPages () {
        Optional<String> vmStk;

        try (Stream<String> processCodePages = Files.lines(BasePath.resolve("status"))) {
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
