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
    if (selectedNode) {
        Ext.Ajax.request({
            url: selectedNode.attributes.uri + "/" + text,
            method: "POST",
            success: function() {
                eventManager.fireEvent("display-status-message", { text: "Folder " + text + " Added Successfully", level: "info" });
                eventManager.fireEvent("node-selection", selectedNode);
            },
            failure: msgFolderCreateFailed
        });
    } else {
        msgFolderCreateFailed();
    }
}

function msgFolderCreateFailed() {
    eventManager.fireEvent("display-status-message", { text: "Failed to add New Folder", level: "error" });
}