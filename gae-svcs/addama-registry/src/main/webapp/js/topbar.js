TopBar = Ext.extend(Object, {
    whoamiListeners: [],

    initialize: function(container, menus) {
        Ext.get(container).applyStyles({
            width: "100%",
            height: "20px",
            border: "1px solid #c3daf9"
        });

        var tb = new Ext.Toolbar({
            buttonAlign: "right"
        });
        tb.render(container);

        var me = this;
        Ext.Ajax.request({
            url: "/addama/users/whoami",
            method: "GET",
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json && json.email) {
                    tb.add({ text: json.email, xtype: 'tbtext' });
                    tb.add({ text: 'Sign out', xtype: 'tbbutton',
                        handler:function() {document.location = json.logoutUrl}});
                } else if (json && json.loginUrl) {
                    tb.add({ text: 'Sign in', xtype: 'tbbutton',
                        handler:function() {document.location = json.loginUrl}});
                } else {
                    tb.add({ text: "Not logged in" });
                }

                tb.doLayout();

                if (me.whoamiListeners) {
                    for (var i = 0; i < me.whoamiListeners.length; i++) {
                        me.whoamiListeners[i](json);
                    }
                }
            },
            failure: function() {
                tb.add({ text: "Not logged in" });
                tb.doLayout();
            }
        });

        if (menus && menus.items) {
            for (var i = 0; i < menus.items.length; i++) {
                this.addMenu(menus.items[i], tb);
            }
        }
    },

    onWhoami: function(callback) {
        this.whoamiListeners[this.whoamiListeners.length] = callback;
    },

    addMenu: function(menu, tb) {
        var uri = menu.uri;
        var label = menu.label;
        Ext.Ajax.request({
            url: uri,
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

                    tb.add({
                        cls: "x-btn-text-icon",
                        text: label,
                        menu: newMenu
                    });
                }
                tb.doLayout();
            },
            failure: function() {
                tb.doLayout();
            }
        });
    }
});
