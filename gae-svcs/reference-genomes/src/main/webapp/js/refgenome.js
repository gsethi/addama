var referenceGenomeService = {
    getChromUri: function(build, chromosome, start, end) {
        var chromUri = "/refgenome";
        if (!build) {
            return chromUri;
        }
        chromUri += "/" + build;
        if (!start) {
            return chromUri;
        }
        chromUri += "/" + start;
        if (!end) {
            return chromUri;
        }
        chromUri += "/" + end;
        return chromUri;
    },

    getSequence: function (chromosomeUri, sequenceCallback) {
        Ext.Ajax.request({
            url: chromosomeUri,
            method: "get",
            success: sequenceCallback
        });
    },

    getGeneInformation: function(geneIdentity, geneCallback) {
        this.loadItems("/refgenome/genes/" + geneIdentity, geneCallback);
    },

    getGeneByChromosomeUri: function(chromuri, geneCallback) {
        this.loadItems(chromuri + "/genes", geneCallback);
    },

    loadBuilds: function(buildItemCallback) {
        this.loadItems("/refgenome", buildItemCallback);
    },

    loadChromosomes: function(buildUri, chromItemCallback) {
        this.loadItems(buildUri, chromItemCallback);
    },

    getChromosomeInformation: function(chromUri, chromCallback) {
        this.loadChrom(chromUri, chromCallback);
    },

    loadChrom: function (uri, itemCallback) {
        Ext.Ajax.request({
            url: uri,
            method:"get",
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json.length && json.name) { itemCallback(json); }
            }
        });
    },

    loadItems: function (uri, itemCallback) {
        Ext.Ajax.request({
            url: uri,
            method:"get",
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json && json.items) {
                    for (var i = 0; i < json.items.length; i++) {
                        itemCallback(json.items[i]);
                    }
                }
            }
        });
    }
};