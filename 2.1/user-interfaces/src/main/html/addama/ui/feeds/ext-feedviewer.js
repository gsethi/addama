FeedGrid = function(uri, config) {
    if (!config) {
        config = {};
    }
    
    Ext.apply(this, config);

    this.store = new Ext.data.Store({
        proxy: new Ext.data.HttpProxy({
            url: uri,
            method: 'get'
        }),

        reader: new Ext.data.XmlReader(
            {record: 'item'},
            ['title', 'author', {name:'pubDate', type:'date'}, 'link', 'description', 'content']
        )
    });
    this.store.setDefaultSort('pubDate', "DESC");

    if (config.columns && config.columns.length) {
        this.columns = config.columns;
    } else {
        this.columns = [
            {
                id: 'title',
                header: "Title",
                dataIndex: 'title',
                sortable:true
            }, {
                id: 'last',
                header: "Date",
                dataIndex: 'pubDate',
                sortable:true
            }, {
                id: 'author',
                header: "Author",
                dataIndex: 'author',
                sortable:true
            }
        ];        
    }

    var feedGridId = config.feedGridId;
    if (!feedGridId) {
        feedGridId = "topic-grid";
    }

    FeedGrid.superclass.constructor.call(this, {
        region: 'center',
        id: feedGridId,
        autoHeight: true,
        loadMask: {
            msg:'Loading Feed...'
        },
        viewConfig: {
            forceFit:true,
            enableRowBody:true,
            showPreview:true
        }
    });

    this.store.load();
};

Ext.extend(FeedGrid, Ext.grid.GridPanel, {});

