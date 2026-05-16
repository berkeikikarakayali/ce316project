package com.ce316.iae;

import com.ce316.iae.dao.ConfigurationDAO;
import com.ce316.iae.dao.EvaluationResultDAO;
import com.ce316.iae.dao.ProjectDAO;
import com.ce316.iae.dao.StudentSubmissionDAO;
import com.ce316.iae.db.DatabaseService;
import com.ce316.iae.db.JsonArrayCodec;
import com.ce316.iae.engine.Executioner;
import com.ce316.iae.model.ImportMode;
import com.ce316.iae.model.ImportResult;
import com.ce316.iae.model.LanguageConfig;
import com.ce316.iae.model.NormalizationMode;
import com.ce316.iae.model.Project;
import com.ce316.iae.model.SkippedEntry;
import com.ce316.iae.model.StudentReport;
import com.ce316.iae.model.ValidationResult;
import com.ce316.iae.service.ComparisonService;
import com.ce316.iae.service.ConfigurationService;
import com.ce316.iae.service.FileManager;
import com.ce316.iae.service.ImportExportService;
import com.ce316.iae.service.ReportingService;
import com.ce316.iae.util.RunArgsCodec;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.awt.Desktop;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class App extends Application {

    private final DatabaseService db = new DatabaseService();

    private Stage primaryStage;

    private ComboBox<LanguageConfig> configurationCombo;
    private TextField assignmentNameField;
    private TextField zipFolderField;
    private TextField expectedOutputField;
    private TextField mainSourceFilenameField;
    private TextArea runArgsArea;
    private Spinner<Integer> compileTimeoutSpinner;
    private Spinner<Integer> runTimeoutSpinner;
    private ComboBox<NormalizationMode> normalizationCombo;

    private TableView<StudentReport> resultsTable;
    private TextArea logArea;

    private Label projectPathLabel;

    private final AtomicBoolean runInFlight = new AtomicBoolean(false);

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("Integrated Assignment Environment");

        BorderPane root = new BorderPane();

        MenuBar menuBar = buildMenuBar(stage);
        root.setTop(menuBar);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab projectTab = new Tab("Project", buildProjectPane());
        Tab resultsTab = new Tab("Results", buildResultsPane());
        Tab logTab = new Tab("Run log", buildLogPane());
        tabs.getTabs().addAll(projectTab, resultsTab, logTab);

        root.setCenter(tabs);

        Scene scene = new Scene(root, 1040, 720);
        stage.setScene(scene);
        stage.show();
    }

    private MenuBar buildMenuBar(Stage stage) {
        MenuBar bar = new MenuBar();

        Menu file = new Menu("File");
        MenuItem itemNew = new MenuItem("New project…");
        itemNew.setOnAction(e -> onNewProject());
        MenuItem itemOpen = new MenuItem("Open project…");
        itemOpen.setOnAction(e -> onOpenProject());
        MenuItem itemSave = new MenuItem("Save project");
        itemSave.setOnAction(e -> onSaveProject());
        MenuItem itemExit = new MenuItem("Exit");
        itemExit.setOnAction(e -> Platform.exit());
        file.getItems().addAll(itemNew, itemOpen, itemSave, new SeparatorMenuItem(), itemExit);

        Menu configuration = new Menu("Configuration");
        MenuItem manage = new MenuItem("Manage configurations…");
        manage.setOnAction(e -> openConfigurationEditor());
        MenuItem exportCfg = new MenuItem("Export configurations…");
        exportCfg.setOnAction(e -> exportConfigurations());
        MenuItem importCfg = new MenuItem("Import configurations…");
        importCfg.setOnAction(e -> importConfigurations());
        configuration.getItems().addAll(manage, exportCfg, importCfg);

        Menu runMenu = new Menu("Run");
        MenuItem runItem = new MenuItem("Execute assignment…");
        runItem.setOnAction(e -> executeAssignmentAsync());
        runMenu.getItems().add(runItem);

        Menu help = new Menu("Help");
        MenuItem manual = new MenuItem("User manual…");
        manual.setOnAction(e -> openManual());
        help.getItems().add(manual);

        bar.getMenus().addAll(file, configuration, runMenu, help);
        return bar;
    }

    private VBox buildProjectPane() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(12));

        projectPathLabel = new Label("No project open.");
        box.getChildren().add(projectPathLabel);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);

        int row = 0;
        assignmentNameField = new TextField();
        grid.add(new Label("Assignment name"), 0, row);
        grid.add(assignmentNameField, 1, row++);

        configurationCombo = new ComboBox<>();
        configurationCombo.setPrefWidth(420);
        configurationCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(LanguageConfig object) {
                return object == null ? "" : object.getName();
            }

            @Override
            public LanguageConfig fromString(String string) {
                return null;
            }
        });
        grid.add(new Label("Language configuration"), 0, row);
        grid.add(configurationCombo, 1, row++);

        zipFolderField = new TextField();
        Button browseZip = new Button("Browse…");
        browseZip.setOnAction(e -> browseDirectory(zipFolderField));
        HBox zipRow = new HBox(8, zipFolderField, browseZip);
        HBox.setHgrow(zipFolderField, Priority.ALWAYS);
        grid.add(new Label("ZIP folder"), 0, row);
        grid.add(zipRow, 1, row++);

        expectedOutputField = new TextField();
        Button browseExpected = new Button("Browse…");
        browseExpected.setOnAction(e -> browseExpectedOutput(expectedOutputField));
        HBox expRow = new HBox(8, expectedOutputField, browseExpected);
        HBox.setHgrow(expectedOutputField, Priority.ALWAYS);
        grid.add(new Label("Expected output file"), 0, row);
        grid.add(expRow, 1, row++);

        mainSourceFilenameField = new TextField();
        mainSourceFilenameField.setPromptText("Optional — e.g. main.c (otherwise inferred)");
        grid.add(new Label("Main source filename"), 0, row);
        grid.add(mainSourceFilenameField, 1, row++);

        runArgsArea = new TextArea();
        runArgsArea.setPrefRowCount(3);
        runArgsArea.setPromptText("JSON argv extras e.g. [\"alice\",\"bob\"] or whitespace-separated legacy tokens");
        grid.add(new Label("Extra argv (project)"), 0, row);
        grid.add(runArgsArea, 1, row++);

        compileTimeoutSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 3600, 60));
        grid.add(new Label("Compile timeout (s)"), 0, row);
        grid.add(compileTimeoutSpinner, 1, row++);

        runTimeoutSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 3600, 30));
        grid.add(new Label("Run timeout (s)"), 0, row);
        grid.add(runTimeoutSpinner, 1, row++);

        normalizationCombo = new ComboBox<>(FXCollections.observableArrayList(NormalizationMode.values()));
        normalizationCombo.getSelectionModel().select(NormalizationMode.STRICT);
        grid.add(new Label("Normalization mode"), 0, row);
        grid.add(normalizationCombo, 1, row++);

        Button saveBtn = new Button("Save project settings");
        saveBtn.setOnAction(e -> saveProjectFieldsSafely());

        box.getChildren().addAll(grid, saveBtn);
        return box;
    }

    private BorderPane buildResultsPane() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(12));

        resultsTable = new TableView<>();
        TableColumn<StudentReport, String> colStudent = new TableColumn<>("Student");
        colStudent.setPrefWidth(140);
        colStudent.setCellValueFactory(new PropertyValueFactory<>("studentId"));

        TableColumn<StudentReport, String> colStatus = new TableColumn<>("Status");
        colStatus.setPrefWidth(160);
        colStatus.setCellValueFactory(cdf -> new javafx.beans.property.ReadOnlyObjectWrapper<>(
                cdf.getValue().getStatus() != null ? cdf.getValue().getStatus().name() : ""));

        TableColumn<StudentReport, String> colTs = new TableColumn<>("Timestamp");
        colTs.setPrefWidth(220);
        colTs.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        TableColumn<StudentReport, String> colMsg = new TableColumn<>("Message");
        colMsg.setPrefWidth(420);
        colMsg.setCellValueFactory(new PropertyValueFactory<>("errorMessage"));
        resultsTable.getColumns().setAll(Arrays.asList(colStudent, colStatus, colTs, colMsg));

        HBox buttons = new HBox(8);
        Button refresh = new Button("Reload results");
        refresh.setOnAction(e -> reloadResultsTableSafely());
        Button exportCsv = new Button("Export CSV…");
        exportCsv.setOnAction(e -> exportResultsCsvSafely());
        buttons.getChildren().addAll(refresh, exportCsv);

        pane.setCenter(resultsTable);
        pane.setBottom(buttons);
        return pane;
    }

    private BorderPane buildLogPane() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(12));
        logArea = new TextArea();
        logArea.setEditable(false);
        pane.setCenter(logArea);
        return pane;
    }

    private void browseDirectory(TextField target) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose ZIP folder");
        Optional.ofNullable(chooser.showDialog(primaryStage)).ifPresent(dir ->
                target.setText(dir.toPath().toAbsolutePath().toString()));
    }

    private void browseExpectedOutput(TextField target) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose expected output file");
        Optional.ofNullable(chooser.showOpenDialog(primaryStage)).ifPresent(file ->
                target.setText(file.toPath().toAbsolutePath().toString()));
    }

    private void onNewProject() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Create project (.iae)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("IAE project", "*.iae"));
        java.io.File file = chooser.showSaveDialog(primaryStage);
        if (file == null) {
            return;
        }
        Path path = file.toPath();
        try {
            db.createNewProject(path);
            projectPathLabel.setText("Project: " + path.toAbsolutePath());
            reloadConfigurationsCombo();
            loadProjectFieldsFromDb();
            reloadResultsTableSafely();
            logArea.clear();
            appendLog("Created project at " + path.toAbsolutePath());
        } catch (Exception ex) {
            alertError("Could not create project", ex.getMessage());
        }
    }

    private void onOpenProject() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open project (.iae)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("IAE project", "*.iae"));
        java.io.File file = chooser.showOpenDialog(primaryStage);
        if (file == null) {
            return;
        }
        Path path = file.toPath();
        try {
            db.openProject(path);
            projectPathLabel.setText("Project: " + path.toAbsolutePath());
            reloadConfigurationsCombo();
            loadProjectFieldsFromDb();
            reloadResultsTableSafely();
            logArea.clear();
            appendLog("Opened project at " + path.toAbsolutePath());
        } catch (Exception ex) {
            alertError("Could not open project", ex.getMessage());
        }
    }

    private void onSaveProject() {
        saveProjectFieldsSafely();
    }

    private void saveProjectFieldsSafely() {
        if (!db.isOpen()) {
            alertWarn("Save project", "No project is open.");
            return;
        }
        try {
            ProjectDAO dao = new ProjectDAO(db.connection());
            Project project = dao.loadProject();
            if (project == null) {
                alertWarn("Save project", "Project row missing.");
                return;
            }
            fillProjectFromUi(project);
            dao.updateProject(project);
            db.saveProject();
            appendLog("Project saved.");
        } catch (Exception ex) {
            alertError("Save failed", ex.getMessage());
        }
    }

    private void fillProjectFromUi(Project project) {
        project.setName(trimToNull(assignmentNameField.getText()));
        LanguageConfig sel = configurationCombo.getSelectionModel().getSelectedItem();
        project.setConfigurationId(sel != null ? sel.getId() : null);
        project.setZipFolderPath(trimToNull(zipFolderField.getText()));
        project.setExpectedOutputPath(trimToNull(expectedOutputField.getText()));
        project.setMainSourceFilename(trimToNull(mainSourceFilenameField.getText()));
        project.setRunArgs(trimToNull(runArgsArea.getText()));
        project.setCompileTimeoutSec(compileTimeoutSpinner.getValue());
        project.setRunTimeoutSec(runTimeoutSpinner.getValue());
        project.setNormalizationMode(normalizationCombo.getSelectionModel().getSelectedItem());
    }

    private void loadProjectFieldsFromDb() {
        if (!db.isOpen()) {
            return;
        }
        try {
            ProjectDAO dao = new ProjectDAO(db.connection());
            Project project = dao.loadProject();
            if (project == null) {
                return;
            }
            assignmentNameField.setText(nullToEmpty(project.getName()));
            zipFolderField.setText(nullToEmpty(project.getZipFolderPath()));
            expectedOutputField.setText(nullToEmpty(project.getExpectedOutputPath()));
            mainSourceFilenameField.setText(nullToEmpty(project.getMainSourceFilename()));
            runArgsArea.setText(nullToEmpty(project.getRunArgs()));

            compileTimeoutSpinner.getValueFactory().setValue(project.getCompileTimeoutSec());
            runTimeoutSpinner.getValueFactory().setValue(project.getRunTimeoutSec());

            NormalizationMode norm = project.getNormalizationMode();
            normalizationCombo.getSelectionModel().select(norm != null ? norm : NormalizationMode.STRICT);

            LanguageConfig match = null;
            Integer cfgId = project.getConfigurationId();
            if (cfgId != null) {
                ConfigurationService svc = new ConfigurationService(new ConfigurationDAO(db.connection()));
                svc.reloadFromDb();
                match = svc.findById(cfgId);
            }
            configurationCombo.getSelectionModel().select(match);
        } catch (Exception ex) {
            alertError("Could not load project fields", ex.getMessage());
        }
    }

    private void reloadConfigurationsCombo() {
        configurationCombo.getItems().clear();
        if (!db.isOpen()) {
            return;
        }
        try {
            ConfigurationService svc = new ConfigurationService(new ConfigurationDAO(db.connection()));
            svc.reloadFromDb();
            configurationCombo.getItems().addAll(svc.listAll());
        } catch (SQLException ex) {
            alertError("Could not load configurations", ex.getMessage());
        }
    }

    private void reloadResultsTableSafely() {
        resultsTable.getItems().clear();
        if (!db.isOpen()) {
            return;
        }
        try {
            EvaluationResultDAO dao = new EvaluationResultDAO(db.connection());
            resultsTable.setItems(FXCollections.observableArrayList(dao.findAllJoinedWithStudent()));
        } catch (SQLException ex) {
            alertError("Could not load evaluation results", ex.getMessage());
        }
    }

    private void exportResultsCsvSafely() {
        if (!db.isOpen()) {
            alertWarn("Export CSV", "No project open.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        java.io.File target = chooser.showSaveDialog(primaryStage);
        if (target == null) {
            return;
        }
        try {
            EvaluationResultDAO dao = new EvaluationResultDAO(db.connection());
            List<StudentReport> rows = dao.findAllJoinedWithStudent();
            ReportingService svc = new ReportingService(dao);
            svc.exportCsv(target.toPath(), rows);
            appendLog("Exported CSV → " + target.toPath().toAbsolutePath());
        } catch (Exception ex) {
            alertError("CSV export failed", ex.getMessage());
        }
    }

    private void openConfigurationEditor() {
        if (!db.isOpen()) {
            alertWarn("Configurations", "Open or create a project first.");
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(primaryStage);
        dialog.setTitle("Language configurations");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        ListView<LanguageConfig> list = new ListView<>();
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(LanguageConfig item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " (." + item.getFileExtension() + ")");
            }
        });

        Runnable reloadList = () -> {
            try {
                ConfigurationService svc = new ConfigurationService(new ConfigurationDAO(db.connection()));
                svc.reloadFromDb();
                list.setItems(FXCollections.observableArrayList(svc.listAll()));
            } catch (SQLException ex) {
                alertError("Reload configurations failed", ex.getMessage());
            }
        };

        Button add = new Button("Add…");
        Button edit = new Button("Edit…");
        Button delete = new Button("Delete");

        add.setOnAction(e -> {
            Optional<LanguageConfig> cfg = editLanguageConfigDialog(null);
            cfg.ifPresent(c -> {
                try {
                    ValidationResult vr = c.validate();
                    if (!vr.isValid()) {
                        alertWarn("Validation failed", vr.getMessage());
                        return;
                    }
                    ConfigurationService svc = new ConfigurationService(new ConfigurationDAO(db.connection()));
                    svc.upsert(c);
                    reloadList.run();
                    reloadConfigurationsCombo();
                } catch (SQLException ex) {
                    alertError("Could not save configuration", ex.getMessage());
                }
            });
        });

        edit.setOnAction(e -> {
            LanguageConfig selected = list.getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            Optional<LanguageConfig> cfg = editLanguageConfigDialog(selected);
            cfg.ifPresent(c -> {
                try {
                    ValidationResult vr = c.validate();
                    if (!vr.isValid()) {
                        alertWarn("Validation failed", vr.getMessage());
                        return;
                    }
                    ConfigurationService svc = new ConfigurationService(new ConfigurationDAO(db.connection()));
                    svc.upsert(c);
                    reloadList.run();
                    reloadConfigurationsCombo();
                } catch (SQLException ex) {
                    alertError("Could not update configuration", ex.getMessage());
                }
            });
        });

        delete.setOnAction(e -> {
            LanguageConfig selected = list.getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Delete configuration '" + selected.getName() + "'?",
                    ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText("Confirm delete");
            Optional<ButtonType> ans = confirm.showAndWait();
            if (ans.isPresent() && ans.get() == ButtonType.YES) {
                try {
                    ConfigurationService svc = new ConfigurationService(new ConfigurationDAO(db.connection()));
                    svc.delete(selected.getName());
                    reloadList.run();
                    reloadConfigurationsCombo();
                } catch (SQLException ex) {
                    alertError("Could not delete configuration", ex.getMessage());
                }
            }
        });

        HBox buttons = new HBox(8, add, edit, delete);
        VBox content = new VBox(10, list, buttons);
        VBox.setVgrow(list, Priority.ALWAYS);
        list.setPrefHeight(420);
        dialog.getDialogPane().setContent(content);

        reloadList.run();
        dialog.showAndWait();
    }

    private Optional<LanguageConfig> editLanguageConfigDialog(LanguageConfig seed) {
        Dialog<LanguageConfig> dlg = new Dialog<>();
        dlg.initOwner(primaryStage);
        dlg.setTitle(seed == null ? "New configuration" : "Edit configuration");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));

        TextField name = new TextField(seed != null ? seed.getName() : "");
        TextField ext = new TextField(seed != null ? seed.getFileExtension() : "");
        TextField compiler = new TextField(seed != null ? nullToEmpty(seed.getCompilerPath()) : "");

        TextArea compileArgs = new TextArea(JsonArrayCodec.encode(seed != null ? seed.getCompileArgs() : List.of("-o", "main", "{SRC}")));
        compileArgs.setPrefRowCount(4);

        TextArea runArgs = new TextArea(JsonArrayCodec.encode(seed != null ? seed.getRunArgs() : List.of("./main")));
        runArgs.setPrefRowCount(4);

        int row = 0;
        grid.add(new Label("Language name"), 0, row);
        grid.add(name, 1, row++);
        grid.add(new Label("File extension"), 0, row);
        grid.add(ext, 1, row++);
        grid.add(new Label("Compiler/interpreter path"), 0, row);
        grid.add(compiler, 1, row++);
        grid.add(new Label("Compile args (JSON array)"), 0, row);
        grid.add(compileArgs, 1, row++);
        grid.add(new Label("Run args (JSON array)"), 0, row);
        grid.add(runArgs, 1, row++);

        dlg.getDialogPane().setContent(grid);

        dlg.setResultConverter(btn -> {
            if (btn != ButtonType.OK) {
                return null;
            }
            try {
                List<String> cArgs = JsonArrayCodec.decode(compileArgs.getText());
                List<String> rArgs = JsonArrayCodec.decode(runArgs.getText());
                LanguageConfig cfg = new LanguageConfig(
                        name.getText().trim(),
                        ext.getText().trim(),
                        compiler.getText().trim(),
                        cArgs,
                        rArgs);
                if (seed != null && seed.getId() != null) {
                    cfg.setId(seed.getId());
                }
                return cfg;
            } catch (IllegalArgumentException ex) {
                alertWarn("Invalid JSON", ex.getMessage());
                return null;
            }
        });

        return dlg.showAndWait();
    }

    private void exportConfigurations() {
        if (!db.isOpen()) {
            alertWarn("Export", "Open a project first.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export configurations JSON");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        java.io.File target = chooser.showSaveDialog(primaryStage);
        if (target == null) {
            return;
        }
        try {
            ImportExportService svc = new ImportExportService();
            svc.exportToFile(new ConfigurationDAO(db.connection()), target.toPath());
            appendLog("Exported configurations → " + target.toPath().toAbsolutePath());
        } catch (Exception ex) {
            alertError("Export failed", ex.getMessage());
        }
    }

    private void importConfigurations() {
        if (!db.isOpen()) {
            alertWarn("Import", "Open a project first.");
            return;
        }
        ChoiceDialog<ImportMode> modeDlg = new ChoiceDialog<>(ImportMode.MERGE,
                Arrays.asList(ImportMode.values()));
        modeDlg.initOwner(primaryStage);
        modeDlg.setTitle("Import mode");
        modeDlg.setHeaderText("Choose import strategy");
        modeDlg.setContentText("MERGE updates matching names; REPLACE clears existing configs first.");
        Optional<ImportMode> mode = modeDlg.showAndWait();
        if (mode.isEmpty()) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import configurations JSON");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        java.io.File src = chooser.showOpenDialog(primaryStage);
        if (src == null) {
            return;
        }

        try {
            ConfigurationService svc = new ConfigurationService(new ConfigurationDAO(db.connection()));
            svc.reloadFromDb();
            ImportExportService importer = new ImportExportService();
            ImportResult result = importer.importFromFile(svc, src.toPath(), mode.get());

            StringBuilder msg = new StringBuilder();
            msg.append(result.isSuccess() ? "Import finished.\n" : "Import failed.\n");
            if (!result.isSuccess()) {
                msg.append(result.getErrorMessage()).append('\n');
            }
            msg.append("Imported: ").append(result.getImported().size()).append('\n');
            if (!result.getSkipped().isEmpty()) {
                msg.append("Skipped:\n");
                for (SkippedEntry se : result.getSkipped()) {
                    msg.append("- ").append(se.getConfig().getName()).append(": ").append(se.getReason()).append('\n');
                }
            }

            Alert summary = new Alert(Alert.AlertType.INFORMATION, msg.toString(), ButtonType.OK);
            summary.setHeaderText("Import summary");
            summary.getDialogPane().setPrefWidth(560);
            summary.showAndWait();

            reloadConfigurationsCombo();
            loadProjectFieldsFromDb();
        } catch (Exception ex) {
            alertError("Import failed", ex.getMessage());
        }
    }

    private void executeAssignmentAsync() {
        if (!db.isOpen()) {
            alertWarn("Run", "Open or create a project first.");
            return;
        }
        if (!runInFlight.compareAndSet(false, true)) {
            alertWarn("Run", "A run is already in progress.");
            return;
        }

        try {
            ProjectDAO projectDAO = new ProjectDAO(db.connection());
            Project project = projectDAO.loadProject();
            if (project == null) {
                alertWarn("Run", "Project metadata missing.");
                runInFlight.set(false);
                return;
            }

            fillProjectFromUi(project);
            projectDAO.updateProject(project);
            db.saveProject();

            LanguageConfig cfg = new ConfigurationService(new ConfigurationDAO(db.connection()))
                    .findById(project.getConfigurationId());
            if (cfg == null) {
                alertWarn("Run", "Select a language configuration before running.");
                runInFlight.set(false);
                return;
            }

            ValidationResult vr = cfg.validate();
            if (!vr.isValid()) {
                alertWarn("Invalid configuration", vr.getMessage());
                runInFlight.set(false);
                return;
            }

            String zipStr = project.getZipFolderPath();
            String expectedStr = project.getExpectedOutputPath();
            if (zipStr == null || zipStr.isBlank()) {
                alertWarn("Run", "ZIP folder path is required.");
                runInFlight.set(false);
                return;
            }
            if (expectedStr == null || expectedStr.isBlank()) {
                alertWarn("Run", "Expected output path is required.");
                runInFlight.set(false);
                return;
            }

            Path zipFolder = Path.of(zipStr);
            Path expected = Path.of(expectedStr);
            if (!Files.isDirectory(zipFolder)) {
                alertWarn("Run", "ZIP folder does not exist:\n" + zipFolder.toAbsolutePath());
                runInFlight.set(false);
                return;
            }
            if (!Files.isRegularFile(expected)) {
                alertWarn("Run", "Expected output file missing:\n" + expected.toAbsolutePath());
                runInFlight.set(false);
                return;
            }

            List<String> projArgs = RunArgsCodec.parseProjectRunArgs(project.getRunArgs());
            String mainSrc = trimToNull(project.getMainSourceFilename());
            int compileTo = project.getCompileTimeoutSec();
            int runTo = project.getRunTimeoutSec();
            NormalizationMode norm = project.getNormalizationMode();

            Thread runner = new Thread(() -> runAssignmentPipeline(cfg, zipFolder, expected, projArgs,
                    mainSrc, compileTo, runTo, norm), "iae-runner");
            runner.setDaemon(true);
            runner.start();
        } catch (Exception ex) {
            runInFlight.set(false);
            alertError("Run preparation failed", ex.getMessage());
        }
    }

    private void runAssignmentPipeline(LanguageConfig cfg,
                                       Path zipFolder,
                                       Path expectedOutput,
                                       List<String> projectRunArgs,
                                       String mainSourceFilenameOrNull,
                                       int compileTimeoutSec,
                                       int runTimeoutSec,
                                       NormalizationMode normalizationMode) {
        try {
            appendLogAsync("Starting batch evaluation…");

            FileManager fm = new FileManager();
            List<com.ce316.iae.model.Submission> submissions = fm.prepareSubmissions(
                    zipFolder,
                    cfg,
                    mainSourceFilenameOrNull);

            appendLogAsync("Prepared submissions: " + submissions.size());

            StudentSubmissionDAO subDao = new StudentSubmissionDAO(db.connection());
            EvaluationResultDAO evalDao = new EvaluationResultDAO(db.connection());

            subDao.deleteAll();
            subDao.insertAll(submissions);

            ReportingService reporting = new ReportingService(evalDao);
            reporting.clearPending();

            ComparisonService comparison = new ComparisonService();
            Executioner executioner = new Executioner(comparison, reporting, msg ->
                    Platform.runLater(() -> appendLog(msg)));

            executioner.executeAll(
                    cfg,
                    submissions,
                    expectedOutput,
                    projectRunArgs,
                    compileTimeoutSec,
                    runTimeoutSec,
                    normalizationMode);

            reporting.saveToProject();
            db.saveProject();

            appendLogAsync("Run complete — results saved.");
            Platform.runLater(this::reloadResultsTableSafely);
        } catch (Exception ex) {
            appendLogAsync("Run failed: " + ex.getMessage());
            Platform.runLater(() -> alertError("Run failed", ex.getMessage()));
        } finally {
            runInFlight.set(false);
        }
    }

    private void appendLog(String line) {
        logArea.appendText(line + "\n");
    }

    private void appendLogAsync(String line) {
        Platform.runLater(() -> appendLog(line));
    }

    private void openManual() {
        try (InputStream in = App.class.getResourceAsStream("/manual/manual.html")) {
            if (in == null) {
                alertWarn("Manual", "Bundled manual resource missing.");
                return;
            }
            Path tmp = Files.createTempFile("iae-manual-", ".html");
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            tmp.toFile().deleteOnExit();

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(tmp.toUri());
            } else {
                Alert alert = new Alert(Alert.AlertType.INFORMATION,
                        "Open this file in a browser:\n" + tmp.toAbsolutePath(),
                        ButtonType.OK);
                alert.setHeaderText("Manual location");
                alert.showAndWait();
            }
        } catch (Exception ex) {
            alertError("Could not open manual", ex.getMessage());
        }
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private void alertError(String header, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR, message == null ? "" : message, ButtonType.OK);
        a.setHeaderText(header);
        a.showAndWait();
    }

    private void alertWarn(String header, String message) {
        Alert a = new Alert(Alert.AlertType.WARNING, message == null ? "" : message, ButtonType.OK);
        a.setHeaderText(header);
        a.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
