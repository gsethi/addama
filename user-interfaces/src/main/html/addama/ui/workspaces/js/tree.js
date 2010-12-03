var tree = new Ext.tree.TreePanel({
    id: "tree-panel",
    title: "Repositories",
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

    Ext.Ajax.request({
        url: "/addama/workspaces",
        method: "GET",
        success: loadWorkspaces,
        failure: function(response) {
            eventManager.fireStatusMessageEvent({ text: "Failed to load workspaces", level: "error" });
        }
    });
}

function loadWorkspaces(response) {
    var repos = Ext.util.JSON.decode(response.responseText);
    transformJsonRepos(repos);

    tree.addListener("click", eventManager.fireNodeSelectEvent, eventManager);
    tree.addListener("expandnode", expandNode, {single: true});

    var root = new Ext.tree.AsyncTreeNode({
        text: 'Repositories',
        draggable:false,
        id:'addamatreetopid',
        children: repos.items
    });
    tree.setRootNode(root);
    tree.render();
}

function transformJsonRepos(data) {
    for (var i = 0; i < data.items.length; i++) {
        var item = data.items[i];
        item.text = item.label ? item.label : item.name;
        item.id = item.uri;
        item.path = item.uri;
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
                    item.path = item.uri;
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
