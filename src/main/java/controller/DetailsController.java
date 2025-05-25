package controller;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import model.Processes;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DetailsController {
    @FXML private TableView<Map<String, Object>> tabelaDetalhesProcessos;
    @FXML private TableColumn<Map<String, Object>, String> colPid;
    @FXML private TableColumn<Map<String, Object>, String> colNome;
    @FXML private TableColumn<Map<String, Object>, String> colUser;
    @FXML private TableColumn<Map<String, Object>, String> colPpid;
    @FXML private TableColumn<Map<String, Object>, String> colThreads;
    @FXML private TableColumn<Map<String, Object>, String> colState;
    @FXML private TableColumn<Map<String, Object>, String> colPriority;
    @FXML private TableColumn<Map<String, Object>, String> colNiceValue;
    @FXML private TableColumn<Map<String, Object>, String> colVmAllocated;
    @FXML private TableColumn<Map<String, Object>, Number> colVmPeak;
    @FXML private TableColumn<Map<String, Object>, String> colPhysMemory;
    @FXML private TableColumn<Map<String, Object>, Number> colExecCodePages;
    @FXML private TableColumn<Map<String, Object>, Number> colLibCodePages;
    @FXML private TableColumn<Map<String, Object>, Number> colHeapPages;
    @FXML private TableColumn<Map<String, Object>, Number> colStackPages;
    @FXML private TableColumn<Map<String, Object>, Number> colRamPercent;
    @FXML private TableColumn<Map<String, Object>, Number> colCpuPercent;
    @FXML private TableColumn<Map<String, Object>, String> colCommandLine;

    private Processes processes;
    private ProgressIndicator loadingIndicator;
    @FXML
    private void initialize() {
        this.processes = new Processes();
        setupColunasTabelaDetalhes();

        loadingIndicator = new ProgressIndicator(-1.0);
        loadingIndicator.setMaxSize(100, 100);
        loadingIndicator.setVisible(false);

        if(tabelaDetalhesProcessos.getParent() instanceof AnchorPane){
            AnchorPane parentPane = (AnchorPane) tabelaDetalhesProcessos.getParent();
            StackPane indicatorPane = new StackPane(loadingIndicator);
            indicatorPane.setMouseTransparent(true);
            parentPane.getChildren().add(indicatorPane);
            AnchorPane.setTopAnchor(indicatorPane, 0.0);
            AnchorPane.setLeftAnchor(indicatorPane, 0.0);
            AnchorPane.setRightAnchor(indicatorPane, 0.0);
            AnchorPane.setBottomAnchor(indicatorPane, 0.0);
        }
        carregarDadosDetalhados();
    }
    private void setupColunasTabelaDetalhes() {
        colPid.setCellValueFactory(cd -> new SimpleStringProperty((String) cd.getValue().get("pid")));
        colNome.setCellValueFactory(cd -> new SimpleStringProperty((String) cd.getValue().get("nome")));
        colUser.setCellValueFactory(cd -> new SimpleStringProperty((String) cd.getValue().get("user")));
        colPpid.setCellValueFactory(cd -> new SimpleStringProperty((String) cd.getValue().get("ppid")));
        colThreads.setCellValueFactory(cd -> new SimpleStringProperty((String) cd.getValue().get("threads")));
        colState.setCellValueFactory(cd -> new SimpleStringProperty((String) cd.getValue().get("state")));
        colPriority.setCellValueFactory(cd -> new SimpleStringProperty((String) cd.getValue().get("priority")));
        colNiceValue.setCellValueFactory(cd -> new SimpleStringProperty((String) cd.getValue().get("niceValue")));
        colVmAllocated.setCellValueFactory(cd -> new SimpleStringProperty((String) cd.getValue().get("vmAllocated")));
        colVmPeak.setCellValueFactory(cd -> new SimpleLongProperty(cd.getValue().get("vmPeak") != null ? ((Number)cd.getValue().get("vmPeak")).longValue() : 0L));
        colPhysMemory.setCellValueFactory(cd -> new SimpleStringProperty((String) cd.getValue().get("physMemory")));
        colExecCodePages.setCellValueFactory(cd -> new SimpleLongProperty(cd.getValue().get("execCodePages") != null ? ((Number)cd.getValue().get("execCodePages")).longValue() : 0L));
        colLibCodePages.setCellValueFactory(cd -> new SimpleLongProperty(cd.getValue().get("libCodePages") != null ? ((Number)cd.getValue().get("libCodePages")).longValue() : 0L));
        colHeapPages.setCellValueFactory(cd -> new SimpleLongProperty(cd.getValue().get("heapPages") != null ? ((Number)cd.getValue().get("heapPages")).longValue() : 0L));
        colStackPages.setCellValueFactory(cd -> new SimpleLongProperty(cd.getValue().get("stackPages") != null ? ((Number)cd.getValue().get("stackPages")).longValue() : 0L));
        colRamPercent.setCellValueFactory(cd -> new SimpleDoubleProperty(cd.getValue().get("ramPercent") != null ? ((Number)cd.getValue().get("ramPercent")).doubleValue() : 0.0));
        colCpuPercent.setCellValueFactory(cd -> new SimpleDoubleProperty(cd.getValue().get("cpuPercent") != null ? ((Number)cd.getValue().get("cpuPercent")).doubleValue() : 0.0));
        colCommandLine.setCellValueFactory(cd -> new SimpleStringProperty((String) cd.getValue().get("commandLine")));

        formatPercentColumn(colRamPercent);
        formatPercentColumn(colCpuPercent);
    }
    private void formatPercentColumn(TableColumn<Map<String, Object>, Number> column) {
        column.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.2f", item.doubleValue()));
            }
        });
    }
    private void carregarDadosDetalhados(){
        loadingIndicator.setVisible(true);
        tabelaDetalhesProcessos.setDisable(true); // Desabilita a tabela enquanto carrega
        tabelaDetalhesProcessos.setItems(FXCollections.emptyObservableList());

        Task<ObservableList<Map<String, Object>>> taskCarregarDados = new Task<>() {
            @Override
            protected ObservableList<Map<String, Object>> call() throws Exception {
                ObservableList<Map<String, Object>> listaDetalhada = FXCollections.observableArrayList();

                List<model.Process> todosOsProcessos = processes.getAllProcesses();
                for (model.Process process : todosOsProcessos) {
                    Map<String, Object> dadosLinha = new HashMap<>();
                    try {
                        String pid = process.getProcessID();
                        dadosLinha.put("pid", pid);
                        dadosLinha.put("nome", process.getProcessName());
                        dadosLinha.put("user", process.getProcessUser());
                        dadosLinha.put("ppid", process.getParentProcessID());
                        dadosLinha.put("threads", process.getProcessNumberOfThreads());
                        dadosLinha.put("state", process.getProcessState());
                        dadosLinha.put("priority", process.getProcessPriority());
                        dadosLinha.put("niceValue", process.getProcessNiceValue());
                        dadosLinha.put("vmAllocated", process.getProcessVirtualMemoryAllocated());
                        dadosLinha.put("vmPeak", process.getProcessVirtualMemoryPeak());
                        dadosLinha.put("physMemory", process.getProcessPhysicalMemoryUsage());
                        dadosLinha.put("execCodePages", process.getProcessExecCodePages());
                        dadosLinha.put("libCodePages", process.getProcessLibCodePages());
                        dadosLinha.put("heapPages", process.getProcessHeapPages());
                        dadosLinha.put("stackPages", process.getProcessStackPages());
                        dadosLinha.put("ramPercent", process.getProcessMemoryPercentage());
                        // AVISO DE PERFORMANCE: getProcessCpuUsage tem sleep de 1s!
                        dadosLinha.put("cpuPercent", process.getProcessCpuUsage());
                        dadosLinha.put("commandLine", process.getProcessCommandLine());

                        listaDetalhada.add(dadosLinha);
                        // System.out.println("DetalhesTask: Adicionado PID " + pid); // Log para cada processo
                    } catch (Exception e) {
                        System.err.println("DetalhesTask: Erro ao processar " + process.getBasePath().getFileName() + ": " + e.getMessage());
                        // e.printStackTrace(); // Para debug mais profundo
                    }
                }
                return listaDetalhada;
            }
        };
        taskCarregarDados.setOnSucceeded(event -> {
            tabelaDetalhesProcessos.setItems(taskCarregarDados.getValue());
            loadingIndicator.setVisible(false);
            tabelaDetalhesProcessos.setDisable(false);
            System.out.println("DetalhesUI: Tabela de detalhes carregada.");
        });

        taskCarregarDados.setOnFailed(event -> {
            loadingIndicator.setVisible(false);
            tabelaDetalhesProcessos.setDisable(false);
            System.err.println("DetalhesUI: Falha ao carregar dados detalhados dos processos.");
            if (taskCarregarDados.getException() != null) {
                taskCarregarDados.getException().printStackTrace();
            }


        });

        new Thread(taskCarregarDados).start();
    }
}


