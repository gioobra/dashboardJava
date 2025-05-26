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

    // Metodo que retorna o total de memoria em gigabytes
    public double getTotalMemory() {
        Optional<String> totalMemory;

        // Coleta a primeira aparicao de MemTotal
        try (Stream<String> memory = Files.lines(PROC_MEMINFO)) {
            totalMemory = memory
                    .filter(content -> content.startsWith("MemTotal:"))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(totalMemory.isPresent()) {
            // Transforma o resultado em gigabytes dividindo ele por 1024x1024
            return Long.parseLong(totalMemory.get().trim().split("\\s+")[1]) / (1024.0 * 1024.0);
        } else {
            return 0;
        }
    }

    // Metodo que retorna o total de memoria livre em gigabytes
    public double getFreeMemory() {
        Optional<String> freeMemory;

        // Coleta a primeira aparicao de MemAvailable
        try (Stream<String> memory = Files.lines(PROC_MEMINFO)) {
            freeMemory = memory
                    .filter(content -> content.startsWith("MemAvailable:"))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(freeMemory.isPresent()) {
            // Transforma o resultado em gigabytes dividindo ele por 1024x1024
            return Long.parseLong(freeMemory.get().trim().split("\\s+")[1]) / (1024.0 * 1024.0);
        } else {
            return 0;
        }
    }

    // Calcula a diferenca entre a memoria total e a livre e devolve quanto esta sendo usada
    public double getMemUsed() {
        return getTotalMemory() - getFreeMemory();
    }

    // Retorna o total de memoria swap em gigabytes
    public double getTotalSwapMemory() {
        Optional<String> swap;

        // Coleta a primeira aparicao de SwapTotal
        try (Stream<String> memory = Files.lines(PROC_MEMINFO)) {
            swap = memory
                    .filter(content -> content.startsWith("SwapTotal:"))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(swap.isPresent()) {
            // Transforma o resultado em gigabytes dividindo ele por 1024x1024
            return Long.parseLong(swap.get().trim().split("\\s+")[1]) / (1024.0 * 1024.0);
        } else {
            return 0;
        }
    }

    // Retorna o total de memoria swap livre em gigabytes
    public double getFreeSwapMemory() {
        Optional<String> freeSwap;

        // Coleta a primeira aparicao de SwapFree
        try (Stream<String> memory = Files.lines(PROC_MEMINFO)) {
            freeSwap = memory
                    .filter(content -> content.startsWith("SwapFree:"))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(freeSwap.isPresent()) {
            // Transforma o resultado em gigabytes dividindo ele por 1024x1024
            return Long.parseLong(freeSwap.get().trim().split("\\s+")[1]) / (1024.0 * 1024.0);
        } else {
            return 0;
        }
    }

    // Retorna o total de memoria swap usada
    public double getSwapUsed() {
        return getTotalSwapMemory() - getFreeSwapMemory();
    }

    // Retorna o total de memoria usada em porcentagem
    public double getMemUsedPercentage() {
        return (getMemUsed() / getTotalMemory()) * 100.0;
    }

    // Retorna o total de memoria swap usada em porcentagem
    public double getSwapUsedPercentage() {
        return (getSwapUsed() / getTotalSwapMemory()) * 100.0;
    }
}