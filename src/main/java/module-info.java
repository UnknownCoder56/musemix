module com.uniqueapps.musemix {
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.controls;
    requires java.desktop;
    requires java.xml.crypto;
    requires atlantafx.base;
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome6;

    opens com.uniqueapps.musemix to javafx.fxml;
    exports com.uniqueapps.musemix;
}