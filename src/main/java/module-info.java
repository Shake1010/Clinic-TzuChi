module com.tzuchi.clinicroomsystem {
    requires javafx.controls;
    requires javafx.graphics;
    requires java.net.http;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires javafx.media;


    requires transitive javafx.base;

    exports com.tzuchi.clinicroomsystem;
    
}