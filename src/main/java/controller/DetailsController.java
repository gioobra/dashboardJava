package controller;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;

import javafx.stage.Stage;
import model.Processes;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DetailsController {
    @FXML private TableView<Map<String, Object>> tabelaDetalhesProcessos;
    @FXML private TableColumn<Map<String, Object>, Number> colPid;
    @FXML private TableColumn<Map<String, Object>, Number> colPpid;
    @FXML private TableColumn<Map<String, Object>, String> colNome;
    @FXML private TableColumn<Map<String, Object>, String> colUser;
    @FXML private TableColumn<Map<String, Object>, Number> colThreads;
    @FXML private TableColumn<Map<String, Object>, String> colState;
    @FXML private TableColumn<Map<String, Object>, Number> colPriority;
    @FXML private TableColumn<Map<String, Object>, Number> colNiceValue;
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

    // Cria e configura o modelo de processos e a tabela
    @FXML
    private void initialize() {
        this.processes = new Processes();
        setupColunasTabelaDetalhes();

        // Cria e configura o indicador de progresso de carregamento
        loadingIndicator = new ProgressIndicator(-1.0);
        loadingIndicator.setMaxSize(100, 100);
        loadingIndicator.setVisible(false);

        // Adiciona o indicador de carregamento
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
    
    /* Configura como vai ser pego o dado em cada coluna da tabela, cada objeto Map<String,
    Object>(que representa uma linha) para popular as células da coluna correspondente */
    private void setupColunasTabelaDetalhes() {
        colPid.setCellFactory(column -> new TableCell<Map<String, Object>, Number>() {
                    @Override
                    protected void updateItem(Number item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setOnMouseClicked(null);
                        } else {
                            setText(item.toString());
                            // Adiciona o evento de duplo-clique
                            setOnMouseClicked(event -> {
                                if (event.getClickCount() == 2 && !isEmpty()) {
                                    int pid = getItem().intValue();
                                    openResourcesWindow(pid);
                                }
                            });
                        }
                    }
                });
        colPpid.setCellValueFactory(cd -> new SimpleLongProperty(cd.getValue().get("ppid") != null ? ((Number)cd.getValue().get("ppid")).longValue() : 0L));
        colNome.setCellValueFactory(cd -> new SimpleStringProperty((String) cd.getValue().get("name")));
        colUser.setCellValueFactory(cd -> new SimpleStringProperty((String) cd.getValue().get("user")));
        colThreads.setCellValueFactory(cd -> new SimpleLongProperty(cd.getValue().get("threads") != null ? ((Number)cd.getValue().get("threads")).longValue() : 0L));
        colState.setCellValueFactory(cd -> new SimpleStringProperty((String) cd.getValue().get("state")));
        colPriority.setCellValueFactory(cd -> new SimpleLongProperty(cd.getValue().get("priority") != null ? ((Number)cd.getValue().get("priority")).longValue() : 0L));
        colNiceValue.setCellValueFactory(cd -> new SimpleLongProperty(cd.getValue().get("niceValue") != null ? ((Number)cd.getValue().get("niceValue")).longValue() : 0L));
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

    // Formata para mostrar valores da coluna como porcentagem com duas casas decimais
    private void formatPercentColumn(TableColumn<Map<String, Object>, Number> column) {
        column.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.2f", item.doubleValue()));
            }
        });
    }

    // Inicia a tarefa em background para carregar os dados de todos os processos, evitando que a UI congele
    private void carregarDadosDetalhados(){
        loadingIndicator.setVisible(true);
        tabelaDetalhesProcessos.setDisable(true); // Desabilita a tabela enquanto carrega
        tabelaDetalhesProcessos.setItems(FXCollections.emptyObservableList());

        // Cria uma task para executar a coleta de dados em outra thread
        Task<ObservableList<Map<String, Object>>> taskCarregarDados = new Task<>() {
            @Override
            protected ObservableList<Map<String, Object>> call() throws Exception {
                ObservableList<Map<String, Object>> listaDetalhada = FXCollections.observableArrayList();

                List<model.Process> todosOsProcessos = processes.getAllProcesses();
                for (model.Process process : todosOsProcessos) {
                    Map<String, Object> dadosLinha = new HashMap<>();
                    try {
                        dadosLinha.put("pid", process.getProcessID());
                        dadosLinha.put("ppid", process.getParentProcessID());
                        dadosLinha.put("name", process.getProcessName());
                        dadosLinha.put("user", process.getProcessUser());
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
                        dadosLinha.put("cpuPercent", process.getProcessCpuUsage());
                        dadosLinha.put("commandLine", process.getProcessCommandLine());

                        listaDetalhada.add(dadosLinha);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                return listaDetalhada;
            }
        };
        
        // Define o que fazer na thread da UI quando der certo
        taskCarregarDados.setOnSucceeded(event -> {
            tabelaDetalhesProcessos.setItems(taskCarregarDados.getValue());
            loadingIndicator.setVisible(false);
            tabelaDetalhesProcessos.setDisable(false);
        });

        // Define o que fazer se nao der certo
        taskCarregarDados.setOnFailed(event -> {
            loadingIndicator.setVisible(false);
            tabelaDetalhesProcessos.setDisable(false);

            if (taskCarregarDados.getException() != null) {
                taskCarregarDados.getException().printStackTrace();
            }
        });
        // Inicia a thread para nao bloquear a UI
        new Thread(taskCarregarDados).start();
    }
    private void openResourcesWindow(int pid) {
        try {
            // Carrega o FXML da nova janela
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ProcessResourcesView.fxml"));
            Parent root = loader.load();

            // **PASSO CRÍTICO**: Obtém a instância do novo controller
            ProcessResourcesController resourcesController = loader.getController();
            // **PASSO CRÍTICO**: Chama o método initData para passar o PID
            resourcesController.initData(pid);

            // Configura e exibe a nova janela (Stage)
            Stage stage = new Stage();
            stage.setTitle("Recursos do Processo " + pid);
            Scene scene = new Scene(root);

            // Opcional: Aplica o mesmo estilo da janela de detalhes
            String cssPath = getClass().getResource("/view/styles.css").toExternalForm();
            if (cssPath != null) {
                scene.getStylesheets().add(cssPath);
            }

            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            // Aqui você pode mostrar um Alert para o usuário, se desejar
        }
    }
}