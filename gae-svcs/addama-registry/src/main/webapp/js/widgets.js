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

        this.messageContainer = Ext.DomHelper.insertFirst(document.body, { tag: "div", cls:"msg-container"}, true);
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
        var addamaBanner = Ext.DomHelper.append(Ext.getBody(), '<div><img src="/images/banner.png" alt="Addama"/></div>', true);
        var appengineLogo = Ext.DomHelper.append(Ext.getBody(), '<div><a href="http://code.google.com/appengine"><img src="https://code.google.com/appengine/images/appengine-silver-120x30.gif" alt="Powered by Google App Engine"/></a></div>', true);
        var link1 = Ext.DomHelper.append(Ext.getBody(), '<div><a href="/html/apikeys.html" target="_blank">Download API Keys</a></div>', true);
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
            margins: "5 5 5 5",
            tbar: [
                this.newToolbarItem("Home", "/"), '-',
                this.newToolbarItem("Browse Files", "/html/workspaces.html"), '-',
                this.newToolbarItem("Query Databases", "/html/datasources.html"), '-',
                this.newToolbarItem("Query Chromosomes", "/html/chromosomes.html"), '-',
                this.newToolbarItem("Download API Keys", "/html/apikeys.html")
            ]
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
    },

    newToolbarItem: function(text, link) {
        return new Ext.Action({
            text: text,
            handler: function() {
                document.location = link;
            }
        });
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
                { name: "contentType" },
                { name: "requestParams" },
                { name: "requestHeaders" },
                { name: "responseText" },
                { name: "isJson", type: "boolean" }
            ]
        });

        this.gridPanel = new Ext.grid.GridPanel({
            region: "center",
            store: this.store,
            columns: [
                { header: "ID", width: 50, dataIndex: 'id', type: "int", sortable: true },
                { header: "Method", width: 75, dataIndex: 'method', sortable: true },
                { header: "URI", width: 400, dataIndex: 'uri', sortable: true },
                { header: "Status Code", width: 75, dataIndex: 'statusCode', sortable: true },
                { header: "Status Text", width: 100, dataIndex: 'statusText', sortable: true },
                { header: "Content Type", width: 100, dataIndex: 'contentType', sortable: true }
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
        var contentType = this.getContentType(response);
        var isJson = (contentType != undefined && contentType.indexOf("ation/json") > 0);

        var newData = {
            id:this.incrementedId(),
            method: options.method,
            uri: options.url,
            statusCode: response.status,
            statusText: response.statusText,
            responseText: response.responseText,
            contentType: contentType,
            requestParams: options.params,
            requestHeaders: options.headers,
            isJson: isJson
        };
        this.GRID_DATA.push(newData);
        this.store.loadData({responses:this.GRID_DATA});
    },

    getContentType: function(response) {
        if (response.getResponseHeader) {
            return response.getResponseHeader("Content-Type");
        }
        return "unknown";
    },

    showResponseContent: function(g, rowIndex, e) {
        var data = this.store.getAt(rowIndex).data;

        var items = [];
        items.push({ title: "REST", html: data.method + " " + data.uri, collapsible: false });

        var isEmpty = function(obj) {
          return Object.keys(obj).length === 0;
        };

        if (data.requestParams) {
            if (!isEmpty(data.requestParams)) {
                var stringify = JSON.stringify(data.requestParams, null, "\u00a0\u00a0\u00a0\u00a0");
                var parameters = Ext.util.Format.nl2br(stringify);
                items.push({ title: "REQUEST PARAMETERS", html: parameters });
            }
        }

        if (data.requestHeaders) {
            delete data.requestHeaders["X-Requested-With"];
            if (!isEmpty(data.requestHeaders)) {
                var stringify = JSON.stringify(data.requestHeaders, null, "\u00a0\u00a0\u00a0\u00a0");
                var headers = Ext.util.Format.nl2br(stringify);
                items.push({ title: "REQUEST HEADERS", html: headers });
            }
        }

        var responseText = data.responseText;
        if (data.isJson) {
            var json = Ext.util.JSON.decode(responseText);
            var stringify = JSON.stringify(json, null, "\u00a0\u00a0\u00a0\u00a0");
            responseText = Ext.util.Format.nl2br(stringify);
        }
        items.push({ title: "RESPONSE", html: responseText });

        new Ext.Window({
            width: 800,
            height: 450,
            autoScroll: true,
            modal: true,
            frame: true,
            defaults: { frame: true, width: "100%", autoScroll: true, collapsible: true, collapsed: false },
            items: items
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
                        Ext.DomHelper.append(appsDiv, org.systemsbiology.addama.js.topbar.RegisterAppsWindow.GenerateHtml(item));
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
    var serviceHomePage = item.url;
    if (serviceHomePage.substring(item.uri.length - 1) == "/") {
        serviceHomePage = serviceHomePage.substring(0, serviceHomePage.length -1);
    }
    serviceHomePage += "/index.html";


    var label = item.label;
    if (!label) {
        label = "Untitled";
    }

    var itemhtml = "";
    itemhtml += "<div class='svcs'>";
    itemhtml += "<h3><a href='" + serviceHomePage + "' target='_blank'>" + label+ "</a></h3>";
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
            autoScroll: true,
            border: true,
            margins: "5 0 5 5",
            width: 275,
            frame: true,
            collapsible: true,
            // tree-specific configs:
            rootVisible: false,
            lines: false,
            singleExpand: true,
            useArrows: true,
            loader: new Ext.tree.TreeLoader(),
            root: this.rootNode
        });
        this.treePanel.on("expandnode", this.expandNode, this);
        this.treePanel.on("expandnode", this.selectTable, this);
        this.treePanel.on("click", this.selectTable, this);

        this.queryEl = new Ext.form.TextArea({ height: 100, width: "100%", html: "limit 100" });

        this.resultsEl = new Ext.Panel({
            title: "Results",
            region: "center",
            layout: "fit",
            frame: true,
            border: true,
            margins: "10 0 0 0"
        });

        var dataPanel = new Ext.Panel({
            margins: "5 5 5 5",
            layout: "border",
            region: "center",
            border: false,
            items:[
                {
                    title: "Query",
                    region: "north",
                    collapsible: true,
                    items: [this.queryEl],
                    bbar:[
                        { text: "Show Results", handler: this.queryHtml, scope: this }, '-',
                        { text: "Export to CSV", handler: this.queryCsv, scope: this }, '-',
                        { text: "Export to TSV", handler: this.queryTsv, scope: this }
                    ]
                },
                this.resultsEl
            ]
        });

        this.mainPanel = new Ext.Panel({
            layout: "border",
            border: false,
            items:[ this.treePanel, dataPanel ]
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
            failure: function(o) {
                org.systemsbiology.addama.js.Message.error("Datasources", "Error Loading: " + o.statusText);                
            },
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
    },

    selectTable: function(node) {
        if (node.attributes.isTable) {
            this.selectedTable = node;
            org.systemsbiology.addama.js.Message.show("Datasources", "Table Selected: " + node.attributes.text);
        }
    },

    queryHtml: function() {
        if (!this.isReadyToQuery()) {
            return;
        }

        var tableUri = this.selectedTable.id;
        var params = { tqx: "out:json_array" };
        var querySql = this.queryEl.getRawValue();
        if (querySql) {
            params["tq"] = querySql;
        }

        Ext.Ajax.request({
            url: tableUri + "/query",
            method: "GET",
            params: params,
            success: function(o) {
                var data = Ext.util.JSON.decode(o.responseText);
                this.loadDataGrid(data);
            },
            failure: function(o) {
                org.systemsbiology.addama.js.Message.error("Datasources", "Error: " + o.statusText);
            },
            scope: this
        });
    },

    queryCsv: function() {
        if (!this.isReadyToQuery()) {
            return;
        }

        var tableUri = this.selectedTable.id;
        var querySql = this.queryEl.getRawValue();
        if (querySql) {
            document.location = tableUri + "/query?tq=" + querySql + "&tqx=reqId:123;out:csv;outFileName:results.csv";
        } else {
            document.location = tableUri + "/query?tqx=reqId:123;out:csv;outFileName:results.csv";
        }
    },

    queryTsv: function() {
        if (!this.isReadyToQuery()) {
            return;
        }

        var tableUri = this.selectedTable.id;
        var querySql = this.queryEl.getRawValue();
        if (querySql) {
            document.location = tableUri + "/query?tq=" + querySql + "&tqx=reqId:123;out:tsv-excel;outFileName:results.tsv";
        } else {
            document.location = tableUri + "/query?tqx=reqId:123;out:tsv-excel;outFileName:results.tsv";
        }
    },

    isReadyToQuery: function() {
        if (!this.selectedTable) {
            org.systemsbiology.addama.js.Message.error("Datasources", "No table selected for query");
            return false;
        }
        
        return true;
    },

    loadDataGrid: function(data) {
        if (data && data.length) {
            org.systemsbiology.addama.js.Message.show("Datasources", "Retrieved " + data.length + " Records");

            var fields = [];
            var columns = [];
            Ext.each(Object.keys(data[0]), function(key) {
                fields.push(key);
                // TODO : Insert data type
                columns.push({ id: key, header: key, dataIndex: key, sortable: true, width: 100 });
            });

            var store = new Ext.data.JsonStore({
                storeId : 'gridResults', autoDestroy: true, root : 'results', fields: fields
            });
            store.loadData({ results: data });

            if (this.selectedTable && this.selectedTable.text) {
                this.resultsEl.setTitle("Results from query on " + this.selectedTable.text);
            }

            var grid = new Ext.grid.GridPanel({
                store: store,
                colModel: new Ext.grid.ColumnModel({
                    defaults: { width: 200, sortable: true },
                    columns: columns
                }),
                stripeRows: true,
                iconCls: 'icon-grid'
            });

            this.resultsEl.removeAll(true);
            this.resultsEl.add(grid);
            this.resultsEl.doLayout();
        } else {
            org.systemsbiology.addama.js.Message.error("Datasources", "No Data Loaded");
        }
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
