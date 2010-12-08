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

function doFileUpload() {
    Ext.getDom("main-content-folder-addsub").innerHTML = "";

    new Ext.form.FormPanel({
        id: 'reposUploadFileForm',
        title: 'Upload File',
        renderTo: "main-content-folder-addsub",
        method: 'POST',
        fileUpload : true,
        border: true,
        items: [
            new Ext.form.FieldSet({
                autoHeight: true,
                autoWidth: true,
                border: false,
                items: [
                    new Ext.form.TextField({
                        fieldLabel: 'Select file',
                        defaultAutoCreate : {tag:"input", enctype:"multipart/form-data", type:"file", size: "35", autocomplete: "off"},
                        name: 'FILE',
                        id: 'reposUploadFileNameId',
                        allowBlank: false
                    }),
                    new Ext.Button({
                        id: 'show-button',
                        text: 'Upload',
                        listeners: {
                            click: uploadFile
                        }
                    })
                ]
            })
        ]
    }).render();
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

function uploadFile() {
    win.show();

    Ext.Ajax.request({
        url: selectedNode.attributes.uri + "/directlink",
        method: "GET",
        success: function(response) {
            var json = Ext.util.JSON.decode(response.responseText);
            if (json.location) {
                Ext.getCmp('reposUploadFileForm').getForm().submit({
                    clientValidation: true,
                    url: json.location + "?x-addama-desired-contenttype=text/html",
                    success: function() {
                        eventManager.fireEvent("display-status-message", { text:  "File Uploaded Successfully", level: "info" });
                        win.close();
                        Ext.getDom("main-content-folder-addsub").innerHTML = "";
                    },
                    failure: msgFileFailed
                });
            } else {
                msgFileFailed();
            }
        },
        failure: msgFileFailed
    });
}

function msgFileFailed() {
    eventManager.fireEvent("display-status-message", { text: "Failed to upload file. Please try Again.", level: "error" });
    win.close();
    Ext.getDom("main-content-folder-addsub").innerHTML = "";
}

function msgFolderCreateFailed() {
    eventManager.fireEvent("display-status-message", { text: "Failed to add New Folder", level: "error" });
}