EventManager = function(config){
    config = config || {};
    if (config.initialConfig) {
        config = config.initialConfig;
    }

    this.initialConfig = config;

    Ext.apply(this, config);
    this.addEvents(
        "node-refresh",
        "display-status-message"
    );
    EventManager.superclass.constructor.call(this);
};

Ext.extend(EventManager, Ext.util.Observable, { ctype: "EventManager" });

