function renderFilePreviewAndLink(layout, node) {
    var uri = node.attributes.uri;
    var name = node.attributes.label ? node.attributes.label : node.attributes.name;

    Ext.getDom("main-content-file-download").innerHTML = "<a href='" + uri + "' target='_blank'>Download '" + name + "'</a>";
    Ext.getDom("main-content-file-preview").innerHTML = "";

    var mimeType = node.attributes.mimeType;
    if (mimeType && mimeType.substring(0, 5) == "image") {
        Ext.getDom("main-content-file-preview").innerHTML = "<img src='" + uri + "' width='50%' heigth='50%'/>";
    }
}

function doCreateFolder() {
    Ext.MessageBox.prompt("Create Folder", "Please enter new folder name", createNewFolder);
}

function createNewFolder(btn, text){
    var createFailedFn = function() {
        eventManager.fireEvent("display-status-message", { text: "Failed to add New Folder", level: "error" });
    };

    var selectedNode = tree.getSelectionModel().getSelectedNode();
    if (selectedNode) {
        Ext.Ajax.request({
            url: selectedNode.attributes.uri + "/" + text,
            method: "POST",
            success: function() {
                eventManager.fireEvent("display-status-message", { text: "Folder " + text + " Added Successfully", level: "info" });
                eventManager.fireEvent("node-refresh", selectedNode);
            },
            failure: createFailedFn
        });
    } else {
        createFailedFn();
    }
}

function displayNodeInContentPanel(node) {
    var nodeCls = node.attributes.cls;

    var layout = contentPanel.layout;
    if (layout) {
        if (nodeCls == "repository" || nodeCls == "folder") {
            layout.setActiveItem("main-content-folder-panel");

            var label = node.attributes.label ? node.attributes.label : node.attributes.name;
            Ext.getDom("main-content-folder-item").innerHTML = "Selected Folder '" + label + "'";

        } else if (nodeCls == "file") {
            layout.setActiveItem("main-content-file-panel");
            renderFilePreviewAndLink(layout, node);
        } else {
            layout.setActiveItem("main-content-start-panel");
        }
    }
}

function displayNodeInPropertiesPanel(node) {
    Ext.getDom("panel-properties").innerHTML = "";
    var annotationsGrid = new Ext.grid.PropertyGrid({
        renderTo: "panel-properties",
        autoHeight: true,
        autoWidth: true,
        selModel: new Ext.grid.RowSelectionModel({singleSelect:true}),
        source: node.attributes
    });
    annotationsGrid.render();
}

function displayStatusMessage(message) {
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
}