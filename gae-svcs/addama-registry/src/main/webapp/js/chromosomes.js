org.systemsbiology.addama.js.widgets.ChromosomesView = Ext.extend(Object, {
    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.widgets.ChromosomesView.superclass.constructor.call(this);

        this.loadTree();
        this.loadChromosomes();
    },

    loadTree: function() {
        this.rootNode = new Ext.tree.AsyncTreeNode();

        this.treePanel = new Ext.tree.TreePanel({
            title: "Chromosomes",
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
        this.treePanel.on("click", this.selectBuild, this);

        this.chromosomeSelector = new Ext.Panel({
            layout: "hbox",
            height: 33,
            border: false,
            frame: true,
            padding: "1 1 1 1",
            margins: "1 1 1 1",
            layoutConfig: { pack: "center", align: "middle", defaultMargins: "0 10 0 0" }
        });

        this.chromosomeLocation = new Ext.Panel({
            chr: "chr",
            start: "start",
            end: "end",
            strand: "",
            border: true,
            frame: true,
            height: 33,
            padding: "1 1 1 1",
            margins: "1 1 1 1",
            items: [],
            doRendering: function() {
                this.removeAll(true);
                this.add({html:"/" + this.chr + "/" + this.start + "/" + this.end + "/" + this.strand });
            }
        });


        this.resultsEl = new Ext.Panel({
            title: "Results",
            region: "center",
            layout: "fit",
            frame: true,
            border: true,
            margins: "10 0 0 0"
        });

        this.chromosomeRangeSelectionEl = new Ext.Panel({
            title: "Chromosome Range Selection",
            region: "north",
            collapsible: true,
            items: [
                this.chromosomeSelector,
                this.chromosomeLocation
            ],
            bbar:[
                { text: "Retrieve Features", handler: this.queryFeatures, scope: this }, '-',
                { text: "Lookup Genes", handler: this.queryGenes, scope: this }
            ]
        });

        var dataPanel = new Ext.Panel({
            margins: "5 5 5 5",
            layout: "border",
            region: "center",
            border: false,
            items:[
                this.chromosomeRangeSelectionEl, this.resultsEl
            ]
        });

        this.mainPanel = new Ext.Panel({
            layout: "border",
            border: false,
            items:[ this.treePanel, dataPanel ]
        });
    },

    selectBuild: function(node) {
        if (node.isBuild) {
            Ext.Ajax.request({
                uri: node.attributes.uri,
                method: "GET",
                success: function(o) {
                    var json = Ext.util.JSON.decode(o.responseText);
                    if (json && json.items) {
                        this.showChromosomes(json.items);
                    }
                },
                failure: function(o) {
                    org.systemsbiology.addama.js.Message.error("Chromosomes", o.responseText);
                },
                scope: this
            })
        }
    },

    selectChromosome: function(radio, checked) {
        if (checked) {
            Ext.Ajax.request({
                url: radio.uri,
                method: "GET",
                success: function(o) {
                    var json = Ext.util.JSON.decode(o.responseText);
                    if (json) {
                        this.showChromosome(json);
                    }
                },
                failure: function(o) {
                    org.systemsbiology.addama.js.Message.error("Chromosomes", o.responseText);
                },
                scope: this
            });
        }
    },

    showBuilds: function(builds) {
        Ext.each(builds, function(item) {
            item.isBuild = true;
            item.text = item.label ? item.label : item.name;
            item.path = item.uri;
            item.leaf = true;
            item.cls = "file";
            this.rootNode.appendChild(item);
        }, this);
    },

    showChromosomes: function(chromosomes) {
        Ext.each(chromosomes, function(chromosome) {
            var chrRadio = new Ext.form.Radio({ name: "chr_radios", value: chromosome.id, fieldLabel: chromosome.label, uri: chromosome.uri });
            chrRadio.on("check", this.selectChromosome, this);
            this.chromosomeSelector.add(chrRadio);
        }, this);

        var firstRadio = this.chromosomeSelector.items[0];
        if (firstRadio) {
            firstRadio.checked = true;
            this.selectChromosome(firstRadio, true);
        }

        this.mainPanel.doLayout();
    },

    showChromosome: function(chromosome) {
        this.chromosomeLocation.chr = chromosome.chromosome;
        this.selectedChrUri = chromosome.uri;
        this.chromosomeLocation.doRendering();

        this.rangeEl = new Ext.Slider({
            width: 1000,
            frame:true,
            values: [chromosome.start, chromosome.end],
            minValue: chromosome.start,
            maxValue: chromosome.end,
        });
        this.rangeEl.on("dragend", this.selectStartEnd, this);

        this.chromosomeRangeSelectionEl.add(this.rangeEl);
        this.mainPanel.doLayout();
    },

    selectStartEnd: function(slider) {
        this.chromosomeLocation.start = slider.getValues()[0];
        this.chromosomeLocation.end = slider.getValues()[1];
        this.chromosomeLocation.doRendering();
        this.mainPanel.doLayout();
    },

    loadChromosomes: function() {
        Ext.Ajax.request({
            url: "/addama/chromosomes",
            method: "GET",
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json && json.items) {
                    this.showBuilds(json.items);
                }
            },
            failure: function(o) {
                org.systemsbiology.addama.js.Message.error("Chromosomes", "Error Loading: " + o.statusText);
            },
            scope: this
        });
    },

    selectNode: function(node) {
        if (node.attributes.isTable) {
            this.selectedTable = node;
            org.systemsbiology.addama.js.Message.show("Chromosomes", "Chromosomes Selected: " + node.attributes.text);
        }
    },

    queryFeatures: function() {
        if (this.isReadyToQuery()) {
            org.systemsbiology.addama.js.Message.show("Chromosomes", "Query Features");
        }
    },

    queryGenes: function() {
        if (!this.isReadyToQuery()) {
            org.systemsbiology.addama.js.Message.show("Chromosomes", "Query Genes");
        }
    },

    isReadyToQuery: function() {
        org.systemsbiology.addama.js.Message.error("Chromosomes", "Not Ready to Query");
        return true;
    },

    loadDataGrid: function(data) {
        if (data && data.length) {
            org.systemsbiology.addama.js.Message.show("Chromosomes", "Retrieved " + data.length + " Records");

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
            org.systemsbiology.addama.js.Message.error("Chromosomes", "No Data Loaded");
        }
    }
});

