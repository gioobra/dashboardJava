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

    // O metodo abaixo coleta o nome do processador, presente dentro de /proc/cpuinfo
    public String getCpuName() {
        //Optional fui utilizado por conta do metodo .findFirst() de Stream
        Optional<String> optionalCpuName;

        // Encontra a primeira aparicao de name no caminho /proc/cpuinfo e salva a string em optionalCpuName
        try (Stream<String> cpuInfo = Files.lines(PROC_CPU_INFO)) {
            optionalCpuName = cpuInfo
                    .filter(content -> content.contains("name"))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Verifica se existe algum conteudo dentro de Optional (é um container, pode ter varias coisas dentro)
        if (optionalCpuName.isPresent()) {
            // O retorno de Optional.get() é um String, as pontas sao cortadas com trim()
            // e o resultado eh dividido atraves de split de acordo com a string "\\s+:\\s+"
            // \\s+ é um regex que identifica espaco e tabulacao
            // o resultado dessa operacao eh uma String[], na qual pegamos a posicao [1],
            // que sera o nome do processador
            // o String[] gerado sera do tipo: cpuName[0] = "name" e cpuName[1] = "{nome da cpu}"
            return optionalCpuName.get().trim().split("\\s+:\\s+")[1];
        } else {
            return "NULL";
        }
    }

    // Metodo utilizasdo para obter o numero de cores de um cpu com base no caminho /proc/cpuinfo
    public int getNumberOfCores() {
        Optional<String> optionalNumberOfCores;
        Optional<String> optionalNumberOfSiblings;

        // Acha o primeiro retorno que contenha "cpu cores"
        try (Stream<String> cpuInfo = Files.lines(PROC_CPU_INFO)) {
            optionalNumberOfCores = cpuInfo
                    .filter(content -> content.contains("cpu cores"))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Acha o primeiro retorno que contenha "siblings"
        try (Stream<String> cpuInfo = Files.lines(PROC_CPU_INFO)) {
            optionalNumberOfSiblings = cpuInfo
                    .filter(content -> content.contains("siblings"))
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (optionalNumberOfCores.isPresent()) {
            if (optionalNumberOfSiblings.isPresent()) {
                // Nesse caso, computadores com hyperthread contam ela como um nucleo,
                // esse fato pode causar inconsistencia, para isso, eh necessario pegar os valor
                // de siblings e cpu cores e devolver o maior, que representa a soma dos dois
                return Math.max(Integer.parseInt(optionalNumberOfCores.get().trim().split("\\s+:\\s+")[1]),
                        Integer.parseInt(optionalNumberOfSiblings.get().trim().split("\\s+:\\s+")[1]));
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    // Metodo que retorna o uso CPU em um intervalo de tempo
    public double getCpuInUse() {
        String[] totalCpuUse = getMainCpuInfo();
        long activeCpuOlder = 0;
        long totalCpuOlder = 0;

        // Comeca em um para cortar o "cpu" que vem junto e o -2 remove "guest" e "guest_nice", pois ja estao
        // inclusos em user[1 e nice[2]
        for (int i = 1; i < (totalCpuUse.length - 2); i++) {
            totalCpuOlder += Long.parseLong(totalCpuUse[i]);
            // Nao leva em consideracao os valores de idle e iowait, que nao sao ativos
            if ((i != 4) && (i != 5)) {
                activeCpuOlder += Long.parseLong(totalCpuUse[i]);
            }
        }

        // Espera 2 segundos
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Pega os dados novamente, ja que eles mudam nesse intervalo de tempo
        totalCpuUse = getMainCpuInfo();
        long activeCpuNewer = 0;
        long totalCpuNewer = 0;

        // Faz mesma coleta listada acima
        for (int i = 1; i < (totalCpuUse.length - 2); i++) {
            totalCpuNewer += Long.parseLong(totalCpuUse[i]);
            if ((i != 4) && (i != 5)) {
                activeCpuNewer += Long.parseLong(totalCpuUse[i]);
            }
        }

        // Realiza a operacao para devovler a porcentagem
        return ((double) (activeCpuNewer - activeCpuOlder)) / (totalCpuNewer - totalCpuOlder) * 100;
    }

    // Retorna a porcentagem que o cpu ficou ocioso
    public double getCpuInIdle() {
        String[] totalCpuUse = getMainCpuInfo();
        long idleCpuOlder = 0;
        long totalCpuOlder = 0;

        // totalCpuOlder continua igual o metodo anterior
        for (int i = 1; i < (totalCpuUse.length - 2); i++) {
            totalCpuOlder += Long.parseLong(totalCpuUse[i]);
            // Para pegar os valores ociosos, so se soma idle e iowait
            if ((i == 4) || (i == 5)) {
                idleCpuOlder += Long.parseLong(totalCpuUse[i]);
            }
        }

        // Espera 2 segundos
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Pega os dados do cpu novamente
        totalCpuUse = getMainCpuInfo();
        long idleCpuNewer = 0;
        long totalCpuNewer = 0;

        // Realiza o mesmo levantamento feito antes do sleep
        for (int i = 1; i < (totalCpuUse.length - 2); i++) {
            totalCpuNewer += Long.parseLong(totalCpuUse[i]);
            if ((i == 4) || (i == 5)) {
                idleCpuNewer += Long.parseLong(totalCpuUse[i]);
            }
        }

        // Retorna a porcentagem de tempo que a cpu ficou ociosa
        return ((double) (idleCpuNewer - idleCpuOlder)) / (totalCpuNewer - totalCpuOlder) * 100;
    }

    // Metodo auxilia que le a primeira linha de /proc/stat, que contem os dados de todas os cores da cpu somados
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
            // retorna um vetor de strings com os dados totais da cpu
            return optionalCpuInfo.get().trim().split("\\s+");
        } else {
            return error;
        }
    }
}