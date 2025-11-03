module ui {
    requires javafx.controls;
    requires javafx.fxml;
    requires atlantafx.base;
    requires java.net.http;

    opens com.example.ui to javafx.fxml; // Replace com.example.ui with the package of your FXML controllers
    exports com.example.ui; // Replace com.example.ui with your main package
}
