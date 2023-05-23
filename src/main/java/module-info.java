module cn.viewcn.nessusdm {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.xml.bind;
    requires org.apache.httpcomponents.httpcore;
    requires org.apache.httpcomponents.httpclient;
    requires fastjson;
    requires okhttp3;
    requires java.sql;
//    requires java.xml.bind;
//    requires org.apache.httpcomponents.httpcore;
//    requires org.apache.httpcomponents.httpclient;
    exports cn.viewcn.nessusrm.core;
    opens cn.viewcn.nessusrm.core to javafx.fxml;
    exports cn.viewcn.nessusrm.gui;
    opens cn.viewcn.nessusrm.gui to javafx.fxml;
}