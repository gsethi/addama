var fileUploadWindow;
var fileUploadFrm;
var uploadProgressWindow;

Ext.onReady(function() {
    uploadProgressWindow = new Ext.Window({
        title: 'Upload Status',
        width: 400,
        minWidth: 350,
        height: 150,
        modal: true,
        closeAction: 'hide',
        bodyStyle: 'padding:10px;',
        html: "File uploading..."
    });

    fileUploadWindow = new Ext.Window({
        title: 'Upload File',
        id: 'uploadfile-window',
        width: 600,
        height: 130,
        modal: true,
        closeAction : 'hide',
        items: []
    });

    fileUploadWindow.on("beforeshow", function () {
        fileUploadFrm = new Ext.form.FormPanel({
            id: 'reposUploadFileForm',
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
                        })
                    ]
                })
            ],
            buttons: [
                new Ext.Button({
                    id: 'show-button',
                    text: 'Upload',
                    listeners: {
                        click: uploadFile
                    }
                })
            ]
        });

        fileUploadWindow.add(fileUploadFrm);
    });

    fileUploadWindow.on("hide", function () {
        fileUploadWindow.removeAll();
    });
});

function doFileUpload() {
    fileUploadWindow.show();
}

function uploadFile() {
    uploadProgressWindow.show();

    var selectedNode = this.treePanel.getSelectionModel().getSelectedNode();
    if (selectedNode) {
        Ext.Ajax.request({
            url: selectedNode.attributes.uri + "/directlink",
            method: "GET",
            success: function(response) {
                var json = Ext.util.JSON.decode(response.responseText);
                if (json.location) {
                    fileUploadFrm.getForm().submit({
                        clientValidation: true,
                        url: json.location + "?x-addama-desired-contenttype=text/html",
                        success: function() {
                            uploadProgressWindow.hide();
                            fileUploadWindow.hide();
                            org.systemsbiology.addama.js.Message.show("Workspaces", "File uploaded successfully");
                            RefreshNodeTree(selectedNode);
                        },
                        failure: function(o) {
                            org.systemsbiology.addama.js.Message.error("Workspaces", "Failed to upload file [" + o.statusText + "]");
                            uploadProgressWindow.hide();
                        }
                    });
                } else {
                    org.systemsbiology.addama.js.Message.error("Workspaces", "Failed to upload file [location not specified]");
                    uploadProgressWindow.hide();
                }
            },
            failure: function(o) {
                org.systemsbiology.addama.js.Message.error("Workspaces", "Failed to upload file [" + o.statusText + "]");
                uploadProgressWindow.hide();
            }
        });
    } else {
        org.systemsbiology.addama.js.Message.error("Workspaces", "Please select a folder");
    }
}
