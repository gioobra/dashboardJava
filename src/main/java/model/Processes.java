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
    private List<Process> processes = new ArrayList<Process>();

    // Aqui, todos os objetos da classe Process são inicializados.
    public Processes () {
        initializeProcesses();
    }

    // Pega todos os processos que estão rodando no /proc, cria um objeto para cada e adiciona na lista de processos.
    private void initializeProcesses() {
        List<Path> procList = getAllProcessesPath();

        for (Path procPath : procList) {
            this.processes.add(new Process(procPath));
        }

        // Sleep de 1000 (1s) para fazer um intervalo no uso de CPU.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // Retorna o total de processos existentes
    public int getTotalProcesses() {
        return this.processes.size();
    }

    // Retorna o total de threads existentes
    public int getTotalThreads() {
        int numberOfThreads = 0;
        List<Path> processesPath = getAllProcessesPath();

        // forEach chama o metodo para contar processo por processo quantas threads tem
        for (Path processPath : processesPath) {
            numberOfThreads += threadCount(processPath);
        }

        return numberOfThreads;
    }

    //Retorna o numero de threads por processo
    private long threadCount(Path process) {
        long threads = 0;

        process = process.resolve("task");

        if(Files.notExists(process)) {
            return 0;
        }

        // Conta quantas aparicoes, toda thread eh marcado por um numero, o regex \\d+ identifica
        // se eh um numero e faz a soma
        try (Stream<Path> taskDir = Files.list(process)) {
            threads = taskDir
                    .filter(path -> path.getFileName().toString().matches("\\d+"))
                    .count();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return threads;
    }

    // Retorna uma lista de enderecos de todos os processos
    private List<Path> getAllProcessesPath() {
        List<Path> AllProcessPath = new ArrayList<>();

        //Todos os processos sao numeros, o regex \\d+ os filtra e coloca numa lista
        try (Stream<Path> process = Files.list(PROC)) {
            AllProcessPath = process
                    .filter(path -> path.getFileName().toString().matches("\\d+"))
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return AllProcessPath;
    }

    // Caso seja novamente todos os processos
    public List<Process> getAllProcesses () {
        return this.processes;
    }
}