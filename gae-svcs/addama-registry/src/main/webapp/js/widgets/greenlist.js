Ext.ns("org.systemsbiology.addama.js");

org.systemsbiology.addama.js.GreenlistMgr = Ext.extend(Object, {
    constructor: function(config) {
        if (!config) {
            config = {};
        }

        Ext.apply(this, config);

        org.systemsbiology.addama.js.GreenlistMgr.superclass.constructor.call(this, config);

        this.store = new Ext.data.ArrayStore({ fields: [ {name: "id"} ] });

        this.listView = new Ext.list.ListView({
            title: "Authorized Users",
            store: this.store,
            emptyText: "No users have been entered.  Domain is open to everyone",
            reserveScrollOffset: true,
            hideHeaders: true,
            padding: "10 10 10 10",
            margins: "10 10 10 10",
            columns: [
                { header: "User", width: 300, sortable: true, dataIndex: "id" }
            ]
        });

        this.panel = new Ext.Panel({
            width:425,
            height:500,
            layout: "fit",
            title: "Authorized Users",
            items: this.listView,
            tbar: [
                new Ext.Button({ text: "Add User", handler: this.addNewUser, scope: this })
            ]
        });

        this.loadGreenlist();
    },

    loadGreenlist: function() {
        org.systemsbiology.addama.js.Message.show("User Access", "Loading authorized users");

        Ext.Ajax.request({
            url: "/addama/greenlist",
            method: "GET",
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json && json.items) {
                    var data = [];
                    Ext.each(json.items, function(item) {
                        data.push([item.id]);
                    });
                    this.store.loadData(data);
                }
            },
            failure: this.handleFailure,
            scope: this
        });
    },

    addNewUser: function() {
        var fld = new Ext.form.TextField({
            name: "label",
            anchor: "100%",
            allowBlank:false,
            labelSeparator: "",
            fieldLabel: "Email Address"
        });

        var fpAddUser = new Ext.FormPanel({
            width: 400,
            frame: true,
            autoHeight: true,
            bodyStyle: 'padding: 10px 10px 0 10px;',
            items: [ fld ],
            buttons: [
                {
                    text: "Save",
                    handler: function() {
                        if (fld.getRawValue()) {
                            this.saveNewUser(fld.getRawValue());
                        }
                    },
                    scope: this
                }
            ]
        });

        this.window = new Ext.Window({
            width:400, autoHeight:true, title: 'Add New User', items: [fpAddUser], frame: true
        });
        this.window.show();
    },

    saveNewUser: function(userEmail) {
        Ext.Ajax.request({
            url: "/addama/greenlist/" + userEmail,
            method: "POST",
            success: function() {
                this.window.close();
                this.loadGreenlist();
            },
            failure: this.handleFailure,
            scope: this
        });
    },

    handleFailure: function(o) {
        org.systemsbiology.addama.js.Message.error("User Access", "Error loading data: " + o.statusText);
    }
});
