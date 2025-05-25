package model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

public class Memory {

    public static final Path PROC_MEMINFO = Paths.get("/proc/meminfo");

    public Memory () {

    }

    public static void main(String[] args) {
        Memory memory = new Memory();
        System.out.println("Total Memory: " + memory.getTotalMemory() + " GB");
        System.out.println("Free Memory: " + memory.getFreeMemory() + " GB");
        System.out.println("Used Memory: " + memory.getMemUsed() + " GB");

        System.out.println("Buffers: " + memory.getBuffers() + " KB");
        System.out.println("Cached: " + memory.getCache() + " MB");

        System.out.println("Total Swap Memory: " + memory.getTotalSwapMemory() + " GB");
        System.out.println("Free Swap Memory: " + memory.getFreeSwapMemory() + " GB");
        System.out.println("Used Swap Memory: " + memory.getSwapUsed() + " GB");

        System.out.println("Memory Usage Percentage: " + memory.getMemUsedPercentage() + " %");
        System.out.println("Swap Usage Percentage: " + memory.getSwapUsedPercentage() + " %");
    }

    public double getTotalMemory() {
        Optional<String> totalMemory;

        try (Stream<String> memory = Files.lines(PROC_MEMINFO)) {
            totalMemory = memory
                    .filter(content -> content.startsWith("MemTotal:"))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(totalMemory.isPresent()) {
            return Long.parseLong(totalMemory.get().trim().split("\\s+")[1]) / (1024.0 * 1024.0);
        } else {
            return 0;
        }
    }

    public double getFreeMemory() {
        Optional<String> freeMemory;

        try (Stream<String> memory = Files.lines(PROC_MEMINFO)) {
            freeMemory = memory
                    .filter(content -> content.startsWith("MemFree:"))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(freeMemory.isPresent()) {
            return Long.parseLong(freeMemory.get().trim().split("\\s+")[1]) / (1024.0 * 1024.0);
        } else {
            return 0;
        }
    }

    public double getMemUsed() {
        return getTotalMemory() - getFreeMemory();
    }

    public long getBuffers() {
        Optional<String> buffers;

        try (Stream<String> memory = Files.lines(PROC_MEMINFO)) {
            buffers = memory
                    .filter(content -> content.startsWith("Buffers:"))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(buffers.isPresent()) {
            return Long.parseLong(buffers.get().trim().split("\\s+")[1]);
        } else {
            return 0;
        }
    }

    public double getCache() {
        Optional<String> cache;

        try (Stream<String> memory = Files.lines(PROC_MEMINFO)) {
            cache = memory
                    .filter(content -> content.startsWith("Cached:"))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(cache.isPresent()) {
            return Long.parseLong(cache.get().trim().split("\\s+")[1]) / 1024.0;
        } else {
            return 0;
        }
    }

    public double getTotalSwapMemory() {
        Optional<String> swap;

        try (Stream<String> memory = Files.lines(PROC_MEMINFO)) {
            swap = memory
                    .filter(content -> content.startsWith("SwapTotal:"))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(swap.isPresent()) {
            return Long.parseLong(swap.get().trim().split("\\s+")[1]) / (1024.0 * 1024.0);
        } else {
            return 0;
        }
    }

    public double getFreeSwapMemory() {
        Optional<String> freeSwap;

        try (Stream<String> memory = Files.lines(PROC_MEMINFO)) {
            freeSwap = memory
                    .filter(content -> content.startsWith("SwapFree:"))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(freeSwap.isPresent()) {
            return Long.parseLong(freeSwap.get().trim().split("\\s+")[1]) / (1024.0 * 1024.0);
        } else {
            return 0;
        }
    }

    public double getSwapUsed() {
        return getTotalSwapMemory() - getFreeSwapMemory();
    }

    public double getMemUsedPercentage() {
        return (getMemUsed() / getTotalMemory()) * 100.0;
    }

    public double getSwapUsedPercentage() {
        return (getSwapUsed() / getTotalSwapMemory()) * 100.0;
    }
}