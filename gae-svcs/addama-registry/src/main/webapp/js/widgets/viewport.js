var DrawViewport = function(tabs) {
    var headerPanel = {
        id: "topbar-panel",
        region: "north",
        border: false,
        frame: false,
        xtype: "box",
        layout: "border",
        split: true,
        items:[
            new Ext.Panel({ contentEl: "container_topbar", region: "north" }),
            new Ext.Panel({ contentEl: "container_banner", region: "center" }),
            new Ext.Panel({ contentEl: "gaelogo", region: "east"})
        ]
    };

    var tabPanel = new Ext.TabPanel({
        id: 'tab-panel',
        renderTo: "container_tabs",
        region: "center",
        items: tabs,
        activeTab: 0,
        frame:true,
        height: "100%",
        margins: "5 5 5 5",
        defaults:{autoHeight: true}
    });

    var footerPanel = new Ext.Panel({
        region:"south",
        layout: "hbox",
        contentEl: "container_footer",
        items:[
        ]
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