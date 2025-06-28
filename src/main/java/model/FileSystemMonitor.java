package model;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileSystemMonitor {

    private static final Path MOUNTS_FILE = Paths.get("/proc/mounts");
    private static final double BYTES_PER_GB = 1024.0 * 1024.0 * 1024.0;

    /**
     * Retorna uma lista de vetores de String onde cada vetor representa uma partição e contém suas informações formatadas.
     * A ordem dos dados no vetor é:
     * [Ponto de Montagem, Dispositivo, Tipo, Tamanho Total (GB), Espaço Usado (GB), Espaço Livre (GB), Uso %]
     */
    public List<String[]> getPartitionsInfo() {
        return getMountedFileStores().stream()
                .map(this::processFileStore)
                .collect(Collectors.toList());
    }

    /**
     * Busca os FileStores para partições físicas montadas no sistema.
     * Retorna uma lista de objetos FileStore.
     */
    private List<FileStore> getMountedFileStores() {
        List<String> allowedTypes = List.of("ext4", "btrfs", "xfs", "vfat", "ntfs", "fuseblk"); //Tipos mais comuns
        List<FileStore> stores = new ArrayList<>();

        try (Stream<String> lines = Files.lines(MOUNTS_FILE)) {
            lines.forEach(line -> {
                String[] parts = line.split("\\s+");
                if (parts.length >= 3 && parts[0].startsWith("/dev/")) {
                    if (allowedTypes.contains(parts[2])) {
                        try {
                            stores.add(Files.getFileStore(Paths.get(parts[1])));
                        } catch (IOException e) {
                            // Ignora partições inacessíveis
                        }
                    }
                }
            });
        } catch (IOException e) {
            System.err.println("Falha ao ler o arquivo /proc/mounts: " + e.getMessage());
        }
        return stores;
    }

    // Processa um único objeto FileStore, extraindo e formatando suas informaçõe em um vetor de Strings.
    private String[] processFileStore(FileStore store) {
        try {
            long totalSpace = store.getTotalSpace();
            long usableSpace = store.getUsableSpace();
            long usedSpace = totalSpace - usableSpace;

            // Formatação dos dados
            String totalGB = bytesToGBString(totalSpace);
            String usedGB = bytesToGBString(usedSpace);
            String usableGB = bytesToGBString(usableSpace);
            String usagePercentage = formatPercentage(usedSpace, totalSpace);

            String fullString = store.toString(); // Exemplo: / (/dev/sda2)
            String mountPoint = fullString.substring(0, fullString.indexOf(" (")); // De acordo como o exemplo, "/"
            String device = fullString.substring(fullString.indexOf(" (") + 2, fullString.length() - 1); // "/dev/sda2"

            return new String[]{
                    mountPoint,
                    device,
                    store.type(), // Metodo do proprio FileStore
                    totalGB,
                    usedGB,
                    usableGB,
                    usagePercentage
            };
        } catch (IOException e) {
            // Se houver erro ao ler um FileStore, retorna um vetor indicando o erro.
            return new String[]{"Erro", e.getMessage(), "", "", "", "", ""};
        }
    }

    // Converte um valor em bytes para uma String formatada em Gigabytes (GB).
    private String bytesToGBString(long bytes) {
        double gb = bytes / BYTES_PER_GB; // Definida no começa do código como estática
        return String.format("%.2f GB", gb);
    }

    // Calcula e formata o percentual de uso.
    private String formatPercentage(long used, long total) {
        if (total == 0) {
            return "0.0%";
        }
        double percentage = (double) used / total * 100.0;
        return new DecimalFormat("#.0'%'").format(percentage);
    }


    // Testando a classe
    public static void main(String[] args) {
        System.out.println("--- Testando a nova implementação do FileSystemMonitor ---");
        FileSystemMonitor monitor = new FileSystemMonitor();
        List<String[]> partitionsData = monitor.getPartitionsInfo();

        if (partitionsData.isEmpty()) {
            System.out.println("Nenhuma partição encontrada ou erro na leitura.");
            return;
        }

        // Itera sobre a lista e imprime cada vetor de string de forma formatada
        for (String[] partition : partitionsData) {
            for(String device : partition) {
                System.out.println(device);
            }
            System.out.println();
        }
    }
}