var DrawViewport = function(tabs) {
    var headerPanel = {
        id: "topbar-panel",
        region: "north",
        height: 54,
        border: false,
        frame: false,
        xtype: "box",
        split: true,
        items:[
            new Ext.Panel({ contentEl: "container_banner" }),
            new Ext.Panel({ contentEl: "container_topbar" })
        ]
    };

    var tabPanel = new Ext.TabPanel({
        id: 'tab-panel',
        renderTo: "container_tabs",
        region: "center",
        items: tabs
    });

    var footerPanel = new Ext.Panel({
        region:"south",
        contentEl: "container_footer"
    });

    new Ext.Viewport({
        layout:'border',
        items:[
            headerPanel,
            tabPanel,
            footerPanel
        ]
    });
};