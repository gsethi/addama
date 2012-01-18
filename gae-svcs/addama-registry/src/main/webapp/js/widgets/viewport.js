Ext.ns("org.systemsbiology.addama.js");

org.systemsbiology.addama.js.Viewport = Ext.extend(Object, {
    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.Viewport.superclass.constructor.call(this);

        this.drawViewport();
    },

    drawViewport: function() {
        var addamaBanner = Ext.DomHelper.append(Ext.getBody(), '<div><a href="http://addama.org" target="_blank"><img src="/images/banner.png" alt="Addama"/></a></div>', true);
        var appengineLogo = Ext.DomHelper.append(Ext.getBody(), '<div><a href="http://code.google.com/appengine"><img src="https://code.google.com/appengine/images/appengine-silver-120x30.gif" alt="Powered by Google App Engine"/></a></div>', true);
        var link1 = Ext.DomHelper.append(Ext.getBody(), '<div><a href="/addama/apikeys/file">API Keys</a></div>', true);
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
                var ajaxMonitor = new org.systemsbiology.addama.js.AjaxMonitor();
                this.tabs.push(ajaxMonitor.gridPanel);
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

        new Ext.Viewport({
            layout:'border',
            items:[ headerPanel, tabPanel, footerPanel ]
        });
    }
});

org.systemsbiology.addama.js.AjaxMonitor = Ext.extend(Object, {
    SEQUENCE_ID: 1,
    GRID_DATA: [],

    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.AjaxMonitor.superclass.constructor.call(this);

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

org.systemsbiology.addama.js.AppsPanel = Ext.extend(Object, {
    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.AppsPanel.superclass.constructor.call(this);

        Ext.Ajax.request({
            url: "/addama/apps",
            method: "GET",
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json && json.items) {
                    var html = "";
                    Ext.each(json.items, function(item) {
                        html += "<li><a href='" + item.uri + "' target='_blank'>" + item.label + "</a></li>";
                    });
                    Ext.DomHelper(this.contentEl, "<ul>" + html + "</ul>");
                }
            },
            scope: this
        });

    }
});

org.systemsbiology.addama.js.ServicesPanel = Ext.extend(Object, {
    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.ServicesPanel.superclass.constructor.call(this);

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
                    Ext.DomHelper(this.contentEl, "<ul>" + html + "</ul>");
                }
            },
            scope: this
        });

    }
});
