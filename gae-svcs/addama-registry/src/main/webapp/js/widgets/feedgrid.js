Ext.ns("org.systemsbiology.addama.js");

org.systemsbiology.addama.js.FeedGrid = Ext.extend(Object, {

    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.FeedGrid.superclass.constructor.call(this);

        var store = new Ext.data.Store({
            proxy: new Ext.data.HttpProxy({ url: this.url, method: 'get' }),
            sortInfo: { field: "pubDate", direction: "DESC" },
            reader: new Ext.data.XmlReader(
                {record: 'item'},
                ['title', 'author', {name:'pubDate', type:'date'}, 'link', 'description', 'content']
            )
        });

        this.gridPanel = new Ext.grid.GridPanel({
            store: store,
            autoHeight: true,
            loadMask: { msg:'Loading Feed...' },
            viewConfig: { forceFit:true, enableRowBody:true, showPreview:true },
            columns: [
                { id: 'title', header: "Title", dataIndex: 'title', sortable:true },
                { id: 'last', header: "Date", dataIndex: 'pubDate', sortable:true },
                { id: 'author', header: "Author", dataIndex: 'author', sortable:true }
            ]
        });
        store.load();
    }
});

