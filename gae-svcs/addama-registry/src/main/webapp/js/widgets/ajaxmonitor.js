Ext.ns("org.systemsbiology.addama.js");

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
                { name: "uri" },
                { name: "sentAt", type: "date" },
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
                { header: "URI", width: 200, dataIndex: 'uri', sortable: true },
                { header: "Date", width: 100, dataIndex: 'sentAt', type: "date", sortable: true, renderer: Ext.util.Format.dateRenderer() },
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
            uri: options.url,
            sentAt: new Date(),
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
