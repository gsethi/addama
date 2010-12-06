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

Ext.extend(EventManager, Ext.util.Observable, {
    ctype: "EventManager",

    fireNodeSelectEvent: function(node) {
        this.fireEvent("node-selection", node);
    },

    registerNodeSelectListener: function(callback, scope, opts) {
        this.addListener("node-selection", callback, scope, opts);
    },

    fireStatusMessageEvent: function(message) {
        this.fireEvent("display-status-message", message);
    },

    registerStatusMessageListener: function(callback, scope, opts) {
        this.addListener("display-status-message", callback, scope, opts);
    }
});

var eventManager = new EventManager();
eventManager.registerStatusMessageListener(function(message) {
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
eventManager.registerNodeSelectListener(function(node) {
    var nodeCls = node.attributes.cls;

    var layout = contentPanel.layout;
    if (layout) {
        if (nodeCls == "repository") {
            layout.setActiveItem("main-content-repository-panel");
            renderReposSubFolderForm(node);
            renderReposUploadFileForm(node);
        } else if (nodeCls == "file") {
            layout.setActiveItem("main-content-file-panel");
            renderFilePreviewAndLink(layout, node);
        } else if (nodeCls == "folder") {
            layout.setActiveItem("main-content-folder-panel");
            renderSubFolderForm(node);
            renderUploadFileForm(node);
        } else {
            layout.setActiveItem("main-content-start-panel");
        }
    }
});

eventManager.registerNodeSelectListener(function(node) {
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
