package controller;

import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableView;
import javafx.util.Duration;
import model.CpuMonitor;
import model.CPU;
import javafx.scene.chart.XYChart;
import model.Processes;

public class MainController {
    @FXML
    private ProgressBar barraUsoCPU;
    @FXML
    private Label valorCPU;
    @FXML
    private ProgressBar barraUsoMemoria;
    @FXML
    private Label valorMemoria;
    @FXML
    private Label valorNProcessos;
    @FXML
    private Label valorThreads;
    @FXML
    private ListView<String> CPUinfo;
    @FXML
    private TableView<Object> tabelaProcessos;
    @FXML
    private Button detalhes;
    @FXML
    private LineChart<Number, Number> graficoCPU;
    @FXML
    private PieChart graficoRAM;
    /*
    @FXML private TableColumn<Integer, Integer> colunaNome;
    @FXML private TableColumn<String, String> colunaPID;
    @FXML private TableColumn<String, String> colunaUser;
    @FXML private TableColumn<Double, Double> colunaCpuPercent;
    @FXML private TableColumn<Double, Double> colunaMemoriaPercent;
     */

    private CpuMonitor cpuMonitor;
    //private CPU cpu;
    private XYChart.Series<Number, Number> valoresCPU;
    private XYChart.Series<Number, Number> valoresCPUIdle;
    private int tempo = 0;
    private static final int MAX_PONTOS_GRAFICO_CPU = 60;
    //private Processes process;
    private ScheduledService<SystemDataSnapshot> updateService;

    private static class SystemDataSnapshot {
        double cpuUsage;
        double cpuIdle;
        long numProcesses;
        long numThreads;
    }

    @FXML
    private void initialize() {
        this.cpuMonitor = new CpuMonitor("/proc/cpuinfo", "/proc/stat", "/proc", "/etc/passwd");
        double usoCpuInicial = this.cpuMonitor.getCpuUsage(0);
        //this.process = new Processes();

        setupStaticInfo();
        setupCpuChart();
        //setupProcessTableColumns();

        iniciarAtualizacoesPeriodicas();
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

    /*private void setupProcessTableColumns() {
        colunaPid.setCellValueFactory(new PropertyValueFactory<>("pid"));
        colunaNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colunaCpuPercent.setCellValueFactory(new PropertyValueFactory<>("cpuPercent"));
        colunaMemoriaPercent.setCellValueFactory(new PropertyValueFactory<>("memoriaPercent"));
    }
     */
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


            valorMemoria.setText("Memoria: - % (Não implementado)");


            System.out.println("Atualização da UI: População da tabela de processos ainda pendente no CpuMonitor.");

            this.tempo++;

        });

        // O que fazer se a Task falhar (roda na Thread do JavaFX)
        updateService.setOnFailed(event -> {
            System.err.println("Erro ao atualizar dados do sistema:");
            updateService.getException().printStackTrace();
            // Você pode querer mostrar uma mensagem de erro na UI aqui
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