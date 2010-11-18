var RangeWidget = Class.create({
    initialize: function(container) {
        this.container = container;
        this.eventListeners = [];
        this.load();
    },

    load: function() {
        var eventPublisher = this;
        Ext.Ajax.request({
            url: "/addama/refgenome/hg18/chr1",
            method: "get",
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json && json.length) {
                    eventPublisher.drawWidget(0, json.length);
                }
            }
        })
    },

    drawWidget: function(min, max) {
        var eventPublisher = this;

        var startValue = parseInt((max - min) / 2);
        var offset = parseInt((max - min) / 100);
        var leftValue = startValue - offset;
        var rightValue = startValue + offset;

        new Ext.Slider({
            renderTo: this.container,
            width   : "90%",
            minValue: min,
            maxValue: max,
            values  : [leftValue, rightValue],
            plugins : new Ext.slider.Tip(),
            listeners: {
                dragend: function() {
                    eventPublisher.calculateRange(this);
                }
            }

        });
    },

    calculateRange: function(slider) {
        var values = slider.getValues();
        this.publishEvent({ chromosomeUri: "/addama/refgenome/hg18/chr1/" + values[0] + "/" + values[1] });
    },

    addEventListener: function(listener) {
        this.eventListeners[this.eventListeners.length] = listener;
    },

    publishEvent: function(event) {
        if (this.eventListeners.length) {
            for (var i = 0; i < this.eventListeners.length; i++) {
                this.eventListeners[i](event);
            }
        }
    }
});

