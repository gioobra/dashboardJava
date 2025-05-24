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
    public static final Path ETC_PASSWD = Paths.get("/etc/passwd");

    public Processes () {

    }

    public static void main(String[] args) {
        Processes processes = new Processes();
        System.out.println("Number of process: " + processes.getTotalProcesses());
        System.out.println("Number of threads: " + processes.getTotalThreads());
        processes.printProcessesInformations();
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

    public void printProcessesInformations() {

        for (Path processPath : getAllProcessesPath()) {
            System.out.println("Process: " + processPath.getFileName()
                    + "\n\tUser: " + getProcessUser(processPath.toString())
                    + "\n\t\tThreads: " + getProcessNumberOfThreads(processPath.toString())
                    + "\n");
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
}
