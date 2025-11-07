module ui {
    requires javafx.controls;
    requires javafx.fxml;
    requires atlantafx.base;
    requires java.net.http;
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome6;

    opens com.example.ui to javafx.fxml; // Replace com.example.ui with the package of your FXML controllers
    exports com.example.ui;

}
