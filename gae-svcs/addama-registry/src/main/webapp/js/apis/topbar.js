Ext.ns("org.systemsbiology.addama.js");

Ext.Ajax.on('requestexception', function(c, o) {
    if (o.status == 401) {
        if (org.systemsbiology.addama.js.Message) {
            org.systemsbiology.addama.js.Message.error("Unauthorized Access", "Your access to this resource has been denied.  Please contact an administrator.");
        }
    }
});

org.systemsbiology.addama.js.TopBar = Ext.extend(Ext.util.Observable, {

    constructor: function(config) {
        Ext.apply(this, config);

        this.toolbar = new Ext.Toolbar({
            buttonAlign: "right",
            renderTo: this.contentEl
        });

        this.addEvents({
            /**
             * @event whoami
             * Fires after the whoami REST call is returned.
             * @param {Object} json represents logged-in user object
             */
            whoami: true
        });

        org.systemsbiology.addama.js.TopBar.superclass.constructor.call(this);

        Ext.Ajax.request({
            url: "/addama/users/whoami",
            method: "GET",
            scope: this,
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json && json.email) {
                    this.toolbar.add({ text: json.email,
                        menu: [
                            this.newLinkMenuItem("Addama Open Source Project", "http://addama.org"),
                            this.newLinkMenuItem("Google Privacy Policy", "http://www.google.com/intl/en/privacy"),
                            this.newLinkMenuItem("Your Google Account", "https://accounts.google.com/b/0/ManageAccount"),
                            this.newLinkMenuItem("What is App Engine?", "http://code.google.com/appengine")
                        ]
                    });
                    this.toolbar.add({ xtype: 'tbseparator' });

                    var apikeysAction = new Ext.Action({
                        text: "API Keys",
                        handler: function(){
                            new org.systemsbiology.addama.js.ApiKeysWindow({isAdmin:json.isAdmin});
                        }
                    });

                    this.toolbar.add({ text: "Links",
                        menu: [
                            this.newLinkMenuItem("Home", "/"),
                            apikeysAction,
                            this.newLinkMenuItem("Query Databases", "/html/datasources.html"),
                            this.newLinkMenuItem("Browse Workspaces", "/html/workspaces.html"),
                            this.newLinkMenuItem("View Job Results", "/html/jobs.html"),
                            this.newLinkMenuItem("Test Channels", "/html/channels.html"),
                            this.newLinkMenuItem("Test Feeds", "/html/feeds.html")
                        ]
                    });
                    this.toolbar.add({ xtype: 'tbseparator' });

                    if (this.addAdminMenu(json)) {
                        this.toolbar.add({ xtype: 'tbseparator' });
                    }

                    this.toolbar.add({ text: 'Sign out', xtype: 'tbbutton',
                        handler:function() { document.location = json.logoutUrl; }
                    });
                    
                    this.toolbar.doLayout();
                    this.fireEvent("whoami", json);
                } else {
                    this.toolbar.add({ text: "Not logged in" });
                    this.toolbar.doLayout();
                }
            },
            failure: function(o) {
                this.toolbar.add({ text: "Error: " + o.statusText });
                this.toolbar.doLayout();
            }
        });
    },

    addAdminMenu: function(json) {
        if (json.isAdmin) {
            var refreshUI = new Ext.Action({
                text: 'Refresh UI Version',
                handler: function(){
                    Ext.Ajax.request({
                        url: "/addama/apps/refresh", method: "POST",
                        success: function() { document.location = document.location.href; }
                    });
                }
            });

            var registerAppsAction = new Ext.Action({
                text: "Register Apps",
                handler: function(){
                    new org.systemsbiology.addama.js.RegisterAppsWindow();
                }
            });


            var app_id = document.location.hostname.replace(".appspot.com", "");

            this.toolbar.add({
                text: "Administration",
                menu: [
                    refreshUI,
                    registerAppsAction,
                    this.newLinkMenuItem("Manage User Access", "/html/greenlist.html"),
                    this.newLinkMenuItem("App Engine Console", "https://appengine.google.com/dashboard?&app_id=" + app_id)
                ]
            });

            return true;
        }
        return false;
    },

    newLinkMenuItem: function(text, link) {
        return new Ext.Action({
            text: text,
            handler: function() {
                document.location = link;
            }
        });
    }
});

org.systemsbiology.addama.js.ApiKeysWindow = Ext.extend(Object, {

    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.ApiKeysWindow.superclass.constructor.call(this);

        var msgTxt = "<h4>Generated API Keys are managed by domain administrators through the App Engine Console</h4>";
        msgTxt += "<br/>";
        msgTxt += "Each user is assigned a private API key to support secure programmatic access to Addama services.";
        msgTxt += "<b>Do NOT share your API key<b>.  Treat the same as a password.";
        if (this.isAdmin) {
            msgTxt += "<br/><br/><br/>";
            msgTxt += "The 'addama.properties' file is used by Addama local services to register securely.";
            msgTxt += "<br/>";
            msgTxt += "Enter the public URL in the text field below for your web services host (e.g. https://webservices.example.com) to automatically generate";
            msgTxt += "<br/>";
            msgTxt += "<br/>";
        }

        var items = [];
        items.push({ region:"center", html: msgTxt, margins: "5 5 5 5", padding: "5 5 5 5" });

        if (this.isAdmin) {
            var fld = new Ext.form.TextField({
                name: "serviceHostUrl",
                anchor: "100%",
                labelSeparator: "",
                fieldLabel: "Web services host URL"
            });
            
            items.push(new Ext.form.FormPanel({
                frame:true,
                region:"south",
                margins: "5 5 5 5",
                padding: "5 5 5 5",
                width: 500,
                items: [fld],
                buttons: [
                    {
                        text: "Generate addama.properties",
                        handler: function() {
                            var serviceUrl = fld.getRawValue();
                            if (serviceUrl) {
                                document.location = "/addama/apikeys/addama.properties?serviceUrl=" + serviceUrl;
                            } else {
                                document.location = "/addama/apikeys/addama.properties";
                            }
                            win.close();
                        }
                    }
                ]
            }));
        }

        var win = new Ext.Window({
            title: "API Keys",
            closable: true,
            modal: true,
            closeAction: "hide",
            width: 600,
            minWidth: 400,
            height: 400,
            padding: "5 5 5 5",
            items: items,
            tbar: [
                {
                    text: "Download API Key File",
                    handler: function() {
                        document.location = "/addama/apikeys/file";
                        win.close();
                    }
                }
            ]
        });
        win.show();
    }
});

org.systemsbiology.addama.js.RegisterAppsWindow = Ext.extend(Object, {
    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.RegisterAppsWindow.constructor.call(this);

        this.loadAppsGrid();
        this.loadAppsForm();

        var win = new Ext.Window({
            title: "Register Applications",
            closable: true,
            modal: true,
            closeAction: "hide",
            width: 1000,
            minWidth: 400,
            layout:"border",
            height: 500,
            padding: "5 5 5 5",
            items: [ this.registerAppForm, this.registeredAppsPanel ]
        });
        win.show();
    },

    loadAppsGrid: function() {
        this.store = new Ext.data.ArrayStore({
            fields: [
                {name: "id"},
                {name: "label"},
                {name: "url"},
                {name: "logo"},
                {name: "description"}
            ]
        });

        this.registeredAppsPanel = new Ext.grid.GridPanel({
            title: "Registered Applications",
            store: this.store,
            region: "center",
            width: 600,
            columns: [
                { header: "ID", width: 75, sortable: true, dataIndex: "id" },
                { header: "Label", width: 120, sortable: true, dataIndex: "label" },
                { header: "URL", width: 200, sortable: true, dataIndex: "url" },
                { header: "Logo", width: 100, sortable: true, dataIndex: "logo" },
                { header: "Description", width: 300, sortable: true, dataIndex: "description" }
            ]
        });

        this.loadAppsData();
    },

    loadAppsData: function() {
        Ext.Ajax.request({
            method: "GET",
            url: "/addama/apps",
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json && json.items) {
                    var data = [];
                    Ext.each(json.items, function(item) {
                        data.push([ item.id, item.label, item.url, item.logo, item.description ]);
                    });
                    this.store.loadData(data);
                } else {
                    org.systemsbiology.addama.js.Message.show("Registered Applications", "No applications have been registered");
                }
            },
            scope: this
        });
    },

    loadAppsForm: function() {
        var fldId = new Ext.form.TextField({ name: "id", fieldLabel: "Application ID" });
        var fldLabel = new Ext.form.TextField({ name: "label", fieldLabel: "Label" });
        var fldUrl = new Ext.form.TextField({ name: "url", fieldLabel: "Root Content URL" });
        var fldLogo = new Ext.form.TextField({ name: "logo", fieldLabel: "Application Logo" });
        var fldDescription = new Ext.form.TextField({ name: "description", fieldLabel: "Description" });

        this.registerAppForm = new Ext.form.FormPanel({
            frame:true,
            region:"west",
            margins: "5 5 5 5",
            padding: "5 5 5 5",
            width: 400,
            defaults: { anchor: "100%", labelSeparator: "" },
            items: [ fldId, fldLabel, fldUrl, fldLogo, fldDescription ],
            buttons: [
                {
                    text: "Preview Logo",
                    handler: function() {
                        var imgUrl = fldUrl.getRawValue() + "/" + fldLogo.getRawValue();
                        new Ext.Window({
                            title: "Preview Logo",
                            closable: true,
                            modal: true,
                            closeAction: "hide",
                            width: 200,
                            height: 200,
                            padding: "5 5 5 5",
                            items: [
                                { html: "<div class='apps'><img src='" + imgUrl + "' alt='Image Not Found at " + imgUrl + "'/></div>" }
                            ]
                        }).show();
                    }
                },
                {
                    text: "Save",
                    handler: function() {
                        var newLabel = fldLabel.getRawValue();
                        var app = {
                            id: fldId.getRawValue(),
                            label: newLabel,
                            url: fldUrl.getRawValue(),
                            logo: fldLogo.getRawValue(),
                            description: fldDescription.getRawValue()
                        };

                        Ext.Ajax.request({
                            method: "POST",
                            url: "/addama/apps",
                            params: {
                                app: Ext.util.JSON.encode(app)
                            },
                            success: function() {
                                org.systemsbiology.addama.js.Message.show("Register Applications", "Application '" + newLabel + "' registered successfully");
                                this.loadAppsData();
                            },
                            scope: this
                        })

                    }
                }
            ]
        });
    }
});

