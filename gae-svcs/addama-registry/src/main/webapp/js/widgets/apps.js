Ext.ns("org.systemsbiology.addama.js");

org.systemsbiology.addama.js.AppsPanel = Ext.extend(Object, {

    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.AppsPanel.superclass.constructor.call(this);

        Ext.Ajax.request({
            url: "/addama/apps",
            method: "get",
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json && json.items) {
                    var html = "";
                    Ext.each(json.items, function(item) {
                        html += "<li><a href='" + item.uri + "'>" + item.label + "</a></li>";
                    });
                    Ext.getDom(this.contentEl).innerHTML = "<ul>" + html + "</ul>";
                }
            },
            scope: this
        });

    }
});

