Ext.ns("org.systemsbiology.addama.js.widgets.chromosomes");

org.systemsbiology.addama.js.widgets.chromosomes.View = Ext.extend(Object, {
    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.widgets.chromosomes.View.superclass.constructor.call(this);

        this.uriBuilder = new org.systemsbiology.addama.js.widgets.chromosomes.UriBuilder();
        this.uriBuilder.on("change", function() {
            this.resultsPanel.setTitle("Results for [" + this.uriBuilder.fullUri() + "]");
            if (this.uriBuilder.minimum != null) {
                this.rangeMinDisplayField.setRawValue("min value: " + this.uriBuilder.minimum);
            } else {
                this.rangeMinDisplayField.setRawValue("");
            }
            if (this.uriBuilder.maximum != null) {
                this.rangeMaxDisplayField.setRawValue("max value: " + this.uriBuilder.maximum);
            } else {
                this.rangeMaxDisplayField.setRawValue("");
            }
            this.mainPanel.doLayout();
        }, this);

        this.loadPanels();
        this.loadBuilds();

        this.query(Ext.History.getToken());
    },

    loadPanels: function() {
        this.buildSelector = new Ext.form.ComboBox({
            mode: "local",
            store: new Ext.data.ArrayStore({ fields:["uri","label"], id:0}),
            lazyRender:true,
            disabled: true,
            valueField: 'uri',
            displayField: 'label',
            emptyText: "Select a datasource",
            fieldLabel: "Datasources",
            listeners:{ scope: this, 'select': this.selectBuild }
        });

        this.chromosomeSelector = new Ext.form.ComboBox({
            mode: "local",
            store: new Ext.data.ArrayStore({ fields:["uri","label"], id:0}),
            lazyRender:true,
            disabled: true,
            valueField: 'label',
            displayField: 'label',
            emptyText: "Select a chromosome",
            fieldLabel: "Chromosomes",
            listeners:{ scope: this, 'select': this.selectChromosome }
        });

        this.rangeStartField = new Ext.form.NumberField();
        this.rangeEndField = new Ext.form.NumberField();
        this.rangeMinDisplayField = new Ext.form.DisplayField({disabled:true});
        this.rangeMaxDisplayField = new Ext.form.DisplayField({disabled:true});
        this.rangeStartField.on("change", this.uriBuilder.setStart, this.uriBuilder);
        this.rangeEndField.on("change", this.uriBuilder.setEnd, this.uriBuilder);

        this.retrieveFeaturesBtn = new Ext.Button({ text: "Retrieve Features", handler: this.queryFeatures, scope: this });
        this.retrieveGenesBtn = new Ext.Button({ text: "Lookup Genes", handler: this.queryGenes, scope: this });

        var rangeControls = new Ext.form.FieldSet({
            title: "Chromosome Query Controls",
            region: "north",
            defaults: {
                labelSeparator: ""
            },
            collapsible: true,
            items:[
                this.buildSelector, this.chromosomeSelector,
                new Ext.form.CompositeField({fieldLabel: "Start", items:[this.rangeStartField, this.rangeMinDisplayField]}),
                new Ext.form.CompositeField({fieldLabel: "End", items:[this.rangeEndField, this.rangeMaxDisplayField]})
            ],
            buttonAlign: "left",
            buttons: [ this.retrieveFeaturesBtn, '-', this.retrieveGenesBtn ]
        });

        this.resultsPanel = new Ext.Panel({
            title: "Results",
            region: "center",
            layout: "fit",
            frame: true,
            border: true,
            margins: "10 0 0 0"
        });

        this.mainPanel = new Ext.Panel({
            margins: "5 5 5 5",
            padding: "5 5 5 5",
            frame: true,
            border: false,
            items:[ rangeControls, this.resultsPanel ]
        });
    },

    loadBuilds: function() {
        Ext.Ajax.request({
            url: "/addama/chromosomes",
            method: "GET",
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json) this.displayItems(this.buildSelector, json.items);
            },
            failure: function(o) {
                org.systemsbiology.addama.js.Message.error("Chromosomes", "Error Loading: " + o.statusText);
            },
            scope: this
        });
    },

    displayItems: function(comboBox, items, sorterFn) {
        if (comboBox && items) {
            var data = [];
            Ext.each(items, function(item) {
                data.push([item.uri,item.label]);
            });
            if (sorterFn) {
                data.sort(sorterFn);
            } else {
                data.sort();
            }
            comboBox.store.loadData(data);
            comboBox.enable();
        }
    },

    chromosomeSorter: function() {
        return function(a, b) {
            if (a && b) {
                if (a.length >= 2 && b.length >= 2) {
                    var valA = a[1];
                    var valB = b[1];
                    if (valA && valB) {
                        var result = valA.length - valB.length;
                        if (result != 0) return result;
                        if ([valA,valB].sort()[0] == valA) return -1;
                        return 1;
                    }
                }
            }
            return 0;
        };
    },

    selectBuild: function(cmb, record) {
        Ext.Ajax.request({
            url: record.data.uri,
            method: "GET",
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json) {
                    this.uriBuilder.setBuildUri(record.data.uri);
                    this.displayItems(this.chromosomeSelector, json.items, this.chromosomeSorter());
                }
            },
            failure: function(o) {
                org.systemsbiology.addama.js.Message.error("Chromosomes", "Error: " + o.statusText);
            },
            scope: this
        })
    },

    selectChromosome: function(cmb, record) {
        Ext.Ajax.request({
            url: record.data.uri,
            method: "GET",
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json) {
                    this.uriBuilder.setChromosome(json.chromosome, json.start, json.end);
                }
            },
            failure: function(o) {
                org.systemsbiology.addama.js.Message.error("Chromosomes", "Error: " + o.statusText);
            },
            scope: this
        });
    },

    queryFeatures: function() {
        if (this.uriBuilder.isReady()) {
            this.query(this.uriBuilder.featuresQueryURI());
        } else {
            org.systemsbiology.addama.js.Message.error("Chromosomes", "Incomplete Chromosome Features Lookup : " + this.uriBuilder.fullUri());
        }
    },

    queryGenes: function() {
        if (this.uriBuilder.isReady()) {
            this.query(this.uriBuilder.genesQueryURI());
        } else {
            org.systemsbiology.addama.js.Message.error("Chromosomes", "Incomplete Chromosome Genes Lookup : " + this.uriBuilder.fullUri());
        }
    },

    query: function(targetUri) {
        if (targetUri) {
            Ext.Ajax.request({
                url: targetUri,
                method: "GET",
                success: function(o) {
                    var json = Ext.util.JSON.decode(o.responseText);
                    if (json) {
                        if (json.data) {
                            this.loadGeneData(json.data);
                        } else if (json.items) {
                            this.loadFeatureData(json.items);
                        }
                        Ext.History.add(targetUri, true);
                    }
                },
                failure: function(o) {
                    org.systemsbiology.addama.js.Message.error("Chromosomes", "Error Loading: " + o.statusText);
                },
                scope: this
            });
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
                store: store, stripeRows: true, iconCls: 'icon-grid', height: 400,
                colModel: new Ext.grid.ColumnModel({ defaults: { width: 200, sortable: true }, columns: columns })
            }));
        } else {
            org.systemsbiology.addama.js.Message.error("Chromosomes", "No Feature Data Loaded");
        }
    }
});

org.systemsbiology.addama.js.widgets.chromosomes.UriBuilder = Ext.extend(Ext.util.Observable, {
    buildUri: null,
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
        if (this.start == null) return false;
        if (this.end == null) return false;
        return true;
    },

    setChromosome: function(c, min, max) {
        this.chromosome = c;
        this.minimum = min;
        this.maximum = max;
        this.fireEvent("change");
    },

    setBuildUri: function(uri) {
        this.buildUri = uri;
        this.fireEvent("change");
    },

    setStart: function(fld, v) {
        this.start = v;
        this.fireEvent("change");
    },

    setEnd: function(fld, v) {
        this.end = v;
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

    fullUri: function() {
        var tempchr = "chr";
        if (this.chromosome) tempchr = this.chromosome;
        var tempstart = "start";
        if (this.start) tempstart = this.start;
        var tempend = "end";
        if (this.end) tempend = this.end;

        var fullUri = "";
        if (this.buildUri) {
            fullUri += this.buildUri;
        }
        fullUri += "/" + tempchr + "/" + tempstart + "/" + tempend;
        if (this.strand) {
            fullUri += "/" + this.strand;
        }
        return fullUri;
    }
});