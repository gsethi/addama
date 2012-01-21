Ext.ns("org.systemsbiology.addama.js.widgets");

/*
 * Global Singletons
 */
org.systemsbiology.addama.js.Message = null;

/*
 * Widgets for Addama UI
 */
org.systemsbiology.addama.js.widgets.MessageHelper = Ext.extend(Object, {
    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.widgets.MessageHelper.superclass.constructor.call(this);

        this.messageContainer = Ext.DomHelper.insertFirst(document.body, {id:'container_js_message'}, true);
    },

    show: function(title, message) {
        this.display(title, "msg", message);
    },

    error: function(title, message) {
        this.display(title, "msg-error", message);
    },

    display: function(title, divClass, message) {
        var msgBox = '<div class="' + divClass + '"><h3>' + title + '</h3><p>' + message + '</p></div>';
        var messageEl = Ext.DomHelper.append(this.messageContainer, msgBox, true);
        messageEl.hide();
        messageEl.slideIn('t', { duration:1 }).pause(3).puff('t', { duration:1 }).ghost('t', {duration:1, remove:true});
    }
});

org.systemsbiology.addama.js.widgets.Viewport = Ext.extend(Object, {
    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.widgets.Viewport.superclass.constructor.call(this);

        this.drawViewport();
    },

    drawViewport: function() {
        var addamaBanner = Ext.DomHelper.append(Ext.getBody(), '<div><a href="http://addama.org" target="_blank"><img src="/images/banner.png" alt="Addama"/></a></div>', true);
        var appengineLogo = Ext.DomHelper.append(Ext.getBody(), '<div><a href="http://code.google.com/appengine"><img src="https://code.google.com/appengine/images/appengine-silver-120x30.gif" alt="Powered by Google App Engine"/></a></div>', true);
        var link1 = Ext.DomHelper.append(Ext.getBody(), '<div><a href="/addama/apikeys/file">Download API Keys</a></div>', true);
        var link2 = Ext.DomHelper.append(Ext.getBody(), '<div><a href="http://addama.org" target="_blank">Addama Open Source Project</a></div>', true);
        var link3 = Ext.DomHelper.append(Ext.getBody(), '<div><a href="http://www.systemsbiology.org" target="_blank">Institute for Systems Biology</a></div>', true);
        var link4 = Ext.DomHelper.append(Ext.getBody(), '<div><a href="http://shmulevich.systemsbiology.net/" target="_blank">Shmulevich Lab</a></div>', true);
        var link5 = Ext.DomHelper.append(Ext.getBody(), '<div><a href="http://codefor.systemsbiology.net/" target="_blank">Code for Systems Biology</a></div>', true);
        var link6 = Ext.DomHelper.append(Ext.getBody(), '<div><a href="http://www.ncbi.nlm.nih.gov/pubmed/19265554" target="_blank">PMID 19265554</a></div>', true);

        var headerPanel = new Ext.Panel({
            region: "north",
            border: false,
            frame: false,
            layout: "border",
            height: 99,
            defaults: { border: false, frame: false },
            items:[
                { contentEl: this.topbarEl, region: "north", height: 30 },
                { contentEl: addamaBanner.id, region: "center" },
                { contentEl: appengineLogo.id, region: "east", width: 130 }
            ]
        });

        if (this.tabs && this.tabs.length) {
            var firstTab = this.tabs[0];
            if (firstTab && !firstTab.title) {
                firstTab.title = "Main";
            }
            Ext.each(this.tabs, function(tab) {
                if (tab && !tab.title) {
                    tab.title = "...";
                }
            });
            if (this.activateAjaxMonitor) {
                this.tabs.push(new org.systemsbiology.addama.js.widgets.AjaxMonitor().gridPanel);
            }
        }

        var tabPanel = new Ext.TabPanel({
            region: "center",
            items: this.tabs,
            activeTab: 0,
            border: true,
            margins: "5 5 5 5"
        });

        var footerPanel = new Ext.Panel({
            region:"south",
            height: 33,
            layout: "hbox",
            border: false,
            frame: true,
            padding: "1 1 1 1",
            margins: "1 1 1 1",
            layoutConfig: { pack: "center", align: "middle", defaultMargins: "0 10 0 0" },
            items:[ link1.dom, link2.dom, link3.dom, link4.dom, link5.dom, link6.dom ]
        });

        new Ext.Viewport({ layout:'border', items:[ headerPanel, tabPanel, footerPanel ] });
    }
});

org.systemsbiology.addama.js.widgets.AjaxMonitor = Ext.extend(Object, {
    SEQUENCE_ID: 1,
    GRID_DATA: [],

    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.widgets.AjaxMonitor.superclass.constructor.call(this);

        this.createGrid();
        Ext.Ajax.on('requestcomplete', this.addResponseToGrid, this);
    },

    createGrid: function() {
        this.store = new Ext.data.JsonStore({
            storeId: "store-ajax-monitor",
            root: "responses",
            idProperty: "id",
            sortInfo: {field: "id", direction: "DESC"},
            fields: [
                { name: "id", type: "int"},
                { name: "method" },
                { name: "uri" },
                { name: "statusCode" },
                { name: "statusText" },
                { name: "responseText" },
                { name: "isJson", type: "boolean" }
            ]
        });

        this.gridPanel = new Ext.grid.GridPanel({
            region: "center",
            store: this.store,
            columns: [
                { header: "ID", width: 50, dataIndex: 'id', type: "int", sortable: true, hidden: true },
                { header: "Method", width: 75, dataIndex: 'method' },
                { header: "URI", width: 400, dataIndex: 'uri', sortable: true },
                { header: "Status Code", width: 75, dataIndex: 'statusCode', sortable: true },
                { header: "Status Text", width: 100, dataIndex: 'statusText' },
                { header: "Response Text", width: 300, dataIndex: 'responseText', hidden: true }
            ],
            stripeRows: true,
            columnLines: true,
            frame:true,
            title: "Requests",
            collapsible: false,
            animCollapse: false,
            iconCls: 'icon-grid'
        });
        this.gridPanel.on("rowclick", this.showResponseContent, this);
    },

    addResponseToGrid: function(connection, response, options) {
        var contentType = response.getResponseHeader("Content-Type");
        var isJson = (contentType != undefined && contentType.indexOf("ation/json") > 0);

        var newData = {
            id:this.incrementedId(),
            method: options.method,
            uri: options.url,
            statusCode: response.status,
            statusText: response.statusText,
            responseText: response.responseText,
            isJson: isJson
        };
        this.GRID_DATA.push(newData);
        this.store.loadData({responses:this.GRID_DATA});
    },

    showResponseContent: function(g, rowIndex, e) {
        var data = this.store.getAt(rowIndex).data;
        var responseText = data.responseText;
        if (data.isJson) {
            var json = Ext.util.JSON.decode(responseText);
            var stringify = JSON.stringify(json, null, "\u00a0\u00a0\u00a0\u00a0");
            responseText = Ext.util.Format.nl2br(stringify);
        }

        new Ext.Window({
            width:800,
            height:400,
            autoScroll: true,
            modal: true,
            title: "Response Content",
            html: responseText
        }).show();
    },

    incrementedId: function() {
        return this.SEQUENCE_ID++;
    }

});

org.systemsbiology.addama.js.widgets.AppsPanel = Ext.extend(Object, {
    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.widgets.AppsPanel.superclass.constructor.call(this);

        Ext.Ajax.request({
            url: "/addama/apps",
            method: "GET",
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json && json.items) {
                    var appsDiv = Ext.get(this.contentEl);
                    Ext.each(json.items, function(item) {
                        Ext.DomHelper.append(appsDiv, org.systemsbiology.addama.js.topbar.GenerateAppPanelHtml(item));
                    });
                }
            },
            scope: this
        });
    }
});

org.systemsbiology.addama.js.widgets.ServicesPanel = Ext.extend(Ext.util.Observable, {
    constructor: function(config) {
        Ext.apply(this, config);

        this.addEvents("loadService");

        org.systemsbiology.addama.js.widgets.ServicesPanel.superclass.constructor.call(this);

        this.on("loadService", this.loadService, this);

        Ext.Ajax.request({
            url: "/addama/services",
            method: "GET",
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json && json.items) {
                    Ext.each(json.items, function(item) {
                        this.fireEvent("loadService", item.uri);
                    }, this);

                }
            },
            scope: this
        });
    },

    loadService: function(uri) {
        Ext.Ajax.request({
            url: uri,
            method: "GET",
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json) {
                    var html = org.systemsbiology.addama.js.widgets.ServicesPanel.GenerateHtml(json);
                    Ext.DomHelper.append(Ext.get(this.contentEl), html);
                }
            },
            scope: this
        })
    }
});

org.systemsbiology.addama.js.widgets.ServicesPanel.GenerateHtml = function(item) {
    var versionUrl = item.url;
    if (versionUrl.substring(item.uri.length - 1) == "/") {
        versionUrl = versionUrl.substring(0, versionUrl.length -1);
    }
    versionUrl += "/version";


    var label = item.label;
    if (!label) {
        label = "Untitled";
    }

    var itemhtml = "";
    itemhtml += "<div class='svcs'>";
    itemhtml += "<h3><a href='" + versionUrl + "' target='_blank'>" + label+ "</a></h3>";
    if (item.items) {
        itemhtml += "<ul>";
        Ext.each(item.items, function(mapping) {
           itemhtml += "<li>" + mapping.uri + "<div class='svcs_map_label'>" + mapping.label + "</div></li>";
        });
        itemhtml += "</ul>";
    }
    itemhtml += "</div>";
    return itemhtml;
};

org.systemsbiology.addama.js.widgets.GreenlistWindow = Ext.extend(Object, {
    constructor: function(config) {
        if (!config) {
            config = {};
        }

        Ext.apply(this, config);

        org.systemsbiology.addama.js.widgets.GreenlistWindow.superclass.constructor.call(this, config);

        this.store = new Ext.data.ArrayStore({ fields: [ {name: "id"} ] });

        this.listView = new Ext.list.ListView({
            title: "Authorized Users",
            store: this.store,
            emptyText: "No users have been entered.  Domain is open to everyone",
            reserveScrollOffset: true,
            hideHeaders: true,
            padding: "10 10 10 10",
            margins: "10 10 10 10",
            columns: [
                { header: "User", width: 300, sortable: true, dataIndex: "id" }
            ]
        });

        this.panel = new Ext.Panel({
            width:425,
            height:500,
            layout: "fit",
            title: "Authorized Users",
            items: this.listView,
            tbar: [
                new Ext.Button({ text: "Add User", handler: this.addNewUser, scope: this })
            ]
        });

        this.loadGreenlist();
    },

    loadGreenlist: function() {
        org.systemsbiology.addama.js.Message.show("User Access", "Loading authorized users");

        Ext.Ajax.request({
            url: "/addama/greenlist",
            method: "GET",
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json && json.items) {
                    var data = [];
                    Ext.each(json.items, function(item) {
                        data.push([item.id]);
                    });
                    this.store.loadData(data);
                }
            },
            failure: this.handleFailure,
            scope: this
        });
    },

    addNewUser: function() {
        var fld = new Ext.form.TextField({
            name: "label",
            anchor: "100%",
            allowBlank:false,
            labelSeparator: "",
            fieldLabel: "Email Address"
        });

        var fpAddUser = new Ext.FormPanel({
            width: 400,
            frame: true,
            autoHeight: true,
            bodyStyle: 'padding: 10px 10px 0 10px;',
            items: [ fld ],
            buttons: [
                {
                    text: "Save",
                    handler: function() {
                        if (fld.getRawValue()) {
                            this.saveNewUser(fld.getRawValue());
                        }
                    },
                    scope: this
                }
            ]
        });

        this.window = new Ext.Window({
            width:400, autoHeight:true, title: 'Add New User', items: [fpAddUser], frame: true
        });
        this.window.show();
    },

    saveNewUser: function(userEmail) {
        Ext.Ajax.request({
            url: "/addama/greenlist/" + userEmail,
            method: "POST",
            success: function() {
                this.window.close();
                this.loadGreenlist();
            },
            failure: this.handleFailure,
            scope: this
        });
    },

    handleFailure: function(o) {
        org.systemsbiology.addama.js.Message.error("User Access", "Error loading data: " + o.statusText);
    }
});

org.systemsbiology.addama.js.widgets.DatasourcesView = Ext.extend(Object, {
    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.widgets.DatasourcesView.superclass.constructor.call(this);

        this.loadTree();
        this.loadDatabases();
    },

    loadTree: function() {
        this.rootNode = new Ext.tree.AsyncTreeNode();

        this.treePanel = new Ext.tree.TreePanel({
            title: "Datasources",
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
            root: this.rootNode
        });
        this.treePanel.on("expandnode", this.expandNode, this);
        this.treePanel.on("click", this.selectTable, this);

        var dataPanel = {
            margins: "5 5 5 0",
            layout: "border",
            region: "center",
            border: true,
            split: true,
            items:[
                new Ext.Panel({
                    title: "Query",
                    region: "north",
                    contentEl: "container_sql",
                    width:810,
                    bbar: new Ext.Toolbar({
                        items: [
                            { text: "Show Results", handler: this.queryHtml, scope: this },
                            { text: "Export to CSV", handler: this.queryCsv, scope: this },
                            { text: "Export to TSV", handler: this.queryTsv, scope: this }
                        ]
                    })
                }),
                { title: "Results", region: "center", contentEl: "container_preview", width:810, height: 400 }
            ]
        };

        this.mainPanel = new Ext.Panel({
            title: "Main",
            contentEl: this.contentEl,
            margins: "5 5 5 0",
            layout: "border",
            border: true,
            split: true,
            items:[
                this.treePanel,
                dataPanel
            ]
        });
    },

    loadDatabases: function() {
        Ext.Ajax.request({
            url: "/addama/datasources",
            method: "GET",
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json && json.items) {
                    Ext.each(json.items, function(database) {
                        database.isDb = true;
                    });
                    this.addNodes(this.rootNode, json.items);
                }
            },
            failure: this.displayFailure,
            scope: this
        });
    },

    expandNode: function(node) {
        if (node.id == "addamatreetopid") {
            return;
        }

        Ext.Ajax.request({
            url: node.id,
            method: "GET",
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json && json.items) {
                    this.addNodes(node, json.items);
                }
            },
            scope: this,
            failure: function() {
                node.expandable = false;
            }
        });
    },

    addNodes: function(node, items) {
        var parentPath = "/addama/datasources";
        if (!node.isRoot) {
            parentPath = node.id;
        }

        Ext.each(items, function(item) {
            if (!item.id) {
                if (item.uri) {
                    item.id = item.uri;
                } else if (item.name) {
                    item.id = parentPath + "/" + item.name;
                }
            }

            if (!this.treePanel.getNodeById(item.id)) {
                item.text = item.label ? item.label : item.name;
                if (item.datatype) {
                    item.text += " [" + item.datatype + "]";
                }

                item.path = item.id;
                item.leaf = node.attributes.isTable;
                item.isTable = node.attributes.isDb;
                if (item.leaf) {
                    item.cls = "file";
                } else {
                    item.cls = "folder";
                    item.children = [];
                }
                node.appendChild(item);
            }
        }, this);
        node.renderChildren();
    },

    displayFailure: function(o) {
        org.systemsbiology.addama.js.Message.error("Datasources", "Error Loading: " + o.statusText);
    },

    selectTable: function(node) {
        if (node.attributes.isTable) {
            this.selectedTable = node;
            org.systemsbiology.addama.js.Message.show("Datasources", "Table Selected: " + node.attributes.text);
            this.expandNode(this.selectedTable);
        }
    },

    queryHtml: function() {
        if (!this.isReadyToQuery()) {
            return;
        }

        Ext.getDom("container_preview").innerHTML = "";

        var tableUri = this.selectedTable.id;
        var childNodes = this.selectedTable.childNodes;
        var querySql = Ext.getDom("textarea_sql").value;

        Ext.Ajax.request({
            url: tableUri + "/query",
            method: "GET",
            params: {
                tq: querySql,
                tqx: "out:json_array"
            },
            success: function(o) {
                var data = Ext.util.JSON.decode(o.responseText);
                if (data) {
                    org.systemsbiology.addama.js.Message.show("Datasources", "Retrieved " + data.length + " Records");

                    var fields = [];
                    var columns = [];
                    Ext.each(childNodes, function(childNode) {
                        var fld = childNode.attributes.name;
                        fields.push(fld);
                        // TODO : Insert data type
                        columns.push({ id: fld, header: fld, dataIndex: fld, sortable: true });
                    });

                    var store = new Ext.data.JsonStore({
                        storeId : 'arrayStore',
                        autoDestroy: true,
                        idProperty: fields[0],
                        root : 'results',
                        fields: fields
                    });

                    store.loadData({ results: data });

                    var grid = new Ext.grid.GridPanel({
                        store: store,
                        colModel: new Ext.grid.ColumnModel({
                            defaults: {
                                width: 200, sortable: true
                            },
                            columns: columns
                        }),
                        viewConfig: {
                            forceFit: true
                        },
                        stripeRows: true,
                        frame: true,
                        border: false,
                        width: 600,
                        height: 600,
                        autoScroll: true,
                        iconCls: 'icon-grid'
                    });

                    grid.render("container_preview");
                }
            },
            failure: this.displayFailure,
            scope: this
        });
    },

    queryCsv: function() {
        if (!this.isReadyToQuery()) {
            return;
        }

        var tableUri = this.selectedTable.id;
        var querySql = Ext.getDom("textarea_sql").value;
        document.location = tableUri + "/query?tq=" + querySql + "&tqx=reqId:123;out:csv;outFileName:results.csv";
    },

    queryTsv: function() {
        if (!this.isReadyToQuery()) {
            return;
        }

        var tableUri = this.selectedTable.id;
        var querySql = Ext.getDom("textarea_sql").value;
        document.location = tableUri + "/query?tq=" + querySql + "&tqx=reqId:123;out:tsv-excel;outFileName:results.tsv";
    },

    isReadyToQuery: function() {
        if (!this.selectedTable) {
            org.systemsbiology.addama.js.Message.error("Datasources", "No table selected for query");
            return false;
        }

        if (!Ext.getDom("textarea_sql").value) {
            org.systemsbiology.addama.js.Message.error("Datasources", "SQL statement not entered. Defaulting to SELECT *");
            return true;
        }

        return true;
    }
});

/*
 * Instantiated on import of this script
 */
Ext.onReady(function() {
    org.systemsbiology.addama.js.Message = new org.systemsbiology.addama.js.widgets.MessageHelper();

    Ext.Ajax.on('requestexception', function(c, o) {
        if (o.status == 401) {
            org.systemsbiology.addama.js.Message.error("Unauthorized Access", "Your access to this resource has been denied.  Please contact an administrator.");
        }
    });
});
