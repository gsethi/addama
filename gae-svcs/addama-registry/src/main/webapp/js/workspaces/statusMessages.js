var statusBar = new Ext.ux.StatusBar({
    text: "Ready",
    id: 'basic-statusbar',
    iconCls: "x-status-valid"
});
statusBar.displayMessage = function(message) {
    statusBar.setStatus({
        text: message,
        iconCls: "x-status-valid",
        clear: {
            wait: 5000,
            anim: false,
            useDefaults: false
        }
    });
};
statusBar.displayError = function(message) {
    statusBar.setStatus({
        text: message,
        iconCls: "x-status-error",
        clear: {
            wait: 5000,
            anim: false,
            useDefaults: false
        }
    });
};