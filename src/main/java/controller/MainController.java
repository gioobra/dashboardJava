package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import model.CpuMonitor;
import model.CPU;
import java.io.IOException;
import javafx.collections.FXCollections;
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
    private CPU cpu;
    private XYChart.Series<Number, Number> valoresCPU;
    private int tempo = 0;
    private static final int MAX_PONTOS_GRAFICO_CPU= 60;
    private Processes process;

    @FXML
    private void initialize() {
        this.cpuMonitor = new CpuMonitor("/proc/cpuinfo", "/proc/stat", "/proc", "/etc/passwd");
        double usoCpuInicial = this.cpuMonitor.getCpuUsage(0);
        this.process = new Processes();

        barraUsoCPU.setProgress(usoCpuInicial / 100.0);
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
    private void adicionarPontoGraficoCPU(double usoCPU){
        graficoCPU.getData().add(new XYChart.Data<>(tempo++, usoCPU));

        if (graficoCPU.getData().size() > MAX_PONTOS_GRAFICO_CPU) {
            graficoCPU.getData().remove(0);
        }
    }

    /*@FXML
    private void detalhes() {

    }
     */
}