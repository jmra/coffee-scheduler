module com.coffeescheduler {
    requires javafx.controls;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires org.apache.poi.ooxml;
    requires org.apache.poi.poi;

    opens com.coffeescheduler.model to com.fasterxml.jackson.databind;
    opens com.coffeescheduler.io to com.fasterxml.jackson.databind;

    exports com.coffeescheduler.ui;
    exports com.coffeescheduler.model;
    exports com.coffeescheduler.generator;
    exports com.coffeescheduler.io;
}
