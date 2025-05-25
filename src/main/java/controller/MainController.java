package controller;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.util.Duration;
import model.CpuMonitor;
import model.Memory;
import model.CPU;
import javafx.scene.chart.XYChart;
import model.Processes;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
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
    @FXML private TableColumn<Map<String, Object>, String> colunaPid;
    @FXML private TableColumn<Map<String, Object>, String> colunaUser;
    @FXML private TableColumn<Map<String, Object>, Number> colunaCpu;
    @FXML private TableColumn<Map<String, Object>, Number> colunaMemoria;


    private CpuMonitor cpuMonitor;
    private Processes processes;
    private Memory memory;
    //private CPU cpu;
    private XYChart.Series<Number, Number> valoresCPU;
    private XYChart.Series<Number, Number> valoresCPUIdle;
    private int tempo = 0;
    private static final int MAX_PONTOS_GRAFICO_CPU = 60;
    private ScheduledService<SystemDataSnapshot> updateService;

    private static class SystemDataSnapshot {
        double cpuUsage;
        double cpuIdle;
        long numProcesses;
        long numThreads;
        double memoryUsedPercent;
        double memoryUsedGB;
        double memoryFreeGB;
        double memoryTotalGB;
        ObservableList<Map<String, Object>> processList;
    }

    @FXML
    private void initialize() {
        this.cpuMonitor = new CpuMonitor("/proc/cpuinfo", "/proc/stat", "/proc", "/etc/passwd");
        this.processes = new Processes();
        this.memory = new Memory();

        setupStaticInfo();
        setupCpuChart();
        setupProcessTableColumns();
        setupMemoryPieChart();

        iniciarAtualizacoesPeriodicas();
    }
    private void setupMemoryPieChart(){
        graficoRAM.setTitle("Uso de RAM");
        graficoRAM.setLegendVisible(true);
        graficoRAM.setLabelsVisible(true);
        graficoRAM.setLegendSide(Side.TOP);
        graficoRAM.setAnimated(false);
        ObservableList<PieChart.Data> initialData = FXCollections.observableArrayList(
                new PieChart.Data("Carregando...", 1)
        );
        graficoRAM.setData(initialData);
    }

    private void setupStaticInfo() {
        CPUinfo.getItems().clear();
        CPUinfo.getItems().add("CPU: " + this.cpuMonitor.getCpuName());
        CPUinfo.getItems().add("Cores: " + this.cpuMonitor.getNumberOfCores());
    }

    private void setupCpuChart() {
        valoresCPU = new XYChart.Series<>();
        valoresCPU.setName("Uso CPU (%)");
        graficoCPU.getData().add(valoresCPU);

        valoresCPUIdle = new XYChart.Series<>();
        valoresCPUIdle.setName("CPU Idle (%)");
        graficoCPU.getData().add(valoresCPUIdle);

        graficoCPU.setAnimated(false);
        graficoCPU.getXAxis().setLabel("Tempo (s)");
        graficoCPU.getYAxis().setLabel("Uso/Idle (%)");
    }

    private void setupProcessTableColumns() {
        colunaNome.setCellValueFactory(cellData ->
                new SimpleStringProperty((String) cellData.getValue().get("nome"))
        );
        colunaPid.setCellValueFactory(cellData ->
                new SimpleStringProperty((String) cellData.getValue().get("pid"))
        );
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

    private void iniciarAtualizacoesPeriodicas() {
        updateService = new ScheduledService<SystemDataSnapshot>() {
            @Override
            protected Task<SystemDataSnapshot> createTask() {
                return new Task<>() {
                    @Override
                    protected SystemDataSnapshot call() throws Exception {

                        SystemDataSnapshot snapshot = new SystemDataSnapshot();

                        snapshot.cpuUsage = cpuMonitor.getCpuUsage(0);
                        snapshot.cpuIdle = cpuMonitor.getCpuIdle(0);

                        snapshot.numProcesses = cpuMonitor.getTotalProcesses();
                        snapshot.numThreads = cpuMonitor.getTotalThreads();

                        snapshot.memoryUsedPercent = memory.getMemUsedPercentage();
                        snapshot.memoryUsedGB = memory.getMemUsed();
                        snapshot.memoryFreeGB = memory.getFreeMemory();
                        snapshot.memoryTotalGB = memory.getTotalMemory();

                        ObservableList<Map<String, Object>> processDataList = FXCollections.observableArrayList();
                        List<Path> todosOsCaminhosDeProcessos = processes.getAllProcessesPath();
                        for (Path caminhoDoProcesso : todosOsCaminhosDeProcessos) {
                            Map<String, Object> rowData = new HashMap<>();
                            try {
                                String pid = processes.getProcessID(caminhoDoProcesso);
                                rowData.put("nome", processes.getProcessName(caminhoDoProcesso));
                                rowData.put("pid", pid);
                                rowData.put("user", processes.getProcessUser(caminhoDoProcesso.toString()));
                                double cpuProc = processes.getProcessCpuUsage(caminhoDoProcesso);
                                rowData.put("cpu", cpuProc);
                                double memProc = processes.getProcessMemoryPercentage(caminhoDoProcesso);
                                rowData.put("memoria", memProc);
                                processDataList.add(rowData);
                            } catch (Exception e) {
                                System.err.println("Task.call() - Erro ao processar " + caminhoDoProcesso.getFileName() + ": " + e.getMessage());
                            }
                        }
                        snapshot.processList = processDataList;

                        return snapshot;
                    }
                };
            }
        };

        updateService.setPeriod(Duration.seconds(5)); // Atualizar a cada 5 segundos
        updateService.setRestartOnFailure(true);


        updateService.setOnSucceeded(event -> {
            SystemDataSnapshot snapshot = updateService.getValue(); // Pega os dados coletados

            long tempoNoGrafico = (long)this.tempo * 5;

            // Atualizar barra e label de CPU
            barraUsoCPU.setProgress(snapshot.cpuUsage / 100.0);
            valorCPU.setText(String.format("CPU: %.2f %%", snapshot.cpuUsage));

            // Atualizar gráfico de CPU
            adicionarPontoGraficoCPU(tempoNoGrafico, snapshot.cpuUsage, snapshot.cpuIdle);

            // Atualizar labels de processos e threads
            valorNProcessos.setText(String.format("Número de Processos: %d", snapshot.numProcesses));
            valorThreads.setText(String.format("Threads: %d", snapshot.numThreads));

            barraUsoMemoria.setProgress(snapshot.memoryUsedPercent / 100.0);
            valorMemoria.setText(String.format("Memoria: %.2f %%", snapshot.memoryUsedPercent));

            ObservableList<PieChart.Data> ramPieChartData = FXCollections.observableArrayList(
                    new PieChart.Data(String.format("Usada: %.2f GB", snapshot.memoryUsedGB), snapshot.memoryUsedGB),
                    new PieChart.Data(String.format("Livre: %.2f GB", snapshot.memoryFreeGB), snapshot.memoryFreeGB)
            );
            graficoRAM.setData(ramPieChartData);

            if(snapshot.processList != null){
                tabelaProcessos.setItems(snapshot.processList);
            }
            else{
                tabelaProcessos.setItems(FXCollections.emptyObservableList());
            }
            this.tempo++;

        });

        // O que fazer se a Task falhar (roda na Thread do JavaFX)
        updateService.setOnFailed(event -> {
            System.err.println("Erro ao atualizar dados do sistema:");
            if(updateService.getException() != null) {
                updateService.getException().printStackTrace();
            }
        });

        updateService.start(); // Inicia o serviço
    }

    public void shutdown() {
        if (updateService != null) {
            updateService.cancel();
            System.out.println("Serviço de atualização parado.");
        }
    }


    private void adicionarPontoGraficoCPU(long xValue, double usoCPU, double idleCPU) {
        if (valoresCPU != null) {
            valoresCPU.getData().add(new XYChart.Data<>(xValue, usoCPU));

            if (valoresCPU.getData().size() > MAX_PONTOS_GRAFICO_CPU) {
                valoresCPU.getData().remove(0);
            }
        }
        else{
            System.err.println("A série 'valoresCPU' para o gráfico de CPU não foi inicializada!");
        }

        if (valoresCPUIdle != null) {
            valoresCPUIdle.getData().add(new XYChart.Data<>(xValue, idleCPU));
            if (valoresCPUIdle.getData().size() > MAX_PONTOS_GRAFICO_CPU) {
                valoresCPUIdle.getData().remove(0);
            }
        }
        else{
            System.err.println("A série 'valoresCPUIdle' não foi inicializada!");
        }
    }
}
    /*@FXML
    private void detalhes() {

    }
     */
//}