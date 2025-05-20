package controller;

import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;

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
    private TableView<Object> tableaProcessos;

    @FXML
    private Button detalhes;

    @FXML
    private LineChart<Number,Number> graficoCPU;

    @FXML
    private PieChart graficoRAM;

    @FXML
    private void initialize() {
        valorCPU.setText("CPU: - %");
        valorMemoria.setText("Memoria: - %");
        valorNProcessos.setText("Número de Processos: - ");
        valorThreads.setText("Threads: - ");


    }

    @FXML
    private void detalhes() {

    }
}