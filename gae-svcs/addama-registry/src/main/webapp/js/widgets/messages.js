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
        messageEl.slideIn('t', { duration:1 }).pause(3).puff('t', { duration:1 }).ghost('t', {duration:1, remove:true});
    }
});

Ext.onReady(function() {
    org.systemsbiology.addama.js.Message = new org.systemsbiology.addama.js.MessageBox();
});