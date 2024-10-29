module cn.viewcn.nessusrm {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.activation;

    requires fastjson;
    requires java.sql;
    requires okhttp3;
    requires java.xml.bind;
    requires org.apache.httpcomponents.httpcore;
    requires org.apache.httpcomponents.httpclient;

    opens cn.viewcn.nessusrm.gui to javafx.fxml,java.activation,fastjson,org.apache.httpcomponents.httpclient,org.apache.httpcomponents.httpcore;
    exports cn.viewcn.nessusrm.gui;
}