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
    var selectedNode = tree.getSelectionModel().getSelectedNode();
    if (selectedNode) {
        Ext.Ajax.request({
            url: selectedNode.attributes.uri + "/" + text,
            method: "POST",
            success: function() {
                statusBar.displayMessage("Folder '" + text + "' added successfully");
                eventManager.fireEvent("node-refresh", selectedNode);
            },
            failure: function() {
                statusBar.displayError("Failed to add new folder");
            }
        });
    } else {
        statusBar.displayError("Please select a folder");
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
