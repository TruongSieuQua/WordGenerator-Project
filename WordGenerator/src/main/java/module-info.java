module edu.truongjenny.WordGenerator {
    requires javafx.controls;
    requires javafx.fxml;
	requires org.json;
	requires org.jsoup;
	requires java.desktop;
	requires transitive javafx.graphics;

    opens edu.truongjenny.WordGenerator to javafx.fxml;
    exports edu.truongjenny.WordGenerator;
}