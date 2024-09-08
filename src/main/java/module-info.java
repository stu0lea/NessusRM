module cn.viewcn.nessusrm {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    requires fastjson;
    requires java.sql;
    requires okhttp3;
    requires java.xml.bind;
    requires org.apache.httpcomponents.httpcore;
    requires org.apache.httpcomponents.httpclient;


    opens cn.viewcn.nessusrm.gui to javafx.fxml;
    exports cn.viewcn.nessusrm.gui;
}