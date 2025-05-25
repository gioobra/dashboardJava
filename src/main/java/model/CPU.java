package model;

import java.util.Optional;
import java.util.stream.Stream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

public class CPU {

    public static final Path PROC_CPU_INFO = Paths.get("/proc/cpuinfo");
    public static final Path PROC_STAT = Paths.get("/proc/stat");

    public CPU() {

    }

    public static void main(String[] args) {
        CPU cpu = new CPU();
        System.out.println("CPU: " + cpu.getCpuName());
        System.out.println("Number of Cores: " + cpu.getNumberOfCores());
        System.out.println("CPU usage: " + cpu.getCpuInUse() + "%");
        System.out.println("CPU idle: " + cpu.getCpuInIdle() + "%");
    }

    public String getCpuName() {
        Optional<String> optionalCpuName;

        try (Stream<String> cpuInfo = Files.lines(PROC_CPU_INFO)) {
            optionalCpuName = cpuInfo
                    .filter(content -> content.contains("name"))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (optionalCpuName.isPresent()) {
            return optionalCpuName.get().trim().split("\\s+:\\s+")[1];
        } else {
            return "NULL";
        }
    }

    public int getNumberOfCores() {
        Optional<String> optionalNumberOfCores;
        Optional<String> optionalNumberOfSiblings;

        try (Stream<String> cpuInfo = Files.lines(PROC_CPU_INFO)) {
            optionalNumberOfCores = cpuInfo
                    .filter(content -> content.contains("cpu cores"))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (Stream<String> cpuInfo = Files.lines(PROC_CPU_INFO)) {
            optionalNumberOfSiblings = cpuInfo
                    .filter(content -> content.contains("siblings"))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (optionalNumberOfCores.isPresent()) {
            if (optionalNumberOfSiblings.isPresent()) {
                return Math.max(Integer.parseInt(optionalNumberOfCores.get().trim().split("\\s+:\\s+")[1]),
                        Integer.parseInt(optionalNumberOfSiblings.get().trim().split("\\s+:\\s+")[1]));
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    public double getCpuInUse() {
        String[] totalCpuUse = getMainCpuInfo();
        long activeCpuOlder = 0;
        long totalCpuOlder = 0;

        for (int i = 1; i < (totalCpuUse.length - 2); i++) {
            totalCpuOlder += Long.parseLong(totalCpuUse[i]);
            if ((i != 4) && (i != 5)) {
                activeCpuOlder += Long.parseLong(totalCpuUse[i]);
            }
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        totalCpuUse = getMainCpuInfo();
        long activeCpuNewer = 0;
        long totalCpuNewer = 0;

        for (int i = 1; i < (totalCpuUse.length - 2); i++) {
            totalCpuNewer += Long.parseLong(totalCpuUse[i]);
            if ((i != 4) && (i != 5)) {
                activeCpuNewer += Long.parseLong(totalCpuUse[i]);
            }
        }

        return ((double) (activeCpuNewer - activeCpuOlder)) / (totalCpuNewer - totalCpuOlder) * 100;
    }

    public double getCpuInIdle() {
        String[] totalCpuUse = getMainCpuInfo();
        long idleCpuOlder = 0;
        long totalCpuOlder = 0;

        for (int i = 1; i < (totalCpuUse.length - 2); i++) {
            totalCpuOlder += Long.parseLong(totalCpuUse[i]);
            if ((i == 4) || (i == 5)) {
                idleCpuOlder += Long.parseLong(totalCpuUse[i]);
            }
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        totalCpuUse = getMainCpuInfo();
        long idleCpuNewer = 0;
        long totalCpuNewer = 0;

        for (int i = 1; i < (totalCpuUse.length - 2); i++) {
            totalCpuNewer += Long.parseLong(totalCpuUse[i]);
            if ((i == 4) || (i == 5)) {
                idleCpuNewer += Long.parseLong(totalCpuUse[i]);
            }
        }

        return ((double) (idleCpuNewer - idleCpuOlder)) / (totalCpuNewer - totalCpuOlder) * 100;
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
}