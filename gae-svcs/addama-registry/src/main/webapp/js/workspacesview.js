Ext.ns("org.systemsbiology.addama.js.widgets.workspaces");

/*
 * Global Singleton
 */
org.systemsbiology.addama.js.widgets.workspaces.FileUpload = null;
org.systemsbiology.addama.js.widgets.workspaces.TreePanel = null;

/*
 * Widgets
 */
org.systemsbiology.addama.js.widgets.workspaces.WorkspacesTabPanel = Ext.extend(Object, {
    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.widgets.workspaces.WorkspacesTabPanel.superclass.constructor.call(this);

        this.loadPanels();
        this.loadTree();
    },

    loadPanels: function() {
        this.startPanel = new Ext.Panel({
            layout: "fit",
            bodyStyle: "padding:25px",
            contentEl: "container_start"
        });

        this.folderViewPanel = new Ext.Panel({
            layout: "fit",
            bodyStyle: "padding:25px",
            contentEl: "container_folder_content"
        });

        this.fileViewPanel = new Ext.Panel({
            layout: "fit",
            bodyStyle: "padding:25px",
            contentEl: "container_file_content"
        });

        this.contentPanel = new Ext.Panel({
            layout: "card",
            region: "center",
            margins: "5 0 5 5",
            activeItem: 0,
            border: false,
            items: [ this.startPanel, this.folderViewPanel, this.fileViewPanel ]
        });

        this.treePanel = new Ext.tree.TreePanel({
            id: "tree-panel",
            title: "Workspaces",
            contentEl: "container_tree",
            region:"west",
            split: true,
            minSize: 150,
            autoScroll: true,
            border: true,
            margins: "5 0 5 5",
            width: 275,
            maxSize: 500,
            // tree-specific configs:
            rootVisible: false,
            lines: false,
            singleExpand: true,
            useArrows: true,
            loader: new Ext.tree.TreeLoader(),
            root: new Ext.tree.AsyncTreeNode()
        });
        this.treePanel.on("expandnode", this.lookupNodeItems, this);
        this.treePanel.on("expandnode", this.displayNodeInContentPanel, this);
        this.treePanel.on("expandnode", this.displayNodeProperties, this);
        this.treePanel.on("click", this.displayNodeInContentPanel, this);
        this.treePanel.on("click", this.displayNodeProperties, this);

        org.systemsbiology.addama.js.widgets.workspaces.TreePanel = this.treePanel;

        this.fileUploadControl = new org.systemsbiology.addama.js.widgets.workspaces.FileUploadControl({ treePanel: this.treePanel });

        this.propertyGrid = new Ext.grid.PropertyGrid({
            title: "Properties",
            border:true,
            width: 320,
            region: "south",
            minSize: 100,
            maxSize: 500,
            autoScroll: true,
            collapsed: true,
            collapsible: true,
            titleCollapse: true,
            autoHeight: true,
            autoWidth: true,
            store: new Ext.data.JsonStore()
        });

        this.mainPanel = new Ext.Panel({
            title: "Main",
            margins: "5 5 5 0",
            layout: "border",
            border: true,
            split: true,
            items:[ this.treePanel, this.contentPanel, this.propertyGrid ]
        });
    },

    displayNodeInContentPanel: function(node) {
        if (this.lastSelectedNodeId && this.lastSelectedNodeId == node.id) {
            return;
        }

        this.lastSelectedNodeId = node.id;

        var nodeCls = node.attributes.cls;

        var layout = this.contentPanel.layout;
        if (layout) {
            if (nodeCls == "repository" || nodeCls == "folder") {
                layout.setActiveItem(this.folderViewPanel.id);

                var label = node.attributes.label ? node.attributes.label : node.attributes.name;
                Ext.getDom("container_folder_item").innerHTML = "Selected Folder '" + label + "'";

            } else if (nodeCls == "file") {
                layout.setActiveItem(this.fileViewPanel.id);
                this.renderFilePreviewAndLink(layout, node);
            } else {
                layout.setActiveItem(this.startPanel.id);
            }
        }
    },

    displayNodeProperties: function (node) {
        this.propertyGrid.setSource(node.attributes);
    },

    loadTree: function() {
        Ext.Ajax.request({
            url: "/addama/workspaces",
            method: "GET",
            success: function(response) {
                var repos = this.getJsonRepos(response);

                this.treePanel.setRootNode(new Ext.tree.AsyncTreeNode({
                    text: 'Workspaces',
                    draggable:false,
                    id:'addamatreetopid',
                    children: repos.items
                }));

                org.systemsbiology.addama.js.Message.show("Workspaces", "Ready to browse");
            },
            scope: this,
            failure: function(response) {
                org.systemsbiology.addama.js.Message.error("Workspaces", "Failed to load workspaces: " + response.statusText);
            }
        });
    },

    getJsonRepos: function (response) {
        var data = Ext.util.JSON.decode(response.responseText)
        for (var i = 0; i < data.items.length; i++) {
            var item = data.items[i];
            item.text = item.label ? item.label : item.name;
            item.id = item.uri;
            item.path = "/";
            item.isRepository = true;
            item.cls = "repository";
            item.leaf = false;
            if (i == 0) {
                item.expanded = true;
            }
            item.children = [];
        }
        return data;
    },

    lookupNodeItems: function (node) {
        if (node.id == "addamatreetopid") {
            return;
        }

        var parentPath = node.attributes.path;
        if (parentPath == "/") {
            parentPath = "";
        }

        Ext.Ajax.request({
            url: node.id,
            method: "GET",
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json && json.items) {
                    Ext.each(json.items, function(item) {
                        if (!this.treePanel.getNodeById(item.uri)) {
                            item.text = item.label ? item.label : item.name;
                            item.id = item.uri;
                            item.path = parentPath + "/" + item.name;
                            if (item.isFile) {
                                item.leaf = true;
                                item.cls = "file";
                            } else {
                                item.leaf = false;
                                item.cls = "folder";
                                item.children = [];
                            }
                            node.appendChild(item);
                        }
                    }, this);
                    node.renderChildren();
                }
            },
            scope: this,
            failure: function() {
                node.expandable = false;
            }
        });
    },

    renderFilePreviewAndLink: function(layout, node) {
        var uri = node.attributes.uri;
        var name = node.attributes.label ? node.attributes.label : node.attributes.name;

        Ext.getDom("container_file_download").innerHTML = "<a href='" + uri + "' target='_blank'>Download '" + name + "'</a>";
        Ext.getDom("container_file_preview").innerHTML = "";

        var mimeType = node.attributes.mimeType;
        if (mimeType && mimeType.substring(0, 5) == "image") {
            Ext.getDom("container_file_preview").innerHTML = "<img class='img_preview' src='" + uri + "'/>";
        }
    }
});

org.systemsbiology.addama.js.widgets.workspaces.FileUploadControl = Ext.extend(Object, {
    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.widgets.workspaces.FileUploadControl.superclass.constructor.call(this);

        org.systemsbiology.addama.js.widgets.workspaces.FileUpload = this;

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
            org.systemsbiology.addama.js.widgets.workspaces.RefreshNodeTree(selectedNode);
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

/*
 * Static Functions
 */
org.systemsbiology.addama.js.widgets.workspaces.RefreshNodeTree = function(node) {
    if (node.isExpanded()) {
        node.collapse();
    }
    node.expand();
};

org.systemsbiology.addama.js.widgets.workspaces.CreateFolder = function() {
    var fn = function(btn, text) {
        var selectedNode = org.systemsbiology.addama.js.widgets.workspaces.TreePanel.getSelectionModel().getSelectedNode();
        if (selectedNode) {
            Ext.Ajax.request({
                url: selectedNode.attributes.uri + "/" + text,
                method: "POST",
                success: function() {
                    org.systemsbiology.addama.js.Message.show("Workspaces", "Folder '" + text + "' added successfully");
                    org.systemsbiology.addama.js.widgets.workspaces.RefreshNodeTree(selectedNode);
                },
                failure: function(o) {
                    org.systemsbiology.addama.js.Message.error("Workspaces", "Failed to add new folder [" + o.statusText + "]");
                }
            });
        } else {
            org.systemsbiology.addama.js.Message.error("Workspaces", "Please select a folder");
        }
    };

    Ext.MessageBox.prompt("Create Folder", "Please enter new folder name", fn);
};

org.systemsbiology.addama.js.widgets.workspaces.DoFileUpload = function() {
    org.systemsbiology.addama.js.widgets.workspaces.FileUpload.fileUploadWindow.show();
};