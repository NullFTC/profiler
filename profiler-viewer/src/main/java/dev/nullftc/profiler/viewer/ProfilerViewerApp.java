package dev.nullftc.profiler.viewer;

import dev.nullftc.profiler.analysis.AnalysisFinding;
import dev.nullftc.profiler.analysis.ProfilerAnalyzer;
import dev.nullftc.profiler.analysis.SpanAggregate;
import dev.nullftc.profiler.analysis.SpanAggregates;
import dev.nullftc.profiler.importer.TraceImporters;
import dev.nullftc.profiler.trace.TraceSession;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class ProfilerViewerApp extends Application {
    private final AdbTraceService adbTraceService = new AdbTraceService();
    private final TableView<SpanAggregate> aggregateTable = new TableView<>();
    private final TableView<AdbTraceFile> deviceTraceTable = new TableView<>();
    private final ListView<String> findingsList = new ListView<>();
    private final Label statusLabel = new Label("Open a profiler JSON or CSV trace");

    @Override
    public void start(Stage stage) {
        configureTable();
        configureDeviceTraceTable();

        Button openButton = new Button("Open Trace");
        openButton.setOnAction(event -> openTrace(stage));
        Button refreshDeviceButton = new Button("Refresh Device");
        refreshDeviceButton.setOnAction(event -> refreshDeviceTraces());
        Button analyzeDeviceButton = new Button("Analyze Selected");
        analyzeDeviceButton.setOnAction(event -> analyzeSelectedDeviceTrace());

        HBox toolbar = new HBox(8, openButton, refreshDeviceButton, analyzeDeviceButton, statusLabel);
        toolbar.setPadding(new Insets(8));

        TitledPane devicePane = new TitledPane("Device traces: " + AdbTraceService.DEFAULT_DEVICE_DIR, deviceTraceTable);
        devicePane.setCollapsible(false);

        SplitPane analysisPane = new SplitPane(aggregateTable, findingsList);
        analysisPane.setDividerPositions(0.65);

        SplitPane splitPane = new SplitPane(devicePane, analysisPane);
        splitPane.setDividerPositions(0.30);

        BorderPane root = new BorderPane(splitPane);
        root.setTop(toolbar);

        stage.setTitle("FTC Profiler Offline Analyzer");
        stage.setScene(new Scene(root, 1100, 720));
        stage.show();

        List<String> arguments = getParameters().getUnnamed();
        if (!arguments.isEmpty()) {
            loadTrace(new File(arguments.get(0)));
        }
    }

    private void configureTable() {
        TableColumn<SpanAggregate, String> name = column("Span", SpanAggregate::getName);
        TableColumn<SpanAggregate, String> count = column("Count", aggregate -> String.valueOf(aggregate.getCount()));
        TableColumn<SpanAggregate, String> total = column("Total ms", aggregate -> format(aggregate.getTotalMillis()));
        TableColumn<SpanAggregate, String> average = column("Avg ms", aggregate -> format(aggregate.getAverageMillis()));
        TableColumn<SpanAggregate, String> max = column("Max ms", aggregate -> format(aggregate.getMaxMillis()));
        aggregateTable.getColumns().add(name);
        aggregateTable.getColumns().add(count);
        aggregateTable.getColumns().add(total);
        aggregateTable.getColumns().add(average);
        aggregateTable.getColumns().add(max);
        aggregateTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
    }

    private void configureDeviceTraceTable() {
        TableColumn<AdbTraceFile, String> name = deviceColumn("File", AdbTraceFile::getName);
        TableColumn<AdbTraceFile, String> modified = deviceColumn("Updated", AdbTraceFile::getModified);
        TableColumn<AdbTraceFile, String> size = deviceColumn("Size", trace -> formatBytes(trace.getBytes()));
        deviceTraceTable.getColumns().add(name);
        deviceTraceTable.getColumns().add(modified);
        deviceTraceTable.getColumns().add(size);
        deviceTraceTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        deviceTraceTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                analyzeSelectedDeviceTrace();
            }
        });
    }

    private void openTrace(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open profiler trace");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Profiler traces", "*.json", "*.csv"));
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }
        loadTrace(file);
    }

    private void loadTrace(File file) {
        try {
            TraceSession session = TraceImporters.load(file.toPath());
            aggregateTable.setItems(FXCollections.observableArrayList(SpanAggregates.byName(session)));
            List<AnalysisFinding> findings = new ProfilerAnalyzer().analyze(session).getFindings();
            findingsList.setItems(FXCollections.observableArrayList(findings.stream().map(this::formatFinding).toList()));
            statusLabel.setText(session.getName() + " - " + session.getSpans().size() + " spans");
        } catch (IOException | RuntimeException exception) {
            statusLabel.setText("Could not load trace: " + exception.getMessage());
        }
    }

    private void refreshDeviceTraces() {
        statusLabel.setText("Scanning device traces...");
        Thread worker = new Thread(() -> {
            try {
                List<AdbTraceFile> files = adbTraceService.listTraceFiles();
                Platform.runLater(() -> {
                    deviceTraceTable.setItems(FXCollections.observableArrayList(files));
                    statusLabel.setText("Device traces: " + files.size());
                });
            } catch (IOException | InterruptedException exception) {
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                Platform.runLater(() -> showError("Could not scan device", exception.getMessage()));
            }
        }, "adb-trace-list");
        worker.setDaemon(true);
        worker.start();
    }

    private void analyzeSelectedDeviceTrace() {
        AdbTraceFile selected = deviceTraceTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a device trace");
            return;
        }

        statusLabel.setText("Pulling " + selected.getName() + "...");
        Thread worker = new Thread(() -> {
            try {
                Path localFile = adbTraceService.pull(selected);
                Platform.runLater(() -> loadTrace(localFile.toFile()));
            } catch (IOException | InterruptedException exception) {
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                Platform.runLater(() -> showError("Could not pull trace", exception.getMessage()));
            }
        }, "adb-trace-pull");
        worker.setDaemon(true);
        worker.start();
    }

    private void showError(String title, String message) {
        statusLabel.setText(title + ": " + message);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.show();
    }

    private String formatFinding(AnalysisFinding finding) {
        return finding.getSeverity() + " - " + finding.getTitle() + "\n" + finding.getEvidence() + "\n" + finding.getRecommendation();
    }

    private static String format(double value) {
        return String.format("%.3f", value);
    }

    private static TableColumn<SpanAggregate, String> column(String title, ValueReader reader) {
        TableColumn<SpanAggregate, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(reader.read(data.getValue())));
        return column;
    }

    private static TableColumn<AdbTraceFile, String> deviceColumn(String title, DeviceValueReader reader) {
        TableColumn<AdbTraceFile, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(reader.read(data.getValue())));
        return column;
    }

    private static String formatBytes(long bytes) {
        if (bytes >= 1_048_576L) {
            return String.format("%.1f MB", bytes / 1_048_576.0);
        }
        if (bytes >= 1024L) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        return bytes + " B";
    }

    private interface ValueReader {
        String read(SpanAggregate aggregate);
    }

    private interface DeviceValueReader {
        String read(AdbTraceFile traceFile);
    }
}
