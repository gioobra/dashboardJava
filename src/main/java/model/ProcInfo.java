package model;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.io.IOException;

public class ProcInfo {

  Path cpuUsagePath;
  Path cpuInfoPath;

  public ProcInfo(String cpuUsagePath, String cpuInfoPath) {
    this.cpuUsagePath = Paths.get(cpuUsagePath);
    this.cpuInfoPath = Paths.get(cpuInfoPath);
  }

  public double CpuUsage() throws IOException
  {
    long total_1 = 0;
    long active_1 = 0;

    long total_2 = 0;
    long active_2 = 0;

    List<String> cpuUsageData = Files.readAllLines(cpuUsagePath);

    String cpu = cpuUsageData.get(0);
    String cpuTotal[] = cpu.split("\\s+");

    for (int i = 1; i < cpuTotal.length; i++) {
      total_1 += Long.parseLong(cpuTotal[i]);
      if((i != 3) && (i != 4) && (i < 8)) {
        active_1 += Long.parseLong(cpuTotal[i]);
      }
    }

    try {
      Thread.sleep(500);
    } catch (Exception e) {
      Thread.currentThread().interrupt();
    }

    cpuUsageData = Files.readAllLines(cpuUsagePath);
    cpu = cpuUsageData.get(0);
    String cpuTotal_2[] = cpu.split("\\s+");

    for (int j = 1; j < cpuTotal_2.length; j++) {
      total_2 += Long.parseLong(cpuTotal_2[j]);
      if((j != 3) && (j != 4) && (j < 8)) {
        active_2 += Long.parseLong(cpuTotal_2[j]);
      }
    }

    double var_total = total_2 - total_1;
    double var_active = active_2 - active_1;

    //System.out.println("CPU total 1: " + total_1 + "\n" + "CPU Active 1: " + active_1 + "\n");
    //System.out.println("CPU total 2: " + total_2 + "\n" + "CPU Active 2: " + active_2 + "\n");

    return (double) (var_active/var_total) * 100;
  }

  public List<String> CpuInformation() throws IOException
  {
    return Files.readAllLines(cpuInfoPath);
  }

  public static void main(String[] args) {
    ProcInfo processador = new ProcInfo("/proc/stat","/proc/cpuinfo");
    try {
      for (int i = 0; i < 20; i++) {
        System.out.println(processador.CpuUsage());
      }
      System.out.println("Uso da CPU:\n"+processador.CpuUsage());
      //System.out.println("\nCpu info:\n" + processador.CpuInformation());
    } catch (Exception e) {
      System.out.println("Erro: " + e.getMessage());
    }
  }
}
