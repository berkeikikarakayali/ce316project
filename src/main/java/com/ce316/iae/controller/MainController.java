package com.ce316.iae.controller;

import com.ce316.iae.dao.ConfigurationDAO;
import com.ce316.iae.dao.EvaluationResultDAO;
import com.ce316.iae.dao.ProjectDAO;
import com.ce316.iae.dao.StudentSubmissionDAO;
import com.ce316.iae.db.DatabaseService;
import com.ce316.iae.engine.Executioner;
import com.ce316.iae.file.FileManager;
import com.ce316.iae.model.ComparisonStatus;
import com.ce316.iae.model.LanguageConfig;
import com.ce316.iae.model.NormalizationMode;
import com.ce316.iae.model.Project;
import com.ce316.iae.model.StudentReport;
import com.ce316.iae.model.Submission;
import com.ce316.iae.service.ComparisonService;
import com.ce316.iae.service.ConfigurationService;
import com.ce316.iae.service.ImportExportService;
import com.ce316.iae.service.ImportMode;
import com.ce316.iae.service.ImportResult;
import com.ce316.iae.service.ReportingService;
import com.ce316.iae.service.SkippedEntry;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JavaFX shell wiring persistence, configurations, batch execution, and results (Req 3–4, 7–9).
 */
public class MainController {

    private static final String META_ZIP = "submission_zip_directory";
    private static final String META_SRC = "submission_main_source_file";

    private final DatabaseService databaseService = new DatabaseService();
    private final ComparisonService comparisonService = new ComparisonService();
    private final ReportingService reportingService = new ReportingService();
    private final FileManager fileManager = new FileManager();

    private Stage primaryStage;
    private ConfigurationDAO configurationDAO;
    private ProjectDAO projectDAO;
    private StudentSubmissionDAO submissionDAO;
    private EvaluationResultDAO evaluationDAO;
    private ConfigurationService configurationService;
    private Executioner executioner;

    private final ObservableList<StudentReport> resultsItems = FXCollections.observableArrayList();

    private int currentImageIndex = 0;

    private final String[] helpImages = {
            "/images/help1.png",
            "/images/help2.png",
            "/images/help3.png",
            "/images/help4.png",
            "/images/help5.png"
    };

    @FXML private TabPane tabPane;
    @FXML private Tab resultsTab;
    @FXML private Label projectPathLabel;
    @FXML private ListView<String> configurationListView;
    @FXML private TextField projectNameField;
    @FXML private ComboBox<String> activeConfigCombo;
    @FXML private TextField expectedOutputField;
    @FXML private TextField runArgsField;
    @FXML private ComboBox<NormalizationMode> normalizationCombo;
    @FXML private Spinner<Integer> compileTimeoutSpinner;
    @FXML private Spinner<Integer> runTimeoutSpinner;
    @FXML private TextField zipDirectoryField;
    @FXML private TextField sourceFileNameField;
    @FXML private Button runEvaluationButton;
    @FXML private ProgressIndicator runProgressIndicator;
    @FXML private Label assignmentHintLabel;
    @FXML private Label resultsSummaryLabel;
    @FXML private TableView<StudentReport> resultsTableView;


    @FXML
    private void onGreenHover(MouseEvent e) {
        ((Button)e.getSource()).setStyle(
                "-fx-background-color: #388E3C; -fx-text-fill: white; -fx-cursor: hand;");
    }

    @FXML
    private void onGreenExit(MouseEvent e) {
        ((Button)e.getSource()).setStyle(
                "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-cursor: hand;");
    }

    @FXML
    private void onBlueHover(MouseEvent e) {
        ((Button)e.getSource()).setStyle(
                "-fx-background-color: #1976D2; -fx-text-fill: white; -fx-cursor: hand;");
    }

    @FXML
    private void onBlueExit(MouseEvent e) {
        ((Button)e.getSource()).setStyle(
                "-fx-background-color: #2196F3; -fx-text-fill: white; -fx-cursor: hand;");
    }

    @FXML
    private void onRedHover(MouseEvent e) {
        ((Button)e.getSource()).setStyle(
                "-fx-background-color: #D32F2F; -fx-text-fill: white; -fx-cursor: hand;");
    }

    @FXML
    private void onRedExit(MouseEvent e) {
        ((Button)e.getSource()).setStyle(
                "-fx-background-color: #F44336; -fx-text-fill: white; -fx-cursor: hand;");
    }
    @FXML
    private void onHelp() {

        Stage helpStage = new Stage();

        // title
        Label titleLabel = new Label("Helper Menu");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        // photo
        Image image = new Image(
                Objects.requireNonNull(
                        getClass().getResourceAsStream(helpImages[currentImageIndex])
                )
        );

        ImageView imageView = new ImageView(image);

        imageView.setFitWidth(500);
        imageView.setFitHeight(250);
        imageView.setPreserveRatio(true);

        // buttons
        Button previousButton = new Button("<");
        Button nextButton = new Button(">");

        nextButton.setOnAction(e -> {

            currentImageIndex++;

            if (currentImageIndex >= helpImages.length) {
                currentImageIndex = 0;
            }

            imageView.setImage(
                    new Image(
                            Objects.requireNonNull(
                                    getClass().getResourceAsStream(
                                            helpImages[currentImageIndex]
                                    )
                            )
                    )
            );
        });

        previousButton.setOnAction(e -> {

            currentImageIndex--;

            if (currentImageIndex <= helpImages.length) {
                currentImageIndex = 0;
            }

            imageView.setImage(
                    new Image(
                            Objects.requireNonNull(
                                    getClass().getResourceAsStream(
                                            helpImages[currentImageIndex]
                                    )
                            )
                    )
            );
        });

        HBox buttonBox = new HBox(15, previousButton, nextButton);
        buttonBox.setStyle("-fx-alignment: center;");

        // main layout
        VBox root = new VBox(25);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-alignment: center;");

        root.getChildren().addAll(
                titleLabel,
                imageView,
                buttonBox
        );

        Scene scene = new Scene(root, 700, 500);

        helpStage.setTitle("Help");
        helpStage.setScene(scene);
        helpStage.show();
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setOnCloseRequest(e -> databaseService.closeProject());
    }

    @FXML
    private void initialize() {
        normalizationCombo.setItems(FXCollections.observableArrayList(NormalizationMode.values()));

        compileTimeoutSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 86_400, 60));
        runTimeoutSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 86_400, 30));

        TableColumn<StudentReport, String> colStudent = new TableColumn<>("Student");
        colStudent.setPrefWidth(110);
        colStudent.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getStudentId() != null ? c.getValue().getStudentId() : ""));
        TableColumn<StudentReport, String> colStatus = new TableColumn<>("Status");
        colStatus.setPrefWidth(100);
        colStatus.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getStatus() != null ? c.getValue().getStatus().name() : ""));
        TableColumn<StudentReport, String> colNorm = new TableColumn<>("Normalization");
        colNorm.setPrefWidth(130);
        colNorm.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getNormalizationMode() != null
                        ? c.getValue().getNormalizationMode().name() : ""));
        TableColumn<StudentReport, String> colTs = new TableColumn<>("Timestamp");
        colTs.setPrefWidth(180);
        colTs.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getTimestamp() != null ? c.getValue().getTimestamp() : ""));
        TableColumn<StudentReport, String> colErr = new TableColumn<>("Message");
        colErr.setPrefWidth(260);
        colErr.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getErrorMessage() != null ? c.getValue().getErrorMessage() : ""));
        TableColumn<StudentReport, String> colPrev = new TableColumn<>("Output preview");
        colPrev.setPrefWidth(220);
        colPrev.setCellValueFactory(c -> {
            String a = c.getValue().getActualOutput();
            if (a == null) {
                return new SimpleStringProperty("");
            }
            String oneLine = a.replace('\n', ' ').trim();
            if (oneLine.length() > 80) {
                oneLine = oneLine.substring(0, 80) + "...";
            }
            return new SimpleStringProperty(oneLine);
        });
        resultsTableView.getColumns().setAll(colStudent, colStatus, colNorm, colTs, colErr, colPrev);
        resultsTableView.setItems(resultsItems);
        resultsTableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                StudentReport selected = resultsTableView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    showStudentDetailDialog(selected);
                }
            }
        });

        refreshUiEnabledState();
    }

    private void refreshUiEnabledState() {
        boolean open = databaseService.isOpen();
        projectNameField.setDisable(!open);
        activeConfigCombo.setDisable(!open);
        expectedOutputField.setDisable(!open);
        runArgsField.setDisable(!open);
        normalizationCombo.setDisable(!open);
        compileTimeoutSpinner.setDisable(!open);
        runTimeoutSpinner.setDisable(!open);
        zipDirectoryField.setDisable(!open);
        sourceFileNameField.setDisable(!open);
        runEvaluationButton.setDisable(!open);
        configurationListView.setDisable(!open);
    }

    private void attachDaosAndServices() throws SQLException {
        configurationDAO = new ConfigurationDAO(databaseService.connection());
        projectDAO = new ProjectDAO(databaseService.connection());
        submissionDAO = new StudentSubmissionDAO(databaseService.connection());
        evaluationDAO = new EvaluationResultDAO(databaseService.connection());
        configurationService = new ConfigurationService(configurationDAO);
        configurationService.loadFromProject();
        executioner = new Executioner(configurationService, comparisonService, reportingService);
    }

    private String readMeta(String key) throws SQLException {
        try (PreparedStatement ps = databaseService.connection().prepareStatement(
                "SELECT value FROM meta WHERE key = ?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getString(1) != null ? rs.getString(1) : "";
            }
        }
    }

    private void writeMeta(String key, String value) throws SQLException {
        try (PreparedStatement ps = databaseService.connection().prepareStatement(
                "INSERT OR REPLACE INTO meta(key, value) VALUES(?, ?)")) {
            ps.setString(1, key);
            ps.setString(2, value != null ? value : "");
            ps.executeUpdate();
        }
    }

    private void refreshConfigurationListView() {
        List<String> items = new java.util.ArrayList<>();
        for (LanguageConfig lc : configurationService.listAll()) {
            String display = lc.getName() + "  (" + lc.getFileExtension() + ")";
            items.add(display);
        }
        configurationListView.setItems(FXCollections.observableArrayList(items));
    }

    private void refreshLanguageComboFromService() {
        List<String> names = configurationService.listAll().stream().map(LanguageConfig::getName).toList();
        String prev = activeConfigCombo.getValue();
        activeConfigCombo.getItems().setAll(names);
        if (prev != null && names.contains(prev)) {
            activeConfigCombo.setValue(prev);
        } else if (!names.isEmpty()) {
            activeConfigCombo.setValue(names.get(0));
        }
    }

    private void syncActiveLanguageAfterComboChange() {
        String name = activeConfigCombo.getValue();
        if (name != null && configurationService.listAll().stream().anyMatch(c -> name.equals(c.getName()))) {
            configurationService.setActiveLanguage(name);
        }
    }

    private Project buildProjectFromUi() throws SQLException {
        Project p = projectDAO.loadProject();
        Objects.requireNonNull(p);
        p.setName(trimOrNull(projectNameField.getText()));

        String lang = activeConfigCombo.getValue();
        Integer cfgId = null;
        if (lang != null) {
            LanguageConfig lc = configurationService.getConfigForLanguage(lang);
            if (lc != null) {
                cfgId = lc.getId();
            }
        }
        p.setConfigurationId(cfgId);
        p.setExpectedOutputPath(trimOrNull(expectedOutputField.getText()));
        p.setRunArgs(trimOrNull(runArgsField.getText()));
        p.setCompileTimeoutSec(compileTimeoutSpinner.getValue());
        p.setRunTimeoutSec(runTimeoutSpinner.getValue());
        p.setNormalizationMode(normalizationCombo.getValue() != null
                ? normalizationCombo.getValue() : NormalizationMode.STRICT);
        return p;
    }

    private static String trimOrNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private void loadUiFromDatabase() throws SQLException {
        Project p = projectDAO.loadProject();
        projectNameField.setText(p.getName() != null ? p.getName() : "");
        expectedOutputField.setText(p.getExpectedOutputPath() != null ? p.getExpectedOutputPath() : "");
        runArgsField.setText(p.getRunArgs() != null ? p.getRunArgs() : "");
        normalizationCombo.setValue(p.getNormalizationMode());
        compileTimeoutSpinner.getValueFactory().setValue(p.getCompileTimeoutSec());
        runTimeoutSpinner.getValueFactory().setValue(p.getRunTimeoutSec());

        zipDirectoryField.setText(readMeta(META_ZIP));
        String srcMeta = readMeta(META_SRC);
        sourceFileNameField.setText(srcMeta.isEmpty() ? "main.c" : srcMeta);

        refreshConfigurationListView();
        refreshLanguageComboFromService();
        syncActiveConfigCombo(p);

        resultsItems.setAll(evaluationDAO.findAll());
        resultsSummaryLabel.setText(buildSummaryLabel(resultsItems));

        Path cp = databaseService.currentProjectPath();
        projectPathLabel.setText(cp != null ? cp.toString() : "(no project open)");
        refreshUiEnabledState();
    }

    private void syncActiveConfigCombo(Project p) {
        refreshLanguageComboFromService();
        if (p.getConfigurationId() != null) {
            for (LanguageConfig lc : configurationService.listAll()) {
                if (p.getConfigurationId().equals(lc.getId())) {
                    activeConfigCombo.setValue(lc.getName());
                    try {
                        configurationService.setActiveLanguage(lc.getName());
                    } catch (IllegalArgumentException ignored) {
                        // ignore
                    }
                    return;
                }
            }
        }
        syncActiveLanguageAfterComboChange();
    }

    private static String buildSummaryLabel(List<StudentReport> reports) {
        if (reports.isEmpty()) {
            return "No rows.";
        }
        int pass = 0, fail = 0, err = 0;
        for (StudentReport r : reports) {
            if (r.getStatus() == null) {
                err++;
            } else {
                switch (r.getStatus()) {
                    case PASS: pass++; break;
                    case FAIL: fail++; break;
                    default: err++; break;
                }
            }
        }
        return String.format("Students: %d — PASS %d, FAIL %d, other %d",
                reports.size(), pass, fail, err);
    }

    private void persistEverything() throws SQLException {
        configurationService.saveToProject(configurationDAO);
        Project p = buildProjectFromUi();
        projectDAO.updateProject(p);
        writeMeta(META_ZIP, zipDirectoryField.getText());
        writeMeta(META_SRC, sourceFileNameField.getText());
        databaseService.saveProject();
    }

    @FXML
    private void onNewProject() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Create project");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("IAE project", "*.iae"));
        fc.setInitialFileName("assignment.iae");
        File f = fc.showSaveDialog(primaryStage);
        if (f == null) {
            return;
        }
        Path target = f.toPath();
        try {
            databaseService.createNewProject(target);
            attachDaosAndServices();
            loadUiFromDatabase();
            resultsItems.clear();
            reportingService.clearReports();
            resultsSummaryLabel.setText("No rows.");
            tabPane.getSelectionModel().select(1);
            assignmentHintLabel.setText("Project created. Add configurations, save, then run.");
        } catch (Exception ex) {
            alertError("New project", ex.getMessage());
            databaseService.closeProject();
            refreshUiEnabledState();
        }
    }

    @FXML
    private void onOpenProject() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Open project");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("IAE project", "*.iae"));
        File f = fc.showOpenDialog(primaryStage);
        if (f == null) {
            return;
        }
        try {
            databaseService.openProject(f.toPath());
            attachDaosAndServices();
            loadUiFromDatabase();
            assignmentHintLabel.setText("Project loaded.");
            tabPane.getSelectionModel().select(2);
        } catch (Exception ex) {
            alertError("Open project", ex.getMessage());
            databaseService.closeProject();
            refreshUiEnabledState();
        }
    }

    @FXML
    private void onSaveProject() {
        try {
            if (!databaseService.isOpen()) {
                alertError("Save project", "No project is open.");
                return;
            }
            persistEverything();
            assignmentHintLabel.setText("Saved.");
        } catch (Exception ex) {
            alertError("Save project", ex.getMessage());
        }
    }

    @FXML
    private void onExit() {
        Platform.exit();
    }

    @FXML
    private void onBrowseExpectedOutput() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Expected output file");
        File f = fc.showOpenDialog(primaryStage);
        if (f != null) {
            expectedOutputField.setText(f.getAbsolutePath());
        }
    }

    @FXML
    private void onBrowseZipDirectory() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Folder containing student ZIP files");
        File start = dc.showDialog(primaryStage);
        if (start != null) {
            zipDirectoryField.setText(start.getAbsolutePath());
        }
    }

    @FXML
    private void onRunEvaluation() {
        if (!databaseService.isOpen()) {
            alertError("Run", "Open or create a project first.");
            return;
        }
        String zipDir = zipDirectoryField.getText() != null ? zipDirectoryField.getText().trim() : "";
        String mainSrc = sourceFileNameField.getText() != null ? sourceFileNameField.getText().trim() : "";
        String expectedPath = expectedOutputField.getText() != null ? expectedOutputField.getText().trim() : "";

        if (activeConfigCombo.getValue() == null || configurationService.listAll().isEmpty()) {
            alertError("Run", "Add at least one configuration and select it.");
            return;
        }
        if (!Files.isDirectory(Path.of(zipDir))) {
            alertError("Run", "ZIP directory is missing or not a folder.");
            return;
        }
        if (mainSrc.isEmpty()) {
            alertError("Run", "Enter the main source file name (e.g. main.c).");
            return;
        }
        if (expectedPath.isEmpty() || !Files.isRegularFile(Path.of(expectedPath))) {
            alertError("Run", "Choose a valid expected output file.");
            return;
        }

        syncActiveLanguageAfterComboChange();

        runProgressIndicator.setVisible(true);
        runEvaluationButton.setDisable(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                synchronized (databaseService) {
                    persistEverything();
                    evaluationDAO.deleteAll();
                    submissionDAO.deleteAll();

                    List<Submission> subs = fileManager.prepareSubmissions(zipDir, mainSrc);
                    if (subs.isEmpty()) {
                        throw new IllegalStateException("No ZIP submissions processed. Check folder and source file name.");
                    }
                    submissionDAO.insertAll(subs);

                    reportingService.clearReports();
                    Project p = projectDAO.loadProject();
                    executioner.executeAll(subs,
                            p.getExpectedOutputPath(),
                            p.getRunArgs() != null ? p.getRunArgs() : "",
                            p.getCompileTimeoutSec(),
                            p.getRunTimeoutSec(),
                            p.getNormalizationMode().name());
                    reportingService.saveToProject(evaluationDAO);
                    databaseService.saveProject();
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            runProgressIndicator.setVisible(false);
            runEvaluationButton.setDisable(false);
            resultsItems.setAll(reportingService.getReports());
            resultsSummaryLabel.setText(buildSummaryLabel(resultsItems));
            tabPane.getSelectionModel().select(resultsTab);
            assignmentHintLabel.setText("Evaluation finished.");
        });
        task.setOnFailed(e -> {
            runProgressIndicator.setVisible(false);
            runEvaluationButton.setDisable(false);
            Throwable t = task.getException();
            alertError("Run failed", t != null ? t.getMessage() : "Unknown error");
        });

        Thread worker = new Thread(task, "iae-batch-run");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void onExportCsv() {
        if (resultsItems.isEmpty()) {
            alertError("Export CSV", "No results to export.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Export CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        fc.setInitialFileName("results.csv");
        File f = fc.showSaveDialog(primaryStage);
        if (f == null) {
            return;
        }
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(f.toPath(), StandardCharsets.UTF_8))) {
            pw.println("studentId,status,normalizationMode,timestamp,errorMessage,actualOutputPreview,diffLineCount");
            for (StudentReport r : resultsItems) {
                pw.println(r.toCSVRow());
            }
        } catch (Exception ex) {
            alertError("Export CSV", ex.getMessage());
        }
    }

    @FXML
    private void onAddConfiguration() {
        if (!databaseService.isOpen()) {
            alertError("Configuration", "Open a project first.");
            return;
        }
        editLanguageDialog(null).ifPresent(cfg -> {
            try {
                configurationService.addConfig(cfg);
                refreshConfigurationListView();
                refreshLanguageComboFromService();
                persistEverything();
            } catch (Exception ex) {
                alertError("Configuration", ex.getMessage());
            }
        });
    }

    @FXML
    private void onEditConfiguration() {
        if (!databaseService.isOpen()) {
            alertError("Configuration", "Open a project first.");
            return;
        }
        String name = toConfigName(configurationListView.getSelectionModel().getSelectedItem());
        if (name == null) {
            alertError("Configuration", "Select a configuration.");
            return;
        }
        LanguageConfig existing = configurationService.getConfigForLanguage(name);
        if (existing == null) {
            return;
        }
        editLanguageDialog(existing).ifPresent(cfg -> {
            try {
                configurationService.updateConfig(cfg);
                refreshConfigurationListView();
                refreshLanguageComboFromService();
                persistEverything();
            } catch (Exception ex) {
                alertError("Configuration", ex.getMessage());
            }
        });
    }

    @FXML
    private void onDeleteConfiguration() {
        if (!databaseService.isOpen()) {
            alertError("Configuration", "Open a project first.");
            return;
        }
        String name = toConfigName(configurationListView.getSelectionModel().getSelectedItem());
        if (name == null) {
            alertError("Configuration", "Select a configuration.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Remove configuration");
        confirm.setHeaderText("Remove \"" + name + "\"?");
        Optional<ButtonType> r = confirm.showAndWait();
        if (r.isEmpty() || r.get() != ButtonType.OK) {
            return;
        }
        try {
            configurationService.removeConfig(name);
            refreshConfigurationListView();
            refreshLanguageComboFromService();
            persistEverything();
        } catch (Exception ex) {
            alertError("Configuration", ex.getMessage());
        }
    }

    private Optional<LanguageConfig> editLanguageDialog(LanguageConfig seed) {
        Dialog<LanguageConfig> dialog = new Dialog<>();
        dialog.setTitle(seed == null ? "New configuration" : "Edit configuration");
        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));

        TextField nameField = new TextField();
        TextField extField = new TextField();
        TextField compilerField = new TextField();
        TextArea compileArgsArea = new TextArea();
        compileArgsArea.setPromptText("One argument per line (placeholders like {source}, {workDir})");
        compileArgsArea.setPrefRowCount(4);
        TextArea runArgsArea = new TextArea();
        runArgsArea.setPromptText("One argument per line");
        runArgsArea.setPrefRowCount(4);

        if (seed != null) {
            nameField.setText(seed.getName());
            nameField.setDisable(true);
            extField.setText(seed.getFileExtension());
            compilerField.setText(seed.getCompilerPath() != null ? seed.getCompilerPath() : "");
            compileArgsArea.setText(String.join("\n", seed.getCompileArgs()));
            runArgsArea.setText(String.join("\n", seed.getRunArgs()));
        }

        grid.add(new Label("Language name"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("File extension"), 0, 1);
        grid.add(extField, 1, 1);
        grid.add(new Label("Compiler path"), 0, 2);
        grid.add(compilerField, 1, 2);
        grid.add(new Label("Compile arguments"), 0, 3);
        grid.add(compileArgsArea, 1, 3);
        grid.add(new Label("Run arguments"), 0, 4);
        grid.add(runArgsArea, 1, 4);

        pane.setContent(grid);

        Button okBtn = (Button) pane.lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            LanguageConfig draft = new LanguageConfig(
                    nameField.getText() != null ? nameField.getText().trim() : "",
                    extField.getText() != null ? extField.getText().trim() : "",
                    blankToNull(compilerField.getText()),
                    linesToArgs(compileArgsArea.getText()),
                    linesToArgs(runArgsArea.getText()));
            if (seed != null) {
                draft.setId(seed.getId());
            }
            var vr = draft.validate();
            if (!vr.isValid()) {
                ev.consume();
                alertError("Invalid configuration", vr.getMessage());
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) {
                return null;
            }
            LanguageConfig cfg = new LanguageConfig(
                    nameField.getText() != null ? nameField.getText().trim() : "",
                    extField.getText() != null ? extField.getText().trim() : "",
                    blankToNull(compilerField.getText()),
                    linesToArgs(compileArgsArea.getText()),
                    linesToArgs(runArgsArea.getText()));
            if (seed != null) {
                cfg.setId(seed.getId());
            }
            return cfg;
        });

        return dialog.showAndWait();
    }

    private static String toConfigName(String displayName) {
        if (displayName == null) {
            return null;
        }
        int idx = displayName.indexOf("  (");
        if (idx >= 0) {
            return displayName.substring(0, idx);
        }
        return displayName;
    }

    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static List<String> linesToArgs(String text) {
        List<String> out = new java.util.ArrayList<>();
        if (text == null) {
            return out;
        }
        for (String line : text.split("\\R")) {
            String t = line.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    @FXML
    private void onImportConfigurations() {
        if (!databaseService.isOpen()) {
            alertError("Import", "Open a project first.");
            return;
        }
        Alert modeAlert = new Alert(Alert.AlertType.CONFIRMATION);
        modeAlert.setTitle("Import mode");
        modeAlert.setHeaderText("How should existing configurations be handled?");
        ButtonType mergeBtn = new ButtonType("Merge (keep existing)");
        ButtonType replaceBtn = new ButtonType("Replace all");
        modeAlert.getButtonTypes().setAll(mergeBtn, replaceBtn, ButtonType.CANCEL);
        Optional<ButtonType> modeChoice = modeAlert.showAndWait();
        if (modeChoice.isEmpty() || modeChoice.get() == ButtonType.CANCEL) {
            return;
        }
        ImportMode mode = modeChoice.get() == replaceBtn ? ImportMode.REPLACE : ImportMode.MERGE;

        FileChooser fc = new FileChooser();
        fc.setTitle("Import configurations");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON config", "*.json"));
        File f = fc.showOpenDialog(primaryStage);
        if (f == null) {
            return;
        }
        try {
            ImportResult result = new ImportExportService(configurationService).importFromFile(f.toPath(), mode);
            if (!result.isSuccess()) {
                alertError("Import failed", result.getErrorMessage());
                return;
            }
            refreshConfigurationListView();
            refreshLanguageComboFromService();
            persistEverything();

            StringBuilder msg = new StringBuilder();
            msg.append("Imported ").append(result.getImportedConfigs().size()).append(" configuration(s).");
            if (!result.getSkippedEntries().isEmpty()) {
                msg.append("\n\nSkipped ").append(result.getSkippedEntries().size())
                   .append(" (compiler not found on this machine):");
                for (SkippedEntry s : result.getSkippedEntries()) {
                    msg.append("\n  • ").append(s.getConfig().getName()).append(": ").append(s.getReason());
                }
            }
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Import complete");
            info.setHeaderText(null);
            info.setContentText(msg.toString());
            info.showAndWait();
        } catch (Exception ex) {
            alertError("Import", ex.getMessage());
        }
    }

    @FXML
    private void onExportConfigurations() {
        if (!databaseService.isOpen()) {
            alertError("Export", "Open a project first.");
            return;
        }
        if (configurationService == null || configurationService.listAll().isEmpty()) {
            alertError("Export", "No configurations to export. Add at least one first.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Export configurations");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON config", "*.json"));
        fc.setInitialFileName("configurations.json");
        File f = fc.showSaveDialog(primaryStage);
        if (f == null) {
            return;
        }
        try {
            new ImportExportService(configurationService).exportToFile(f.toPath());
            int count = configurationService.listAll().size();
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Export complete");
            info.setHeaderText(null);
            info.setContentText("Exported " + count + " configuration(s) to:\n" + f.getAbsolutePath());
            info.showAndWait();
        } catch (Exception ex) {
            alertError("Export", ex.getMessage());
        }
    }

    private void showStudentDetailDialog(StudentReport report) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Detail — " + report.getStudentId());
        dialog.setHeaderText("Status: " + (report.getStatus() != null ? report.getStatus().name() : "?")
                + "   |   " + (report.getTimestamp() != null ? report.getTimestamp() : ""));

        javafx.scene.control.TabPane tabs = new javafx.scene.control.TabPane();
        tabs.setTabClosingPolicy(javafx.scene.control.TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setPrefWidth(700);
        tabs.setPrefHeight(440);

        tabs.getTabs().add(makeDetailTab("Actual Output", report.getActualOutput()));
        tabs.getTabs().add(makeDetailTab("Expected Output", report.getExpectedOutput()));
        tabs.getTabs().add(makeDetailTab("Diff", buildDiffText(report)));
        tabs.getTabs().add(makeDetailTab("Error / Compiler Log", report.getErrorMessage()));

        // Open on the most relevant tab based on status
        ComparisonStatus status = report.getStatus();
        if (status == ComparisonStatus.COMPILE_ERROR
                || status == ComparisonStatus.TIMEOUT
                || status == ComparisonStatus.ERROR) {
            tabs.getSelectionModel().select(3); // Error / Compiler Log
        } else if (status == ComparisonStatus.FAIL) {
            tabs.getSelectionModel().select(2); // Diff
        }

        dialog.getDialogPane().setContent(tabs);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private static String buildDiffText(StudentReport report) {
        ComparisonStatus status = report.getStatus();
        if (status == null) return "(no status data)";
        switch (status) {
            case COMPILE_ERROR: return "(no comparison — compilation failed)";
            case TIMEOUT:       return "(no comparison — process timed out)";
            case ERROR:         return "(no comparison — an error prevented execution)";
            case PASS:          return "(outputs are identical — PASS)";
            default: break;
        }
        // FAIL: use stored diff lines if available
        List<String> lines = report.getDiffLines();
        if (lines != null && !lines.isEmpty()) {
            return String.join("\n", lines);
        }
        // Fallback: recompute diff from actual vs expected stored in the report
        String actual = report.getActualOutput();
        String expected = report.getExpectedOutput();
        if (actual == null || expected == null) {
            return "(diff data not available)";
        }
        String[] actualLines   = actual.replace("\r\n", "\n").split("\n", -1);
        String[] expectedLines = expected.replace("\r\n", "\n").split("\n", -1);
        StringBuilder sb = new StringBuilder();
        int maxLen = Math.max(actualLines.length, expectedLines.length);
        for (int i = 0; i < maxLen; i++) {
            String a = i < actualLines.length   ? actualLines[i]   : "<missing>";
            String e = i < expectedLines.length ? expectedLines[i] : "<missing>";
            if (!a.equals(e)) {
                sb.append("line ").append(i + 1)
                  .append(": expected [").append(e).append("] got [").append(a).append("]\n");
            }
        }
        return sb.length() > 0 ? sb.toString().trim() : "(no line differences found)";
    }

    private static Tab makeDetailTab(String title, String content) {
        javafx.scene.control.TextArea area = new javafx.scene.control.TextArea(content != null ? content : "");
        area.setEditable(false);
        area.setWrapText(true);
        area.setFont(javafx.scene.text.Font.font("Monospaced", 12));
        return new Tab(title, area);
    }

    private static void alertError(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message != null ? message : "");
        a.showAndWait();
    }
}
