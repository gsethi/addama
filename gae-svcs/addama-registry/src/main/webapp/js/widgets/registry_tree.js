TreeLoader = function() {
    this.collectedItems = new Array();
    this.queue = new Array();

    this.generateTree = function(rootItem, callback) {
        this.collectedItems.push(rootItem);
        this.queueCompleted = callback;

        Ext.Ajax.addListener('requestcomplete', this.handleQueue, this, { delay: 300 });

        this.makeRequest(rootItem.uri, this.queue);
    };

    this.handleQueue = function() {
        if (this.queue.length > 0) {
            var item = this.queue.shift();
            if (item && item.uri) {
                this.makeRequest(item.uri, this.collectedItems);
            }
        } else {
            Ext.Ajax.removeListener('requestcomplete', this.handleQueue);
            this.queueCompleted(this.collectedItems);
        }
    };

    this.makeRequest = function(uri, itemArray) {
        Ext.Ajax.request({
            url: uri,
            method: "GET",
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json && json.items) {
                    for (var i = 0; i < json.items.length; i++) {
                        itemArray.push(json.items[i]);
                    }
                }
            }
        });
    };
};
