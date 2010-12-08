var topBarPanel = new Ext.Panel({
    contentEl: "container_topbar"
});

var headerPanel = {
    id: "panel-title-control",
    xtype: "box",
    region: "north",
    border: true,
    split: true,
    items:[topBarPanel]
};

var footerPanel = {
    region: 'south',
    collapsible: false,
    split: true,
    autoHeight: true,
    contentEl: "container_footer"
};

var startPanel = {
    id: "main-content-start-panel",
    layout: "fit",
    bodyStyle: "padding:25px",
    contentEl: "main-content-start"
};

var folderViewPanel = {
    id: "main-content-folder-panel",
    layout: "fit",
    bodyStyle: "padding:25px",
    contentEl: "main-content-folder"
};

var fileViewPanel = {
    id: "main-content-file-panel",
    layout: "fit",
    bodyStyle: "padding:25px",
    contentEl: "main-content-file"
};

var contentPanel = new Ext.Panel({
    id: "panel-content-control",
    title: "Main",
    layout: "card",
    activeItem: 0,
    border: false,
    items: [ startPanel, folderViewPanel, fileViewPanel ]
});

var browsePanel = {
    id: "layout-browser",
    layout: "fit",
    region:"west",
    border: true,
    split: true,
    margins: "33 0 5 5",
    width: 275,
    minSize: 100,
    maxSize: 500,
    layoutConfig: {
        titleCollapse: true,
        hideCollapseTool: true,
        animate: true,
        activeOnTop: false
    },
    items: [tree]
};

var propertiesPanel = {
    id: "panel-properties-control",
    title: "Properties",
    layout: "fit",
    border:true,
    margins: "33 5 5 0",
    width: 320,
    minSize: 100,
    maxSize: 500,
    animate: true,
    activeOnTop: false,
    bodyStyle: "padding-bottom:15px; background:#eee;",
    autoScroll: true,
    contentEl: "panel-properties"
};

var mainPanel = new Ext.TabPanel({
    id: "panel-main-control",
    xtype: "tabpanel",
    region: "center",
    activeTab: 0,
    margins: "33 5 5 0",
    border: true,
    split: true,
    items:[ contentPanel, propertiesPanel],
    bbar: statusBar
});

var eventManager = new EventManager();

Ext.onReady(function() {
    new Ext.Viewport({
        layout: "border",
        items: [headerPanel, browsePanel, footerPanel, mainPanel],
        renderTo: Ext.getBody()
    });
    loadTree();

    eventManager.addListener("node-refresh", displayNodeInContentPanel);
    eventManager.addListener("node-refresh", displayNodeInPropertiesPanel);
//    eventManager.addListener("node-refresh", expandNode);
});

