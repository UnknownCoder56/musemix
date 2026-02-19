package com.uniqueapps.musemix;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import javax.sound.midi.*;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.ResourceBundle;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class HomeController implements Initializable, EventHandler<MouseEvent> {

    // General UI variables
    @FXML
    private TabPane parentTabPane;

    // Tester UI variables
    @FXML
    private Label instrumentLabel;
    @FXML
    private TextField instrument;
    @FXML
    private TextField note;
    @FXML
    private TextField duration;
    @FXML
    private TextArea orchestraList;
    @FXML
    private Button play;
    @FXML
    private Button loopInstruments;
    @FXML
    private Button loopNotes;
    @FXML
    private Button loopAll;
    @FXML
    private Button loopRandom;
    @FXML
    private CheckBox wait;

    // Composer UI variables
    @FXML
    private VBox noteHeaderColumn;
    @FXML
    private ListView<Step> sequencerGrid;
    @FXML
    private Region playheadOverlay;
    @FXML
    private StackPane recordModeInstrumentChoiceBoxStackPane;
    @FXML
    private Label recordStepLabel;
    @FXML
    private Button playCompositionButton;
    @FXML
    private Button pauseCompositionButton;
    @FXML
    private Button resetTimelineButton;
    @FXML
    private Button scrollToPlayheadButton;
    @FXML
    private Button changeTempoButton;
    @FXML
    private Button addStepButton;
    @FXML
    private Button removeStepButton;
    @FXML
    private Button addRowButton;
    @FXML
    private Button removeRowButton;
    @FXML
    private Button recordModeButton;
    @FXML
    private Button exportMidiButton;
    @FXML
    private Button importMidiButton;
    @FXML
    private ChoiceBox<String> recordModeInstrumentChoiceBox;

    // General variables (tester + composer)
    private int MAX_INSTRUMENTS = 0;
    private Instrument[] orchestra;

    // For composer function
    private static final int[] DEFAULT_NOTES = {60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72};
    private final ObservableList<Step> steps = FXCollections.observableArrayList(step -> new Observable[]{step.cellsProperty()});
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "timeline-thread");
        t.setDaemon(true);
        return t;
    });
    private ScheduledFuture<?> timelineFuture;
    private final AtomicBoolean isPaused = new AtomicBoolean(true);
    // 0 - timelineFuture not started yet, playheadAnimator waiting
    // 1 onward - timelineFuture starts and sets 1, playheadAnimator begins
    private final AtomicInteger playheadStep = new AtomicInteger(0);
    private int tempo = 60;
    private boolean recordMode = false;

    // For composer visuals
    private static final double STEP_WIDTH = 100.0;
    private ScrollBar hBar;
    private VirtualFlow<ListCell<Step>> virtualFlow;
    private AnimationTimer playheadAnimator;
    private double stepDurationNanos;
    private boolean scrollToPlayhead = false;
    private final IntegerProperty recordColumn = new SimpleIntegerProperty(0);

    @SuppressWarnings("unchecked")
    @FXML
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // General initialization
        parentTabPane.getStyleClass().add(TabPane.STYLE_CLASS_FLOATING);

        // Tester initialization
        StringBuilder sb = new StringBuilder();
        sb.append("Instruments:\n");
        try (Synthesizer synthesizer = MidiSystem.getSynthesizer()) {
            synthesizer.open();
            orchestra = synthesizer.getLoadedInstruments();
            MAX_INSTRUMENTS = orchestra.length;
            sb.append("\n").append(InstrumentCellData.DRUM).append(") ").append("Drum Kit");
            for (int i = 0; i < MAX_INSTRUMENTS; i++) {
                sb.append("\n").append(i).append(") ").append(orchestra[i].getName().trim());
            }
        } catch (MidiUnavailableException e) {
            new Alert(Alert.AlertType.ERROR, "MIDI system is unavailable").showAndWait();
            System.exit(1);
        }
        orchestraList.setText(sb.toString());
        instrumentLabel.setText("Instrument (0 - " + (MAX_INSTRUMENTS - 1) + " or " + InstrumentCellData.DRUM + ")");


        // Composer initialization

        // Remove all default container styling that causes separators/gaps and add support for translucency
        noteHeaderColumn.setStyle("-fx-border-width: 0px");
        sequencerGrid.setStyle("-fx-padding: 0; -fx-background-color: transparent; -fx-background-insets: 0; -fx-border-width: 0;");

        // Note header column initialization
        Label labelRC = new Label("Note/Step");
        labelRC.setStyle("-fx-text-fill: white; -fx-padding: 3px; -fx-background-color: \"#333333\"; -fx-border-color: gray; -fx-border-width: 1px;");
        labelRC.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(labelRC, Priority.ALWAYS);
        labelRC.setAlignment(Pos.CENTER);
        labelRC.setTextAlignment(TextAlignment.CENTER);
        noteHeaderColumn.getChildren().addFirst(labelRC);
        AtomicInteger index = new AtomicInteger(1);
        Arrays.stream(DEFAULT_NOTES).forEach(note -> {
            NoteHeaderCell noteHeaderCell = new NoteHeaderCell(this, note, index.get(), 0);
            noteHeaderColumn.getChildren().add(index.get(), noteHeaderCell);
            index.getAndIncrement();
        });

        // Sequencer grid initialization with special focus on avoiding creating nodes inside updateItem unless required
        sequencerGrid.setSelectionModel(null);
        sequencerGrid.setItems(steps);
        sequencerGrid.setCellFactory(stepListView -> new ListCell<>() {
            private final VBox column = new VBox();
            private final Label stepLabel = new Label();
            {
                // Remove all default cell styling that causes separators/gaps and add support for translucency
                setStyle("-fx-padding: 0; -fx-background-color: transparent; -fx-background-insets: 0; -fx-border-width: 0;");

                VBox.setVgrow(column, Priority.ALWAYS);
                column.setMinWidth(STEP_WIDTH);
                column.setPrefWidth(STEP_WIDTH);
                column.setMaxWidth(STEP_WIDTH);
                column.setSpacing(0);
                column.setAlignment(Pos.TOP_CENTER);

                stepLabel.setStyle("-fx-text-fill: white; -fx-padding: 3px; -fx-background-color: \"#333333\"; -fx-border-color: gray; -fx-border-width: 1px;");
                stepLabel.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(stepLabel, Priority.ALWAYS);
                stepLabel.setAlignment(Pos.CENTER);
                stepLabel.setTextAlignment(TextAlignment.CENTER);
                column.getChildren().add(stepLabel);
            }
            @Override
            protected void updateItem(Step step, boolean empty) {
                super.updateItem(step, empty);
                if (empty || step == null) {
                    setGraphic(null);
                    return;
                }
                stepLabel.setText(Integer.toString(step.getIndex()));
                if (column.getChildren().size() - 1 > step.getCells().size()) {
                    while (column.getChildren().size() - 1 > step.getCells().size()) {
                        column.getChildren().remove(1);
                    }
                } else if (column.getChildren().size() - 1 < step.getCells().size()) {
                    while (column.getChildren().size() - 1 < step.getCells().size()) {
                        Label cellLabel = createCellLabel();
                        column.getChildren().add(cellLabel);
                    }
                }
                for (int i = 0; i < step.getCells().size(); i++) {
                    InstrumentCellData cellData = step.getCells().get(i);
                    Label cellLabel = (Label) column.getChildren().get(i + 1);
                    cellLabel.textProperty().unbind();
                    cellLabel.textProperty().bind(cellData.labelProperty());
                    cellLabel.setUserData(cellData);
                }
                setGraphic(column);
            }
            private Label createCellLabel() {
                Label cellLabel = new Label();
                cellLabel.setStyle("-fx-text-fill: white; -fx-padding: 3px; -fx-border-color: gray; -fx-border-width: 1px;");
                cellLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                cellLabel.setMinHeight(40);
                VBox.setVgrow(cellLabel, Priority.ALWAYS);
                HBox.setHgrow(cellLabel, Priority.ALWAYS);
                cellLabel.setFont(Font.font(cellLabel.getFont().getFamily(), FontWeight.BOLD, cellLabel.getFont().getSize() + 2));
                cellLabel.setAlignment(Pos.CENTER);
                cellLabel.setTextAlignment(TextAlignment.CENTER);
                cellLabel.setOnMouseClicked(HomeController.this);
                return cellLabel;
            }
        });

        // Initialize 16 steps with empty cells (which will be set as and when user wishes)
        for (int i = 1; i <= 16; i++) {
            Step step = new Step(i);
            for (int j = 1; j <= DEFAULT_NOTES.length; j++) {
                InstrumentCellData instrumentCellData = new InstrumentCellData(j, i);
                step.getCells().add(instrumentCellData);
            }
            steps.add(step);
        }

        // Initial timeline future initialization with default tempo
        createTimeline();

        // After UI is rendered, find the horizontal scrollbar and virtual flow for playhead scrolling and visibility logic, then attach to playhead overlay height
        Platform.runLater(() -> {
            for (Node node : sequencerGrid.lookupAll(".scroll-bar")) {
                if (node instanceof ScrollBar scrollBar && scrollBar.getOrientation() == Orientation.HORIZONTAL) {
                    hBar = scrollBar;
                    break;
                }
            }
            virtualFlow = (VirtualFlow<ListCell<Step>>) sequencerGrid.lookup(".virtual-flow");
            noteHeaderColumn.paddingProperty().bind(Bindings.createObjectBinding(() -> {
                double bottomPadding = hBar.isVisible() ? hBar.getHeight() : 0;
                return new Insets(0, 0, bottomPadding, 0);
            }, hBar.visibleProperty(), hBar.heightProperty()));

            playheadOverlay.maxHeightProperty().bind(Bindings.createDoubleBinding(() -> hBar.isVisible() ? virtualFlow.getHeight() - hBar.getHeight() : virtualFlow.getHeight(), virtualFlow.heightProperty(), hBar.visibleProperty(), hBar.heightProperty()));

            // Force VirtualFlow layout refresh on window maximize (VirtualFlow doesn't auto-refresh on instant size changes)
            Window window = sequencerGrid.getScene().getWindow();
            if (window instanceof javafx.stage.Stage stage) {
                stage.maximizedProperty().addListener((obs, wasMaximized, isMaximized) ->
                        Platform.runLater(() -> virtualFlow.requestLayout()));
            }
        });

        // Record mode setup (set key listeners to TabPane as it is an event accepting node, and we need to consume its own switching function)
        recordStepLabel.managedProperty().bind(recordStepLabel.visibleProperty());
        recordStepLabel.textProperty().bind(Bindings.createStringBinding(() -> "Step: " + ((recordColumn.get() % steps.size()) + 1), recordColumn));
        recordModeInstrumentChoiceBoxStackPane.managedProperty().bind(recordModeInstrumentChoiceBoxStackPane.visibleProperty());
        recordModeInstrumentChoiceBox.getItems().addFirst("Drum Kit");
        recordModeInstrumentChoiceBox.getItems().addAll(1, Arrays.stream(orchestra).map(Instrument::getName).map(String::trim).toList());
        recordModeInstrumentChoiceBox.getSelectionModel().selectFirst();
        parentTabPane.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (recordMode) {
                if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.KP_UP) {
                    recordModeInstrumentChoiceBox.getSelectionModel().selectPrevious();
                    event.consume();
                } else if (event.getCode() == KeyCode.DOWN || event.getCode() == KeyCode.KP_DOWN) {
                    recordModeInstrumentChoiceBox.getSelectionModel().selectNext();
                    event.consume();
                } else {
                    char c = event.getCode().getChar().charAt(0);
                    if (event.isShiftDown() && Character.isDigit(c) && event.getCode() != KeyCode.DIGIT0 && event.getCode() != KeyCode.NUMPAD0) {
                        int row = Integer.parseInt(String.valueOf(c)) - 1;
                        if (row >= 0 && row < noteHeaderColumn.getChildren().size() - 1) {
                            int col = recordColumn.get() % steps.size();
                            InstrumentCellData cellData = steps.get(col).getCells().get(row);
                            cellData.setInstrument(InstrumentCellData.INACTIVE, orchestra);
                        }
                        recordColumn.set(recordColumn.get() + 1);
                        event.consume();
                    }
                }
            }
        });
        parentTabPane.addEventHandler(KeyEvent.KEY_TYPED, event -> {
            if (recordMode && event.getCharacter() != null && !event.getCharacter().isEmpty()) {
                char c = event.getCharacter().charAt(0);
                if (Character.isDigit(c) && c != '0') {
                    int row = Integer.parseInt(String.valueOf(c)) - 1;
                    if (row >= 0 && row < noteHeaderColumn.getChildren().size() - 1) {
                        int col = recordColumn.get() % steps.size();
                        int instr = recordModeInstrumentChoiceBox.getSelectionModel().getSelectedIndex();
                        instr = instr == 0 ? InstrumentCellData.DRUM : instr - 1; // Map "Drum Kit" to DRUM constant, else actual instrument index
                        InstrumentCellData cellData = steps.get(col).getCells().get(row);
                        // If amongst the last few (upto 10) cells there be one whose duration would cover the step before current step (with same instrument),
                        // add duration to that one instead of adding a new note
                        int lastFewCellsToCheck = Math.min(10, steps.size() - 1);
                        InstrumentCellData addDurationCell = null;
                        for (int i = 0; i < lastFewCellsToCheck; i++) {
                            int currentCheckColumn = (col - i + steps.size() - 1) % steps.size();
                            InstrumentCellData currentCell = steps.get(currentCheckColumn).getCells().get(row);
                            if (currentCell.getInstrument() != instr) continue;
                            int currentCellDuration = currentCell.getDuration();
                            int currentCellEndColumn = (currentCheckColumn + currentCellDuration - 1) % steps.size();
                            if (currentCellEndColumn == (col - 1 + steps.size()) % steps.size()) {
                                addDurationCell = currentCell;
                                break;
                            }
                        }
                        if (addDurationCell != null) {
                            addDurationCell.setDuration(addDurationCell.getDuration() + 1, orchestra);
                        } else {
                            cellData.setInstrument(instr, orchestra);
                        }
                        recordColumn.set(recordColumn.get() + 1);
                    }
                } else if (Character.isSpaceChar(c)) {
                    recordColumn.set(recordColumn.get() + 1);
                }
            }
        });
    }

    @FXML
    private void playClicked() {
        try {
            Thread.startVirtualThread(new Player(Integer.parseInt(instrument.getText()), Integer.parseInt(note.getText()), Double.parseDouble(duration.getText())));
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR, "Invalid input").showAndWait();
        }
    }

    @FXML
    private void loopInstrumentsClicked() {
        new Thread(() -> {
            try {
                Platform.runLater(() -> lockTesterInstrumentAndControls(true));
                for (int i = 0; i < MAX_INSTRUMENTS; i++) {
                    final int x = i;
                    if (Integer.parseInt(note.getText()) >= 0 && Integer.parseInt(note.getText()) <= 127) {
                        Platform.runLater(() -> instrument.setText(String.valueOf(x)));
                        Thread thread = Thread.startVirtualThread(new Player(x, Integer.parseInt(note.getText()), Double.parseDouble(duration.getText())));
                        if (wait.isSelected()) thread.join();
                    } else {
                        Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Invalid input").showAndWait());
                        break;
                    }
                }
            } catch (NumberFormatException | InterruptedException e) {
                Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Invalid input").showAndWait());
            } finally {
                Platform.runLater(() -> lockTesterInstrumentAndControls(false));
            }
        }).start();
    }

    @FXML
    private void loopNotesClicked() {
        new Thread(() -> {
            try {
                Platform.runLater(() -> lockTesterNoteAndControls(true));
                for (int i = 0; i <= 127; i++) {
                    final int x = i;
                    if (Integer.parseInt(instrument.getText()) >= 0 && Integer.parseInt(instrument.getText()) <= 127) {
                        Platform.runLater(() -> note.setText(String.valueOf(x)));
                        Thread thread = Thread.startVirtualThread(new Player(Integer.parseInt(instrument.getText()), x, Double.parseDouble(duration.getText())));
                        if (wait.isSelected()) thread.join();
                    } else {
                        Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Invalid input").showAndWait());
                        break;
                    }
                }
            } catch (NumberFormatException | InterruptedException e) {
                Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Invalid input").showAndWait());
            } finally {
                Platform.runLater(() -> lockTesterNoteAndControls(false));
            }
        }).start();
    }

    @FXML
    private void loopAllClicked() {
        new Thread(() -> {
            try {
                Platform.runLater(() -> lockTesterInstrumentNoteAndControls(true));
                for (int i = 0; i <= 127; i++) {
                    for (int j = 0; j < MAX_INSTRUMENTS; j++) {
                        final int x = i;
                        final int y = j;
                        Platform.runLater(() -> note.setText(String.valueOf(x)));
                        Platform.runLater(() -> instrument.setText(String.valueOf(y)));
                        Thread thread = Thread.startVirtualThread(new Player(y, x, Double.parseDouble(duration.getText())));
                        if (wait.isSelected()) thread.join();
                    }
                }
            } catch (NumberFormatException | InterruptedException e) {
                Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Invalid input").showAndWait());
            } finally {
                Platform.runLater(() -> lockTesterInstrumentNoteAndControls(false));
            }
        }).start();
    }

    @FXML
    private void loopRandomClicked() {
        new Thread(() -> {
            try {
                Platform.runLater(() -> lockTesterInstrumentNoteAndControls(true));
                for (int i = 1; i <= 5; i++) {
                    final int x = (int) (Math.random() * 128);
                    final int y = (int) (Math.random() * MAX_INSTRUMENTS);
                    Platform.runLater(() -> note.setText(String.valueOf(x)));
                    Platform.runLater(() -> instrument.setText(String.valueOf(y)));
                    Thread thread = Thread.startVirtualThread(new Player(y, x, Double.parseDouble(duration.getText())));
                    if (wait.isSelected()) thread.join();
                }
            } catch (NumberFormatException | InterruptedException e) {
                Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Invalid input").showAndWait());
            } finally {
                Platform.runLater(() -> lockTesterInstrumentNoteAndControls(false));
            }
        }).start();
    }

    @FXML
    private void playCompositionClicked() {
        if (playheadAnimator == null) {
            startPlayhead();
        } else {
            resumePlayhead();
        }
    }

    @FXML
    private void pauseCompositionClicked() {
        pausePlayhead();
    }

    @FXML
    private void resetTimelineClicked() {
        // Instead of checking grid and turning notes off (also potentially missing past removed notes), we turn off all channels through Player
        Player.playAllOff();
        // We set it to 0, when timelineFuture starts it will set to 1 for the first step
        playheadStep.set(0);
        if (isPaused.get()) {
            stopPlayhead();
        }
    }

    @FXML
    private void scrollToPlayheadClicked() {
        scrollToPlayhead = !scrollToPlayhead;
        scrollToPlayheadButton.getGraphic().getStyleClass().set(1, scrollToPlayhead ? "negative-toolbar-button" : "general-toolbar-button");
    }

    @FXML
    private void changeTempoClicked() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Change Tempo");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField tempoField = new TextField(String.valueOf(tempo));
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.add(new Label("Tempo (BPM):"), 0, 0);
        grid.add(tempoField, 1, 0);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    int newTempo = Integer.parseInt(tempoField.getText());
                    if (newTempo > 0) {
                        tempo = newTempo;
                        return true;
                    } else {
                        new Alert(Alert.AlertType.ERROR, "Tempo must be positive").showAndWait();
                    }
                } catch (NumberFormatException e) {
                    new Alert(Alert.AlertType.ERROR, "Invalid input").showAndWait();
                }
            }
            return false;
        });
        dialog.showAndWait().ifPresent(changed -> {
            if (changed) {
                createTimeline();
            }
        });
    }

    @FXML
    public void addStepClicked() {
        int newStep = steps.size() + 1;
        Step step = new Step(newStep);
        for (int i = 1; i < noteHeaderColumn.getChildren().size(); i++) {
            InstrumentCellData instrumentCellData = new InstrumentCellData(i, newStep);
            step.getCells().add(instrumentCellData);
        }
        steps.add(step);
    }

    @FXML
    public void removeStepClicked() {
        if (steps.size() > 1) {
            steps.removeIf(step -> step.getIndex() == steps.size());
        }
    }

    @FXML
    public void addRowClicked() {
        int newRow = noteHeaderColumn.getChildren().size();
        NoteHeaderCell noteHeaderCell = new NoteHeaderCell(this, DEFAULT_NOTES[(newRow - 1) % DEFAULT_NOTES.length], newRow, 0);
        noteHeaderColumn.getChildren().add(newRow, noteHeaderCell);
        for (int i = 0; i < steps.size(); i++) {
            InstrumentCellData instrumentCellData = new InstrumentCellData(newRow, i + 1);
            steps.get(i).getCells().add(instrumentCellData);
        }
    }

    @FXML
    public void removeRowClicked() {
        if (noteHeaderColumn.getChildren().size() > 2) {
            noteHeaderColumn.getChildren().removeIf(node -> {
                if (!(node instanceof NoteHeaderCell noteHeaderCell)) return false;
                return noteHeaderCell.getRow() == noteHeaderColumn.getChildren().size() - 1;
            });
            for (int i = 1; i <= steps.size(); i++) {
                steps.get(i - 1).getCells().removeLast();
            }
        }
    }

    @FXML
    public void recordModeClicked() {
        recordMode = !recordMode;
        recordColumn.set(0);
        recordModeButton.getGraphic().getStyleClass().set(1, recordMode ? "negative-toolbar-button" : "general-toolbar-button");
        recordStepLabel.setVisible(recordMode);
        recordModeInstrumentChoiceBoxStackPane.setVisible(recordMode);
        lockComposerControlsExceptRecord(recordMode);
        pauseCompositionClicked();
        resetTimelineClicked();
    }

    private void lockTesterControls(boolean lock) {
        play.setDisable(lock);
        loopInstruments.setDisable(lock);
        loopNotes.setDisable(lock);
        loopAll.setDisable(lock);
        loopRandom.setDisable(lock);
    }

    private void lockTesterInstrumentAndControls(boolean lock) {
        instrument.setDisable(lock);
        lockTesterControls(lock);
    }

    private void lockTesterNoteAndControls(boolean lock) {
        note.setDisable(lock);
        lockTesterControls(lock);
    }

    private void lockTesterInstrumentNoteAndControls(boolean lock) {
        instrument.setDisable(lock);
        note.setDisable(lock);
        lockTesterControls(lock);
    }

    private void lockComposerControlsExceptRecord(boolean lock) {
        playCompositionButton.setDisable(lock);
        pauseCompositionButton.setDisable(lock);
        resetTimelineButton.setDisable(lock);
        scrollToPlayheadButton.setDisable(lock);
        changeTempoButton.setDisable(lock);
        addStepButton.setDisable(lock);
        removeStepButton.setDisable(lock);
        addRowButton.setDisable(lock);
        removeRowButton.setDisable(lock);
        exportMidiButton.setDisable(lock);
        importMidiButton.setDisable(lock);
    }

    private double getPlayheadX(double currentStepDouble, int stepIndex) {
        if (virtualFlow == null) return -1;
        Step firstVisibleStep = virtualFlow.getFirstVisibleCell().getItem();
        Step lastVisibleStep = virtualFlow.getLastVisibleCell().getItem();
        // stepIndex is now 1-based (1 to steps.size())
        if (stepIndex < firstVisibleStep.getIndex() || stepIndex > lastVisibleStep.getIndex()) {
            return -1;
        }
        if (scrollToPlayhead && hBar.getValue() != 1.0 && hBar.isVisible()) {
            return 0;
        }
        // currentStepDouble is 1-based, subtract 1 to get 0-based position for pixel calculation
        return ((currentStepDouble - 1) - (firstVisibleStep.getIndex() - 1)) * STEP_WIDTH;
    }

    private void startPlayhead() {
        playheadOverlay.setVisible(true);
        stepDurationNanos = (60.0 / tempo / 4) * 1_000_000_000;
        long playheadStartTime = System.nanoTime();
        if (playheadAnimator != null) {
            playheadAnimator.stop();
            isPaused.set(true);
        }
        playheadAnimator = new javafx.animation.AnimationTimer() {
            private int lastCol = -1;
            private long stepStartTime = playheadStartTime;

            @Override
            public void handle(long now) {
                double currentStepDouble;
                int col = playheadStep.get();
                // Wait (also hide playhead and reset scrolling) till timelineFuture starts and sets 1 for the first time
                if (col == 0) {
                    playheadOverlay.setVisible(false);
                    playheadOverlay.setTranslateX(0);
                    if (scrollToPlayhead) {
                        hBar.setValue(0);
                    }
                    return;
                }

                // Normal run logic
                if (col != lastCol) {
                    stepStartTime = now;
                    lastCol = col;
                }
                long elapsed = now - stepStartTime;
                double fraction = Math.min(elapsed / stepDurationNanos, 1.0);
                currentStepDouble = col + fraction;
                // Even though playback goes till steps size, playhead animator has to go fractionally above it till last step completion
                // And rather than resetting local currentStepDouble, we just wait till main playheadStep gets updated by timelineFuture
                if (currentStepDouble > steps.size() + 1) return;
                double toSetX = getPlayheadX(currentStepDouble, col);
                playheadOverlay.setVisible(toSetX != -1);
                playheadOverlay.setTranslateX(toSetX);
                if (scrollToPlayhead) {
                    if (hBar == null) return;
                    // currentStepDouble is now 1-based, so subtract 1 to get 0-based position for scroll calculation
                    hBar.setValue(Math.clamp(((currentStepDouble - 1) * STEP_WIDTH) / (steps.size() * STEP_WIDTH - sequencerGrid.getWidth()), 0.0, 1.0));
                }
            }
        };
        playheadAnimator.start();
        isPaused.set(false);
    }

    private void pausePlayhead() {
        if (playheadAnimator != null) {
            playheadAnimator.stop();
            isPaused.set(true);
        }
    }

    private void resumePlayhead() {
        if (playheadAnimator != null) {
            playheadAnimator.start();
            isPaused.set(false);
        }
    }

    private void stopPlayhead() {
        if (playheadAnimator != null) {
            playheadAnimator.stop();
            playheadAnimator = null;
            isPaused.set(true);
        }
        playheadOverlay.setVisible(false);
    }

    private void createTimeline() {
        pauseCompositionClicked();
        long periodNanos = TimeUnit.SECONDS.toNanos(60) / tempo / 4;
        if (timelineFuture != null && !timelineFuture.isDone()) {
            timelineFuture.cancel(false);
        }
        timelineFuture = scheduler.scheduleAtFixedRate(() -> {
            if (isPaused.get()) return;

            // P.S. - This approach (and waiting of playheadAnimator) was needed because it is found that timelineFuture starts with a significant delay
            // P.S. - The 1-based playheadStep also helps with proper pixel calculation for scrolling and playhead, while keeping 0 free for waiting stage
            // When timelineFuture starts, it sets playheadStep to 1 for the first step, and then playheadAnimator starts
            // For subsequent steps, playheadStep remains as it is for the entire step duration due to set-and-use logic
            playheadStep.set(playheadStep.get() + 1);
            if (playheadStep.get() > steps.size()) {
                playheadStep.set(1);
            }

            int currentStep = playheadStep.get();

            for (int i = 1; i < noteHeaderColumn.getChildren().size(); i++) {
                NoteHeaderCell noteHeaderCell = (NoteHeaderCell) noteHeaderColumn.getChildren().get(i);
                if (noteHeaderCell == null) continue;
                for (int s = 1; s <= steps.size(); s++) {
                    InstrumentCellData cell = steps.get(s - 1).getCells().get(i - 1);
                    if (cell != null) {
                        int instr = cell.getInstrument();
                        if (instr != InstrumentCellData.INACTIVE) {
                            int dur = cell.getDuration();
                            int endStep = ((s - 1 + dur) % steps.size()) + 1;
                            if (endStep == currentStep) {
                                Player.playNoteOff(instr, noteHeaderCell.getNote());
                                Platform.runLater(noteHeaderCell::highlightOff);
                            }
                        }
                    }
                }
            }

            for (int i = 1; i < noteHeaderColumn.getChildren().size(); i++) {
                NoteHeaderCell noteHeaderCell = (NoteHeaderCell) noteHeaderColumn.getChildren().get(i);
                if (noteHeaderCell == null) continue;
                InstrumentCellData currentCell = steps.get(currentStep - 1).getCells().get(i - 1);
                if (currentCell != null) {
                    int instr = currentCell.getInstrument();
                    if (instr != InstrumentCellData.INACTIVE) {
                        Player.playNoteOn(instr, noteHeaderCell.getNote());
                        Platform.runLater(noteHeaderCell::highlightOn);
                    }
                }
            }
        }, 0, periodNanos, TimeUnit.NANOSECONDS);
        resetTimelineClicked();
    }

    @FXML
    private void exportMidiClicked() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export MIDI File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("MIDI Files (*.mid)", "*.mid"));
        fileChooser.setInitialFileName("composition.mid");
        File file = fileChooser.showSaveDialog(sequencerGrid.getScene().getWindow());
        if (file == null) return;

        Dialog<Void> waitDialog = new Dialog<>();
        waitDialog.setTitle("Exporting Composition");
        waitDialog.setHeaderText("Processing MIDI data...");
        ProgressIndicator progress = new ProgressIndicator();
        progress.setProgress(-1);
        StackPane progressPane = new StackPane();
        progressPane.getChildren().add(progress);
        StackPane.setAlignment(progress, Pos.CENTER);
        waitDialog.getDialogPane().setContent(progressPane);
        Window waitDialogWindow = waitDialog.getDialogPane().getScene().getWindow();
        waitDialog.show();

        Thread.startVirtualThread(() -> {
            try {
                int resolution = 480;
                int ticksPerStep = resolution / 4;
                Sequence sequence = new Sequence(Sequence.PPQ, resolution);
                int microsecondsPerBeat = 60000000 / tempo;
                byte[] tempoData = new byte[]{
                        (byte) ((microsecondsPerBeat >> 16) & 0xFF),
                        (byte) ((microsecondsPerBeat >> 8) & 0xFF),
                        (byte) (microsecondsPerBeat & 0xFF)
                };
                Map<Integer, Track> instrumentTracks = new HashMap<>();
                for (int r = 1; r < noteHeaderColumn.getChildren().size(); r++) {
                    NoteHeaderCell noteHeaderCell = (NoteHeaderCell) noteHeaderColumn.getChildren().get(r);
                    if (noteHeaderCell == null) continue;
                    int noteValue = noteHeaderCell.getNote();
                    for (int c = 0; c < steps.size(); c++) {
                        InstrumentCellData cell = steps.get(c).getCells().get(r - 1);
                        if (cell == null || cell.getInstrument() == InstrumentCellData.INACTIVE) continue;
                        int instr = cell.getInstrument();
                        int dur = cell.getDuration();
                        boolean isDrum = (instr == InstrumentCellData.DRUM);
                        int channel = isDrum ? 9 : (instr % 16 == 9 ? 10 : instr % 16);
                        int program = isDrum ? 0 : instr;
                        Track track = instrumentTracks.computeIfAbsent(instr, k -> {
                            Track t = sequence.createTrack();
                            try {
                                if (instrumentTracks.isEmpty()) {
                                    MetaMessage tempoMessage = new MetaMessage();
                                    tempoMessage.setMessage(0x51, tempoData, 3);
                                    t.add(new MidiEvent(tempoMessage, 0));
                                }
                                if (!isDrum) {
                                    ShortMessage programChange = new ShortMessage();
                                    programChange.setMessage(ShortMessage.PROGRAM_CHANGE, channel, program, 0);
                                    t.add(new MidiEvent(programChange, 0));
                                }
                            } catch (Exception ignored) {
                            }
                            return t;
                        });
                        long startTick = (long) c * ticksPerStep;
                        long endTick = startTick + (long) dur * ticksPerStep;
                        ShortMessage noteOn = new ShortMessage();
                        noteOn.setMessage(ShortMessage.NOTE_ON, channel, noteValue, 100);
                        track.add(new MidiEvent(noteOn, startTick));
                        ShortMessage noteOff = new ShortMessage();
                        noteOff.setMessage(ShortMessage.NOTE_OFF, channel, noteValue, 0);
                        track.add(new MidiEvent(noteOff, endTick));
                    }
                }
                if (instrumentTracks.isEmpty()) {
                    Platform.runLater(() -> {
                        waitDialogWindow.hide();
                        new Alert(Alert.AlertType.WARNING, "No notes to export").showAndWait();
                    });
                    return;
                }
                MidiSystem.write(sequence, 1, file);
                Platform.runLater(() -> Platform.runLater(() -> {
                    waitDialogWindow.hide();
                    new Alert(Alert.AlertType.INFORMATION, "MIDI exported successfully!").showAndWait();
                }));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    waitDialogWindow.hide();
                    new Alert(Alert.AlertType.ERROR, "Failed to export MIDI: " + e.getMessage()).showAndWait();
                });
            }
        });
    }

    @FXML
    private void importMidiClicked() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import MIDI File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("MIDI Files (*.mid, *.midi)", "*.mid", "*.midi"));
        File file = fileChooser.showOpenDialog(sequencerGrid.getScene().getWindow());
        if (file == null) return;

        Dialog<Void> waitDialog = new Dialog<>();
        waitDialog.setTitle("Preparing Composition");
        waitDialog.setHeaderText("Processing MIDI data...");
        ProgressIndicator progress = new ProgressIndicator();
        progress.setProgress(-1);
        StackPane progressPane = new StackPane();
        progressPane.getChildren().add(progress);
        StackPane.setAlignment(progress, Pos.CENTER);
        waitDialog.getDialogPane().setContent(progressPane);
        Window waitDialogWindow = waitDialog.getDialogPane().getScene().getWindow();
        waitDialog.show();

        Thread.startVirtualThread(() -> {
            try {
                Sequence sequence = MidiSystem.getSequence(file);
                int resolution = sequence.getResolution();
                int midiTempo = 500000;
                long ticksPerStep = resolution / 4;
                TreeSet<Integer> allNotes = new TreeSet<>();
                int[] channelInstruments = new int[16];
                record NoteEvent(long startTick, int instrument, int note, int durationSteps) {
                }
                List<NoteEvent> noteEvents = new ArrayList<>();
                Map<Integer, Map<Integer, Long>> activeNotes = new HashMap<>();
                for (Track track : sequence.getTracks()) {
                    for (int i = 0; i < track.size(); i++) {
                        MidiEvent event = track.get(i);
                        MidiMessage message = event.getMessage();
                        if (message instanceof MetaMessage meta) {
                            if (meta.getType() == 0x51 && meta.getData().length == 3) {
                                byte[] data = meta.getData();
                                midiTempo = ((data[0] & 0xFF) << 16) | ((data[1] & 0xFF) << 8) | (data[2] & 0xFF);
                            }
                        } else if (message instanceof ShortMessage sm) {
                            int channel = sm.getChannel();
                            int command = sm.getCommand();
                            int noteValue = sm.getData1();
                            int velocity = sm.getData2();
                            if (command == ShortMessage.PROGRAM_CHANGE) {
                                channelInstruments[channel] = sm.getData1();
                            } else if (command == ShortMessage.NOTE_ON && velocity > 0) {
                                long tick = event.getTick();
                                int instrument = (channel == 9) ? InstrumentCellData.DRUM : channelInstruments[channel];
                                allNotes.add(noteValue);
                                activeNotes.computeIfAbsent(channel, k -> new HashMap<>()).put(noteValue, tick);
                                activeNotes.get(channel).put(noteValue | (instrument << 16), tick);
                            } else if (command == ShortMessage.NOTE_OFF || (command == ShortMessage.NOTE_ON && velocity == 0)) {
                                long endTick = event.getTick();
                                Map<Integer, Long> channelActive = activeNotes.get(channel);
                                if (channelActive != null) {
                                    int instrument = (channel == 9) ? InstrumentCellData.DRUM : channelInstruments[channel];
                                    int key = noteValue | (instrument << 16);
                                    Long startTick = channelActive.remove(key);
                                    if (startTick != null) {
                                        long durationTicks = endTick - startTick;
                                        int durationSteps = (int) Math.max(1, durationTicks / ticksPerStep);
                                        noteEvents.add(new NoteEvent(startTick, instrument, noteValue, durationSteps));
                                    }
                                }
                            }
                        }
                    }
                }
                if (noteEvents.isEmpty()) {
                    Platform.runLater(() -> {
                        waitDialogWindow.hide();
                        new Alert(Alert.AlertType.WARNING, "No notes found in MIDI file").showAndWait();
                    });
                    return;
                }
                int bpm = (int) Math.round(60000000.0 / midiTempo);
                long maxTick = noteEvents.stream().mapToLong(e -> e.startTick + (long) e.durationSteps * ticksPerStep).max().orElse(0);
                int totalSteps = (int) ((maxTick / ticksPerStep) + 1);
                List<Integer> notes = new ArrayList<>(allNotes);
                List<List<Integer>> grid = new ArrayList<>();
                List<List<Integer>> durations = new ArrayList<>();
                for (int n = 0; n < notes.size(); n++) {
                    List<Integer> row = new ArrayList<>();
                    List<Integer> durRow = new ArrayList<>();
                    for (int s = 0; s < totalSteps; s++) {
                        row.add(InstrumentCellData.INACTIVE);
                        durRow.add(1);
                    }
                    grid.add(row);
                    durations.add(durRow);
                }
                for (NoteEvent ne : noteEvents) {
                    int step = (int) (ne.startTick / ticksPerStep);
                    if (step >= totalSteps) step = totalSteps - 1;
                    int rowIndex = notes.indexOf(ne.note);
                    if (rowIndex >= 0 && step < totalSteps) {
                        grid.get(rowIndex).set(step, ne.instrument);
                        durations.get(rowIndex).set(step, ne.durationSteps);
                    }
                }
                CountDownLatch clearLatch = new CountDownLatch(1);
                Platform.runLater(() -> {
                    clearGrid();
                    clearLatch.countDown();
                });
                clearLatch.await();
                tempo = bpm;
                for (int i = 0; i < notes.size(); i++) {
                    int noteValue = notes.get(i);
                    NoteHeaderCell noteHeaderCell = new NoteHeaderCell(this, noteValue, i + 1, 0);
                    int finalI = i;
                    Platform.runLater(() -> noteHeaderColumn.getChildren().add(finalI + 1, noteHeaderCell));
                }
                for (int c = 1; c <= totalSteps; c++) {
                    Step step = new Step(c);
                    CountDownLatch stepLatch = new CountDownLatch(1);
                    Platform.runLater(() -> {
                        steps.add(step);
                        stepLatch.countDown();
                    });
                    stepLatch.await();
                }
                for (int r = 0; r < grid.size(); r++) {
                    List<Integer> row = grid.get(r);
                    List<Integer> durRow = durations.get(r);
                    for (int c = 0; c < row.size(); c++) {
                        int instr = row.get(c);
                        int dur = durRow.get(c);
                        InstrumentCellData instrumentCellData = new InstrumentCellData(r + 1, c + 1);
                        if (instr != InstrumentCellData.INACTIVE) {
                            instrumentCellData.setInstrumentAndDuration(instr, dur, orchestra);
                        }
                        int finalC = c;
                        int finalR = r;
                        Platform.runLater(() -> steps.get(finalC).getCells().add(finalR, instrumentCellData));
                    }
                }
                Platform.runLater(() -> Platform.runLater(() -> {
                    createTimeline();
                    waitDialogWindow.hide();
                    new Alert(Alert.AlertType.INFORMATION, "MIDI imported: " + notes.size() + " notes, " + totalSteps + " steps, " + bpm + " BPM").showAndWait();
                }));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    waitDialogWindow.hide();
                    new Alert(Alert.AlertType.ERROR, "Failed to import MIDI: " + e.getMessage()).showAndWait();
                });
            }
        });
    }

    private void clearGrid() {
        steps.clear();
        noteHeaderColumn.getChildren().removeIf(node -> node instanceof NoteHeaderCell);
    }

    public void shutdown() {
        if (timelineFuture != null && !timelineFuture.isDone()) {
            timelineFuture.cancel(false);
        }
        scheduler.shutdown();
        Player.shutdownSynthesizer();
    }

    @Override
    public void handle(MouseEvent mouseEvent) {
        if (mouseEvent.getButton() == MouseButton.PRIMARY) {
            Node node = mouseEvent.getSource() instanceof Node ? (Node) mouseEvent.getSource() : null;
            switch (node) {
                case NoteHeaderCell noteHeaderCell -> {
                    ContextMenu noteMenu = new ContextMenu();
                    NoteHeaderCell.NOTE_MAP.forEach((noteValue, noteName) -> {
                        MenuItem item = new MenuItem(noteName);
                        item.setOnAction(event -> noteHeaderCell.setNote(noteValue));
                        noteMenu.getItems().add(item);
                    });
                    noteMenu.show(noteHeaderCell, mouseEvent.getScreenX(), mouseEvent.getScreenY());
                }
                case Label instrumentCell -> {
                    InstrumentCellData instrumentCellData = (InstrumentCellData) instrumentCell.getUserData();
                    int currentInstrument = instrumentCellData.getInstrument();
                    if (currentInstrument == InstrumentCellData.INACTIVE) {
                        ContextMenu instrumentMenu = new ContextMenu();
                        MenuItem drumItem = new MenuItem("Drum Kit");
                        drumItem.setOnAction(event -> instrumentCellData.setInstrument(InstrumentCellData.DRUM, orchestra));
                        instrumentMenu.getItems().add(drumItem);
                        for (int i = 0; i < MAX_INSTRUMENTS; i++) {
                            final int instr = i;
                            MenuItem item = new MenuItem(orchestra[instr].getName().trim());
                            item.setOnAction(event -> instrumentCellData.setInstrument(instr, orchestra));
                            instrumentMenu.getItems().add(item);
                        }
                        instrumentMenu.show(instrumentCell, mouseEvent.getScreenX(), mouseEvent.getScreenY());
                    } else {
                        instrumentCellData.setInstrument(InstrumentCellData.INACTIVE, orchestra);
                    }
                }
                case null, default -> {
                }
            }
        } else if (mouseEvent.getButton() == MouseButton.SECONDARY) {
            Node node = mouseEvent.getSource() instanceof Node ? (Node) mouseEvent.getSource() : null;
            if (node instanceof NoteHeaderCell) return;
            if (!(node instanceof Label instrumentCell)) return;
            InstrumentCellData instrumentCellData = (InstrumentCellData) instrumentCell.getUserData();
            if (instrumentCellData.getInstrument() != InstrumentCellData.INACTIVE) {
                ContextMenu durationMenu = new ContextMenu();
                for (int d = 1; d <= 16; d++) {
                    final int dur = d;
                    MenuItem item = new MenuItem("Duration: " + d + " step" + (d > 1 ? "s" : ""));
                    item.setOnAction(event -> instrumentCellData.setDuration(dur, orchestra));
                    durationMenu.getItems().add(item);
                }
                durationMenu.getItems().add(new SeparatorMenuItem());
                MenuItem customItem = new MenuItem("Custom duration...");
                customItem.setOnAction(event -> {
                    TextInputDialog dialog = new TextInputDialog(String.valueOf(instrumentCellData.getDuration()));
                    dialog.setTitle("Set Duration");
                    dialog.setHeaderText("Enter duration in steps:");
                    dialog.showAndWait().ifPresent(result -> {
                        try {
                            int dur = Integer.parseInt(result);
                            if (dur >= 1) {
                                instrumentCellData.setDuration(dur, orchestra);
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    });
                });
                durationMenu.getItems().add(customItem);
                durationMenu.show(instrumentCell, mouseEvent.getScreenX(), mouseEvent.getScreenY());
            }
        }
    }
}