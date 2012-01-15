Ext.ns("org.systemsbiology.addama.js");

org.systemsbiology.addama.js.Message = null;

org.systemsbiology.addama.js.MessageBox = Ext.extend(Object, {
    constructor: function(config) {
        Ext.apply(this, config);

        org.systemsbiology.addama.js.MessageBox.superclass.constructor.call(this);

        this.messageContainer = Ext.DomHelper.insertFirst(document.body, {id:'container_js_message'}, true);
    },

    show: function(title, format) {
        var s = Ext.String.format.apply(String, Array.prototype.slice.call(arguments, 1));
        var msgBox = '<div class="msg"><h3>' + title + '</h3><p>' + s + '</p></div>';
        var m = Ext.DomHelper.append(this.messageContainer, msgBox, true);
        m.hide();
        m.slideIn('t').ghost("t", { delay: 1000, remove: true});
    },

    error: function(title, format) {
        var s = Ext.String.format.apply(String, Array.prototype.slice.call(arguments, 1));
        var msgBox = '<div class="x-status-error"><h3>' + title + '</h3><p>' + s + '</p></div>';
        var m = Ext.DomHelper.append(this.messageContainer, msgBox, true);
        m.hide();
        m.slideIn('t').ghost("t", { delay: 1000, remove: true});
    }
});

Ext.onReady(function() {
    org.systemsbiology.addama.js.Message = new org.systemsbiology.addama.js.MessageBox();
});