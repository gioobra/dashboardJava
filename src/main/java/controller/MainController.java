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
    @FXML private ProgressBar barraUsoCPU;
    @FXML private Label valorCPU;
    @FXML private ProgressBar barraUsoMemoria;
    @FXML private Label valorMemoria;
    @FXML private Label valorNProcessos;
    @FXML private Label valorThreads;
    @FXML private ListView<String> CPUinfo;
    @FXML private TableView<Object> tabelaProcessos;
    @FXML private Button detalhes;
    @FXML private LineChart<Number,Number> graficoCPU;
    @FXML private PieChart graficoRAM;
    /*
    @FXML private TableColumn<Integer, Integer> colunaPid;
    @FXML private TableColumn<String, String> colunaNome;
    @FXML private TableColumn<Double, Double> colunaCpuPercent;
    @FXML private TableColumn<Double, Double> colunaMemoriaPercent;
     */

    private CpuMonitor cpuMonitor;
    //private CPU cpu;
    private XYChart.Series<Number, Number> valoresCPU;
    //private int tempo = 0;
    //private static final int MAX_PONTOS_GRAFICO_CPU= 60;
    //private Processes process;
    private ScheduledService<SystemDataSnapshot> updateService;

    private static class SystemDataSnapshot{
        double cpuUsage;
        double NumProcesses;
        double NumThreads;

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
        graficoCPU.setAnimated(false);
        graficoCPU.getXAxis().setLabel("Tempo (intervalos de 5s)");
        graficoCPU.getYAxis().setLabel("Uso (%)");
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

                        snapshot.NumProcesses = cpuMonitor.getTotalProcesses();
                        snapshot.NumThreads = cpuMonitor.getTotalThreads();

                        return snapshot;
                    }
                };
            }
        };

        updateService.setPeriod(Duration.seconds(5)); // Atualizar a cada 5 segundos
        updateService.setRestartOnFailure(true);


        updateService.setOnSucceeded(event -> {
            SystemDataSnapshot snapshot = updateService.getValue(); // Pega os dados coletados


            // Atualizar barra e label de CPU
            barraUsoCPU.setProgress(snapshot.cpuUsage / 100.0);
            valorCPU.setText(String.format("CPU: %.2f %%", snapshot.cpuUsage));

            // Atualizar gráfico de CPU
            //adicionarPontoGraficoCPU(snapshot.cpuUsage);

            // Atualizar labels de processos e threads
            valorNProcessos.setText(String.format("Número de Processos: %d", snapshot.NumProcesses));
            valorThreads.setText(String.format("Threads: %d", snapshot.NumThreads));


            valorMemoria.setText("Memoria: - % (Não implementado)");



            System.out.println("Atualização da UI: População da tabela de processos ainda pendente no CpuMonitor.");

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
        /*barraUsoCPU.setProgress(usoCpuInicial / 100.0);
        valorCPU.setText(String.format("CPU: %.2f %%", usoCpuInicial));

        barraUsoMemoria.setProgress(); // mudar quando tiver
        valorMemoria.setText("Memoria: - %");

        valorNProcessos.setText("Número de Processos: " + this.process.getTotalProcesses());
        valorThreads.setText("Threads: " + this.process.getTotalThreads());

        valoresCPU = new XYChart.Series<>();
        valoresCPU.setName("Uso CPU (%)");
        graficoCPU.getData().add(valoresCPU);
        graficoCPU.setAnimated(false);
        graficoCPU.getXAxis().setLabel("Tempo (s)");
        graficoCPU.getYAxis().setLabel("Uso (%)");

        CPUinfo.getItems().add("CPU: " + this.cpu.getCpuName());
        CPUinfo.getItems().add("Cores: " + this.cpu.getNumberOfCores());
        CPUinfo.getItems().add("Uso CPU: " + this.cpu.getCpuInUse() + "%");
        CPUinfo.getItems().add(("CPU em idle: ") + this.cpu.getCpuInIdle() + "%");

        /*
        colunaPid.setCellValueFactory(new PropertyValueFactory<>("pid"));
        colunaNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colunaCpuPercent.setCellValueFactory(new PropertyValueFactory<>("cpuPercent"));
        colunaMemoriaPercent.setCellValueFactory(new PropertyValueFactory<>("memoriaPercent"));
         */

    }
    /*private void adicionarPontoGraficoCPU(double usoCPU){
        graficoCPU.getData().add(new XYChart.Data<>(tempo++, usoCPU));

        if (graficoCPU.getData().size() > MAX_PONTOS_GRAFICO_CPU) {
            graficoCPU.getData().remove(0);
        }
    }

    /*@FXML
    private void detalhes() {

    }
     */
//}