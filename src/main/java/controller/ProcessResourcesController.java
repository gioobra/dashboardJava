package controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import model.ProcessResourceMonitor;

import java.util.List;

public class ProcessResourcesController {

    @FXML private Label titleLabel;
    @FXML private TableView<ResourceInfo> resourcesTableView;
    @FXML private TableColumn<ResourceInfo, String> descriptorColumn;
    @FXML private TableColumn<ResourceInfo, String> typeColumn;
    @FXML private TableColumn<ResourceInfo, String> detailsColumn;

    /**
     * Usamos um record para uma representação de dados forte e segura para a tabela.
     */
    private record ResourceInfo(String descriptor, String type, String details) {}

    /**
     * Este método é chamado pelo DetailsController para passar o PID e iniciar o carregamento dos dados.
     * @param pid O ID do processo a ser monitorado.
     */
    public void initData(int pid) {
        titleLabel.setText("Recursos Abertos para o Processo: " + pid);
        loadResources(pid);
    }

    /**
     * Configura as colunas da tabela e inicia a tarefa de carregamento dos recursos.
     */
    private void loadResources(int pid) {
        // Mapeia as colunas para as propriedades do record ResourceInfo
        descriptorColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().descriptor()));
        typeColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().type()));
        detailsColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().details()));

        // Cria uma Task para buscar os dados em background, sem travar a UI
        Task<ObservableList<ResourceInfo>> loadTask = new Task<>() {
            @Override
            protected ObservableList<ResourceInfo> call() throws Exception {
                // Instancia o novo model com o PID recebido
                ProcessResourceMonitor monitor = new ProcessResourceMonitor(pid);
                // Busca os dados brutos (List<String[]>)
                List<String[]> rawData = monitor.getAllOpenResources();

                // Converte os dados brutos para uma lista de objetos ResourceInfo
                ObservableList<ResourceInfo> resources = FXCollections.observableArrayList();
                for (String[] resource : rawData) {
                    resources.add(new ResourceInfo(resource[0], resource[1], resource[2]));
                }
                return resources;
            }
        };

        loadTask.setOnSucceeded(event -> {
            // Quando a task termina, atualiza a tabela na thread do JavaFX
            resourcesTableView.setItems(loadTask.getValue());
        });

        loadTask.setOnFailed(event -> {
            loadTask.getException().printStackTrace();
            resourcesTableView.setPlaceholder(new Label("Erro ao carregar recursos para o processo."));
        });

        new Thread(loadTask).start();
    }
}