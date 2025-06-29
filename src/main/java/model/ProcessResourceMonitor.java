package model;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Responsável por monitorar os recursos de E/S de um processo específico.
public class ProcessResourceMonitor {

    private final int pid;

    public ProcessResourceMonitor(int pid) {
        this.pid = pid;
    }

    /**
     * Retorna uma lista de vetores de String. Cada vetor representa um recurso aberto.
     * Ordem no vetor: [0] = Descritor, [1] = Tipo, [2] = Detalhes
     */
    public List<String[]> getAllOpenResources() {
        List<String[]> resources = new ArrayList<>();
        Path fdDirectory = Paths.get("/proc/" + this.pid + "/fd");

        if (!Files.isDirectory(fdDirectory)) {
            return Collections.emptyList(); // Caso esteja vazia, ou de fato não tem nada, ou não pôde ser lido.
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(fdDirectory)) {
            for (Path descriptorPath : stream) {
                String[] infoArray = processFileDescriptor(descriptorPath);
                resources.add(infoArray);
            }
        } catch (IOException e) {
            System.err.println("Não foi possível ler o diretório de descritores para o PID " + this.pid + ": " + e.getMessage());
        }

        return resources;
    }

    // Processa um único descritor de arquivo e o transforma em um vetor de String.
    private String[] processFileDescriptor(Path descriptorPath) {
        String descriptor = descriptorPath.getFileName().toString();
        String type = "Desconhecido";
        String details = "";

        try {
            String target = Files.readSymbolicLink(descriptorPath).toString();

            if (target.startsWith("/")) {
                type = "Arquivo";
                details = target;
            } else if (target.startsWith("pipe:")) {
                type = "Pipe";
                details = target;
            } else if (target.startsWith("socket:")) {
                type = "Socket";
                details = target;
            } else if (target.startsWith("anon_inode:")) {
                type = "Recurso Anônimo";
                details = target;
            } else {
                details = target;
            }
        } catch (IOException e) {
            details = "Erro ao ler o link: " + e.getMessage();
        }

        // Em vez de criar um objeto ResourceInfo, criamos e retornamos um vetor de String.
        return new String[]{descriptor, type, details};
    }

    public static void main(String[] args) {
        int testPid = 2854;
        System.out.println("--- Testando Recursos Abertos para o PID: " + testPid + " ---");

        ProcessResourceMonitor monitor = new ProcessResourceMonitor(testPid);
        List<String[]> resources = monitor.getAllOpenResources();

        if (resources.isEmpty()) {
            System.out.println("Nenhum recurso encontrado ou processo inacessível.");
            return;
        }

        for (String[] resource : resources) {
            System.out.println("Descritor: " + resource[0] + "\n\tTipo de Recurso: " + resource[1] + "\n\t\tDetalhes: " + resource[2] + "\n");
        }
    }
}