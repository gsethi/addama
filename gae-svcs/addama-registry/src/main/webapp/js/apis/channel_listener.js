ChannelListener = Ext.extend(Ext.util.Observable, {
    constructor: function(config) {
        this.numberOfReopens = 0;
        this.maxReopens = 10;
        this.listeners = [];
        this.channelUri = "/addama/channels/mine";
        this.addEvents('open', 'message', 'error', 'close');

        if (!config) config = {};
        
        Ext.apply(this, config);

        ChannelListener.superclass.constructor.call(this, config);

        this.on({
            "open": function() {
                console.log("channel: opened");
            },
            "message": function() {
                console.log("channel: message");
            },
            "error": function() {
                console.log("channel: error");
            },
            "close": function() {
                console.log("channel: closed");
            }
        });

        this.on("close", function() {
            if (this.numberOfReopens++ < this.maxReopens) {
                console.log("Reopening Channel");
                this.openChannel();
            } else {
                console.log("Exceeded max re-open tries");
            }
        }, this);

        this.openChannel();
    },

    openChannel: function() {
        Ext.Ajax.request({
            url: this.channelUri,
            method: "GET",
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json && json.token) {
                    var channel = new goog.appengine.Channel(json.token);
                    var socket = channel.open();

                    var observable = this;
                    socket.onopen = function(a) {
                        observable.fireEvent('open', a);
                    };
                    socket.onmessage = function(a) {
                        observable.fireEvent('message', a);
                    };
                    socket.onerror = function(a) {
                        observable.fireEvent('error', a);
                    };
                    socket.onclose = function(a) {
                        observable.fireEvent('close', a);
                    };
                }
            },
            scope: this
        });
    }
});



