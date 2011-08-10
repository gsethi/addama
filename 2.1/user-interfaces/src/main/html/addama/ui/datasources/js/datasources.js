Ext.onReady(function() {
    var panel = new Ext.Panel({
        id:'main-panel',
        baseCls:'x-plain',
        renderTo: "container",
        layout:'table',
        layoutConfig: {columns:3},
        defaults: {frame:true, width:200, height: 200},
        items:[
            { title:'Addama Datasources', contentEl: 'container_datasources', autoScroll: true },
            { title:'Query', contentEl: 'container_sql', width:600 },
            { title:'Columns', contentEl: 'container_columns', autoScroll: true },
            { title:'Tables', contentEl: 'container_tables', height: 400, autoScroll: true },
            { title:'Results', contentEl: 'container_preview', width:810, height: 400, colspan:2, autoScroll: true },
            { title:'Messages', contentEl: 'container_errors', width:1020, height: 100, colspan:3, autoScroll: true }
        ]
    });
});