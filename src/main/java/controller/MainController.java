package controller;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.Memory;
import model.CPU;
import javafx.scene.chart.XYChart;
import model.Process;
import model.Processes;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainController {
    @FXML private ProgressBar barraUsoCPU;
    @FXML private Label valorCPU;
    @FXML private ProgressBar barraUsoMemoria;
    @FXML private Label valorMemoria;
    @FXML private Label valorNProcessos;
    @FXML private Label valorThreads;
    @FXML private ListView<String> CPUinfo;
    @FXML private TableView<Map<String, Object>> tabelaProcessos;
    @FXML private Button detalhes;
    @FXML private LineChart<Number, Number> graficoCPU;
    @FXML private PieChart graficoRAM;
    @FXML private TableColumn<Map<String, Object>, String> colunaNome;
    @FXML private TableColumn<Map<String, Object>, Number> colunaPid;
    @FXML private TableColumn<Map<String, Object>, String> colunaUser;
    @FXML private TableColumn<Map<String, Object>, Number> colunaCpu;
    @FXML private TableColumn<Map<String, Object>, Number> colunaMemoria;
    @FXML private LineChart<Number, Number> graficoSwap;

    private CPU cpu;
    private Processes processes;
    private Memory memory;
    private XYChart.Series<Number, Number> valoresCPU;
    private XYChart.Series<Number, Number> valoresCPUIdle;
    private int tempo = 0;
    private static final int MAX_PONTOS_GRAFICO_CPU = 600;
    private ScheduledService<SystemDataSnapshot> updateService;
    private XYChart.Series<Number, Number> valoresSwap;

    private static class SystemDataSnapshot {
        double cpuUsage;
        double cpuIdle;
        long numProcesses;
        long numThreads;
        double memoryUsedPercent;
        double memoryUsedGB;
        double memoryFreeGB;
        double swapUsagePercent;
        ObservableList<Map<String, Object>> processList;
    }

    @FXML
    private void initialize() {
        this.cpu = new CPU();
        this.memory = new Memory();
        this.processes = new Processes();

        setupStaticInfo();
        setupCpuChart();
        setupProcessTableColumns();
        setupMemoryPieChart();
        setupSwapChart();

        iniciarAtualizacoesPeriodicas();
    }

    /* Metodo para colocar as info da CPU */
    private void setupStaticInfo() {
        CPUinfo.getItems().clear();
        CPUinfo.getItems().add("CPU: " + this.cpu.getCpuName());
        CPUinfo.getItems().add("Cores: " + this.cpu.getNumberOfCores());
    }

    /* Metodo para configurar o grafico da CPU */
    private void setupCpuChart() {
        // Essas primeiras linhas sao para o grafico de uso
        valoresCPU = new XYChart.Series<>();
        valoresCPU.setName("Uso CPU (%)");
        graficoCPU.getData().add(valoresCPU);
        graficoCPU.setTitle("Uso CPU");

        // Essas sao para o grafico de idle
        valoresCPUIdle = new XYChart.Series<>();
        valoresCPUIdle.setName("CPU Idle (%)");
        graficoCPU.getData().add(valoresCPUIdle);

        graficoCPU.setAnimated(false);
        graficoCPU.getXAxis().setLabel("Tempo (s)");
        graficoCPU.getYAxis().setLabel("Uso/Idle (%)");
    }

    /* Metodo para fazer a tabela de processos, cada cellData pega o Map da linha atual,
    entao pega o elemento especifico (nome, pid, etc...) e testa se eh diferente de null */
    private void setupProcessTableColumns() {
        colunaNome.setCellValueFactory(cellData ->
                new SimpleStringProperty((String) cellData.getValue().get("nome"))
        );
        colunaPid.setCellValueFactory(cellData -> {
            Object pidValue = cellData.getValue().get("pid");
            return new SimpleLongProperty(pidValue != null ? ((Number) pidValue).longValue() : 0);
        });
        colunaUser.setCellValueFactory(cellData ->
                new SimpleStringProperty((String) cellData.getValue().get("user"))
        );
        colunaCpu.setCellValueFactory(cellData -> {
            Object cpuValue = cellData.getValue().get("cpu");
            return new SimpleDoubleProperty(cpuValue != null ? ((Number) cpuValue).doubleValue() : 0.0);
        });
        colunaMemoria.setCellValueFactory(cellData -> {
            Object memValue = cellData.getValue().get("memoria");
            return new SimpleDoubleProperty(memValue != null ? ((Number) memValue).doubleValue() : 0.0);
        });
    }

    /* Metodo para fazer o grafico pizza de memoria */
    private void setupMemoryPieChart() {
        graficoRAM.setTitle("Uso de RAM (" + String.format("%.2f", memory.getTotalMemory()) + "GB)");
        graficoRAM.setLegendVisible(true);
        graficoRAM.setLabelsVisible(true);
        graficoRAM.setLegendSide(Side.TOP);
        graficoRAM.setAnimated(false);
        ObservableList<PieChart.Data> initialData = FXCollections.observableArrayList(
                new PieChart.Data("Carregando...", 1) // Faz um placeholder antes dos dados reais
        );
        graficoRAM.setData(initialData);
    }

    /* Metodo para fazer o grafico da memoria swap */
    private void setupSwapChart() {
        valoresSwap = new XYChart.Series<>();
        valoresSwap.setName("Swap (%)");
        graficoSwap.setTitle("Uso Swap (" + String.format("%.2f", memory.getTotalSwapMemory()) + "GB)");
        graficoSwap.getData().add(valoresSwap);
    }

    /* Metodo para colocar os processos em paralelos/thread, cria uma instancia de ScheduledService */
    private void iniciarAtualizacoesPeriodicas() {
        updateService = new ScheduledService<SystemDataSnapshot>() {
            @Override
            protected Task<SystemDataSnapshot> createTask() { // Task que vai ser executada repetidamente em uma thread
                return new Task<>() {
                    @Override
                    protected SystemDataSnapshot call() throws Exception {

                        SystemDataSnapshot snapshot = new SystemDataSnapshot(); // Objeto que guarda todos os dados

                        snapshot.cpuUsage = cpu.getCpuInUse();
                        snapshot.cpuIdle = cpu.getCpuInIdle();

                        snapshot.numProcesses = processes.getTotalProcesses();
                        snapshot.numThreads = processes.getTotalThreads();

                        snapshot.memoryUsedPercent = memory.getMemUsedPercentage();
                        snapshot.memoryUsedGB = memory.getMemUsed();
                        snapshot.memoryFreeGB = memory.getFreeMemory();

                        snapshot.swapUsagePercent = memory.getSwapUsedPercentage();

                        // Prepara a lista para os dados detalhados dos processos
                        ObservableList<Map<String, Object>> processDataList = FXCollections.observableArrayList();

                        processes = new Processes();
                        List<Process> todosOsProcessos = processes.getAllProcesses();

                        for (Process process : todosOsProcessos) {
                            Map<String, Object> rowData = new HashMap<>(); // Hashmap que vai guardar os valores
                            try {
                                rowData.put("nome", process.getProcessName());
                                rowData.put("pid", process.getProcessID());
                                rowData.put("user", process.getProcessUser());
                                String cpuProc = String.format(Locale.US, "%.2f", process.getProcessCpuUsage());
                                rowData.put("cpu", Double.parseDouble(cpuProc));
                                String memProc = String.format(Locale.US,"%.2f", process.getProcessMemoryPercentage());
                                rowData.put("memoria", Double.parseDouble(memProc));
                                processDataList.add(rowData);
                            } catch (Exception e) {
                                throw new RuntimeException(e); // Lanca uma excecao se uma task der errado
                            }
                        }
                        snapshot.processList = processDataList;
                        return snapshot;
                    }
                };
            }
        };

        updateService.setPeriod(Duration.seconds(5)); // Atualizar a cada 5 segundos
        updateService.setRestartOnFailure(true); // Reinicia se falha alguma task

        // Define a acao a ser executada no JavaFX apos a task ser concluida
        updateService.setOnSucceeded(event -> {
            SystemDataSnapshot snapshot = updateService.getValue(); // Pega os dados coletados

            long tempoNoGrafico = (long) this.tempo * 5; // Atualiza a barra e label de CPU

            // Atualiza a barra e label de CPU
            barraUsoCPU.setProgress(snapshot.cpuUsage / 100.0);
            valorCPU.setText(String.format("CPU: %.2f %%", snapshot.cpuUsage));

            // Atualiza o gráfico de CPU
            adicionarPontoGraficoCPU(tempoNoGrafico, snapshot.cpuUsage, snapshot.cpuIdle);

            // Atualiza labels de processos e threads
            valorNProcessos.setText(String.format("Número de Processos: %d", snapshot.numProcesses));
            valorThreads.setText(String.format("Threads: %d", snapshot.numThreads));

            // Atualiza a barra e label de memoria
            barraUsoMemoria.setProgress(snapshot.memoryUsedPercent / 100.0);
            valorMemoria.setText(String.format("Memoria: %.2f %%", snapshot.memoryUsedPercent));

            // Prepara e atualiza os dados do grafico pizza de RAM
            ObservableList<PieChart.Data> ramPieChartData = FXCollections.observableArrayList(
                    new PieChart.Data(String.format("Usada: %.2f GB", snapshot.memoryUsedGB), snapshot.memoryUsedGB),
                    new PieChart.Data(String.format("Livre: %.2f GB", snapshot.memoryFreeGB), snapshot.memoryFreeGB)
            );
            graficoRAM.setData(ramPieChartData);

            // Atualiza o gráfico do Swap
            adicionarPontoGraficoSwap(tempoNoGrafico, snapshot.swapUsagePercent);

            // Checa se os dados nao sao nulos
            if (snapshot.processList != null) {
                tabelaProcessos.setItems(snapshot.processList);
            } else {
                tabelaProcessos.setItems(FXCollections.emptyObservableList());
            }
            this.tempo++;

        });

        // O que fazer se a Task falhar (roda na Thread do JavaFX)
        updateService.setOnFailed(event -> {
            System.err.println("Erro ao atualizar dados do sistema:");
            if (updateService.getException() != null) {
                updateService.getException().printStackTrace();
            }
        });

        updateService.start(); // Inicia o serviço
    }

    // Metodo para desligar tudo apos fechar o programa
    public void shutdown() {
        if (updateService != null) {
            updateService.cancel();
        }
    }

    // Metodo para validar os dados adquiridos e colocalos no grafico de Swap
    private void adicionarPontoGraficoSwap(long XValue, double usoSwap){
        if(valoresSwap != null){
            valoresSwap.getData().add(new XYChart.Data<>(XValue, usoSwap));

            if(valoresSwap.getData().size() > MAX_PONTOS_GRAFICO_CPU){
                valoresSwap.getData().remove(0);
            }
        }
    }

    // Metodo para validar os dados adquiridos e colocalos no grafico de CPU
    private void adicionarPontoGraficoCPU(long xValue, double usoCPU, double idleCPU) {
        if (valoresCPU != null) {
            valoresCPU.getData().add(new XYChart.Data<>(xValue, usoCPU));

            if (valoresCPU.getData().size() > MAX_PONTOS_GRAFICO_CPU) {
                valoresCPU.getData().remove(0);
            }
        } else {
            System.err.println("A série 'valoresCPU' para o gráfico de CPU não foi inicializada!");
        }

        if (valoresCPUIdle != null) {
            valoresCPUIdle.getData().add(new XYChart.Data<>(xValue, idleCPU));
            if (valoresCPUIdle.getData().size() > MAX_PONTOS_GRAFICO_CPU) {
                valoresCPUIdle.getData().remove(0);
            }
        } else {
            System.err.println("A série 'valoresCPUIdle' não foi inicializada!");
        }
    }

    // Metodo para fazer surgir uma nova pagina ao apertar o botao detalhes
    @FXML
    private void handleDetalhesButtonAction() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/DetailsView.fxml"));
            Parent root = loader.load();

            Stage detalhesStage = new Stage();
            detalhesStage.setTitle("Detalhes Avançados dos Processos");
            Scene detalhesScene = new Scene(root, 1160, 600);
            String cssPath = getClass().getResource("/view/styles.css").toExternalForm();
            detalhesScene.getStylesheets().add(cssPath);
            detalhesStage.setScene(detalhesScene);

            detalhesStage.show();

        } catch (IOException e) {
            e.printStackTrace(); // Log do erro
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro ao Abrir Detalhes");
            alert.setHeaderText(null);
            alert.setContentText("Não foi possível carregar a janela de detalhes avançados.\nVerifique o console para mais informações.");
            alert.showAndWait();
        }
    }
}