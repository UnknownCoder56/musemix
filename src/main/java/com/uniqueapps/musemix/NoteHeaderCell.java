package com.uniqueapps.musemix;

import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.effect.Bloom;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.LinkedHashMap;

public class NoteHeaderCell extends Label {

    public static final LinkedHashMap<Integer, String> NOTE_MAP = new LinkedHashMap<>();
    private static final LinkedHashMap<Integer, Background> NOTE_BACKGROUND_MAP = new LinkedHashMap<>();
    private static final Background DEFAULT_BACKGROUND = new Background(new BackgroundFill(Color.rgb(255, 255, 255, 0.2), null, null));
    private static final Bloom BLOOM = new Bloom(0.6);

    private int note;
    private final int row;
    private final int column;

    static {
        // MIDI note (alongside Drum channel note) numbers and their corresponding note names
        NOTE_MAP.put(21, "A0");
        NOTE_MAP.put(22, "A#0");
        NOTE_MAP.put(23, "B0");
        NOTE_MAP.put(24, "C1");
        NOTE_MAP.put(25, "C#1");
        NOTE_MAP.put(26, "D1");
        NOTE_MAP.put(27, "D#1");
        NOTE_MAP.put(28, "E1");
        NOTE_MAP.put(29, "F1");
        NOTE_MAP.put(30, "F#1");
        NOTE_MAP.put(31, "G1");
        NOTE_MAP.put(32, "G#1");
        NOTE_MAP.put(33, "A1");
        NOTE_MAP.put(34, "A#1");
        NOTE_MAP.put(35, "B1 / Acoustic Bass Drum");
        NOTE_MAP.put(36, "C2 / Bass Drum 1");
        NOTE_MAP.put(37, "C#2 / Side Stick");
        NOTE_MAP.put(38, "D2 / Acoustic Snare");
        NOTE_MAP.put(39, "D#2 / Hand Clap");
        NOTE_MAP.put(40, "E2 / Electric Snare");
        NOTE_MAP.put(41, "F2 / Low Floor Tom");
        NOTE_MAP.put(42, "F#2 / Closed Hi-Hat");
        NOTE_MAP.put(43, "G2 / High Floor Tom");
        NOTE_MAP.put(44, "G#2 / Pedal Hi-Hat");
        NOTE_MAP.put(45, "A2 / Low Tom");
        NOTE_MAP.put(46, "A#2 / Open Hi-Hat");
        NOTE_MAP.put(47, "B2 / Low-Mid Tom");
        NOTE_MAP.put(48, "C3 / Hi-Mid Tom");
        NOTE_MAP.put(49, "C#3 / Crash Cymbal 1");
        NOTE_MAP.put(50, "D3 / High Tom");
        NOTE_MAP.put(51, "D#3 / Ride Cymbal 1");
        NOTE_MAP.put(52, "E3 / Chinese Cymbal");
        NOTE_MAP.put(53, "F3 / Ride Bell");
        NOTE_MAP.put(54, "F#3 / Tambourine");
        NOTE_MAP.put(55, "G3 / Splash Cymbal");
        NOTE_MAP.put(56, "G#3 / Cowbell");
        NOTE_MAP.put(57, "A3 / Crash Cymbal 2");
        NOTE_MAP.put(58, "A#3 / Vibraslap");
        NOTE_MAP.put(59, "B3 / Ride Cymbal 2");
        NOTE_MAP.put(60, "C4 / Hi Bongo");
        NOTE_MAP.put(61, "C#4 / Low Bongo");
        NOTE_MAP.put(62, "D4 / Mute Hi Conga");
        NOTE_MAP.put(63, "D#4 / Open Hi Conga");
        NOTE_MAP.put(64, "E4 / Low Conga");
        NOTE_MAP.put(65, "F4 / High Timbale");
        NOTE_MAP.put(66, "F#4 / Low Timbale");
        NOTE_MAP.put(67, "G4 / High Agogo");
        NOTE_MAP.put(68, "G#4 / Low Agogo");
        NOTE_MAP.put(69, "A4 / Cabasa");
        NOTE_MAP.put(70, "A#4 / Maracas");
        NOTE_MAP.put(71, "B4 / Short Whistle");
        NOTE_MAP.put(72, "C5 / Long Whistle");
        NOTE_MAP.put(73, "C#5 / Short Guiro");
        NOTE_MAP.put(74, "D5 / Long Guiro");
        NOTE_MAP.put(75, "D#5 / Claves");
        NOTE_MAP.put(76, "E5 / Hi Wood Block");
        NOTE_MAP.put(77, "F5 / Low Wood Block");
        NOTE_MAP.put(78, "F#5 / Mute Cuica");
        NOTE_MAP.put(79, "G5 / Open Cuica");
        NOTE_MAP.put(80, "G#5 / Mute Triangle");
        NOTE_MAP.put(81, "A5 / Open Triangle");
        NOTE_MAP.put(82, "A#5");
        NOTE_MAP.put(83, "B5");
        NOTE_MAP.put(84, "C6");
        NOTE_MAP.put(85, "C#6");
        NOTE_MAP.put(86, "D6");
        NOTE_MAP.put(87, "D#6");
        NOTE_MAP.put(88, "E6");
        NOTE_MAP.put(89, "F6");
        NOTE_MAP.put(90, "F#6");
        NOTE_MAP.put(91, "G6");
        NOTE_MAP.put(92, "G#6");
        NOTE_MAP.put(93, "A6");
        NOTE_MAP.put(94, "A#6");
        NOTE_MAP.put(95, "B6");
        NOTE_MAP.put(96, "C7");
        NOTE_MAP.put(97, "C#7");
        NOTE_MAP.put(98, "D7");
        NOTE_MAP.put(99, "D#7");
        NOTE_MAP.put(100, "E7");
        NOTE_MAP.put(101, "F7");
        NOTE_MAP.put(102, "F#7");
        NOTE_MAP.put(103, "G7");
        NOTE_MAP.put(104, "G#7");
        NOTE_MAP.put(105, "A7");
        NOTE_MAP.put(106, "A#7");
        NOTE_MAP.put(107, "B7");
        NOTE_MAP.put(108, "C8");

        // VIBGYOR background fills
        NOTE_BACKGROUND_MAP.put(0, new Background(new BackgroundFill(Color.web("#F44336"), null, null))); // RED
        NOTE_BACKGROUND_MAP.put(1, new Background(new BackgroundFill(Color.web("#FF5722"), null, null))); // ORANGE
        NOTE_BACKGROUND_MAP.put(2, new Background(new BackgroundFill(Color.web("#FF9800"), null, null))); // YELLOW
        NOTE_BACKGROUND_MAP.put(3, new Background(new BackgroundFill(Color.web("#4CAF50"), null, null))); // GREEN
        NOTE_BACKGROUND_MAP.put(4, new Background(new BackgroundFill(Color.web("#2196F3"), null, null))); // BLUE
        NOTE_BACKGROUND_MAP.put(5, new Background(new BackgroundFill(Color.web("#3F51B5"), null, null))); // INDIGO
        NOTE_BACKGROUND_MAP.put(6, new Background(new BackgroundFill(Color.web("#673AB7"), null, null))); // VIOLET
    }

    public NoteHeaderCell(EventHandler<MouseEvent> mouseHandler, int note, int row, int column) {
        super();
        setTextFill(Color.WHITE);
        setPadding(new Insets(3));
        setText(NOTE_MAP.get(note));
        setBorder(new Border(new BorderStroke(Color.GRAY, BorderStrokeStyle.SOLID, null, new BorderWidths(1))));
        setBackground(DEFAULT_BACKGROUND);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        setMinHeight(40);
        VBox.setVgrow(this, Priority.ALWAYS);
        HBox.setHgrow(this, Priority.ALWAYS);
        setOnMouseClicked(mouseHandler);

        this.note = note;
        this.row = row;
        this.column = column;
    }

    public int getNote() {
        return note;
    }

    public void setNote(int note) {
        this.note = note;
        setText(NOTE_MAP.get(note));
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }

    public void highlightOn() {
        setEffect(BLOOM);
        setBackground(NOTE_BACKGROUND_MAP.get((note - 21) % 7));
    }

    public void highlightOff() {
        setEffect(null);
        setBackground(DEFAULT_BACKGROUND);
    }
}
