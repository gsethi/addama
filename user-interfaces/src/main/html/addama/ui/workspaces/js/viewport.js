var headerPanel = {
    id: "panel-title-control",
    xtype: "box",
    region: "north",
    border: true,
    split: true,
    applyTo: "header",
    height: 30
};

var footerPanel = {
    region: 'south',
    collapsible: true,
    split: true,
    autoHeight: true,
    footerCfg: {
        tag: 'h2',
        align: 'center',
        cls: 'x-panel-footer',
        html:   '<a href="http://informatics.systemsbiology.net/informatics/">Research Informatics</a> | <a href="http://shmulevich.systemsbiology.net/">Shmulevich Lab</a> | <a href="http://www.systemsbiology.org/">Institute for Systems Biology</a>'
                + '<br><a href="http://informatics.systemsbiology.net/addama/">Supported by Addama</a> | <a href="http://www.ncbi.nlm.nih.gov/pubmed/19265554">Cite PMID 19265554</a>'
    }
};

var startPanel = {
    id: "main-content-start-panel",
    layout: "fit",
    bodyStyle: "padding:25px",
    contentEl: "main-content-start"
};

var repoViewPanel = {
    id: "main-content-repository-panel",
    layout: "fit",
    bodyStyle: "padding:25px",
    contentEl: "main-content-repository"
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
    items: [ startPanel, repoViewPanel, folderViewPanel, fileViewPanel ]
});

var statusBar = new Ext.ux.StatusBar({
    text: "Ready",
    id: 'basic-statusbar',
    iconCls: "x-status-valid"
});

var browsePanel = {
    id: "layout-browser",
    layout: "fit",
    title: "Browse",
    region:"west",
    border: true,
    split: true,
    margins: "33 0 5 5",
    width: 275,
    minSize: 100,
    maxSize: 500,
    collapsible: true,
    layoutConfig: {
        titleCollapse: true,
        hideCollapseTool: true,
        animate: true,
        activeOnTop: false
    },
    items: [tree]
};

var mainPanel = new Ext.TabPanel({
    id: "panel-main-control",
    xtype: "tabpanel",
    region: "center",
    activeTab: 0,
    margins: "33 5 5 0",
    border: true,
    split: true,
    items:[ contentPanel],
    bbar: statusBar
});

var propertiesPanel = {
    id: "panel-properties-control",
    title: "Properties",
    layout: "fit",
    region:"east",
    border:true,
    margins: "33 5 5 0",
    width: 320,
    minSize: 100,
    maxSize: 500,
    collapsible: true,
    titleCollapse: true,
    animate: true,
    activeOnTop: false,
    bodyStyle: "padding-bottom:15px; background:#eee;",
    autoScroll: true,
    contentEl: "panel-properties"
};

Ext.onReady(function() {
    new Ext.Viewport({
        layout: "border",
        items: [headerPanel, browsePanel, footerPanel, mainPanel, propertiesPanel],
        renderTo: Ext.getBody()
    });
    console.log("viewport completed");

    loadTree();
});

