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
    margins: "5 0 5 5",
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
    margins: "5 5 5 0",
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
    margins: "5 5 5 0",
    border: true,
    split: true,
    items:[ contentPanel, propertiesPanel],
    bbar: statusBar
});

Ext.onReady(function() {
    new Ext.Panel({
        layout: "border",
        items: [browsePanel, mainPanel],
        renderTo: "container_main"
    });
});
Ext.onReady(loadTree);

