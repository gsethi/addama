EventManager = function(config){
    config = config || {};
    if (config.initialConfig) {
        config = config.initialConfig;
    }

    this.initialConfig = config;

    Ext.apply(this, config);
    this.addEvents(
        "node-selection",
        "display-status-message"
    );
    EventManager.superclass.constructor.call(this);
};

Ext.extend(EventManager, Ext.util.Observable, { ctype: "EventManager" });

var eventManager = new EventManager();
eventManager.addListener("display-status-message", function(message) {
    var level = message.level || "info";
    var text = message.text || message;
    var icon = "x-status-valid";
    if (level === "error") {
        icon = "x-status-error"
    }
    statusBar.setStatus({
        text: text,
        iconCls: icon,
        clear: {
            wait: 5000,
            anim: false,
            useDefaults: false
        }
    });
});
eventManager.addListener("node-selection", function(node) {
    selectedNode = node;
});

eventManager.addListener("node-selection", function(node) {
    var nodeCls = node.attributes.cls;

    var layout = contentPanel.layout;
    if (layout) {
        if (nodeCls == "repository") {
            layout.setActiveItem("main-content-folder-panel");
            renderFolderInMain(node);
            renderReposUploadFileForm(node);
        } else if (nodeCls == "file") {
            layout.setActiveItem("main-content-file-panel");
            renderFilePreviewAndLink(layout, node);
        } else if (nodeCls == "folder") {
            layout.setActiveItem("main-content-folder-panel");
            renderFolderInMain(node);
            renderUploadFileForm(node);
        } else {
            layout.setActiveItem("main-content-start-panel");
        }
    }
});

eventManager.addListener("node-selection", function(node) {
    Ext.getDom("panel-properties").innerHTML = "";
    var annotationsGrid = new Ext.grid.PropertyGrid({
        renderTo: "panel-properties",
        autoHeight: true,
        autoWidth: true,
        selModel: new Ext.grid.RowSelectionModel({singleSelect:true}),
        source: node.attributes
    });
    annotationsGrid.render();
});
