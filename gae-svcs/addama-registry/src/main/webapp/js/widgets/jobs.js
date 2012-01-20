Ext.ns("org.systemsbiology.addama.js");

org.systemsbiology.addama.js.JobBean = Ext.extend(Object, {
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

org.systemsbiology.addama.js.JobsFetch = Ext.extend(Ext.util.Observable, {
    constructor: function() {
        Ext.apply(this, { queue: [] });

        this.addEvents({ queued: true, fetched: true });

        org.systemsbiology.addama.js.JobsFetch.superclass.constructor.call(this);

        this.on("queued", function() {
            while (this.queue.length > 0) {
                var toolUri = this.queue.shift();
                if (toolUri) {
                    this.fetchJobs(toolUri);
                }
            }
        }, this);

        org.systemsbiology.addama.js.apis.channels.Listener.on("message", function(a) {
            var event = Ext.util.JSON.decode(a.data);
            if (event.job) {
                this.addJob(event.job);
                this.refreshGrid();
            }
        }, this);
    },

    fetchTools: function(tooluris) {
        Ext.each(tooluris, function(tooluri) {
            this.queue.push(tooluri);
        }, this);
        this.fireEvent("queued");
    },

    fetchJobs: function(toolUri) {
        Ext.Ajax.request({
            url: toolUri + "/jobs",
            method: "GET",
            params: {
                "_dc": Ext.id()
            },
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json && json.items) {
                    for (var i = 0; i < json.items.length; i++) {
                        this.addJob(json.items[i]);
                    }
                }

                if (this.queue.length == 0) {
                    this.fireEvent("fetched");
                }
            },
            scope: this
        });
    }
});

org.systemsbiology.addama.js.BaseJobsGrid = Ext.extend(org.systemsbiology.addama.js.JobsFetch, {
    constructor: function(config) {
        Ext.apply(this, config);

        this.jobPointers = [];
        this.gridData = [];
        if (!this.gridPanelConfig) {
            this.gridPanelConfig = {};
        }

        org.systemsbiology.addama.js.BaseJobsGrid.superclass.constructor.call(this);

        this.on("fetched", this.refreshGrid, this);

        this.initRowExpander();
        this.initSelectionModel();
        this.renderGrid();
        this.fetchData();
        this.initRefreshTask();
    },

    initRowExpander: function() {
        if (this.rowExpander != null) {
            return;
        }

        this.rowExpander = new Ext.ux.grid.RowExpander({
            tpl: new Ext.Template([
                '<b>Results:</b>',
                '<p>',
                '<span id="container_details_results_{idx}"></span>'
            ])
        });
        this.rowExpander.on("expand", function(rowexp, record) {
            var jobUri = record.data.uri;
            var containerEl = "container_details_results_" + record.data.idx;

            Ext.Ajax.request({
                url: jobUri,
                method: "GET",
                params: {
                    "_dc": Ext.id()
                },
                success: function(o) {
                    // track links
                    var linkIds = [];
                    var linkId = function() {
                        var lid = Ext.id();
                        linkIds[linkIds.length] = lid;
                        return lid;
                    };

                    var ahrefs = [];

                    var json = Ext.util.JSON.decode(o.responseText);
                    if (json && json.items) {
                        // build links
                        Ext.each(json.items, function(item) {
                            ahrefs[ahrefs.length] = "<a target='_blank' id='" + linkId() + "' href='" + item.uri + "'>" + item.name + "</a>";
                        });
                    } else {
                        ahrefs[ahrefs.length] = "No results available.  Use 'Refresh' button to check again.";
                    }

                    ahrefs[ahrefs.length] = "<a target='_blank' id='" + linkId() + "' href='" + jobUri + "/log'>Job Log</a>";

                    // render links
                    var items = [];
                    Ext.each(ahrefs, function(ahref) {
                        items[items.length] = { html: ahref, frame: false, border: false };
                    });

                    new Ext.Panel({ items: items, renderTo: containerEl });

                    // register event listener
                    Ext.each(linkIds, function(lid) {
                        var elem = Ext.get(lid);
                        if (elem) {
                            Ext.EventManager.on(elem, "click", trackDownloadLink);
                        }
                    });
                }
            });

            return true;
        }, this);
    },

    initSelectionModel: function() {
        if (this.selectionModel != null) {
            return;
        }

        this.selectionModel = new Ext.grid.CheckboxSelectionModel();
        this.selectionModel.on("selectionchange", this.enableButtonsOnSelectionChange, this);
    },

    enableButtonsOnSelectionChange: function(sm) {
        if (sm.getCount()) {
            this.grid.stopButton.enable();
            this.grid.deleteButton.enable();
        } else {
            this.grid.stopButton.disable();
            this.grid.deleteButton.disable();
        }
    },

    initRefreshTask: function() {
        if (this.refreshTask != null) {
            return;
        }

        this.refreshTask = { run: this.refetch, interval: 10000, scope: this };
    },

    renderGrid: function() {
        this.store = new Ext.data.GroupingStore({
            reader: new Ext.data.ArrayReader({}, this.getStoreColumns()),
            data: this.gridData,
            sortInfo: {field: 'lastModified', direction: "DESC"},
            groupField:'lastChangeDay',
            groupOnSort: false,
            groupDir: "DESC"
        });

        var defaultConfig = {
            store: this.store,
            columns: this.getGridColumns(),
            view: new Ext.grid.GroupingView({
                forceFit:true,
                startCollapsed: false,
                groupTextTpl: '{text} ({[values.rs.length]} {[values.rs.length > 1 ? "items" : "item"]})'
            }),
            sm: this.selectionModel,
            tbar: [
                { text: 'Refresh', iconCls:'refresh', ref: "../refreshButton" },
                '-',
                { text: 'Stop Selected', iconCls:'stop', disabled: true, ref: "../stopButton" },
                '-',
                { text: 'Delete Selected', iconCls:'delete', disabled: true, ref: "../deleteButton" },
                '-',
                { text: 'Auto-Refresh', enableToggle: true, toggleHandler: this.onAutoRefresh, pressed: false, scope: this }
            ],
            stripeRows: true,
            columnLines: true,
            frame:true,
            plugins: this.rowExpander,
            title: "Jobs",
            collapsible: false,
            animCollapse: false,
            iconCls: 'icon-grid'
        };

        this.grid = new Ext.grid.GridPanel(Ext.apply(defaultConfig, this.gridPanelConfig));

        this.grid.stopButton.on("click", function() {
            this.selectionModel.each(function(row) {
                Ext.Ajax.request({
                    url: row.data.uri + "/stop",
                    method: "post",
                    failure: this.showMessage,
                    scope: this
                });
            }, this);
            this.selectionModel.clearSelections(true);
        }, this);

        this.grid.deleteButton.on("click", function() {
            this.selectionModel.each(function(row) {
                Ext.Ajax.request({
                    url: row.data.uri + "/delete",
                    method: "post",
                    success: function() {
                        this.store.remove(row);
                    },
                    failure: this.showMessage,
                    scope:this
                });
            }, this);
        }, this);

        this.grid.refreshButton.on("click", this.refetch, this);
    },

    refetch: function() {
        this.jobPointers = [];
        this.gridData = [];
        this.refreshGrid();
        this.fetchData();
    },

    refreshGrid: function() {
        this.store.loadData(this.gridData);
    },

    onAutoRefresh: function(item, pressed) {
        if (this.refreshTask != null) {
            if (pressed) {
                Ext.TaskMgr.start(this.refreshTask);
            } else {
                Ext.TaskMgr.stop(this.refreshTask);
            }
        }
    },

    showMessage: function(o) {
        var json = Ext.util.JSON.decode(o.responseText);
        if (json && json.message) {
            Ext.MessageBox.alert('Status', json.message);
        }
    },

    getJobPointer: function(job) {
        var jobPointer = this.jobPointers[job.uri];
        if (!jobPointer) {
            jobPointer = this.gridData.length;
            this.jobPointers[job.uri] = jobPointer;
        }
        return jobPointer;
    }
});

org.systemsbiology.addama.js.JobsGrid = Ext.extend(org.systemsbiology.addama.js.BaseJobsGrid, {

    constructor: function(config) {
        Ext.apply(this, Ext.applyIf(config, { tools: [] }));

        org.systemsbiology.addama.js.JobsGrid.superclass.constructor.call(this);
    },

    getStoreColumns: function() {
        return [
            { name: 'idx' },
            { name: 'uri' },
            { name: 'tool' },
            { name: 'lastChangeDay', type: 'date' },
            { name: 'job' },
            { name: 'owner' },
            { name: 'status' },
            { name: 'message' },
            { name: 'reasonCode', type: 'int' },
            { name: 'durationInSeconds', type: 'int' },
            { name: 'lastModified', type: "date" }
        ];
    },

    getGridColumns: function() {
        return [
            this.rowExpander,
            this.selectionModel,
            {
                header: "Date", width: 25, dataIndex: 'lastChangeDay', type: "date",
                sortable: true, hidden: true, renderer: Ext.util.Format.dateRenderer()
            },
            { header: "URI", width: 25, dataIndex: 'uri', sortable: false, hidden: true },
            { header: "Tool", width: 40, sortable: true, dataIndex: 'tool', id:'tool' },
            { header: "Job", width: 40, sortable: true, dataIndex: 'job' },
            { header: "Owner", width: 30, hidden: true, dataIndex: 'owner' },
            { header: "Status", width: 20, sortable: true, dataIndex: 'status' },
            { header: "Reason", width: 20, sortable: true, dataIndex: 'message' },
            { header: "Code", width: 10, sortable: true, dataIndex: 'reasonCode' },
            { header: "Duration (secs)", width: 20, sortable: true, dataIndex: 'durationInSeconds' },
            {
                header: "Last Modified", width: 30, dataIndex: 'lastModified', type: "date",
                sortable: true, hidden: true, renderer: getFormattedDate
            }
        ]
    },

    fetchData: function() {
        Ext.Ajax.request({
            url: "/addama/tools",
            method: "GET",
            params: {
                "_dc": Ext.id()
            },
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json && json.items) {
                    var uris = [];
                    Ext.each(json.items, function(item) {
                        this.tools[item.uri] = item;
                        uris[uris.length] = item.uri;
                    }, this);
                    this.fetchTools(uris);
                }
            },
            scope: this
        });
    },

    addJob: function(job) {
        var tool = this.tools[job.script];
        if (tool) {
            jobBean = new org.systemsbiology.addama.js.JobBean(job);
            var jobPointer = this.getJobPointer(job);
            this.gridData[jobPointer] = [
                jobPointer,
                job.uri,
                tool.label,
                job.lastModified,
                jobBean.getLabel(),
                jobBean.getOwner(),
                job.status,
                job.message,
                job.returnCode,
                job.durationInSeconds,
                job.lastModified
            ];
        }
    }

});

org.systemsbiology.addama.js.JobsGridForTool = Ext.extend(org.systemsbiology.addama.js.BaseJobsGrid, {

    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.JobsGridForTool.superclass.constructor.call(this);
    },

    getStoreColumns: function() {
        return [
            {name: 'idx'},
            {name: 'uri'},
            {name: 'lastChangeDay', type: 'date' },
            {name: 'job'},
            {name: 'owner'},
            {name: 'status'},
            {name: 'message'},
            {name: 'reasonCode', type: 'int'},
            {name: 'durationInSeconds', type: 'int'},
            {name: 'lastModified', type: 'date' }
        ];
    },

    getGridColumns: function() {
        return [
            this.rowExpander,
            this.selectionModel,
            {
                header: "Date", width: 25, dataIndex: 'lastChangeDay', type: "date",
                sortable: true, hidden: true, renderer: Ext.util.Format.dateRenderer()
            },
            { header: "URI", width: 25, dataIndex: 'uri', sortable: false, hidden: true },
            { header: "Job", width: 40, sortable: true, dataIndex: 'job' },
            { header: "Owner", width: 30, hidden: true, dataIndex: 'owner' },
            { header: "Status", width: 20, sortable: true, dataIndex: 'status' },
            { header: "Reason", width: 20, sortable: true, dataIndex: 'message' },
            { header: "Code", width: 10, sortable: true, dataIndex: 'reasonCode' },
            { header: "Duration (secs)", width: 20, sortable: true, dataIndex: 'durationInSeconds' },
            { header: "Last Modified", sortable: true, hidden: true, dataIndex: 'lastModified',
                type: "date", renderer: getFormattedDate }
        ];
    },

    fetchData: function() {
        this.fetchJobs(this.tool);
    },

    addJob: function(job) {
        var jobPointer = this.getJobPointer(job);
        var jobBean = new org.systemsbiology.addama.js.JobBean(job);
        this.gridData[jobPointer] = [
            jobPointer,
            job.uri,
            job.lastModified,
            jobBean.getLabel(),
            jobBean.getOwner(),
            job.status,
            job.message,
            job.returnCode,
            job.durationInSeconds,
            job.lastModified
        ];
    }

});

function parseCarriageReturns(text) {
    var txt = text;
    while (txt.indexOf("\n") != -1) {
        txt = txt.replace("\n", "<br/>");
    }
    return txt;
}

function trackDownloadLink(evt, elem) {
    try {
        var trackUri = elem.href;
        if (trackUri) {
            trackUri = trackUri.replace(".", "_");
            trackUri = trackUri.replace(" ", "_");
            _gaq.push(["_trackPageview", trackUri]);
        }
    }
    catch(err) {
    }
}

function getFormattedDate(v) {
    return formatDate(v, "MM/dd/y hh:mm:ss a");
}
