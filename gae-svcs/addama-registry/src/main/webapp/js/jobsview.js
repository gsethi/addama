Ext.ns("org.systemsbiology.addama.js.widgets.jobs");

org.systemsbiology.addama.js.widgets.jobs.JobBean = Ext.extend(Object, {
    constructor: function(item) {
        this.item = item;
    },

    getLabel: function() {
        var item = this.item;
        if (item.label) {
            return item.label;
        }
        if (item.uri) {
            return item.uri.substring(item.uri.lastIndexOf("/") + 1, item.uri.length);
        }
        return "Untitled";
    },

    getOwner: function() {
        if (this.item.owner) {
            var ausers = "/addama/users/";
            if (this.item.owner.indexOf(ausers) >= 0) {
                return this.item.owner.substring(ausers.length);
            }
            return this.item.owner;
        }
        return "anonymous";
    }

});

org.systemsbiology.addama.js.widgets.jobs.Grid = Ext.extend(Object, {
    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.widgets.jobs.Grid.superclass.constructor.call(this);

        this.renderGrid();

        if (this.toolUri) {
            this.loadToolJobs(this.toolUri);
        }
    },

    loadToolJobs: function(toolUri) {
        Ext.Ajax.request({
            url: toolUri + "/jobs",
            method: "GET",
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json) {
                    this.displayJobs(json.items);
                }
            },
            failure: function(o) {
                org.systemsbiology.addama.js.Message.error("Jobs", "Error: " + o.statusText);
            },
            scope: this
        });
    },

    displayJobs: function(items) {
        var data = [];
        if (items && items.length) {
            Ext.each(items, function(item) {
                data.push(this.addJob(item));
            }, this);
        }
        this.jobsStore.loadData(data);
    },

    renderGrid: function() {
        this.detailsButton = new Ext.Button({ text: 'Show Details', iconCls:'show', disabled: true });
        this.stopButton = new Ext.Button({ text: 'Stop Selected', iconCls:'stop', disabled: true });
        this.deleteButton = new Ext.Button({ text: 'Delete Selected', iconCls:'delete', disabled: true });
        var selectionModel = new Ext.grid.RowSelectionModel({singleSelect:true});

        this.detailsButton.on("click", function() {
            selectionModel.each(this.loadRowDetails, this);
        }, this);

        this.stopButton.on("click", function() {
            selectionModel.each(function(row) {
                Ext.Ajax.request({
                    url: row.data.uri + "/stop",
                    method: "POST",
                    failure: function(o) {
                        org.systemsbiology.addama.js.Message.error("Stop Job", o.statusText);
                    },
                    scope: this
                });
            }, this);
        }, this);

        this.deleteButton.on("click", function() {
            selectionModel.each(function(row) {
                Ext.Ajax.request({
                    url: row.data.uri + "/delete",
                    method: "POST",
                    success: function() {
                        this.store.remove(row);
                    },
                    failure: function(o) {
                        org.systemsbiology.addama.js.Message.error("Delete Job", o.statusText);
                    },
                    scope:this
                });
            }, this);
        }, this);

        selectionModel.on("selectionchange", function(sm) {
            if (sm.getCount()) {
                this.detailsButton.enable();
                this.stopButton.enable();
                this.deleteButton.enable();
            } else {
                this.detailsButton.disable();
                this.stopButton.disable();
                this.deleteButton.disable();
            }
        }, this);

        this.jobsStore = new Ext.data.GroupingStore({
            reader: new Ext.data.ArrayReader({}, [
                { name: 'uri' },
                { name: 'job' },
                { name: 'status' },
                { name: 'owner' },
                { name: 'reasonCode', type: 'int' },
                { name: 'message' },
                { name: 'durationInSeconds', type: 'int' },
                { name: 'created', type: 'date' },
                { name: 'lastModified', type: "date" }
            ]),
            sortInfo: {field: 'lastModified', direction: "DESC"},
            groupField:'status',
            groupOnSort: false,
            groupDir: "DESC"
        });

        var dtRender = Ext.util.Format.dateRenderer('m/d/Y H:i:s');

        this.gridPanel = new Ext.grid.GridPanel({
            region: "center",
            store: this.jobsStore,
            columns: [
                { header: "Job", width: 40, sortable: true, dataIndex: 'job' },
                { header: "Status", width: 20, sortable: true, dataIndex: 'status', hidden: true },
                { header: "Code", width: 10, sortable: true, dataIndex: 'reasonCode' },
                { header: "Reason", width: 20, sortable: true, dataIndex: 'message' },
                { header: "Created", width: 25, dataIndex: 'created', type: "date", sortable: true, renderer: dtRender},
                { header: "Last Modified", width: 30, dataIndex: 'lastModified', type: "date", sortable: true, renderer: dtRender},
                { header: "Duration (secs)", width: 20, sortable: true, dataIndex: 'durationInSeconds' },
                { header: "URI", width: 25, dataIndex: 'uri', sortable: false, hidden: true },
                { header: "Owner", width: 30, dataIndex: 'owner', hidden: true }
            ],
            view: new Ext.grid.GroupingView({
                forceFit:true,
                startCollapsed: false,
                groupTextTpl: '{text} ({[values.rs.length]} {[values.rs.length > 1 ? "items" : "item"]})'
            }),
            sm: selectionModel,
            tbar: [ this.detailsButton, '-', this.stopButton, '-', this.deleteButton ],
            stripeRows: true,
            columnLines: true,
            frame:true,
            title: "Jobs",
            collapsible: false,
            animCollapse: false,
            iconCls: 'icon-grid'
        });
    },

    loadRowDetails: function(row) {
        var jobUri = row.data.uri;

        Ext.Ajax.request({
            url: jobUri,
            method: "GET",
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json && json.items) {
                    this.displayRowDetails(json);
                }
            },
            scope: this
        });
    },

    displayRowDetails: function(json) {
        // track links
        var linkIds = [];
        var linkId = function() {
            var lid = Ext.id();
            linkIds.push(lid);
            return lid;
        };

        var jobBean = new org.systemsbiology.addama.js.widgets.jobs.JobBean(json);

        var results = [];
        results.push({ html: "<b><a target='_blank' id='" + linkId() + "' href='" + json.uri + "/log'>Log</a></b>" });
        if (json.items && json.items.length) {
            var html = "";
            Ext.each(json.items, function(item) {
                html += "<li>" + "<a target='_blank' id='" + linkId() + "' href='" + item.uri + "'>" + item.name + "</a>" + "</li>"
            });
            results.push({ title: "Results", html: "<ul>" + html + "</ul>" });
        } else {
            results.push({ title: "Results", html: "No results found" });
        }

        new Ext.Window({
            title: "Job Results for " + jobBean.getLabel(),
            closable: true,
            modal: true,
            closeAction: "hide",
            autoScroll: true,
            defaults: { frame: true, padding: "5 5 5 5", margins: "5 5 5 5", border: false },
            items: results
        }).show();

        // listen to links
        Ext.each(linkIds, function(lid) {
            var elem = Ext.get(lid);
            if (elem) elem.on("click", trackDownloadLink);
        });
    },

    addJob: function(job) {
        var jobBean = new org.systemsbiology.addama.js.widgets.jobs.JobBean(job);
        return [
            job.uri,
            jobBean.getLabel(),
            job.status,
            jobBean.getOwner(),
            job.returnCode,
            job.message,
            job.durationInSeconds,
            Date.parse(job.created),
            Date.parse(job.lastModified)
        ];
    }
});

org.systemsbiology.addama.js.widgets.jobs.View = Ext.extend(org.systemsbiology.addama.js.widgets.jobs.Grid, {
    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.widgets.jobs.View.superclass.constructor.call(this);

        this.loadPanels();
        this.loadTools();
        this.initListeners();
    },

    loadPanels: function() {
        this.toolsStore = new Ext.data.ArrayStore({ fields: [ "label", "uri" ], sortInfo: {field: "label"} });

        this.listView = new Ext.list.ListView({
            store: this.toolsStore,
            region: "west",
            width: 250,
            frame:true,
            border:true,
            emptyText: "No tools found",
            hideHeaders: true,
            columns: [ { header: "Label", width: 300, sortable: true, dataIndex: "label" } ]
        });
        this.listView.on("click", this.loadJobs, this);

        this.renderGrid();

        this.mainPanel = new Ext.Panel({
            layout: "border",
            padding: "5 5 5 5",
            margins: "5 5 5 5",
            frame:true,
            border:true,
            items: [ this.listView, this.gridPanel ]
        });
    },

    loadTools: function() {
        Ext.Ajax.request({
            url: "/addama/tools",
            method: "GET",
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json) {
                    this.displayTools(json.items);
                }
            },
            failure: function(o) {
                org.systemsbiology.addama.js.Message.error("Tools", "Error: " + o.statusText);
            },
            scope: this
        })
    },

    loadJobs: function(view, index, node) {
        var record = this.listView.getRecord(node);
        this.loadToolJobs(record.data.uri);
    },

    initListeners: function() {
        if (org.systemsbiology.addama.js.channels && org.systemsbiology.addama.js.channels.Listener) {
            org.systemsbiology.addama.js.channels.Listener.on("message", function(a) {
                var event = Ext.util.JSON.decode(a.data);
                if (event) {
                    var job = event.job;
                    if (job) {
                        if (job.label) {
                            org.systemsbiology.addama.js.Message.show("Job Status", job.label + ":" + job.status);
                        } else {
                            org.systemsbiology.addama.js.Message.show("Job Status", job.status + ":" + job.uri);
                        }
                    }
                }
            });
        }
    },

    displayTools: function(items) {
        var data = [];
        if (items && items.length) {
            Ext.each(items, function(item) {
                data.push([item.label, item.uri]);
            });
        }
        this.toolsStore.loadData(data);
    }
});
