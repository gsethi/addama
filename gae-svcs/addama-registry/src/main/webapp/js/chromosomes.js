Ext.ns("org.systemsbiology.addama.js.widgets.chromosomes");

org.systemsbiology.addama.js.widgets.chromosomes.View = Ext.extend(Object, {
    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.widgets.chromosomes.View.superclass.constructor.call(this);

        this.uriBuilder = new org.systemsbiology.addama.js.widgets.chromosomes.UriBuilder();
        this.uriBuilder.on("change", function() {
            this.chromosomeLocation.removeAll(true);
            this.chromosomeLocation.add({html: this.uriBuilder.asHtml()});
            this.mainPanel.doLayout();
        }, this);

        this.loadPanels();
        this.loadBuilds();
    },

    loadPanels: function() {
        this.rootNode = new Ext.tree.AsyncTreeNode();

        var treePanel = new Ext.tree.TreePanel({
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
        treePanel.on("click", this.selectBuild, this);

        // TODO : Make scrollable
        this.chromosomeSelector = new Ext.Toolbar({
            enableOverflow: true,
            border: true, frame: true, autoScroll: true, margins: "10 10 10 10"
        });

        this.rangeEl = new Ext.slider.MultiSlider({
            frame:true, disabled: true, border: true,
            values: [0, 100], minValue: 0, maxValue: 100
        });
        this.rangeEl.on("dragend", function(slider) {
            this.uriBuilder.rangeSelected(slider.getValues()[0], slider.getValues()[1]);
        }, this);

        this.chromosomeLocation = new Ext.Panel({ border: true, frame: true, height: 60, items: [] });

        this.retrieveFeaturesBtn = new Ext.Button({ text: "Retrieve Features", handler: this.queryFeatures, scope: this });
        this.retrieveGenesBtn = new Ext.Button({ text: "Lookup Genes", handler: this.queryGenes, scope: this });

        this.resultsPanel = new Ext.Panel({
            title: "Results",
            region: "center",
            layout: "fit",
            frame: true,
            border: true,
            margins: "10 0 0 0"
        });

        var rangeSelectionPanel = new Ext.Panel({
            title: "Chromosome Range Selection",
            region: "north",
            collapsible: true,
            items: [ this.rangeEl, this.chromosomeLocation ],
            tbar: this.chromosomeSelector,
            bbar:[ this.retrieveFeaturesBtn, '-', this.retrieveGenesBtn ]
        });

        var dataPanel = new Ext.Panel({
            margins: "5 5 5 5",
            layout: "border",
            region: "center",
            border: false,
            items:[ rangeSelectionPanel, this.resultsPanel ]
        });

        this.mainPanel = new Ext.Panel({
            layout: "border",
            border: false,
            items:[ treePanel, dataPanel ]
        });
    },

    loadBuilds: function() {
        Ext.Ajax.request({
            url: "/addama/chromosomes",
            method: "GET",
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json) this.displayBuilds(json.items);
            },
            failure: function(o) {
                org.systemsbiology.addama.js.Message.error("Chromosomes", "Error Loading: " + o.statusText);
            },
            scope: this
        });
    },

    displayBuilds: function(items) {
        if (items) {
            Ext.each(items, function(item) {
                item.isBuild = true;
                item.text = item.label ? item.label : item.name;
                item.path = item.uri;
                item.leaf = true;
                item.cls = "file";
                this.rootNode.appendChild(item);
            }, this);
        }
    },

    selectBuild: function(node) {
        if (node.attributes.isBuild) {
            Ext.Ajax.request({
                url: node.attributes.uri,
                method: "GET",
                success: function(o) {
                    var json = Ext.util.JSON.decode(o.responseText);
                    if (json) {
                        this.uriBuilder.buildUri = node.attributes.uri;
                        this.displayBuildItems(json.items);
                    }
                },
                failure: function(o) {
                    org.systemsbiology.addama.js.Message.error("Chromosomes", "Error:" + o.responseText);
                },
                scope: this
            })
        }
    },

    displayBuildItems: function(items) {
        if (items) {
            Ext.each(items, this.addToolbarButton, this);
            this.mainPanel.doLayout();
        }
    },

    addToolbarButton: function(chromosome) {
        this.chromosomeSelector.add({
            text: chromosome.label,
            handler: this.selectChromosome,
            scope: this,
            uri: chromosome.uri
        });
        this.chromosomeSelector.add('-');
    },

    selectChromosome: function(btn) {
        if (btn && btn.uri) {
            Ext.Ajax.request({
                url: btn.uri,
                method: "GET",
                success: function(o) {
                    var json = Ext.util.JSON.decode(o.responseText);
                    if (json && json.start != null && json.length != null && json.end != null) {
                        var midPoint = json.start + (json.length/2);
                        var segment = (json.length/50);
                        this.rangeEl.setMinValue(json.start);
                        this.rangeEl.setMaxValue(json.end);
                        this.rangeEl.setValue(0, midPoint - segment, true);
                        this.rangeEl.setValue(1, midPoint + segment, true);
                        this.rangeEl.enable();

                        this.uriBuilder.chromosomeSelected(json);
                    }
                },
                failure: function(o) {
                    org.systemsbiology.addama.js.Message.error("Chromosomes", "Error " + o.responseText);
                },
                scope: this
            });
        }
    },

    queryFeatures: function() {
        if (this.uriBuilder.isReady()) {
            Ext.Ajax.request({
                url: this.uriBuilder.featuresQueryURI(),
                method: "GET",
                success: function(o) {
                    var json = Ext.util.JSON.decode(o.responseText);
                    if (json) this.loadFeatureData(json.items);
                },
                failure: function(o) {
                    org.systemsbiology.addama.js.Message.error("Chromosomes", "Query Features: " + o.statusText);
                },
                scope: this
            })
        } else {
            org.systemsbiology.addama.js.Message.error("Chromosomes", "Incomplete Chromosome Features Lookup : " + this.uriBuilder.asHtml());
        }
    },

    queryGenes: function() {
        if (this.uriBuilder.isReady()) {
            Ext.Ajax.request({
                url: this.uriBuilder.genesQueryURI(),
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
        } else {
            org.systemsbiology.addama.js.Message.error("Chromosomes", "Incomplete Chromosome Genes Lookup : " + this.uriBuilder.asHtml());
        }
    },

    drawResults: function(dataPanel) {
        this.resultsPanel.removeAll(true);
        this.resultsPanel.add(dataPanel);
        this.resultsPanel.doLayout();
    },

    loadGeneData: function(data) {
        if (data && data.length) {
            var arrayData = [];
            Ext.each(data.sort(), function(item) {
               arrayData.push(item + "<br/>");
            });

            this.drawResults({ html : arrayData, frame: false });
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

            var store = new Ext.data.JsonStore({ autoDestroy: true, root : 'results', fields: fields });
            store.loadData({ results: data });

            this.drawResults(new Ext.grid.GridPanel({
                store: store, stripeRows: true, iconCls: 'icon-grid',
                colModel: new Ext.grid.ColumnModel({ defaults: { width: 200, sortable: true }, columns: columns })
            }));
        } else {
            org.systemsbiology.addama.js.Message.error("Chromosomes", "No Feature Data Loaded");
        }
    }
});

org.systemsbiology.addama.js.widgets.chromosomes.UriBuilder = Ext.extend(Ext.util.Observable, {
    buildUri: null,
    chromosomeUri: null,
    chromosome: null,
    start: null,
    end: null,
    strand: null,

    constructor: function(config) {
        this.addEvents('change');

        Ext.apply(this, config);

        org.systemsbiology.addama.js.widgets.chromosomes.UriBuilder.superclass.constructor.call(this);
    },

    isReady: function() {
        if (!this.buildUri) return false;
        if (!this.chromosome) return false;
        return (this.start != null && this.end != null);
    },

    hasRange: function() {
        if (this.start === undefined) return false;
        if (this.end === undefined) return false;
        return true;
    },

    chromosomeSelected: function(json) {
        console.log("chromosomeSelected(" + json + ")");
        if (json) {
            console.log("chromosomeSelected(" + json.chromosome + "," + json.uri + ")");
            this.chromosome = json.chromosome;
            this.chromosomeUri = json.uri;
            this.fireEvent("change");
        }
    },

    rangeSelected: function(start, end) {
        this.start = start;
        this.end = end;
        this.fireEvent("change");
    },

    featuresQueryURI: function() {
        if (this.isReady()) {
            var uri = this.buildUri + "/" + this.chromosome + "/" + this.start + "/" + this.end;
            if (this.strand) return uri + "/" + this.strand;
            return uri;
        }
        return null;
    },

    genesQueryURI: function() {
        if (this.isReady()) {
            var uri = this.buildUri + "/genes/" + this.chromosome + "/" + this.start + "/" + this.end;
            if (this.strand) return uri + "/" + this.strand;
            return uri;
        }
        return null;
    },

    asHtml: function() {
        var tempchr = "chr";
        if (this.chromosome) tempchr = this.chromosome;
        var tempstart = "start";
        if (this.start) tempstart = this.start;
        var tempend = "end";
        if (this.end) tempend = this.end;

        var html = "";
        html += "<ul class='horizontal-list'>";
        html += "<li>Chromosome [" + tempchr + "]</li>";
        html += "<li>Start [" + tempstart + "]</li>";
        html += "<li>End [" + tempend + "]</li>";
        if (this.strand) {
            html += "<li>Strand [" + this.strand + "]</li>";
        }
        html += "</ul>";
        return html;
    }
});