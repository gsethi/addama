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

function renderSubFolderForm(node) {
    Ext.getDom("main-content-folder-addsub").innerHTML = "";
    var uri = node.attributes.uri;
    var subfolderFormPanel = new Ext.form.FormPanel({
        id: 'folderAddSubForm',
        title: 'New Folder',
        method: 'POST',
        renderTo: "main-content-folder-addsub",
        border: true,
        items: [
            new Ext.form.FieldSet({
                autoHeight: true,
                autoWidth: true,
                border: false,
                items: [
                    new Ext.form.TextField({
                        fieldLabel: 'Folder Name',
                        defaultAutoCreate : {tag:"input", size: "40", autocomplete: "off"},
                        name: 'newSubfolder',
                        id: 'newSubfolderId',
                        allowBlank: false
                    }),
                    new Ext.Button({
                        id: 'show-button',
                        text: 'Add',
                        listeners: {
                            click: function() {
                                createFolder(node, Ext.getCmp("newSubfolderId").getValue());
                            }
                        }
                    })
                ]
            })
        ]
    });
    subfolderFormPanel.render();
    Ext.DomHelper.insertHtml("beforeEnd", Ext.getDom("main-content-folder-addsub"), "<br>");
}

function renderUploadFileForm(node) {
    Ext.getDom("main-content-folder-upload").innerHTML = "";
    var uri = node.attributes.uri;

    var uploadFileFormComponent = new Ext.form.FormPanel({
        id: 'folderUploadFileForm',
        title: 'Upload File',
        renderTo: "main-content-folder-upload",
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
                        id: 'uploadFileNameId',
                        allowBlank: false
                    }),
                    new Ext.Button({
                        id: 'show-button',
                        text: 'Upload',
                        listeners: {click:
                            function() {
                                var formPanel = Ext.getCmp('folderUploadFileForm');
                                var uploadFileName = Ext.getCmp("uploadFileNameId").getValue();
                                uploadFile(formPanel, uploadFileName, node);
                            }
                        }
                    })
                ]
            })
        ]
    });
    uploadFileFormComponent.render();
    Ext.DomHelper.insertHtml("beforeEnd", Ext.getDom("main-content-folder-upload"), "<br>");
}

function renderReposSubFolderForm(node) {
    Ext.getDom("main-content-repository-addsub").innerHTML = "";
    var uri = node.attributes.uri;
    var subfolderFormPanel = new Ext.form.FormPanel({
        id: 'reposAddSubFormId',
        title: 'New Folder',
        method: 'POST',
        renderTo: "main-content-repository-addsub",
        border: true,
        items: [
            new Ext.form.FieldSet({
                autoHeight: true,
                autoWidth: true,
                border: false,
                items: [
                    new Ext.form.TextField({
                        fieldLabel: 'New Folder',
                        defaultAutoCreate : {tag:"input", size: "40", autocomplete: "off"},
                        name: 'reposNewSubfolder',
                        id: 'reposNewSubfolderId',
                        allowBlank: false
                    }),
                    new Ext.Button({
                        id: 'show-button',
                        text: 'Add',
                        listeners: {
                            click: function() {
                                createFolder(node, Ext.getCmp("reposNewSubfolderId").getValue());
                            }
                        }
                    })
                ]
            })
        ]
    });
    subfolderFormPanel.render();
    Ext.DomHelper.insertHtml("beforeEnd", Ext.getDom("main-content-repository-addsub"), "<br>");
}

function renderReposUploadFileForm(node) {
    Ext.getDom("main-content-repository-upload").innerHTML = "";
    var uri = node.attributes.uri;

    var uploadFileFormComponent = new Ext.form.FormPanel({
        id: 'reposUploadFileForm',
        title: 'Upload File',
        renderTo: "main-content-repository-upload",
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
                        listeners: {click:
                            function() {
                                var formPanel = Ext.getCmp('reposUploadFileForm');
                                var uploadFileName = Ext.getCmp("reposUploadFileNameId").getValue();
                                uploadFile(formPanel, uploadFileName, node);
                            }
                        }
                    })
                ]
            })
        ]
    });
    uploadFileFormComponent.render();
    Ext.DomHelper.insertHtml("beforeEnd", Ext.getDom("main-content-repository-upload"), "<br>");
}

function uploadFile(formPanel, uploadFileName, node) {
    Ext.Ajax.request({
        url: node.attributes.uri + "/directlink",
        method: "GET",
        success: function(response) {
            var json = Ext.util.JSON.decode(response.responseText);
            if (json.location) {
                upload(node, formPanel, json.location, uploadFileName);
            } else {
                eventManager.fireEvent("display-status-message", { text: "Failed to upload file. Please try Again.", level: "error" });
            }
        },
        failure: function(response) {
            eventManager.fireEvent("display-status-message", { text: "Failed to upload file. Please try Again.", level: "error" });
        }
    });
}

function createFolder(node, subfolder) {
    Ext.Ajax.request({
        url: node.attributes.uri + "/" + subfolder,
        method: "POST",
        success: function() {
            eventManager.fireEvent("display-status-message", { text: "Folder " + subfolder + " Added Successfully", level: "info" });
            eventManager.fireEvent("node-selection", node);
        },
        failure: function() {
            eventManager.fireEvent("display-status-message", { text: "Failed to add New Folder", level: "error" });
        }
    });
}

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


function upload(node, formPanel, uri, fileName) {
    win.show();

    formPanel.getForm().submit({
        clientValidation: true,
        url: uri + "?x-addama-desired-contenttype=text/html",
        success: function(form, action) {
            eventManager.fireEvent("display-status-message", { text:  "File Uploaded Successfully", level: "info" });
            win.close();
            eventManager.fireEvent("node-selection", node);
        },
        failure: function(form, action) {
            eventManager.fireEvent("display-status-message", { text:  "File Upload Failed", level: "error" });
            win.close();
        }
    });
}
