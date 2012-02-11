Ext.ns("org.systemsbiology.addama.js.experimental");

org.systemsbiology.addama.js.experimental.SelectFileControl = Ext.extend(Ext.util.Observable, {
    constructor: function(config) {
        Ext.apply(this, config);

        this.addEvents({ selectFile: true });

        org.systemsbiology.addama.js.experimental.SelectFileControl.superclass.constructor.call(this);
    },

    setWorkspace: function(workspace) {
        this.workspace = workspace;
    },

    activate: function() {
        if (!this.workspaceUri) {
            Ext.Ajax.request({
                url: "/addama/users/whoami",
                success: function(o) {
                    var email = Ext.util.JSON.decode(o.responseText).email;
                    this.workspaceUri = "/addama/workspaces/" + this.workspace + "/" + email;
                    this.loadSelectWindow();
                },
                scope: this
            });
        } else {
            this.loadSelectWindow();
        }
    },

    loadSelectWindow: function() {
        this.browseWorkspaceTreeControl = new org.systemsbiology.addama.js.experimental.BrowseWorkspaceTreeControl({ workspaceUri: this.workspaceUri });

        var choosePanel = new Ext.Panel({
            border: true, frame: true, autoHeight: true, autoWidth: true,
            html: "<img src='/images/Folder-Open-icon.png' width='48' height='48' />"
        });

        var downloadPanel = new Ext.Panel({
            border: true, frame: true, autoHeight: true, autoWidth: true,
            html: "<img src='/images/download-icon.png' width='48' height='48' />"
        });

        var trashPanel = new Ext.Panel({
            border: true, frame: true, autoHeight: true, autoWidth: true,
            html: "<img src='/images/trashcan-full-icon.png' width='48' height='48' />"
        });

        new org.systemsbiology.addama.js.experimental.DropControl(choosePanel).on("drop", this.chooseFile, this);
        new org.systemsbiology.addama.js.experimental.DropControl(downloadPanel).on("drop", this.downloadSelected, this);
        new org.systemsbiology.addama.js.experimental.DropControl(trashPanel).on("drop", this.deleteNode, this);

        this.controlWindow = new Ext.Window({
            closable: true,
            plain: true,
            width: 800,
            heigth: 500,
            title: "Browse Workspace",
            layout: "auto",
            items: [
                this.browseWorkspaceTreeControl.treePanel
            ],
            tbar: [
                new Ext.Button({
                    text: "Create Folder",
                    handler: this.createFolder,
                    scope: this
                }),
                new Ext.Button({
                    text: "Upload File",
                    handler: this.uploadFile,
                    scope: this
                })
            ],
            fbar: [
                choosePanel, downloadPanel, trashPanel
            ]
        });
        this.controlWindow.show();
    },

    onSelectFile: function(callback) {
        this.on("selectFile", callback);
    },

    createFolder: function() {
        var targetFolder = this.browseWorkspaceTreeControl.lastSelectedFolder;
        Ext.MessageBox.prompt('Create folder', 'Enter new folder name', function(btn, text) {
            Ext.Ajax.request({
                url: targetFolder.id + "/" + text,
                method: "POST",
                success: this.refreshFolder,
                scope: this
            });
        }, this);
    },

    uploadFile: function() {
        var targetFolder = this.browseWorkspaceTreeControl.lastSelectedFolder;
        var ufc = new org.systemsbiology.addama.js.experimental.UploadFileControl({ uploadUri: targetFolder.id });
        ufc.on("uploadComplete", this.refreshFolder, this);
    },

    chooseFile: function(targetFile) {
        if (targetFile && targetFile.attributes.isFile) {
            this.fireEvent("selectFile", targetFile.id);

            Ext.MessageBox.show({
                title: "Selected file",
                msg: targetFile.attributes.label,
                buttons: Ext.MessageBox.OK,
                fn: function() {
                    this.controlWindow.close();
                },
                scope: this
            });
        } else {
            Ext.MessageBox.show({
                title: "Choose File",
                msg: "Please select a file item.",
                buttons: Ext.MessageBox.OK
            });
        }
    },

    deleteNode: function(node) {
        Ext.Msg.show({
           title: "Delete Item",
           msg: "Permanently delete '" + node.attributes.label + "'?",
           buttons: Ext.Msg.YESNO,
           fn: function(answer) {
               if (answer && answer == "yes") {
                   Ext.Ajax.request({
                       url: node.id + "/delete",
                       method: "POST",
                       success: function() {
                           node.parentNode.removeChild(node);
                           node.destroy();
                       }
                   });
               }
           }
        });
    },

    refreshFolder: function() {
        this.browseWorkspaceTreeControl.refreshNode();
    },

    downloadSelected: function(item) {
        if (item.isFile) {
            document.location.href = item.id;
        } else {
            document.location.href = item.id + "/zip";
        }
    }
});

org.systemsbiology.addama.js.experimental.BrowseWorkspaceTreeControl = Ext.extend(Object, {
    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.experimental.BrowseWorkspaceTreeControl.superclass.constructor.call(this);

        this.treePanel = new Ext.tree.TreePanel({
            useArrows: true,
            autoScroll: true,
            animate: true,
            enableDD: true,
            ddGroup: "dd_generic",
            height: 400,
            containerScroll: true,
            border: false,
            dataUrl: this.workspaceUri,
            requestMethod: "GET",
            header: true,
            rootVisible: false,
            root: {
                nodeType: 'async',
                text: 'Workspace',
                draggable: false,
                id: this.workspaceUri,
                isFolder: true
            }
        });

        this.treePanel.on("expandnode", this.expandTreeNode, this);
        this.treePanel.on("afterrender", this.expandRootNode, this);

        new Ext.tree.TreeSorter(this.treePanel, {folderSort:true});
    },

    expandRootNode: function() {
        this.treePanel.getRootNode().expand(true);
        this.lastSelectedFolder = this.treePanel.getRootNode();
    },

    refreshNode: function() {
        if (this.lastSelectedFolder) {
            this.lastSelectedFolder.collapse();
            this.lastSelectedFolder.expand();
        }
    },

    expandTreeNode: function(node) {
        if (node.attributes.isFolder) {
            this.lastSelectedFolder = node;
            this.treePanel.setTitle("path: " + node.id);

            Ext.Ajax.request({
                url: node.id,
                method: "GET",
                success: function(o) {
                    node.removeAll(true);

                    var json = Ext.util.JSON.decode(o.responseText);
                    if (json && json.items) {
                        Ext.each(json.items, function(item) {
                            item.text = item.name;
                            item.id = item.uri;
                            item.path = item.uri;
                            if (item.isFile) {
                                item.isFolder = false;
                                item.leaf = true;
                                item.cls = "file";
                            } else {
                                item.isFolder = true;
                                item.leaf = false;
                                item.cls = "folder";
                            }

                            node.appendChild(item);
                        });

                        node.renderChildren();
                    }
                }
            });
        }
    }

});

org.systemsbiology.addama.js.experimental.DropControl = Ext.extend(Ext.util.Observable, {
    constructor: function(panel) {
        Ext.apply(this, {});

        this.addEvents({ drop: true });

        org.systemsbiology.addama.js.experimental.DropControl.superclass.constructor.call(this);

        panel.on("render", this.initDropTarget, this);
    },

    initDropTarget: function(v) {
        var me = this;
        new Ext.dd.DropTarget(v.getEl().dom, {
            ddGroup: "dd_generic",
            notifyDrop: function(source, e, data) {
                me.fireEvent("drop", data.node);
                return true;
            }
        });
    }
});

org.systemsbiology.addama.js.experimental.UploadFileControl = Ext.extend(Ext.util.Observable, {
    constructor: function(config) {
        Ext.apply(this, config);

        this.addEvents({ uploadComplete: true });

        org.systemsbiology.addama.js.experimental.UploadFileControl.superclass.constructor.call(this);

        var fileUploadField = new Ext.ux.form.FileUploadField({ buttonOnly: true, id: 'form-file', name: 'file-path' });
        fileUploadField.on("fileselected", this.onFileSelected, this);

        this.formPanel = new Ext.FormPanel({
            fileUpload: true,
            frame: true,
            autoHeight: true,
            bodyStyle: 'padding: 10px 10px 0 10px;',
            labelWidth: 50,
            defaults: {
                anchor: '95%',
                allowBlank: false,
                msgTarget: 'side'
            },
            items: [ fileUploadField ]
        });

        var win = new Ext.Window({
            closable: true,
            plain: true,
            width: 300,
            heigth: 300,
            title: "Upload Files",
            layout: "auto",
            items: [ this.formPanel ]
        });
        win.show();

        this.on("uploadComplete", win.close, win);
    },

    onFileSelected: function() {
        Ext.Ajax.request({
            url: this.uploadUri + "/directlink",
            method: "GET",
            success: this.submitUpload,
            scope: this
        });
    },

    submitUpload: function(o) {
        var json = Ext.util.JSON.decode(o.responseText);
        if (json && json.location) {
            this.formPanel.getForm().submit({
                url: json.location + "?x-addama-desired-contenttype=text/html",
                waitMsg: 'Uploading your file...',
                success: function() {
                    this.fireEvent("uploadComplete");
                },
                scope: this
            });
        }
    }
});

org.systemsbiology.addama.js.experimental.CreateOVGBMDataTable = function(ovtabledata, gbmtabledata) {
    var ovdataArray = ovtabledata.map(function(c) {
        return {cancertype: 'OV',
            chr:c.chr, gene: c.options.split('=')[1], start: c.start, end: c.end,
            genecount: c.genecount, patientcount: c.value};
    })

    for (var i = 0; i < gbmtabledata.length; i++) {
        ovdataArray = ovdataArray.concat({cancertype: 'GBM',
            chr:gbmtabledata[i].chr, gene: gbmtabledata[i].options.split('=')[1], start: gbmtabledata[i].start, end: gbmtabledata[i].end,
            genecount: gbmtabledata[i].genecount, patientcount: gbmtabledata[i].value});
    }

    var totalDataArray = {'root': ovdataArray};

    var ovgbmStore = new Ext.data.JsonStore({
        // store configs
        autoDestroy: true,
        data: totalDataArray,
        remoteSort: false,
        sortInfo: {
            field: 'genecount',
            direction: 'DESC'
        },
        storeId: 'ovgbmstore',
        // reader configs
        root: 'root',
        fields: [
            {
                name: 'cancertype'
            },
            {
                name: 'chr'
            },
            {
                name: 'gene'
            },
            {
                type: 'float',
                name: 'start'
            },
            {
                type: 'float',
                name: 'end'
            },
            {
                type: 'float',
                name: 'genecount'
            },
            {
                type: 'float',
                name: 'patientcount'
            }
        ]
    });

    var filters = new Ext.ux.grid.GridFilters({
        // encode and local configuration options defined previously for easier reuse
        encode: false, // json encode the filter query
        local: true,   // defaults to false (remote filtering)
        filters: [
            {
                type: 'float',
                dataIndex: 'start'
            },
            {
                type: 'string',
                dataIndex: 'chr'
            },
            {
                type: 'float',
                dataIndex: 'end'
            },
            {
                type: 'string',
                dataIndex: 'gene'
            },
            {
                type: 'float',
                dataIndex: 'patientcount'
            },
            {
                type: 'float',
                dataIndex: 'genecount'
            },
            {
                type: 'list',
                dataIndex: 'cancertype',
                options: ['OV','GBM']
            }
        ]
    });

    var createColModel = function (finish, start) {

        var columns = [
            {
                header: 'Cancer Type',
                dataIndex: 'cancertype'
            },
            {
                header: 'Gene',
                dataIndex: 'gene',
                width: 150
            },
            {
                header: 'Chr',
                dataIndex: 'chr'
            },
            {
                header: 'Start',
                dataIndex: 'start'
            },
            {
                header: 'End',
                dataIndex: 'end'
            },
            {
                header: '# Patients',
                dataIndex: 'patientcount'
            },
            {
                header: '# Disruptions',
                dataIndex: 'genecount'
            }
        ];

        return new Ext.grid.ColumnModel({
            columns: columns.slice(start || 0, finish),
            defaults: {
                sortable: true,
                width: 120
            }
        });
    };
    var sm = new Ext.grid.RowSelectionModel({
        listeners: {
            'rowselect': function(sm, rowIndex, record) {
                var chrom = record.json.chr;
                var startpos = record.json.start;
                var endpos = record.json.end;
                var gene_symbol = record.json.gene;
                var buffer = 50000;
                onRangeSelection(chrom, startpos - buffer < 0 ? 0 : startpos - buffer, endpos + buffer, gene_symbol);
            }
        }
    });

    return new Ext.grid.GridPanel({
        border: true,
        store: ovgbmStore,
        height: 600,
        colModel: createColModel(7),
        columnLines: true,
        frame: true,
        iconCls:'icon-grid',
        stripeRows: true,
        height: 600,
        width: 700,
        sm: sm,
        loadMask: true,
        plugins: [filters],
        view: new Ext.ux.grid.BufferView({
            // render rows as they come into viewable area.
            scrollDelay: false
        })
    });


};

org.systemsbiology.addama.js.experimental.CreateOVGBMGridPanel = function(tableData) {
    var dataArray = tableData.map(function(c) {
        return {
            chr:c.chr, gene: c.options.split('=')[1], start: c.start, end: c.end,
            patientvalue: c.value, genecount: c.genecount};
    })

    var totalDataArray = {'root': dataArray};

    var ovgbmStore = new Ext.data.JsonStore({
        autoDestroy: true,
        data: totalDataArray,
        remoteSort: false,
        sortInfo: {
            field: 'genecount',
            direction: 'DESC'
        },
        storeId: 'ovgbmstore',
        // reader configs
        root: 'root',
        fields: [
            {
                type: 'float',
                name: 'start'
            },
            {
                name: 'chr'
            },
            {
                type: 'float',
                name: 'end'
            },
            {
                name: 'gene'
            },
            {
                type: 'float',
                name: 'patientvalue'
            },
            {
                type: 'float',
                name: 'genecount'
            }
        ]


    });

    var filters = new Ext.ux.grid.GridFilters({
        // encode and local configuration options defined previously for easier reuse
        encode: false, // json encode the filter query
        local: true,   // defaults to false (remote filtering)
        filters: [
            {
                type: 'float',
                dataIndex: 'start'
            },
            {
                type: 'string',
                dataIndex: 'chr'
            },
            {
                type: 'float',
                dataIndex: 'end'
            },
            {
                type: 'string',
                dataIndex: 'gene'
            },
            {
                type: 'float',
                dataIndex: 'patientvalue'
            },
            {
                type: 'float',
                dataIndex: 'genecount'
            }
        ]
    });

    var createColModel = function (finish, start) {

        var columns = [
            {
                header: 'Gene',
                dataIndex: 'gene',
                width: 150
            },
            {
                header: 'Chr',
                dataIndex: 'chr'
            },
            {
                header: 'Start',
                dataIndex: 'start'
            },
            {
                header: 'End',
                dataIndex: 'end'
            },
            {
                header: '# Patients',
                dataIndex: 'patientvalue'
            },
            {
                header: '# Disruptions',
                dataIndex: 'genecount'
            }
        ];

        return new Ext.grid.ColumnModel({
            columns: columns.slice(start || 0, finish),
            defaults: {
                sortable: true,
                width: 100,
                filterable: true
            }
        });
    };

    var sm = new Ext.grid.RowSelectionModel({
        listeners: {
            'rowselect': function(sm, rowIndex, record) {
                var chrom = record.json.chr;
                var startpos = record.json.start;
                var endpos = record.json.end;
                var gene_symbol = record.json.gene;
                var buffer = 50000;
                onRangeSelection(chrom, startpos - buffer < 0 ? 0 : startpos - buffer, endpos + buffer, gene_symbol);
            }
        }
    });

    return new Ext.grid.GridPanel({
        border: true,
        store: ovgbmStore,
        height: 600,
        colModel: createColModel(6),
        columnLines: true,
        frame: true,
        iconCls:'icon-grid',
        stripeRows: true,
        height: 600,
        width: 700,
        sm: sm,
        loadMask: true,
        plugins: [filters],
        view: new Ext.ux.grid.BufferView({
            // render rows as they come into viewable area.
            scrollDelay: false
        })

    });
};