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

        this.rangeEl = new Ext.slider.MultiSlider({
            frame:true, disabled: true, border: true,
            values: [0, 100], minValue: 0, maxValue: 100
        });
        this.rangeEl.on("dragend", this.selectStartEnd, this);

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

        this.retrieveFeaturesBtn = new Ext.Button({ text: "Retrieve Features", handler: this.queryFeatures, scope: this });
        this.retrieveGenesBtn = new Ext.Button({ text: "Lookup Genes", handler: this.queryGenes, scope: this });

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
            items: [ this.chromosomeSelector, this.rangeEl, this.chromosomeLocation ],
            bbar:[ this.retrieveFeaturesBtn, '-', this.retrieveGenesBtn ]
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
        if (chromosomes && chromosomes.length) {
            chromosomes[0].isFirst = true;
            Ext.each(chromosomes, this.addChromosomeRadio, this);

            this.mainPanel.doLayout();

            var firstRadio = this.chromosomeSelector.items.first();
            if (firstRadio) {
                this.selectChromosome(firstRadio, true);
            }

            this.mainPanel.doLayout();
        }
    },

    addChromosomeRadio: function(chromosome) {
        this.chromosomeSelector.add(new Ext.form.Radio({
            name: "chr_radios",
            value: chromosome.id,
            checked: chromosome.isFirst,
            boxLabel: chromosome.label,
            uri: chromosome.uri,
            listeners: {
                "check": this.selectChromosome
            },
            scope: this
        }));
    },

    showChromosome: function(chromosome) {
        this.chromosomeLocation.chr = chromosome.chromosome;
        this.selectedChrUri = chromosome.uri;
        this.chromosomeLocation.doRendering();

        var midPoint = chromosome.start + (chromosome.length/2);
        var segment = (chromosome.length/50);
        this.rangeEl.setMinValue(chromosome.start);
        this.rangeEl.setMaxValue(chromosome.end);
        this.rangeEl.setValue(0, midPoint - segment, true);
        this.rangeEl.setValue(1, midPoint + segment, true);
        this.rangeEl.enable();
    },

    selectBuild: function(node) {
        if (node.attributes.isBuild) {
            this.selectedBuildUri = node.attributes.uri;

            Ext.Ajax.request({
                url: this.selectedBuildUri,
                method: "GET",
                success: function(o) {
                    var json = Ext.util.JSON.decode(o.responseText);
                    if (json && json.items) {
                        this.showChromosomes(json.items);
                    }
                },
                failure: function(o) {
                    org.systemsbiology.addama.js.Message.error("Chromosomes", "Error:" + o.responseText);
                },
                scope: this
            })
        }
    },

    selectChromosome: function(radio, checked) {
        console.log("selectChromosome(" + radio + "," + checked + ")");
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
                    org.systemsbiology.addama.js.Message.error("Chromosomes", "Error " + radio.uri + ":"  + o.responseText);
                },
                scope: this
            });
        }
    },

    selectStartEnd: function(slider) {
        this.chromosomeLocation.start = slider.getValues()[0];
        this.chromosomeLocation.end = slider.getValues()[1];
        this.chromosomeLocation.doRendering();
        this.mainPanel.doLayout();
    },

    queryFeatures: function() {
        if (this.isReadyToQuery()) {
            var url = this.selectedBuildUri + "/" + this.chromosomeLocation.chr + "/" + this.chromosomeLocation.start + "/" + this.chromosomeLocation.end;
            Ext.Ajax.request({
                url: url,
                method: "GET",
                success: function(o) {
                    var json = Ext.util.JSON.decode(o.responseText);
                    if (json && json.items) {
                        this.loadFeatureData(json.items);
                    }
                },
                failure: function(o) {
                    org.systemsbiology.addama.js.Message.error("Chromosomes", "Error Loading: " + o.statusText);
                },
                scope: this
            })
        }
    },

    queryGenes: function() {
        if (this.isReadyToQuery()) {
            var url = this.selectedBuildUri + "/genes/" + this.chromosomeLocation.chr + "/" + this.chromosomeLocation.start + "/" + this.chromosomeLocation.end;
            Ext.Ajax.request({
                url: url,
                method: "GET",
                success: function(o) {
                    var json = Ext.util.JSON.decode(o.responseText);
                    if (json && json.data) {
                        this.loadGeneData(json.data);
                    }
                },
                failure: function(o) {
                    org.systemsbiology.addama.js.Message.error("Chromosomes", "Error Loading: " + o.statusText);
                },
                scope: this
            })
        }
    },

    isReadyToQuery: function() {
        if (!this.selectedBuildUri) {
            org.systemsbiology.addama.js.Message.error("Chromosomes", "Select a Chromosome Datasource to Query");
            return;
        }

        if (!this.chromosomeLocation.chr) {
            org.systemsbiology.addama.js.Message.error("Chromosomes", "Select a Chromosome");
            return;
        }

        if (this.chromosomeLocation.start === undefined || this.chromosomeLocation.end === undefined) {
            org.systemsbiology.addama.js.Message.error("Chromosomes", "Select a Range to Query");
            return;
        }

        if (this.chromosomeLocation.start == "start" || this.chromosomeLocation.end == "end") {
            org.systemsbiology.addama.js.Message.error("Chromosomes", "Select a Range to Query");
            return;
        }

        return true;
    },

    loadGeneData: function(data) {
        if (data && data.length) {
            var arrayData = [];
            Ext.each(data.sort(), function(item) {
               arrayData.push(item + "<br/>");
            });

            this.resultsEl.removeAll(true);
            this.resultsEl.add({ html : arrayData, frame: false });
            this.resultsEl.doLayout();
        } else {
            org.systemsbiology.addama.js.Message.error("Chromosomes", "No Gene Data Loaded");
        }
    },

    loadFeatureData: function(data) {
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
            org.systemsbiology.addama.js.Message.error("Chromosomes", "No Feature Data Loaded");
        }
    }
});
