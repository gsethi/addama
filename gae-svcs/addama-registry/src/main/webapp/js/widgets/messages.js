Ext.ns("org.systemsbiology.addama.js");

org.systemsbiology.addama.js.Message = null;

org.systemsbiology.addama.js.MessageBox = Ext.extend(Object, {
    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.MessageBox.superclass.constructor.call(this);

        this.messageContainer = Ext.DomHelper.insertFirst(document.body, {id:'container_js_message'}, true);
    },

    show: function(title, message) {
        this.display(title, "msg", message);
    },

    error: function(title, message) {
        this.display(title, "x-status-error", message);
    },

    display: function(title, divClass, message) {
        var msgBox = '<div class="' + divClass + '"><h3>' + title + '</h3><p>' + message + '</p></div>';
        var messageEl = Ext.DomHelper.append(this.messageContainer, msgBox, true);
        messageEl.hide();
        messageEl.slideIn('t', { duration:1 }).pause(2).puff('t', { duration:1 });
    }
});

org.systemsbiology.addama.js.ChannelAsMessages = Ext.extend(Object, {
    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.ChannelAsMessages.superclass.constructor.call(this);

        new org.systemsbiology.addama.js.ChannelListener({
            listeners: {
                open: function() {
                    org.systemsbiology.addama.js.Message.show("Channels", "Broadcasted events will be shown here");
                },
                message: function(a) {
                    if (a && a.data && a.data.message) {
                        org.systemsbiology.addama.js.Message.show("Message", a.data.message);
                    }
                }
            }
        });
    }
});

Ext.onReady(function() {
    org.systemsbiology.addama.js.Message = new org.systemsbiology.addama.js.MessageBox();
});