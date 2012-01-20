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
            margins: "5 5 5 5",
            padding: "5 5 5 5"
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

org.systemsbiology.addama.js.widgets.ApiKeysWindow = Ext.extend(Object, {

    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.widgets.ApiKeysWindow.superclass.constructor.call(this);

        var msgTxt = "<h4>Generated API Keys are managed by domain administrators through the App Engine Console</h4>";
        msgTxt += "<br/>";
        msgTxt += "Each user is assigned a private API key to support secure programmatic access to Addama services.";
        msgTxt += "<b>Do NOT share your API key<b>.  Treat the same as a password.";
        if (this.isAdmin) {
            msgTxt += "<br/><br/><br/>";
            msgTxt += "The 'addama.properties' file is used by Addama local services to register securely.";
            msgTxt += "<br/>";
            msgTxt += "Enter the public URL in the text field below for your web services host (e.g. https://webservices.example.com) to automatically generate";
            msgTxt += "<br/>";
            msgTxt += "<br/>";
        }

        var items = [];
        items.push({ region:"center", html: msgTxt, margins: "5 5 5 5", padding: "5 5 5 5" });

        if (this.isAdmin) {
            var fld = new Ext.form.TextField({
                name: "serviceHostUrl",
                anchor: "100%",
                labelSeparator: "",
                fieldLabel: "Web services host URL"
            });

            items.push(new Ext.form.FormPanel({
                frame:true,
                region:"south",
                margins: "5 5 5 5",
                padding: "5 5 5 5",
                width: 500,
                items: [fld],
                buttons: [
                    {
                        text: "Generate addama.properties",
                        handler: function() {
                            var serviceUrl = fld.getRawValue();
                            if (serviceUrl) {
                                document.location = "/addama/apikeys/addama.properties?serviceUrl=" + serviceUrl;
                            } else {
                                document.location = "/addama/apikeys/addama.properties";
                            }
                            win.close();
                        }
                    }
                ]
            }));
        }

        var win = new Ext.Window({
            title: "API Keys",
            closable: true,
            modal: true,
            closeAction: "hide",
            width: 600,
            minWidth: 400,
            height: 400,
            padding: "5 5 5 5",
            items: items,
            tbar: [
                {
                    text: "Download API Key File",
                    handler: function() {
                        document.location = "/addama/apikeys/file";
                        win.close();
                    }
                }
            ]
        });
        win.show();
    }
});

org.systemsbiology.addama.js.widgets.RegisterAppsWindow = Ext.extend(Object, {
    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.widgets.RegisterAppsWindow.constructor.call(this);

        this.loadAppsGrid();
        this.loadAppsForm();

        var win = new Ext.Window({
            title: "Register Applications",
            closable: true,
            modal: true,
            closeAction: "hide",
            width: 900,
            height: 400,
            padding: "5 5 5 5",
            items: [
                new Ext.TabPanel({
                    activeTab: 0,
                    margins: "5 5 5 5",
                    padding: "5 5 5 5",
                    items: [
                        this.registeredAppsPanel,
                        {
                            title: "New Application",
                            layout:"border",
                            height: 200,
                            frame: true,
                            border: false,
                            margins: "5 5 5 5",
                            padding: "5 5 5 5",
                            items:[ this.registerAppForm, this.appPreviewPanel ]
                        }
                    ]
                })
            ]
        });
        win.show();
    },

    loadAppsGrid: function() {
        this.store = new Ext.data.ArrayStore({
            fields: [
                {name: "id"},
                {name: "label"},
                {name: "url"},
                {name: "logo"},
                {name: "description"}
            ]
        });

        this.registeredAppsPanel = new Ext.grid.GridPanel({
            title: "Registered Applications",
            store: this.store,
            height: 400,
            columns: [
                { header: "ID", width: 75, sortable: true, dataIndex: "id" },
                { header: "Label", width: 120, sortable: true, dataIndex: "label" },
                { header: "URL", width: 200, sortable: true, dataIndex: "url" },
                { header: "Logo", width: 100, sortable: true, dataIndex: "logo" },
                { header: "Description", width: 300, sortable: true, dataIndex: "description" }
            ]
        });

        this.loadAppsData();
    },

    loadAppsData: function() {
        Ext.Ajax.request({
            method: "GET",
            url: "/addama/apps",
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json && json.items) {
                    var data = [];
                    Ext.each(json.items, function(item) {
                        data.push([ item.id, item.label, item.url, item.logo, item.description ]);
                    });
                    this.store.loadData(data);
                } else {
                    org.systemsbiology.addama.js.Message.show("Registered Applications", "No applications have been registered");
                }
            },
            scope: this
        });
    },

    loadAppsForm: function() {
        var fldId = new Ext.form.TextField({ name: "id", fieldLabel: "Application ID" });
        var fldLabel = new Ext.form.TextField({ name: "label", fieldLabel: "Label" });
        var fldUrl = new Ext.form.TextField({ name: "url", fieldLabel: "Root Content URL" });
        var fldLogo = new Ext.form.TextField({ name: "logo", fieldLabel: "Application Logo" });
        var fldDescription = new Ext.form.TextField({ name: "description", fieldLabel: "Description" });

        this.appPreviewPanel = new Ext.Panel({
            region: "east",
            html: org.systemsbiology.addama.js.widgets.GenerateAppPanelHtml({
                uri: "/",
                logo: "",
                label: "Label will go here",
                description:"Description will go here",
                alt:"logo"
            })
        });

        this.registerAppForm = new Ext.form.FormPanel({
            frame:true,
            region:"center",
            width: 400,
            defaults: { anchor: "100%", labelSeparator: "" },
            items: [ fldId, fldLabel, fldUrl, fldLogo, fldDescription ],
            buttons: [
                {
                    text: "Preview",
                    handler: function() {
                        Ext.DomHelper.overwrite(this.appPreviewPanel.el, org.systemsbiology.addama.js.widgets.GenerateAppPanelHtml({
                            id: fldId.getRawValue(),
                            uri: fldUrl.getRawValue(),
                            label: fldLabel.getRawValue(),
                            logo: fldLogo.getRawValue(),
                            description: fldDescription.getRawValue(),
                            alt: "not found"
                        }));
                    },
                    scope: this
                },
                {
                    text: "Save",
                    handler: function() {
                        var newLabel = fldLabel.getRawValue();
                        var app = {
                            id: fldId.getRawValue(),
                            label: newLabel,
                            url: fldUrl.getRawValue(),
                            logo: fldLogo.getRawValue(),
                            description: fldDescription.getRawValue()
                        };

                        Ext.Ajax.request({
                            method: "POST",
                            url: "/addama/apps",
                            params: {
                                app: Ext.util.JSON.encode(app)
                            },
                            success: function() {
                                org.systemsbiology.addama.js.Message.show("Register Applications", "Application '" + newLabel + "' registered successfully");
                                this.loadAppsData();
                            },
                            scope: this
                        })
                    },
                    scope: this
                }
            ]
        });
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
                        Ext.DomHelper.append(appsDiv, org.systemsbiology.addama.js.widgets.GenerateAppPanelHtml(item));
                    });
                }
            },
            scope: this
        });
    }
});

org.systemsbiology.addama.js.widgets.GenerateAppPanelHtml = function(item) {
    var logoAlt = item.alt;
    if (!logoAlt) {
        logoAlt = "app_logo";
    }

    var srcUrl = item.uri;
    if (srcUrl.substring(item.uri.length - 1) == "/") {
        srcUrl = srcUrl.substring(0, srcUrl.length -1);
    }
    srcUrl += "/";

    var logoUrl = "/images/nologo.png";
    if (item.logo) {
        var logoUri = item.logo;
        if (logoUri.substring(0, 1) == "/") {
            logoUri = logoUri.substring(1, logoUri.length);
        }
        logoUrl = srcUrl + logoUri;
    }

    var label = item.label;
    if (!label) {
        label = "Untitled";
    }

    var description = item.description;
    if (!description) {
        description = "";
    }

    var itemhtml = "";
    itemhtml += "<div class='apps'>";
    itemhtml += "<a href='" + srcUrl + "' target='_blank'>";
    itemhtml += "<img src='" + logoUrl + "' alt='" + logoAlt + "'/>";
    itemhtml += "<h3>" + label + "</h3>";
    itemhtml += "</a>";
    itemhtml += "<div class='apps_description'>" + description + "</div>";
    itemhtml += "</div>";
    return itemhtml;
};

org.systemsbiology.addama.js.widgets.ServicesPanel = Ext.extend(Object, {
    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.widgets.ServicesPanel.superclass.constructor.call(this);

        Ext.Ajax.request({
            url: "/addama/services",
            method: "GET",
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json && json.items) {
                    var html = "";
                    Ext.each(json.items, function(item) {
                        html += "<li>" + item.label + ":" + item.url + "</li>";
                    });
                    Ext.DomHelper.append(Ext.get(this.contentEl), "<ul>" + html + "</ul>");
                }
            },
            scope: this
        });

    }
});

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
});