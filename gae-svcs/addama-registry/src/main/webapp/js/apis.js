Ext.ns("org.systemsbiology.addama.js.apis");


Ext.ns("org.systemsbiology.addama.js.apis.channels");

org.systemsbiology.addama.js.apis.channels.Listener = null;

org.systemsbiology.addama.js.apis.channels.Observable = Ext.extend(Ext.util.Observable, {
    constructor: function(config) {
        this.numberOfReopens = 0;
        this.maxReopens = 10;
        this.listeners = [];
        this.channelUri = "/addama/channels/mine";
        this.addEvents('open', 'message', 'error', 'close');

        if (!config) config = {};

        Ext.apply(this, config);

        org.systemsbiology.addama.js.apis.channels.Observable.superclass.constructor.call(this, config);

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

org.systemsbiology.addama.js.apis.channels.Publisher = Ext.extend(Object, {
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

org.systemsbiology.addama.js.apis.channels.MessageListener = Ext.extend(Object, {
    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.apis.channels.MessageListener.superclass.constructor.call(this);

        org.systemsbiology.addama.js.apis.channels.Listener.on("open", function() {
            if (org.systemsbiology.addama.js.Message) {
                org.systemsbiology.addama.js.Message.show("Channels", "Broadcasted events will be shown here");
            } else {
                console.log("messages will not be displayed, import messages.js");
            }
        });
        org.systemsbiology.addama.js.apis.channels.Listener.on("message", function(a) {
            if (a && a.data) {
                var event = Ext.util.JSON.decode(a.data);
                if (event && event.message) {
                    if (org.systemsbiology.addama.js.Message) {
                        var title = "Message";
                        if (event.title) {
                            title = event.title;
                        }
                        org.systemsbiology.addama.js.Message.show(title, event.message);
                    } else {
                        console.log("messages will not be displayed, import messages.js: " + event.message);
                    }
                }
            }
        });
    }
});

Ext.onReady(function() {
    org.systemsbiology.addama.js.apis.channels.Listener = new org.systemsbiology.addama.js.apis.channels.Observable();
});

