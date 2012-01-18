MetadataLinksGrid = Ext.extend(Object, {

    constructor: function(config) {
        if (!config || !config.contentEl || !config.url) {
            return;
        }

        var contentEl = config.contentEl;
        var url = config.url;
        var linksPath = config.linksPath ? config.linksPath : "";

        var loader = new TsvOutputDataLoader();
        if (config.useQuery) {
            url += "?tqx=out:json_array";
            loader = new JsonArrayDataLoader();
        }

        var prepLinksFn = function() {
            var links = document.getElementsByClassName("metadata-download-link");
            if (links && links.length) {
                for (var i = 0; i < links.length; i++) {
                    var link = links[i];
                    link.href = linksPath + link.innerHTML;
                    if (config.onDownloadLink) {
                        link.onclick = config.onDownloadLink;
                    }
                }
            }
        };

        Ext.Ajax.request({
            url: url,
            method: "GET",
            success: function(o) {
                loader.load(o.responseText);

                new Ext.grid.GridPanel({
                    store: new Ext.data.ArrayStore({ fields: loader.fields, data: loader.data, id: "gridstore-" + config.useQuery }),
                    columns: loader.fields,
                    stripeRows: true,
                    columnLines: true,
                    autoExpandColumn: "downloads",
                    frame:true,
                    autoWidth: true,
                    autoHeight: true,
                    collapsible: false,
                    animCollapse: false,
                    renderTo: contentEl,
                    listeners: {
                        viewready: prepLinksFn
                    }
                });
            }
        });
    }
});

CellFormatter = Ext.extend(Object, {
    getValue: function(header, cell) {
        if (header.isDownload) {
            return this.getLinkValue(cell);
        } else {
            return cell;
        }
    },

    getLinkValue: function(cell) {
        if (cell.indexOf(",") > 0) {
            var html = "";
            var files = cell.split(",");
            for (var f = 0; f < files.length; f++) {
                html += '<li><a class="metadata-download-link" href="#">' + files[f] + '</a></li>'
            }
            return "<ul>" + html + "</ul>";
        }
        return '<a class="metadata-download-link" href="#">' + cell + '</a>';
    }
});

JsonArrayDataLoader = Ext.extend(Object, {
    fields: [],
    data: [],
    cellFormatter: new CellFormatter(),

    load: function(response) {
        var jsonArray = Ext.util.JSON.decode(response);
        if (jsonArray && jsonArray.length) {
            this.setColumnHeaders(jsonArray[0]);
            for (var i = 0; i < jsonArray.length; i++) {
                this.addRow(jsonArray[i]);
            }
        }
    },

    setColumnHeaders: function(firstItem) {
        for (var key in firstItem) {
            if (firstItem.hasOwnProperty(key)) {
                var headerId = key.replace(" ", "_").toLowerCase();
                var isDownload = (headerId == "downloads");
                this.fields[this.fields.length] = {
                    id: headerId,
                    name: headerId,
                    dataIndex: headerId,
                    header: key,
                    sortable: !isDownload,
                    isDownload: isDownload
                };
            }
        }
    },

    addRow: function(item) {
        var rowData = [];
        for (var i = 0; i < this.fields.length; i++) {
            var f = this.fields[i];
            rowData[rowData.length] = this.cellFormatter.getValue(f, item[f.header]);
        }
        this.data[this.data.length] = rowData;
    }
});

TsvOutputDataLoader = Ext.extend(Object, {
    fields: [],
    data: [],
    cellFormatter: new CellFormatter(),

    load: function(response) {
        if (response.indexOf("\n") > 0) {
            var rows = response.split("\n");
            if (rows && rows.length) {
                this.setColumnHeaders(rows[0]);

                for (var r = 1; r < rows.length; r++) {
                    var row = rows[r];
                    if (row.indexOf("\t") > 0) {
                        this.addRow(row.split("\t"));
                    }
                }
            }
        }
    },

    setColumnHeaders: function(firstRow) {
        if (firstRow.indexOf("\t") > 0) {
            var columnHeaders = firstRow.split("\t");
            for (var i = 0; i < columnHeaders.length; i++) {
                var columnHeader = columnHeaders[i];
                var headerId = columnHeader.replace(" ", "_").toLowerCase();
                var isDownload = (headerId == "downloads");
                this.fields[this.fields.length] = {
                    id: headerId,
                    name: headerId,
                    dataIndex: headerId,
                    header: columnHeader,
                    sortable: !isDownload,
                    isDownload: isDownload
                };
            }
        }
    },

    addRow: function(cells) {
        var rowData = [];
        for (var i = 0; i < this.fields.length; i++) {
            rowData[rowData.length] = this.cellFormatter.getValue(this.fields[i], cells[i]);
        }
        this.data[this.data.length] = rowData;
    }
});