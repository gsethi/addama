Ext.ns("org.systemsbiology.addama.js");

org.systemsbiology.addama.js.TopBar = Ext.extend(Ext.util.Observable, {

    constructor: function(config) {
        Ext.apply(this, config);

        if (get_parameter("no_topbar")) {
            return;
        }

//        Ext.get(this.contentEl).applyStyles({ width: "100%", height: "20px", border: "1px solid #c3daf9" });
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
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json && json.email) {
                    this.toolbar.add({ text: json.email, xtype: 'tbtext' });
                    this.toolbar.add({ text: 'Sign out', xtype: 'tbbutton',
                        handler:function() {
                            document.location = json.logoutUrl
                        }});
                    this.toolbar.doLayout();
                    this.fireEvent("whoami", json);

                } else if (json && json.loginUrl) {
                    this.toolbar.add({ text: 'Sign in', xtype: 'tbbutton',
                        handler:function() {
                            document.location = json.loginUrl
                        }});
                    this.toolbar.doLayout();
                } else {
                    this.toolbar.add({ text: "Not logged in" });
                    this.toolbar.doLayout();
                }
            },
            failure: function() {
                this.toolbar.add({ text: "Not logged in" });
                this.toolbar.doLayout();
            },
            scope: this
        });
    },

    addMenu: function(menu) {
        Ext.Ajax.request({
            url: menu.uri,
            method: "GET",
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json && json.numberOfItems) {
                    var newMenu = new Ext.menu.Menu();
                    for (var i = 0; i < json.items.length; i++) {
                        var item = json.items[i];
                        newMenu.add({
                            text: item.label,
                            uri: item.uri
                        });
                    }

                    this.toolbar.add({
                        cls: "x-btn-text-icon",
                        text: menu.label,
                        menu: newMenu
                    });
                }
                this.toolbar.doLayout();
            },
            failure: function() {
                this.toolbar.doLayout();
            },
            scope: this
        });
    }
});
