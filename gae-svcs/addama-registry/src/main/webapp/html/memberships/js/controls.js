DomainLevelUsersControl = Ext.extend(Object, {
    constructor: function(containerEl) {
        this.containerEl = containerEl;
        this.currentState();
    },

    currentState: function() {
        Ext.Ajax.request({
            url: "/addama/memberships/domain/users",
            method: "GET",
            success: this.renderPanel,
            scope: this
        });
    },

    renderPanel: function(o) {
        Ext.getDom(this.containerEl).innerHTML = "";

        var json = Ext.util.JSON.decode(o.responseText);
        if (json) {
            var tabItems = [];
            if (json.numberOfItems) {
                var newMember = new NewMembershipButton("New Member", "member");
                var newGuest = new NewMembershipButton("New Guest", "guest");
                var allowAsGuest = new UpdateMembershipButton("Allow as Guests", "guest");
                var allowAsMember = new UpdateMembershipButton("Allow as Members", "member");
                var rejectApplicant = new UpdateMembershipButton("Reject Applicants");
                var revokeMembership = new UpdateMembershipButton("Revoke Memberships");
                this.appendMembershipTab(tabItems, json.items, "Members", "member", [newMember, revokeMembership]);
                this.appendMembershipTab(tabItems, json.items, "Guests", "guest", [newGuest, revokeMembership]);
                this.appendMembershipTab(tabItems, json.items, "Applicants", "applicant", [allowAsGuest, allowAsMember, rejectApplicant]);
            }

            var me = this;
            var refreshFn = function() {
                me.currentState();
            };

            var tabs = new Ext.TabPanel({
                renderTo: this.containerEl,
                width:450,
                activeTab: 0,
                frame:true,
                defaults:{autoHeight: true},
                items: tabItems,
                buttons: [new DomainMembershipButton(json.enabled, refreshFn)]
            });
        }

        setTimeout('Ext.MessageBox.hide();', 500);
    },

    changeState: function() {
        var users = [
            { user: "hrovira@gmail.com", membership: "member"}
        ];

        var me = this;
        Ext.Ajax.request({
            url: "/addama/memberships/domain/users",
            method: "post",
            params: {
                users: Ext.util.JSON.encode(users)
            },
            success: this.currentState,
            scope: this
        });
    },

    appendMembershipTab: function(tabs, items, title, membershipType, customButtons) {
        var tab = this.newMembershipTab(items, title, membershipType, customButtons);
        if (tab) {
            tabs[tabs.length] = tab;
        }
    },

    newMembershipTab: function(items, title, membershipType, customButtons) {
        var data = [];
        for (var i = 0; i < items.length; i++) {
            var item = items[i];
            if (item.membership && item.membership == membershipType) {
                data[data.length] = [item.user]
            }
        }

        if (!data.length) {
            return null;
        }

        var checkboxes = new Ext.grid.CheckboxSelectionModel({
            listeners: {
                selectionchange: function(sm) {
                    var hasSelected = sm.getCount();
                    var topTB = sm.grid.getTopToolbar().items;
                    for (var i = 0; i < topTB.items.length; i++) {
                        var btn = topTB.items[i];
                        if (hasSelected) {
                            btn.enable();
                        } else {
                            if (!btn.doNotDisable) {
                                btn.disable();
                            }
                        }
                    }
                }
            }
        });

        return new Ext.grid.GridPanel({
            store: new Ext.data.ArrayStore({
                data: data,
                fields: [
                    {name: 'user'}
                ]
            }),
            columns: [
                checkboxes,
                { id: 'user', header: 'User', width: 75, sortable: true, dataIndex: 'user' }
            ],
            sm: checkboxes,
            buttonAlign:'center',
            tbar: this.toolbarItems(checkboxes, customButtons),
            stripeRows: true,
            autoExpandColumn: 'user',
            frame:true,
            height: 350,
            width: 600,
            title: title,
            stateful: true,
            stateId: membershipType + 'Grid'
        });
    },

    toolbarItems: function(selectionModel, customButtons) {
        var me = this;
        var refreshFn = function() {
            me.currentState();
        };

        var toolbarItems = [];
        if (customButtons && customButtons.length) {
            for (var i = 0; i < customButtons.length; i++) {
                var customButton = customButtons[i];
                if (customButton.activate) {
                    customButton.activate(selectionModel, refreshFn);
                    toolbarItems[toolbarItems.length] = customButton;
                } else {
                    toolbarItems[toolbarItems.length] = customButton(selectionModel, refreshFn);
                }
            }
        }
        return toolbarItems;
    }
});

DomainMembershipButton = Ext.extend(Object, {
    constructor: function(enabled, callback) {
        this.enabled = enabled;
        this.onSuccess = callback;
        if (this.enabled) {
            this.text = "Disable membership access for entire domain";
        } else {
            this.text = "Enable membership access for entire domain";
        }
    },

    handler: function() {
        Ext.MessageBox.show({
            title: 'Processing, please wait...',
            msg: this.text,
            width:300,
            wait:true,
            progress: true,
            animate: true,
            waitConfig: {interval:500}
        });

        Ext.Ajax.request({
            url: "/addama/memberships/domain",
            method: "post",
            params: {
                enabled: !this.enabled
            },
            success: this.onSuccess,
            scope: this
        });
    }
});

NewMembershipButton = Ext.extend(Object, {
    constructor: function(title, membershipType) {
        this.text = title;
        this.membershipType = membershipType;
        this.doNotDisable = true;
        this.disabled = false;
    },

    activate: function(selectionModel, onSuccess) {
        this.selectionModel = selectionModel;
        this.onSuccess = onSuccess;
    },

    handler: function() {
        var me = this;
        Ext.Msg.prompt("New Membership", "Please enter user's email address:", function(btn, text) {
            if (btn == 'ok') {
                me.submitUsers([
                    { user: text, membership: me.membershipType}
                ]);
            }
        });
    },

    submitUsers: function(users) {
        Ext.MessageBox.show({
            title: 'Processing, please wait...',
            msg: this.text,
            width:300,
            wait:true,
            progress: true,
            animate: true,
            waitConfig: {interval:500}
        });

        Ext.Ajax.request({
            url: "/addama/memberships/domain/users",
            method: "POST",
            params: {
                users: Ext.util.JSON.encode(users)
            },
            success: this.onSuccess,
            scope: this
        });
    }
});

UpdateMembershipButton = Ext.extend(NewMembershipButton, {
    constructor: function(title, membershipType) {
        this.text = title;
        this.membershipType = membershipType;
        this.disabled = true;
        this.doNotDisable = false;
    },

    handler: function() {
        var msType = this.membershipType;

        var users = [];
        this.selectionModel.each(function(row) {
            if (me.membershipType) {
                users[users.length] = { user: row.data.user, membership: msType };
            } else {
                users[users.length] = { user: row.data.user };
            }
        });

        this.submitUsers(users);
    }
});