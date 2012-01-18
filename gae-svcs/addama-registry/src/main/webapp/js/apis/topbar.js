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
                    this.toolbar.add({ text: json.email, xtype: 'tbtext' });
                    if (json.isAdmin) {
                        var refreshUI = new Ext.Action({
                            text: 'Refresh UI Version',
                            handler: function(){
                                Ext.Ajax.request({
                                    url: "/addama/apps/refresh", method: "POST",
                                    success: function() {
                                        document.location = document.location.href;
                                    }
                                });
                            }
                        });
                        var registerApplications = new Ext.Action({
                            text: 'Register Applications',
                            handler: function(){
                                document.location = "/html/apps.html";
                            }
                        });

                        var manageGreenlist = new Ext.Action({
                            text: 'Manage User Access',
                            handler: function(){
                                document.location = "/html/greenlist.html";
                            }
                        });

                        var appengineLink = new Ext.Action({
                            text: "AppEngine Console",
                            handler: function() {
                                var app_id = document.location.hostname.replace(".appspot.com", "");
                                document.location = "https://appengine.google.com/dashboard?&app_id=" + app_id;
                            }
                        })

                        var adminMenu  = {
                            text: "Administration",
                            menu: [refreshUI,registerApplications,manageGreenlist]
                        };
                        
                        this.toolbar.add(adminMenu);
                    }
                    this.toolbar.add({ text: 'Sign out', xtype: 'tbbutton',
                        handler:function() {
                            document.location = json.logoutUrl;
                        }
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
    }
});
