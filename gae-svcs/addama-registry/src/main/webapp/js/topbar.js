Ext.ns("org.systemsbiology.addama.js.topbar");

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
                            this.newLinkMenuItem("Addama Documentation", "http://code.google.com/p/addama/wiki/Overview"), '-',
                            this.newLinkMenuItem("What is App Engine?", "http://code.google.com/appengine"), '-',
                            this.newLinkMenuItem("Google Privacy Policy", "http://www.google.com/intl/en/privacy"),
                            this.newLinkMenuItem("Your Google Account", "https://accounts.google.com/b/0/ManageAccount")
                        ]
                    });
                    this.toolbar.add({ xtype: 'tbseparator' });

                    this.toolbar.add({ text: "Links",
                        menu: [
                            this.newLinkMenuItem("Home", "/"), '-',
                            this.newLinkMenuItem("Download API Keys", "/html/apikeys.html"), '-',
                            this.newLinkMenuItem("Query Databases", "/html/datasources.html"),
                            this.newLinkMenuItem("Browse Files", "/html/workspaces.html"),
                            this.newLinkMenuItem("Query Chromosomes", "/html/chromosomes.html"),
                            this.newLinkMenuItem("View Job Results", "/html/jobs.html"), '-',
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
                this.toolbar.add({ text: "Error: " + o.statusText,
                    menu: [
                        this.newLinkMenuItem("Addama Open Source Project", "http://addama.org"),
                        this.newLinkMenuItem("Addama Documentation", "http://code.google.com/p/addama/wiki/Overview"), '-',
                        this.newLinkMenuItem("What is App Engine?", "http://code.google.com/appengine"), '-',
                        this.newLinkMenuItem("Google Privacy Policy", "http://www.google.com/intl/en/privacy"),
                        this.newLinkMenuItem("Your Google Account", "https://accounts.google.com/b/0/ManageAccount")
                    ]
                });
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
                    new org.systemsbiology.addama.js.topbar.RegisterAppsWindow();
                }
            });

            var greenlistAction = new Ext.Action({
                text: "Manage User Access",
                handler: function(){
                    new org.systemsbiology.addama.js.topbar.GreenlistWindow();
                }
            });

            var addamaPropertiesAction = new Ext.Action({
                text: "Service Registration Keys",
                handler: function(){
                    new org.systemsbiology.addama.js.topbar.RegistryKeysWindow();
                }
            });

            var app_id = document.location.hostname.replace(".appspot.com", "");

            this.toolbar.add({
                text: "Administration",
                menu: [
                    refreshUI,
                    { xtype: "menuseparator" },
                    registerAppsAction,
                    greenlistAction,
                    addamaPropertiesAction,
                    { xtype: "menuseparator" },
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

org.systemsbiology.addama.js.FloatingTopBar = Ext.extend(org.systemsbiology.addama.js.TopBar, {
    logoImg: "/images/isblogo.svg",
    hidden: false,

    constructor: function(config) {
        Ext.apply(this, config);

        var container = Ext.DomHelper.insertFirst(document.body, {tag: "div", cls: "floating_topbar"}, true);
        var img = Ext.DomHelper.append(container, '<img src="' + this.logoImg + '" alt="logo"/>', true);
        img.on("click", function() {
            if (this.hidden) {
                this.contentEl.slideIn('l', { duration:1 });
                this.hidden = false;
            } else {
                this.contentEl.slideOut('l', { duration:1 });
                this.hidden = true;
            }
        }, this);

        this.contentEl = Ext.DomHelper.append(container, { tag: "div" }, true);

        org.systemsbiology.addama.js.FloatingTopBar.superclass.constructor.call(this);

        if (this.hidden) {
            this.contentEl.pause(1).slideOut('l', { duration:1 });
        }
    }
});

org.systemsbiology.addama.js.topbar.RegistryKeysWindow = Ext.extend(Object, {

    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.topbar.RegistryKeysWindow.superclass.constructor.call(this);

        var fld = new Ext.form.TextField({
            name: "serviceHostUrl",
            anchor: "100%",
            labelSeparator: "",
            fieldLabel: "Content Root URL"
        });

        var msgTxt = "";
        msgTxt += "<div class='apikeys'>";
        msgTxt += "<h4>Service Registry Keys</h4>";
        msgTxt += "<ul>";
        msgTxt += "<li>The 'addama.properties' file is used by web services to register securely.</li>";
        msgTxt += "<li>These keys are managed by administrators through the App Engine Console.</li>";
        msgTxt += "<li>Enter your public web services host URL below to generate keys</li>";
        msgTxt += "<li>&nbsp;&nbsp;&nbsp;&nbsp;(e.g. https://webservices.example.com)</li>";
        msgTxt += "</ul>";
        msgTxt += "<div class='apikeys_caution'>Do NOT share these keys.  Treat the same as a password.</div>";
        msgTxt += "</div>";

        var win = new Ext.Window({
            closable: true,
            modal: true,
            closeAction: "hide",
            width: 600,
            defaults: {
                margins: "5 5 5 5",
                padding: "5 5 5 5"
            },
            items: [
                { region:"center", html: msgTxt, frame: true },
                new Ext.form.FormPanel({
                    frame:true,
                    region:"south",
                    items: [fld],
                    buttons: [
                        {
                            text: "Generate",
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
                })
            ]
        });
        win.show();
    }
});

org.systemsbiology.addama.js.topbar.RegisterAppsWindow = Ext.extend(Object, {
    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.topbar.RegisterAppsWindow.constructor.call(this);

        this.loadAppsGrid();
        this.loadAppsForm();

        var win = new Ext.Window({
            title: "Register Applications",
            closable: true,
            modal: true,
            closeAction: "hide",
            width: 900,
            height: 400,
            padding: "5 5 5 5",
            items: [
                new Ext.TabPanel({
                    activeTab: 0,
                    margins: "5 5 5 5",
                    padding: "5 5 5 5",
                    items: [
                        this.registeredAppsPanel,
                        {
                            title: "New Application",
                            layout:"border",
                            height: 200,
                            frame: true,
                            border: false,
                            margins: "5 5 5 5",
                            padding: "5 5 5 5",
                            items:[ this.registerAppForm, this.appPreviewPanel ]
                        }
                    ]
                })
            ]
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
            height: 400,
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
            failure: function(o) {
                org.systemsbiology.addama.js.Message.show("Registered Applications", "Error: " + o.statusText);
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

        this.appPreviewPanel = new Ext.Panel({
            region: "east",
            html: org.systemsbiology.addama.js.topbar.RegisterAppsWindow.GenerateHtml({
                uri: "/",
                logo: "",
                label: "Label will go here",
                description:"Description will go here",
                alt:"logo"
            })
        });

        this.registerAppForm = new Ext.form.FormPanel({
            frame:true,
            region:"center",
            width: 400,
            defaults: { anchor: "100%", labelSeparator: "" },
            items: [ fldId, fldLabel, fldUrl, fldLogo, fldDescription ],
            buttons: [
                {
                    text: "Preview",
                    handler: function() {
                        Ext.DomHelper.overwrite(this.appPreviewPanel.el, org.systemsbiology.addama.js.topbar.RegisterAppsWindow.GenerateHtml({
                            id: fldId.getRawValue(),
                            uri: fldUrl.getRawValue(),
                            label: fldLabel.getRawValue(),
                            logo: fldLogo.getRawValue(),
                            description: fldDescription.getRawValue(),
                            alt: "not found"
                        }));
                    },
                    scope: this
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
                    },
                    scope: this
                }
            ]
        });
    }
});

org.systemsbiology.addama.js.topbar.RegisterAppsWindow.GenerateHtml = function(item) {
    var logoAlt = item.alt;
    if (!logoAlt) {
        logoAlt = "app_logo";
    }

    var srcUrl = item.uri;
    if (srcUrl.substring(item.uri.length - 1) == "/") {
        srcUrl = srcUrl.substring(0, srcUrl.length -1);
    }
    srcUrl += "/";

    var logoUrl = "/images/nologo.png";
    if (item.logo) {
        var logoUri = item.logo;
        if (logoUri.substring(0, 1) == "/") {
            logoUri = logoUri.substring(1, logoUri.length);
        }
        logoUrl = srcUrl + logoUri;
    }

    var label = item.label;
    if (!label) {
        label = "Untitled";
    }

    var description = item.description;
    if (!description) {
        description = "";
    }

    var itemhtml = "";
    itemhtml += "<div class='apps'>";
    itemhtml += "<a href='" + srcUrl + "' target='_blank'>";
    itemhtml += "<img src='" + logoUrl + "' alt='" + logoAlt + "'/>";
    itemhtml += "<h3>" + label + "</h3>";
    itemhtml += "</a>";
    itemhtml += "<div class='apps_description'>" + description + "</div>";
    itemhtml += "</div>";
    return itemhtml;
};

org.systemsbiology.addama.js.topbar.GreenlistWindow = Ext.extend(Object, {
    constructor: function(config) {
        if (!config) {
            config = {};
        }

        Ext.apply(this, config);

        org.systemsbiology.addama.js.topbar.GreenlistWindow.superclass.constructor.call(this, config);

        this.store = new Ext.data.ArrayStore({ fields: [ {name: "id"} ], sortInfo: {field: "id"} });

        this.loadListView();
        this.loadGreenlist();

        var win = new Ext.Window({
            title: "Manage User Access",
            closable: true,
            modal: true,
            closeAction: "hide",
            padding: "5 5 5 5",
            margins: "5 5 5 5",
            items: [
                new Ext.Panel({
                    width:400,
                    height:300,
                    layout: "fit",
                    items: this.listView,
                    tbar: [
                        new Ext.Button({ text: "Add User", handler: this.addNewUser, scope: this }), '-',
                        new Ext.Button({ text: "Add Users", handler: this.addUsers, scope: this })
                    ]
                })
            ]
        });
        win.show();
    },

    loadListView: function() {
        this.listView = new Ext.list.ListView({
            store: this.store,
            emptyText: "No users have been entered.  Domain is open to everyone",
            hideHeaders: true,
            columns: [ { header: "User", width: 300, sortable: true, dataIndex: "id" } ]
        });
    },

    loadGreenlist: function() {
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
            failure: function(o) {
                org.systemsbiology.addama.js.Message.error("Loading Users", "Error: " + o.statusText);
            },
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

        var addUserWindow = new Ext.Window({
            title: "Add User",
            frame: true,
            closable: true,
            modal: true,
            closeAction: "hide",
            items: [
                new Ext.FormPanel({
                    width: 300,
                    frame: true,
                    items: [ fld ],
                    padding: "10 10 10 10",
                    buttons: [
                        {
                            text: "Save",
                            handler: function() {
                                var userEmail = fld.getRawValue();
                                if (userEmail) {
                                    Ext.Ajax.request({
                                        url: "/addama/greenlist/" + userEmail,
                                        method: "POST",
                                        success: function() {
                                            addUserWindow.close();
                                            this.loadGreenlist();
                                        },
                                        failure: function(o) {
                                            org.systemsbiology.addama.js.Message.error("Add New User", "Error: " + o.statusText);
                                        },
                                        scope: this
                                    });
                                }
                            },
                            scope: this
                        }
                    ]
                })
            ]
        });
        addUserWindow.show();
    },

    addUsers: function() {
        var textArea = new Ext.form.TextArea({
            name: "userList",
            height: 150,
            width: 300,
            emptyText: "Enter comma-separated list of emails"
        });

        var addUsersWindow = new Ext.Window({
            title: "Add Users",
            frame: true,
            closable: true,
            modal: true,
            closeAction: "hide",
            items: [
                new Ext.FormPanel({
                    width: 450,
                    frame: true,
                    items: [ textArea ],
//                    padding: "10 10 10 10",
                    buttons: [
                        {
                            text: "Save",
                            handler: function() {
                                var userEmails = textArea.getRawValue();
                                if (userEmails) {
                                    var splits = userEmails.split(",");
                                    var userList = [];
                                    Ext.each(splits, function(item) {
                                        userList.push(item.trim());
                                    });
                                    Ext.Ajax.request({
                                        url: "/addama/greenlist",
                                        method: "POST",
                                        params: {
                                            emails: userList
                                        },
                                        success: function() {
                                            addUsersWindow.close();
                                            this.loadGreenlist();
                                        },
                                        failure: function(o) {
                                            org.systemsbiology.addama.js.Message.error("Add New Users", "Error: " + o.statusText);
                                        },
                                        scope: this
                                    });
                                }
                            },
                            scope: this
                        }
                    ]
                })
            ]
        });
        addUsersWindow.show();
    }
});


