Ext.onReady(function() {
    Ext.get("btn-createfolder").on("click", function(e){
        Ext.MessageBox.prompt("Create Folder", "Please enter new folder name", createNewFolder);
    });

    var fbutton = new Ext.ux.form.FileUploadField({
        renderTo: 'fi-button',
        buttonOnly: true,
        listeners: {
            'fileselected': uploadNewFile
        }
    });
});

var win = new Ext.Window({
    title: 'Upload Status',
    width: 400,
    minWidth: 350,
    height: 150,
    modal: true,
    closeAction: 'hide',
    bodyStyle: 'padding:10px;',
    html: "File uploading...",
    bbar: new Ext.ux.StatusBar({ id: 'upload-file-statusbar', defaultText: 'Ready' })
});
win.on("show", function () {
    var sb = Ext.getCmp("upload-file-statusbar");
    sb.showBusy();
});

function createNewFolder(btn, text){
    if (selectedNode) {
        Ext.Ajax.request({
            url: selectedNode.attributes.uri + "/" + text,
            method: "POST",
            success: function() {
                eventManager.fireEvent("display-status-message", { text: "Folder " + text + " Added Successfully", level: "info" });
                eventManager.fireEvent("node-selection", selectedNode);
            },
            failure: function() {
                eventManager.fireEvent("display-status-message", { text: "Failed to add New Folder", level: "error" });
            }
        });
    } else {
        eventManager.fireEvent("display-status-message", { text: "Invalid Folder Selected", level: "error" });
    }
}

function uploadNewFile() {
    if (!selectedNode) {
        eventManager.fireEvent("display-status-message", { text: "Invalid Folder Selected", level: "error" });
        return;
    }

    win.show();
    Ext.Ajax.request({
        url: selectedNode.attributes.uri + "/directlink",
        method: "GET",
        success: function(response) {
            var json = Ext.util.JSON.decode(response.responseText);
            if (json.location) {
                var form = new Ext.form.BasicForm("frm-uploadfile");
                form.submit({
                    fileUpload: true,
                    clientValidation: true,

                    url: json.location + "?x-addama-desired-contenttype=text/html",
                    success: function(form, action) {
                        eventManager.fireEvent("display-status-message", { text:  "File Uploaded Successfully", level: "info" });
                        eventManager.fireEvent("node-selection", selectedNode);
                    },
                    failure: function(form, action) {
                        eventManager.fireEvent("display-status-message", { text:  "File Upload Failed", level: "error" });
                    }
                });
            } else {
                eventManager.fireEvent("display-status-message", { text: "Failed to upload file. Please try Again.", level: "error" });
            }
            win.close();
        },
        failure: function(response) {
            eventManager.fireEvent("display-status-message", { text: "Failed to upload file. Please try Again.", level: "error" });
            win.close();
        }
    });
}

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
