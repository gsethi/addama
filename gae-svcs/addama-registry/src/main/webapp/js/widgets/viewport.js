Ext.ns("org.systemsbiology.addama.js");

org.systemsbiology.addama.js.Viewport = Ext.extend({
    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.Viewport.superclass.constructor.call(this);

        this.drawViewport();
    },

    drawViewport: function() {
        var addamaBanner = Ext.DomHelper.append(Ext.getBody(), '<div><a href="http://addama.org" target="_blank"><img src="/images/banner.png" alt="Addama"/></a></div>', true);
        var appengineLogo = Ext.DomHelper.append(Ext.getBody(), '<div><img src="http://code.google.com/appengine/images/appengine-silver-120x30.gif" alt="Powered by Google App Engine"/></div>', true);
        var link1 = Ext.DomHelper.append(Ext.getBody(), '<div><a href="/addama/apikeys/file">Download API Keys</a></div>', true);
        var link2 = Ext.DomHelper.append(Ext.getBody(), '<div><a href="http://www.systemsbiology.org" target="_blank">Institute for Systems Biology</a></div>', true);
        var link3 = Ext.DomHelper.append(Ext.getBody(), '<div><a href="http://shmulevich.systemsbiology.net/" target="_blank">Shmulevich Lab</a></div>', true);
        var link4 = Ext.DomHelper.append(Ext.getBody(), '<div><a href="http://codefor.systemsbiology.net/" target="_blank">Code for Systems Biology</a></div>', true);
        var link5 = Ext.DomHelper.append(Ext.getBody(), '<div><a href="http://www.ncbi.nlm.nih.gov/pubmed/19265554" target="_blank">PMID 19265554</a></div>', true);

        var headerPanel = new Ext.Panel({
            region: "north",
            border: false,
            frame: false,
            layout: "border",
            height: 99,
            defaults: { border: false, frame: false },
            items:[
                { contentEl: this.topbarEl, region: "north", height: 30 },
                { contentEl: addamaBanner.id, region: "center" },
                { contentEl: appengineLogo.id, region: "east", width: 130 }
            ]
        });

        var tabPanel = new Ext.TabPanel({
            region: "center",
            items: this.tabs,
            activeTab: 0,
            border: true,
            margins: "5 5 5 5",
            padding: "5 5 5 5"
        });

        var footerPanel = new Ext.Panel({
            region:"south",
            height: 33,
            layout: "hbox",
            border: false,
            frame: true,
            padding: "1 1 1 1",
            margins: "1 1 1 1",
            layoutConfig: { pack: "center", align: "middle", defaultMargins: "0 10 0 0" },
            items:[ link1.dom, link2.dom, link3.dom, link4.dom, link5.dom ]
        });

        new Ext.Viewport({
            layout:'border',
            items:[ headerPanel, tabPanel, footerPanel ]
        });
    }
});