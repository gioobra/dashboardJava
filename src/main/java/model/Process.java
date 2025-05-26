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

    // Constrói o modelo de processo com base no caminho /proc/[pid]. Além disso, armazena o número de ticks da CPU (inicial) utilizada naquele processo.
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

    // Metodo que retorna qual o usuario do processo
    public String getProcessUser() {
        List<String> uidInfo = new ArrayList<>();

        try (Stream<String> readLines = Files.lines(BasePath.resolve("status"))){
            uidInfo = readLines
                    .filter(line -> line.startsWith("Uid:"))
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Chama um metodo auxiliar para ler /etc/passwd
        return getUser(uidInfo.getFirst().split("\\s+")[1]);
    }

    // Metodo que auxilia no encontro de qual usuario tem aquele uid
    private String getUser(String uid) {
        List<String> uidInfo = new ArrayList<>();

        // Le que contem o nome do usuario em questao
        try (Stream<String> passwd = Files.lines(ETC_PASSWD)) {
            uidInfo = passwd
                    .filter(uidLine -> uidLine.contains(uid))
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Pega o nome e retorna
        return uidInfo.getFirst().split(":")[0];
    }

    // Retorno o numero do id do processo
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
            // Transforma em long e retorna
            return Long.parseLong(optionalProcessInfo.get().split(":")[1].trim());
        } else {
            return 0;
        }
    }

    // Metodo que determina qual o id do processo pai do processo atual
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
            // Transforma em long e retorna
            return Long.parseLong(optionalProcessInfo.get().split(":")[1].trim());
        } else {
            return 0;
        }
    }

    // retorna o nome do processo
    public String getProcessName () {
        Optional<String> optionalProcessInfo;

        // Aqui, o caminho /proc/[pid]/comm funciona melhor, pois dentro de comm soh existe o nome do processo
        try (Stream<String> processInfo = Files.lines(BasePath.resolve("comm"))) {
            optionalProcessInfo = processInfo
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(optionalProcessInfo.isPresent()) {
            // Corta as pontas e retorna
            return optionalProcessInfo.get().trim();
        } else {
            return "NULL";
        }
    }

    // Metodo que conta o numero de threads do processo em questao
    public long getProcessNumberOfThreads() {
        List<String> threadInfo = new ArrayList<>();

        // Le a informacao de threads no status
        try (Stream<String> readLines = Files.lines(BasePath.resolve("status"))) {
            threadInfo = readLines
                    .filter(line -> line.startsWith("Threads:"))
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Filtra e retorna como long
        return Long.parseLong(threadInfo.getFirst().split("\\s+")[1]);
    }

    // Metodo que obtem o estado do processo
    public String getProcessState() {
        Optional<String> optionalProcessInfo;

        // Le a string com state e a salve
        try (Stream<String> processInfo = Files.lines(BasePath.resolve("status"))) {
            optionalProcessInfo = processInfo
                    .filter(content -> content.startsWith("State:"))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(optionalProcessInfo.isPresent()) {
            // Filtra, corta as pontas e retorna a string
            return optionalProcessInfo.get().split(":")[1].trim();
        } else {
            return "NULL";
        }
    }

    // Metodo que retorna a quantidadede memoria virtual alocada para o processo
    public String getProcessVirtualMemoryAllocated() {
        Optional<String> optionalProcessInfo;

        // Quem tem esse valor eh vmSize
        try (Stream<String> processInfo = Files.lines(BasePath.resolve("status"))) {
            optionalProcessInfo = processInfo
                    .filter(content -> content.startsWith("VmSize:"))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(optionalProcessInfo.isPresent()) {
            // Filtra e retorna o dado
            return optionalProcessInfo.get().split("\\s+")[1].trim();
        } else {
            return "0";
        }
    }

    // Metodo que retorna o maximo de memoria virtual que um processo pode ter
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

    // Metodo que retorna a quantidade de memoria fisica que um processo esta usando
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
            return "0";
        }
    }

    // Metodo que retorna o prioridade de um processo
    public long getProcessPriority() {
        Optional<String[]> optionalProcessInfo;

        // Dentro do arquivo stat do process, a string toda eh pega
        try (Stream<String> processInfo = Files.lines(BasePath.resolve("stat"))) {
            optionalProcessInfo = processInfo
                    .map(content -> content.split(" "))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(optionalProcessInfo.isPresent()) {
            // O valor de prioridade se encontra no campo 17
            return Long.parseLong(optionalProcessInfo.get()[17]);
        } else {
            return 0;
        }
    }

    // Metodo que retorna o Nice de um processo
    public long getProcessNiceValue() {
        Optional<String[]> optionalProcessInfo;

        // Dentro do arquivo stat do process, a string toda eh pega
        try (Stream<String> processInfo = Files.lines(BasePath.resolve("stat"))) {
            optionalProcessInfo = processInfo
                    .map(content -> content.split(" "))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(optionalProcessInfo.isPresent()) {
            // O valor de prioridade se encontra no campo 18
            return Long.parseLong(optionalProcessInfo.get()[18]);
        } else {
            return 0;
        }
    }

    // Metodo que retorna o comando que chamou o processo
    public String getProcessCommandLine() {
        Optional<String> optionalProcessInfo;

        // O diretorio /proc/[pid]/cmdline soh contem esse dado
        try (Stream<String> processInfo = Files.lines(BasePath.resolve("cmdline"))) {
            optionalProcessInfo = processInfo
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(optionalProcessInfo.isPresent()) {
            // Corta as pontas e retorna
            return optionalProcessInfo.get().trim();
        } else {
            return "0";
        }
    }

    // Metodo que retorna o uso de cpu de um processo
    // funciona igual o valor obtido para cpu ativa e em idle
    // mas nesse caso, eh necessario obter utime e stime do processo
    // realizar a soma de ambos e dividir pelo total de cpu em uso
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

            // O retorno eh em porcentagem
            return ((double) (cpuTimeProcessNewer - cpuTimeProcessInit)) / (totalCpuTimeNewer - totalCpuTimeInit) * 100;
        } else {
            return 0;
        }
    }

    //  Metodo auxiliar ao de cima, retorna um long com o valor de uso da cpu
    private long getTotalCpuUsage () {
        String[] mainCpuInfo = getMainCpuInfo();
        long totalCpuUsage = 0;

        for (int j = 1; j < (mainCpuInfo.length - 2); j++) {
            totalCpuUsage += Long.parseLong(mainCpuInfo[j]);
        }

        return totalCpuUsage;
    }

    //  Metodo auxiliar ao de cima, retorna a string com os dados de cpu
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

    // Metodo que pega a porcentagem de memoria que um proceso esta usando
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
            // O calculo retorna a porcentagem
            return (Double.parseDouble(getProcessPhysicalMemoryUsage()))/ (Double.parseDouble(totalRam.get().trim().split("\\s+")[1])) * 100.0;
        } else {
            return 0.0;
        }
    }

    // Metodo que retorna o valor dos executaveis de codigo do processo
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

    // Metodo que retorna o valor das bibliotecas de codigo do processo
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

    // Metodo que retorna o valor de paginas de heap do processo
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

    // Metodo que retorna as paginas de Stack do processo
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
