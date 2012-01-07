var userWindow;
var fpAddUser;
var greenlistStore = new Ext.data.ArrayStore({
    fields: [ {name: "id"}, {name: "uri"} ]
});

var LoadViewport = function() {
    new Ext.Viewport({
        layout:'border',
        items:[
            {
                id: "topbar-panel",
                region: "north",
                height: 54,
                border: false,
                frame: false,
                xtype: "box",
                split: true,
                items:[
                    new Ext.Panel({ contentEl: "c_banner" }),
                    new Ext.Panel({ contentEl: "c_topbar" })
                ]
            },
            new Ext.grid.GridPanel({
                store: greenlistStore,
                title: "Members",
                tbar: [
                    new Ext.Button(new Ext.Action({ text: "Add User", handler: AddNewUser }))
                ],
                columns: [
                    { header: "User", width: 300, sortable: true, dataIndex: "id" },
                    { header: "Uri", width: 300, sortable: true, dataIndex: "uri", hidden: true }
                ],
                stripeRows: true,
                autoHeight: true,
                autoScroll: true,
                frame: true,
                width: 600,
                region:"center",
                bodyStyle: "padding:10px;",
                style: "padding:10px;",
                contentEl: "c_greenlist"
            })
        ]
    });
};

var LoadGreenlist = function() {
    Ext.MessageBox.show({
        msg: "Loading, please wait...",
        progressText: "Loading...",
        width:300,
        wait:true,
        waitConfig: {interval:200}
    });

    Ext.Ajax.request({
        url: "/addama/greenlist",
        method: "GET",
        success: function(o) {
            var json = Ext.util.JSON.decode(o.responseText);
            if (json && json.items) {
                var data = [];
                Ext.each(json.items, function(item) {
                    data.push([item.id,item.uri]);
                });
                greenlistStore.loadData(data);
                Ext.MessageBox.hide();
            }
        },
        failure: HandleFailure
    });
};

var AddNewUser = function() {
    fpAddUser = new Ext.FormPanel({
        width: 400,
        frame: true,
        autoHeight: true,
        bodyStyle: 'padding: 10px 10px 0 10px;',
        method: 'POST',
        standardSubmit: true,
        items: [
            { xtype: 'textfield', fieldLabel: "User Email", name: "label", allowBlank:false, id:"emailLabel" }
        ],
        buttons: [
            {
                text: 'Submit',
                handler: function() {
                    if (fpAddUser.getForm().isValid()) {
                        Ext.Ajax.request({
                            url: "/addama/greenlist/" + Ext.getDom("emailLabel").value,
                            method: "POST",
                            success: function(o) {
                                userWindow.close();
                                LoadGreenlist();
                            },
                            failure: HandleFailure
                        });
                    }
                }
            }
        ]
    });

    userWindow = new Ext.Window({
        width:400, autoHeight:true, title: 'Add New User', items: fpAddUser, frame: true
    }).show();
};

var HandleFailure = function(o, e) {
    Ext.MessageBox.show({
        title: "Errors Submitting Request",
        msg: o + "<br/>" + e,
        buttons: Ext.MessageBox.OK,
        icon: Ext.MessageBox.ERROR
    });
};