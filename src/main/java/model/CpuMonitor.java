package model;
import java.sql.SQLOutput;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

public class CpuMonitor {

  Path cpuInformationPath;
  Path cpuUsagePath;
  Path proc;
  Path usernamePath;

  public CpuMonitor(String cpuInformationPath, String cpuUsagePath, String proc, String usernamePath) {
    this.cpuInformationPath = Paths.get(cpuInformationPath);
    this.cpuUsagePath = Paths.get(cpuUsagePath);
    this.proc = Paths.get(proc);
    this.usernamePath = Paths.get(usernamePath);
  }

  public double getCpuUsage(int coreNumber) {
    long totalCpuUsage1 = 0;
    long totalCpuUsage2 = 0;

    long totalCpuActive1 = 0;
    long totalCpuActive2 = 0;

    String[] cpuTotal = readCpuInfo(coreNumber);

    totalCpuUsage1 = getTotalCpuUsage(cpuTotal);
    totalCpuActive1 = getTotalCpuActive(cpuTotal);

    try {
      Thread.sleep(1000);
    } catch (Exception e) {
      e.printStackTrace();
    }

    cpuTotal = readCpuInfo(coreNumber);

    totalCpuUsage2 = getTotalCpuUsage(cpuTotal);
    totalCpuActive2 = getTotalCpuActive(cpuTotal);

    return ((double) (totalCpuActive2 - totalCpuActive1) / (totalCpuUsage2 - totalCpuUsage1)) * 100;
  }

  public String[] readCpuInfo(int coreNumber) {
    String cpuData = new String();

    try {
      List<String> cpuUsageData = Files.readAllLines(cpuUsagePath);
      cpuData = cpuUsageData.get(coreNumber);
    } catch (Exception e) {
      e.printStackTrace();
    }

    String[] cpuTotal = cpuData.split("\\s+");

    return cpuTotal;
  }

  public long getTotalCpuUsage(String[] cpuData) {
    long sum = 0;

    //Considerando: user[1], nice[2], system[3], idle[4] iowait[5], irq[6], softirq[7], steal[8]
    //guest[9] e guest_nice[10] ja sao contemplados pelo user[1] e nice[2]
    for (int i = 1; i < cpuData.length - 2; i++) {
      sum += Long.parseLong(cpuData[i]);
    }

    return sum;
  }

  public long getTotalCpuActive(String[] cpuData) {
    long sum = 0;

    //Considerando: user[1], nice[2], system[3], irq[6], softirq[7], steal[8]
    for (int i = 1; i < cpuData.length; i++) {
      if((i != 4) && (i != 5) && (i < 9)) {
        sum += Long.parseLong(cpuData[i]);
      }
    }

    return sum;
  }

  public int getNumberOfCores() {
    int numberOfCores = 0;
    int numberOfSiblings = 0;

    try {
      List<String> cpuInfo = Files.readAllLines(cpuInformationPath);

      for (int i = 0; i < cpuInfo.size(); i++) {
        //System.out.println(cpuInfo.get(i));
        if(cpuInfo.get(i).contains("cpu cores")) {
          numberOfCores = Integer.parseInt(cpuInfo.get(i).replaceAll("^.*cpu cores\\s+:\\s+", "").trim());
        }
        if(cpuInfo.get(i).contains("siblings")) {
          numberOfSiblings = Integer.parseInt(cpuInfo.get(i).replaceAll("^.*siblings\\s+:\\s+", "").trim());
          break;
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
    }

    return numberOfCores > numberOfSiblings ? numberOfCores : numberOfSiblings;
  }

  public String getCpuName() {
    String cpuModel = new String();

    try {
      List<String> cpuInfo = Files.readAllLines(cpuInformationPath);

      for (int i = 0; i < cpuInfo.size(); i++) {
        if(cpuInfo.get(i).contains("model name")) {
          cpuModel = cpuInfo.get(i).replaceAll("^.*model name\\s+:\\s+", "").trim();
          break;
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
    }

    return cpuModel;
  }

  public double getCpuIdle(int coreNumber) {
    long cpuInIdle1 = 0;
    long cpuInIdle2 = 0;

    long totalCpuUsage1 = 0;
    long totalCpuUsage2 = 0;

    String[] cpuTotal = readCpuInfo(coreNumber);

    totalCpuUsage1 = getTotalCpuUsage(cpuTotal);
    cpuInIdle1 = getTotalCpuIdle(cpuTotal);

    try {
      Thread.sleep(1000);
    } catch (Exception e) {
      e.printStackTrace();
    }

    cpuTotal = readCpuInfo(coreNumber);

    totalCpuUsage2 = getTotalCpuUsage(cpuTotal);
    cpuInIdle2 = getTotalCpuIdle(cpuTotal);

    return ((double) (cpuInIdle2 - cpuInIdle1)/(totalCpuUsage2 - totalCpuUsage1)) * 100;
  }

  public long getTotalCpuIdle(String[] cpuTotal) {
    //Trechos em idle: idle[4] e iowait[5]
    long idle = Long.parseLong(cpuTotal[4]);
    idle += Long.parseLong(cpuTotal[5]);

    return idle;
  }

  public long getTotalProcesses() {
    long numberOfProcess = 0;

    try (Stream<Path> processes = Files.list(proc)) {
      numberOfProcess = processes
              .filter(path -> path.getFileName().toString().matches("\\d+"))
              .count();
    } catch (Exception e) {
      e.printStackTrace();
    }

    return numberOfProcess;
  }

  public long getTotalThreads() {
    long numberOfThreads = 0;
    List<Path> processesPath = new ArrayList<>();

    try (Stream<Path> processes = Files.list(proc)) {
      processesPath =
              processes
                      .filter(path -> path.getFileName().toString().matches("\\d+"))
                      .collect(Collectors.toList());
    } catch (Exception e) {
      e.printStackTrace();
    }

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
    } catch (Exception e) {
      e.printStackTrace();
    }

    return threads;
  }

  private List<Path> getAllProcessPath() {
    List<Path> AllProcessPath = new ArrayList<>();

    try (Stream<Path> process = Files.list(proc)) {
      AllProcessPath = process
              .filter(path -> path.getFileName().toString().matches("\\d+"))
              .toList();
    } catch (Exception e) {
      e.printStackTrace();
    }

    return AllProcessPath;
  }

  public String getProcessUser(String process) {
    Path processStatus = Paths.get(process).resolve("status");
    String uid = new String();
    String user = new String();
    List<String> uidInfo = new ArrayList<>();

    try (Stream<String> readLines = Files.lines(processStatus)){
      uidInfo = readLines
              .filter(line -> line.startsWith("Uid:"))
              .collect(Collectors.toList());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    String[] uidAuxiliar = uidInfo.getFirst().split("\\s+");
    uid = uidAuxiliar[1];
    user = getUser(uid);

    return user;
  }

  private String getUser (String uid) {
    Path etcPasswd = Paths.get("/etc/passwd");
    List<String> uidInfo = new ArrayList<>();

    try (Stream<String> passwd = Files.lines(etcPasswd)) {
      uidInfo = passwd
              .filter(uidLine -> uidLine.contains(uid))
              .collect(Collectors.toList());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    String[] user = uidInfo.getFirst().split(":");

    return user[0];
  }

  public void printProcessesAndUsers () {
    List<Path> allProcessesPath = getAllProcessPath();
    List<String> pid = new ArrayList<>();

    for (Path processPath : allProcessesPath) {
      System.out.println("Process: " + processPath.getFileName()
              + " User: " + getProcessUser(processPath.toString())
              + " Threads: " + getProcessNumberOfThreads(processPath.toString()));
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

    String[] numberOfThreads = threadInfo.getFirst().split("\\s+");

    return numberOfThreads[1];
  }

  public String getProcessInformation (Path processPath) {
    String processDetail;
    processDetail = "";
    return processDetail;
  }

  public static void main(String[] args) {
    CpuMonitor cpu = new CpuMonitor("/proc/cpuinfo","/proc/stat","/proc","/etc/passwd");

    //Veraificando o numero de nucleos do processador
    //System.out.println(cpu.getNumberOfCores());

    //for (int i = 0; i < cpu.getNumberOfCores() + 1; i++) {
    //System.out.println("Uso da CPU:\n" + cpu.getCpuUsage(i));
    //}

    //for (int i = 0; i < cpu.getNumberOfCores() + 1; i++) {
    //System.out.println("Idle da CPU:\n" + cpu.getCpuIdle(i));
    //}

    //Verificando o nome do processador
    //System.out.println(cpu.getCpuName());

    //Numero de processos
    //System.out.println(cpu.getTotalProcesses());

    //Numero de threads
    //System.out.println(cpu.getTotalThreads());

    //Path de todos os processos ativos:
    //System.out.println(cpu.getProcessUser("/proc/740"));
    //cpu.getUser();
    cpu.printProcessesAndUsers();
  }
}
