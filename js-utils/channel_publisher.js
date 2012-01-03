ChannelPublisher = Ext.extend(Object, {
    constructor: function() {
        console.log("initializing channel");
        Ext.Ajax.request({
            url: "/addama/channels/mine",
            method: "GET",
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json) {
                    this.channelUri = json.uri;
                }
            },
            scope: this
        });

        return false;
    },

    publishMessage: function(message, callback) {
        this.publishEvent({ message: message }, callback);
    },

    publishEvent: function(event, callback) {
        Ext.Ajax.request({
            url: this.channelUri,
            method: "POST",
            params: {
                event: Ext.util.JSON.encode(event)
            },
            success: function(o) {
                callback(Ext.util.JSON.decode(o.responseText));
            }
        });
    }
});

