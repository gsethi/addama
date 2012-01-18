Ext.ns("org.systemsbiology.addama.js");

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

            var app_id = document.location.hostname.replace(".appspot.com", "");

            this.toolbar.add({
                text: "Administration",
                menu: [
                    refreshUI,
                    this.newLinkMenuItem("Register Applications", "/html/apps.html"),
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

        var items = [];
        items.push({
            text: "Generated API Keys are managed by domain administrators through the App Engine Administration Console"
        });
        items.push({
            text: "Download API Key File",
            handler: function() { document.location = "/addama/apikeys/file"; }
        });
        if (this.isAdmin) {
            var fp = new Ext.form.FormPanel({
                frame:true,
                title: "Generate addama.properties",
                bodyStyle:"padding:5px 5px 0",
                width: 350,
                items: [
                    {
                        id:"serviceHostUrl",
                        anchor: '100%',
                        type: "textfield",
                        fieldLabel: 'Service Host URL',
                        name: "serviceHostUrl"
                    }
                ],
                buttons: [
                    {
                        text: "Generate",
                        handler: function() {
                            var form = fp.getForm();
                            var fld = form.findField("serviceHostUrl");
                            if (fld && fld.getRawValue()) {
                                document.location = "/addama/apikeys/addama.properties?serviceUrl=" + fld.getRawValue();
                            } else {
                                document.location = "/addama/apikeys/addama.properties";
                            }
                        }
                    }
                ]
            });
            items.push(fp);
        }

        new Ext.Window({
            title: "API Keys",
            closable: true,
            closeAction: "hide",
            width: 600,
            minWidth: 400,
            height: 400,
            layout: "fit",
            bodyStyle: "padding: 5px;",
            items: items
        });
    }
});