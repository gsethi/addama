var ChromosomeRangeControl = Class.create({
    initialize: function(container, referenceGenomeUri) {
        this.container = Ext.getDom(container);
        this.referenceGenomeUri = referenceGenomeUri;
        this.selectionListeners = new Array();
        this.chromosomes = new Hash();
        this.selectedChromosome = "none";
        this.geneSymbol = '';
        this.startPosition = 0;
        this.endPosition = 0;
        
        this.container_chromosomes = this.container.id + "_chromosomes";
        this.container_rangecontrol = this.container.id + "_rangecontrol";
        this.container_searchbtn = this.container.id + "_genesearch";
        this.container_flexbar = this.container.id + "_flexbar";

        this.loadControl();
        this.loadChromosomes();
    },

    addSelectionListener: function(listener) {
        this.selectionListeners[this.selectionListeners.length] = listener;
    },

    loadControl: function() {
        var html = "";
        html += "<div id='" + this.container_chromosomes + "'>Loading...</div>";
        html += "<div id='" + this.container_rangecontrol + "'>"
        html += "<div id='" + this.container_flexbar + "'></div></div>";
        html += "<div id='" + this.container_searchbtn + "'></div>";
        this.container.innerHTML = html;
    },

    loadChromosomes: function() {
        var control = this;

        Ext.Ajax.request({
            url: this.referenceGenomeUri,
            method: "get",
            success: function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json && json.items) {
                    for (var i = 0; i < json.items.length; i++) {
                        var item = json.items[i];
                        control.chromosomes.set(item.name, item.uri);
                    }
                }

                control.displayChromosomes();
            }
        });
    },

    displayChromosomes: function() {
        var control = this;

        Ext.getDom(control.container_chromosomes).innerHTML = "";

        var radioButtonArray = new Array();
        this.chromosomes.each(function(chromosome) {
            var radioConfig = new Ext.form.Radio({
                boxLabel: chromosome.key,
                name: 'rb-chromosome'
            });
            radioConfig.on('check',function(cb){
                control.selectedChromosome = cb.boxLabel;
                control.displayRangeControl();
            });
            
           radioButtonArray[radioButtonArray.length] = radioConfig;

        });



        var radioPanel = new Ext.form.FormPanel({
            height: 150,
            width: "100%",
            frame: true,
            renderTo: control.container_chromosomes,
            items: [{
                xtype: 'fieldset',
                autoHeight: true,
                layout: 'form',
                items:[{
                    xtype:'radiogroup',
                    items:radioButtonArray
                },{
                    layout: 'column',
                    columns: 2,
                    items:[ {
                        contentEl: this.container_flexbar
                    },{
                        xtype:'button',
                        text: 'Go',
                        handler: function(){
                            control.publishSelection(control.startPosition, control.endPosition);
                        }
                    }]
                }]
            }]
        });


    },

    displayRangeControl: function() {
        var control = this;

         Ext.Ajax.request({
             url: this.referenceGenomeUri + "/" + this.selectedChromosome,
             method:"get",
             success:function(o) {
                var json = Ext.util.JSON.decode(o.responseText);
                if (json && json.length) {
                    control.startPosition = 0;
                    control.endPosition = json.length;
                    control.renderFlexScroll(0,json.length);
                }
             }
         });
    },

    publishSelection: function(start, end) {
        var chromRangeUri = this.selectedChromosome + "/" + start + "/" + end;
        this.selectionListeners.each(function(listener) {
            listener.onRangeSelection(chromRangeUri);
        });
    },

    publishGeneSelection: function() {
        var control = this;

        this.selectionListeners.each(function(listener) {
            listener.onGeneSelection(control.geneSymbol);
        });

        //also need to make call to get Gene Specifics and update flex Scroll
       /* Ext.Ajax.request({
            url: control.referenceGenomeUri + "/" + control.geneSymbol + "/genes",
                    method:"get",
                    success:function(o) {
                        var json = Ext.util.JSON.decode(o.responseText);
                        if (json && json.length) {
                            control.startPosition = 0;
                            control.endPosition = json.length;
                           control.renderFlexScroll(0,json.length);
                        }
                    }
                });
*/
    },                         

    renderFlexScroll: function(minPosition,maxPosition){
        var control = this;

        var listener = function(x,dx){
            control.startPosition = x;
            control.endPosition =  parseInt(x) + parseInt(dx);
        }

        var flexbar = new org.systemsbiology.visualization.protovis.FlexScroll( Ext.getDom(this.container_flexbar), listener);

        var data ={DATATYPE : "org.systemsbiology.visualization.protovis.models.FlexScrollData", CONTENTS : "test"};


        var options = {plotWidth : 700, plotHeight: 50,
           verticalPadding : 10, horizontalPadding: 30, font :"sans", minPosition: Math.round(minPosition / 1000) ,
           maxPosition: Math.round(maxPosition / 1000), scaleMultiplier : 1000};

        flexbar.draw(data,options)
    }
});
