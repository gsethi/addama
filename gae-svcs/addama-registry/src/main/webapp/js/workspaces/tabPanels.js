Ext.ns("org.systemsbiology.addama.js");

org.systemsbiology.addama.js.TreePanel = null;

org.systemsbiology.addama.js.WorkspacesTabPanel = Ext.extend(Object, {
    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.WorkspacesTabPanel.superclass.constructor.call(this);

        org.systemsbiology.addama.js.TreePanel = this;

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
            id: "panel-content-control",
            title: "Main",
            layout: "card",
            region: "center",
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
            layout: "fit",
            layoutConfig: {
                titleCollapse: true,
                hideCollapseTool: true,
                animate: true,
                activeOnTop: false
            },
            // tree-specific configs:
            rootVisible: false,
            lines: false,
            singleExpand: true,
            useArrows: true,
            loader: new Ext.tree.TreeLoader(),
            root: new Ext.tree.AsyncTreeNode()
        });
        this.treePanel.render();
        this.treePanel.on("expandnode", this.expandNode, this, {single: true});
        this.treePanel.on("expandnode", this.displayNodeInContentPanel, this, {single: true});
        this.treePanel.on("expandnode", this.displayNodeInPropertiesPanel, this, {single: true});
        this.treePanel.on("click", RefreshNodeTree, this, {single: true});
        this.treePanel.on("click", this.displayNodeInContentPanel, this, {single: true});
        this.treePanel.on("click", this.displayNodeInPropertiesPanel, this, {single: true});

        this.propertiesGrid = new Ext.grid.PropertyGrid({
            autoHeight: true,
            autoWidth: true,
            selModel: new Ext.grid.RowSelectionModel({singleSelect:true})
        });

        this.propertiesPanel = new Ext.Panel({
            title: "Properties",
            layout: "fit",
            border:true,
            margins: "5 5 5 0",
            width: 320,
            minSize: 100,
            maxSize: 500,
            animate: true,
            activeOnTop: false,
            bodyStyle: "padding-bottom:15px; background:#eee;",
            autoScroll: true,
            contentEl: "container_properties",
            items: [this.propertiesGrid]
        });

        this.mainPanel = new Ext.Panel({
            id: "panel-main-control",
            margins: "5 5 5 0",
            layout: "border",
            border: true,
            split: true,
            items:[ this.treePanel, this.contentPanel ],
            bbar: statusBar
        });
    },

    displayNodeInContentPanel: function(node) {
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

    displayNodeInPropertiesPanel: function (node) {
        this.propertiesGrid.source = node.attributes;
        this.propertiesGrid.render();
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

                statusBar.displayMessage("Workspaces loaded");
            },
            scope: this,
            failure: function(response) {
                statusBar.displayError("Failed to load workspaces");
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

    expandNode: function (node) {
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
            Ext.getDom("container_file_preview").innerHTML = "<img src='" + uri + "' width='50%' height='50%'/>";
        }
    }
});

function RefreshNodeTree(node) {
    if (node.isExpanded()) {
        node.collapse();
    }
    node.expand();
}

function CreateFolder() {
    var fn = function(btn, text) {
        var selectedNode = org.systemsbiology.addama.js.TreePanel.getSelectionModel().getSelectedNode();
        if (selectedNode) {
            Ext.Ajax.request({
                url: selectedNode.attributes.uri + "/" + text,
                method: "POST",
                success: function() {
                    statusBar.displayMessage("Folder '" + text + "' added successfully");
                    RefreshNodeTree(selectedNode);
                },
                failure: function() {
                    statusBar.displayError("Failed to add new folder");
                }
            });
        } else {
            statusBar.displayError("Please select a folder");
        }
    };

    Ext.MessageBox.prompt("Create Folder", "Please enter new folder name", fn);
}