var tree = new Ext.tree.TreePanel({
    id: "tree-panel",
    title: "Workspaces",
    region:"north",
    split: true,
    minSize: 150,
    autoScroll: true,
    // tree-specific configs:
    rootVisible: false,
    lines: false,
    singleExpand: true,
    useArrows: true,
    loader: new Ext.tree.TreeLoader(),
    root: new Ext.tree.AsyncTreeNode()
});

function loadTree() {
    tree.render();
    tree.addListener("expandnode", expandNode, {single: true});
    tree.addListener("expandnode", displayNodeInContentPanel, {single: true});
    tree.addListener("expandnode", displayNodeInPropertiesPanel, {single: true});
    tree.addListener("click", refreshNodeTree, {single: true});
    tree.addListener("click", displayNodeInContentPanel, {single: true});
    tree.addListener("click", displayNodeInPropertiesPanel, {single: true});

    Ext.Ajax.request({
        url: "/addama/workspaces",
        method: "GET",
        success: function(response) {
            var repos = Ext.util.JSON.decode(response.responseText);
            transformJsonRepos(repos);

            tree.setRootNode(new Ext.tree.AsyncTreeNode({
                text: 'Workspaces',
                draggable:false,
                id:'addamatreetopid',
                children: repos.items
            }));

            statusBar.displayMessage("Workspaces loaded");
        },
        failure: function(response) {
            statusBar.displayError("Failed to load workspaces");
        }
    });
}

function transformJsonRepos(data) {
    for (var i = 0; i < data.items.length; i++) {
        var item = data.items[i];
        item.text = item.label ? item.label : item.name;
        item.id = item.uri;
        item.path = "/";
        item.isRepository = true;
        item.cls = "repository";
        item.leaf = false;
        item.children = [];
    }
}

function expandNode(node) {
    if (node.id == "addamatreetopid") {
        return;
    }

    while (node.hasChildNodes()) {
        node.removeChild(node.item(0), true);
    }

    var parentPath = node.attributes.path;
    if (parentPath == "/") {
        parentPath = "";
    }

    Ext.Ajax.request({
        url: node.id,
        method: "GET",
        success: function(o) {
            var json = Ext.util.JSON.decode(o.responseText);
            if (json && json.items) {
                for (var i = 0; i < json.items.length; i++) {
                    var item = json.items[i];
                    item.text = item.label ? item.label : item.name;
                    item.id = item.uri;
                    item.path = parentPath + "/" + item.name;
                    if (item.isFile) {
                        item.leaf = true;
                        item.cls = "file";
                    } else {
                        item.leaf = false;
                        item.cls = "folder";
                        item.children = [];
                    }
                    node.appendChild(item);
                }
                node.renderChildren();
            }
        },
        failure: function() {
            node.expandable = false;
        }
    });
}

function refreshNodeTree(node) {
    if (node.isExpanded()) {
        node.collapse();
    }
    node.expand();
}
