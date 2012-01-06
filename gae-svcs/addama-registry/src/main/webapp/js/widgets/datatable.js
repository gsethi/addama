function createOVGBMDataTable(ovtabledata, gbmtabledata) {
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


}

function createDataTable(tableData) {
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

}