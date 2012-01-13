Ext.ns("org.systemsbiology.addama.js");

org.systemsbiology.addama.js.AjaxMonitor = Ext.extend(Object, {
    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.AjaxMonitor.superclass.constructor.call(this);

        this.createGrid();
        Ext.Ajax.on('requestcomplete', this.addResponseToGrid, this);
    },

    createGrid: function() {
        this.gridData = [];

        var gridColumns = [
            { header: "Sequence", width: 25, dataIndex: 'sequence', type: "int", sortable: true },
            { header: "URI", width: 25, dataIndex: 'uri', sortable: true },
            { header: "Date", width: 25, dataIndex: 'sentAt', type: "date", sortable: true, renderer: Ext.util.Format.dateRenderer() },
            { header: "Status Code", width: 25, dataIndex: 'statusCode', sortable: true },
            { header: "Status Text", width: 25, dataIndex: 'statusText' }
        ];
        var storeColumns = [
            { name: "sequence", type: "int"},
            { name: "uri" },
            { name: "sentAt", type: "date" },
            { name: "statusCode" },
            { name: "statusText" },
            { name: "responseText" }
        ];

        this.store = new Ext.data.ArrayStore({
            storeId: "store-ajax-monitor",
            reader: new Ext.data.ArrayReader({}, storeColumns),
            data: this.gridData,
            sortInfo: {field: 'sequence', direction: "DESC"}
        });

        this.grid = new Ext.grid.GridPanel({
            store: this.store,
            columns: gridColumns,
            stripeRows: true,
            columnLines: true,
            frame:true,
            title: "Requests",
            collapsible: false,
            animCollapse: false,
            iconCls: 'icon-grid',
            contentEl: this.contentEl,
            listeners: {
                rowclick: function(g, rowIndex, e) {
                    console.log("rowclick=" + rowIndex);
                }
            }
        });

    },

    addResponseToGrid: function(connection, response) {
         this.gridData.push([ this.incrementedId(), connection.url, new Date(), response.status, response.statusText, response.responseText ]);
    },

    incrementedId: function() {
        if (!this.sequenceId) {
            this.sequenceId = 1;
        } else {
            this.sequenceId++;
        }
        return this.sequenceId;
    }

});
