function initViewport(isAdmin, isModerator) {
    var adminItems = [];
    if (isAdmin) {
        adminItems = [
            {
                title: 'Memberships',
                layout: 'fit',
                iconCls: 'x-icon-tickets',
                tabTip: 'Memberships',
                style: 'padding: 10px;',
                contentEl: "c_admin"
            },
            {
                title: 'Domain Level Access',
                iconCls: 'x-icon-users',
                tabTip: 'Domain Access',
                style: 'padding: 10px;',
                contentEl: "c_domain_level_membership_list"
            },
            {
                title: 'Moderators',
                iconCls: 'x-icon-users',
                tabTip: 'Moderators',
                style: 'padding: 10px;',
                contentEl: "c_membership_domain_moderators"
            }
        ];
    }

    var moderatorItems = [];
    if (isAdmin || isModerator) {
        moderatorItems = [
            {
                title: 'Moderated Items',
                iconCls: 'x-icon-configuration',
                tabTip: 'Moderated Items',
                style: 'padding: 10px;',
                contentEl: "c_moderator"
            }
        ];
    }

    var groupPanelItems = [];
    if (adminItems.length) {
        groupPanelItems[groupPanelItems.length] = {
            expanded: true,
            mainItem: 0,
            items: adminItems
        };
    }

    if (moderatorItems.length) {
        groupPanelItems[groupPanelItems.length] = {
            expanded: false,
            items: moderatorItems
        };
    }

    groupPanelItems[groupPanelItems.length] = {
        expanded: false,
        items: [
            {
                title: 'My Memberships',
                iconCls: 'x-icon-configuration',
                tabTip: 'My Membership Status',
                style: 'padding: 10px;',
                contentEl: "c_member"
            }
        ]
    };

    new Ext.Viewport({
        layout:'border',
        items:[
            {
                id: "topbar-panel",
                region: "north",
                height: 30,
                contentEl: "c_topbar",
                frame: false
            },
            {
                region: "center",
                xtype: 'grouptabpanel',
                tabWidth: 200,
                activeGroup: 0,
                items: groupPanelItems
            }
        ]
    });
}
