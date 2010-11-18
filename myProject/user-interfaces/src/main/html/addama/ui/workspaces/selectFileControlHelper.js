function loadTree(workspaceUri, containerDiv) {
    Ext.get(containerDiv).dom.innerHTML = "";
    var tree = new Ext.tree.TreePanel({
        requestMethod: "GET",
        useArrows: true,
        autoScroll: true,
        animate: true,
        enableDD: true,
        containerScroll: true,
        border: false,
        dataUrl: workspaceUri,
        root: {
            nodeType: 'async',
            text: 'Workspace',
            draggable: false,
            id: workspaceUri
        }
    });
    tree.addListener('expandnode', function(node) {
        Ext.Ajax.request({
            url: node.id,
            method: "GET",
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json && json.items) {
                    annotateItems(json);
                    appendItems(json, node);
                }
            },
            failure: function() {
                node.expandable = false;
            }
        });
    }, {single: true});
    tree.addListener("click", selectFile);

    tree.render(containerDiv);
    tree.getRootNode().expand();
}

function annotateItems(json) {
    if (json && json.items) {
        for (var i = 0; i < json.items.length; i++) {
            annotateTreeNode(json.items[i]);
        }
    }
}

function appendItems(json, node) {
    if (json && json.items) {
        while (node.firstChild) {
            var c = node.firstChild;
            node.removeChild(c);
            c.destroy();
        }
        for (var i = 0; i < json.items.length; i++) {
            node.appendChild(json.items[i]);
        }
        node.renderChildren();
    }
}

function annotateTreeNode(item) {
    var treeNode = item;
    treeNode.text = treeNode.name;
    treeNode.id = treeNode.uri;
    treeNode.path = treeNode.uri;
    if (treeNode.isFile) {
        treeNode.leaf = true;
        treeNode.cls = "file";
    } else {
        treeNode.leaf = false;
        treeNode.cls = "folder";
    }
}

function directLink(uri, callback) {
    Ext.Ajax.request({
        url: uri + "/directlink",
        method: "GET",
        success: function(o) {
            var json = Ext.util.JSON.decode(o.responseText);
            if (json && json.location) {
                callback(json.location);
            }
        }
    });
}

function loadUploadFormPanel(uploadUri, containerDiv, callback) {
    var fp = new Ext.FormPanel({
        renderTo: containerDiv,
        fileUpload: true,
        width: 300,
        frame: true,
        title: 'Upload File',
        autoHeight: true,
        bodyStyle: 'padding: 10px 10px 0 10px;',
        labelWidth: 50,
        defaults: {
            anchor: '95%',
            allowBlank: false,
            msgTarget: 'side'
        },
        items: [
            {
                xtype: 'fileuploadfield',
                buttonOnly: true,
                id: 'form-file',
                name: 'file-path',
                listeners: {
                    'fileselected': function(fb, v){
                        directLink(uploadUri, function(directLinkUrl){
                            fp.getForm().submit({
                                url: directLinkUrl,
                                headers: {
                                    "x-addama-desired-contenttype": "text/html"
                                },
                                waitMsg: 'Uploading your file...',
                                success: function(f, o) {
                                    if (callback) {
                                        callback();
                                    }
                                    fp.destroy();
                                }
                            });
                        });
                    }
                }
            }
        ]
    });
}

function openCreateFolderPanel(selectedDirUri, callback) {
    Ext.MessageBox.prompt('Create folder', 'Please enter folder name:', function(btn, text) {
        Ext.Ajax.request({
            url: selectedDirUri + "/" + text,
            method: "POST",
            success: callback
        });
    });
}
