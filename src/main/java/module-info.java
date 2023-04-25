module cn.viewcn.nessusdm {
    requires javafx.controls;
    requires javafx.fxml;


    opens cn.viewcn.nessusrm to javafx.fxml;
    exports cn.viewcn.nessusrm;
}