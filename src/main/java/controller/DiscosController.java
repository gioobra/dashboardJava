package controller;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.layout.StackPane;
import model.FileSystemMonitor;

import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;


public class DiscosController implements Initializable {


    @FXML private TableView<PartitionInfo> diskTableView;
    @FXML private TableColumn<PartitionInfo, String> partitionColumn;
    @FXML private TableColumn<PartitionInfo, String> totalSpaceColumn;
    @FXML private TableColumn<PartitionInfo, String> usedSpaceColumn;
    @FXML private TableColumn<PartitionInfo, String> freeSpaceColumn;
    @FXML private TableColumn<PartitionInfo, Double> usageColumn;

    @FXML private TreeView<Path> directoryTreeView;

    @FXML private TableView<Path> fileDetailsTableView;
    @FXML private TableColumn<Path, String> nameColumn;
    @FXML private TableColumn<Path, String> sizeColumn;
    @FXML private TableColumn<Path, String> dateModifiedColumn;
    @FXML private TableColumn<Path, String> permissionColumn;


    private final FileSystemMonitor monitor = new FileSystemMonitor();
    private ProgressIndicator loadingIndicator;

    //Record para representar os dados da tabela de discos
    private record PartitionInfo(String mountPoint, String totalSize, String usedSize, String freeSize, Double usage) {}

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupLoadingIndicator();
        setupDiskInfoTableColumns();
        setupDirectoryTree();
        setupFileDetailsTableColumns();
        linkTreeToDetailsTable();
        loadDiskInfoData();
    }

    //Cria e posiciona o indicador de progresso sobre a área principal da TreeView
    private void setupLoadingIndicator() {
        loadingIndicator = new ProgressIndicator(-1.0);
        loadingIndicator.setMaxSize(100, 100);
        loadingIndicator.setVisible(false);
        loadingIndicator.setMouseTransparent(true); // Permite cliques através do indicador

        // O StackPane permite sobrepor o indicador de progresso sobre o conteúdo
        if (directoryTreeView.getParent() instanceof SplitPane) {
            // Se o pai é o SplitPane, envolvemos a TreeView em um StackPane
            StackPane treeViewContainer = new StackPane();
            SplitPane splitPane = (SplitPane) directoryTreeView.getParent();
            int treeViewIndex = splitPane.getItems().indexOf(directoryTreeView);

            treeViewContainer.getChildren().addAll(directoryTreeView, loadingIndicator);

            // Substitui a TreeView original pelo nosso novo container
            if (treeViewIndex != -1) {
                splitPane.getItems().set(treeViewIndex, treeViewContainer);
            }
        }
    }



    private void setupDiskInfoTableColumns() {
        partitionColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().mountPoint()));
        totalSpaceColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().totalSize()));
        usedSpaceColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().usedSize()));
        freeSpaceColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().freeSize()));
        usageColumn.setCellValueFactory(cd -> new SimpleDoubleProperty(cd.getValue().usage()).asObject());
        usageColumn.setCellFactory(ProgressBarTableCell.forTableColumn());
    }

    private void setupDirectoryTree() {
        TreeItem<Path> dummyRoot = new TreeItem<>();
        directoryTreeView.setShowRoot(false);
        directoryTreeView.setRoot(dummyRoot);
    }

    private void setupFileDetailsTableColumns() {
        fileDetailsTableView.setPlaceholder(new Label("Selecione um diretório na árvore para ver os arquivos."));
        nameColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getFileName().toString()));
        sizeColumn.setCellValueFactory(cd -> new SimpleStringProperty(formatFileSize(cd.getValue())));
        dateModifiedColumn.setCellValueFactory(cd -> new SimpleStringProperty(formatFileDate(cd.getValue())));
        permissionColumn.setCellValueFactory(cd -> new SimpleStringProperty(formatFilePermissions(cd.getValue())));
    }



    //Carrega os dados dos discos usando uma Task para não bloquear a UI.
    private void loadDiskInfoData() {
        Task<List<PartitionInfo>> loadDisksTask = new Task<>() {
            @Override
            protected List<PartitionInfo> call() {
                // Executa em uma thread de background
                return monitor.getPartitionsInfo().stream()
                        .map(data -> {
                            String usageStr = data[6].replace("%", "").replace(",", ".");
                            double usageValue = Double.parseDouble(usageStr) / 100.0;
                            return new PartitionInfo(data[0], data[3], data[4], data[5], usageValue);
                        })
                        .collect(Collectors.toList());
            }
        };

        loadDisksTask.setOnSucceeded(event -> {
            // Executa de volta na thread do JavaFX
            List<PartitionInfo> loadedPartitions = loadDisksTask.getValue();
            diskTableView.setItems(FXCollections.observableArrayList(loadedPartitions));

            // Popula a árvore com os pontos de montagem
            directoryTreeView.getRoot().getChildren().clear();
            loadedPartitions.forEach(p -> {
                TreeItem<Path> item = createLazyLoadingTreeItem(Paths.get(p.mountPoint()));
                directoryTreeView.getRoot().getChildren().add(item);
            });
        });

        loadDisksTask.setOnFailed(event -> loadDisksTask.getException().printStackTrace());
        new Thread(loadDisksTask).start();
    }

    //Atualiza a tabela de detalhes de arquivos usando uma Task
    private void updateFileDetailsTable(Path directoryPath) {
        loadingIndicator.setVisible(true);
        fileDetailsTableView.setDisable(true);
        fileDetailsTableView.setPlaceholder(new Label("Lendo arquivos em " + directoryPath.getFileName() + "..."));

        Task<ObservableList<Path>> loadFilesTask = new Task<>() {
            @Override
            protected ObservableList<Path> call() {
                ObservableList<Path> files = FXCollections.observableArrayList();
                if (Files.isDirectory(directoryPath)) {
                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath)) {
                        stream.forEach(files::add);
                    } catch (IOException e) {
                        System.err.println("Erro ao listar arquivos: " + e.getMessage());
                    }
                }
                return files;
            }
        };

        loadFilesTask.setOnSucceeded(event -> {
            fileDetailsTableView.setItems(loadFilesTask.getValue());
            fileDetailsTableView.setPlaceholder(new Label("Este diretório está vazio."));
            loadingIndicator.setVisible(false);
            fileDetailsTableView.setDisable(false);
        });

        loadFilesTask.setOnFailed(event -> {
            loadFilesTask.getException().printStackTrace();
            loadingIndicator.setVisible(false);
            fileDetailsTableView.setDisable(false);
        });
        new Thread(loadFilesTask).start();
    }

    //serve para conectar a TreeView com a tabela de detalhes da direita
    private void linkTreeToDetailsTable() {
        directoryTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null) {
                updateFileDetailsTable(newVal.getValue());
            }
        });
    }

    //Method que garante a execução rápida do programa, um diretório so carrega a lista de subpastas no momento que for selecionado
    private TreeItem<Path> createLazyLoadingTreeItem(Path path) {
        TreeItem<Path> item = new TreeItem<>(path);
        // Adiciona um filho "fantasma" para que o nó seja expansível na UI
        if (Files.isDirectory(path)) {
            item.getChildren().add(new TreeItem<>());
        }

        item.expandedProperty().addListener((obs, wasExpanded, isNowExpanded) -> {
            // Se foi expandido e só contém o filho "fantasma", carrega os filhos reais
            if (isNowExpanded && item.getChildren().size() == 1 && item.getChildren().get(0).getValue() == null) {
                loadTreeItemChildren(item);
            }
        });
        return item;
    }

    //Method para inserir os "filhos" de um processo
    private void loadTreeItemChildren(TreeItem<Path> parentItem) {
        parentItem.getChildren().setAll(new TreeItem<>(Paths.get("Carregando...")));

        Task<List<TreeItem<Path>>> loadChildrenTask = new Task<>() {
            @Override
            protected List<TreeItem<Path>> call() {
                List<TreeItem<Path>> children = new ArrayList<>();
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(parentItem.getValue())) {
                    for (Path path : stream) {
                        if (Files.isDirectory(path)) {
                            children.add(createLazyLoadingTreeItem(path));
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Erro ao carregar filhos do diretório: " + e.getMessage());
                }
                return children;
            }
        };

        loadChildrenTask.setOnSucceeded(event -> parentItem.getChildren().setAll(loadChildrenTask.getValue()));
        loadChildrenTask.setOnFailed(event -> {
            loadChildrenTask.getException().printStackTrace();
            parentItem.getChildren().clear(); // Limpa o "Carregando..." em caso de erro
        });

        new Thread(loadChildrenTask).start();
    }

   //Methods para organizar o tamanho, a data e as permissões
    private String formatFileSize(Path path) {
        try {
            if (Files.isDirectory(path)) return "<DIR>";
            long size = Files.size(path);
            if (size <= 0) return "0 B";
            final String[] units = {"B", "KB", "MB", "GB", "TB"};
            int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
            return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
        } catch (IOException e) { return "N/A"; }
    }
    private String formatFileDate(Path path) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(attrs.lastModifiedTime().toMillis());
        } catch (IOException e) { return "N/A"; }
    }
    private String formatFilePermissions(Path path) {
        return (Files.isDirectory(path) ? "d" : "-") + (Files.isReadable(path) ? "r" : "-") + (Files.isWritable(path) ? "w" : "-") + (Files.isExecutable(path) ? "x" : "-");
    }

}