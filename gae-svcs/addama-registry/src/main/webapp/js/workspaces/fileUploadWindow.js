Ext.ns("org.systemsbiology.addama.js");

org.systemsbiology.addama.js.FileUpload = null;

org.systemsbiology.addama.js.FileUploadControl = Ext.extend(Object, {
    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.FileUploadControl.superclass.constructor.call(this);

        org.systemsbiology.addama.js.FileUpload = this;

        this.uploadProgressWindow = new Ext.Window({
            title: 'Upload Status',
            width: 400,
            minWidth: 350,
            height: 150,
            modal: true,
            closeAction: 'hide',
            bodyStyle: 'padding:10px;',
            html: "File uploading..."
        });

        this.fileUploadWindow = new Ext.Window({
            title: 'Upload File',
            width: 600,
            height: 130,
            modal: true,
            closeAction : 'hide',
            items: []
        });
        this.fileUploadWindow.on("beforeshow", this.readyFileUploadWindow, this);
        this.fileUploadWindow.on("hide", this.clearFileUploadWindow, this);
    },

    readyFileUploadWindow: function() {
        var uploadBtn = new Ext.Button({ id: 'show-button', text: 'Upload'});
        uploadBtn.on("click", this.uploadFile, this);

        this.fileUploadFrm = new Ext.form.FormPanel({
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
            buttons: [ uploadBtn ]
        });

        this.fileUploadWindow.add(this.fileUploadFrm);
    },

    clearFileUploadWindow: function() {
        this.fileUploadWindow.removeAll();
    },

    failedUpload: function(message) {
        org.systemsbiology.addama.js.Message.error("Workspaces", message);
        this.uploadProgressWindow.hide();
    },

    uploadFile: function() {
        this.uploadProgressWindow.show();

        var selectedNode = this.treePanel.getSelectionModel().getSelectedNode();
        var me = this;
        var goodUploadFn = function() {
            me.uploadProgressWindow.hide();
            me.fileUploadWindow.hide();
            org.systemsbiology.addama.js.Message.show("Workspaces", "File uploaded successfully");
            RefreshNodeTree(selectedNode);
        };
        var badUploadFn = function(o) {
            me.failedUpload("Failed to upload file [" + o.statusText + "]");
        };

        if (selectedNode) {
            Ext.Ajax.request({
                url: selectedNode.attributes.uri + "/directlink",
                method: "GET",
                success: function(response) {
                    var json = Ext.util.JSON.decode(response.responseText);
                    if (json.location) {
                        var uploadUrl = json.location + "?x-addama-desired-contenttype=text/html";
                        this.fileUploadFrm.getForm().submit({
                            clientValidation: true, url: uploadUrl, success: goodUploadFn, failure: badUploadFn
                        });
                    } else {
                        this.failedUpload("Failed to upload file [location not found]");
                    }
                },
                failure: function(o) {
                    this.failedUpload("Failed to upload file [" + o.statusText + "]");
                },
                scope: this
            });
        } else {
            this.failedUpload("Please select a folder");
        }
    }
});

function DoFileUpload() {
    org.systemsbiology.addama.js.FileUpload.fileUploadWindow.show();
}
