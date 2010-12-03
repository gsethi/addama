Ext.IframeWindow = Ext.extend(Ext.Window, {
    onRender: function() {
        this.bodyCfg = {
            tag: 'iframe',
            src: this.src,
            cls: this.bodyCls,
            style: {
                border: '0px none'
            }
        };
        Ext.IframeWindow.superclass.onRender.apply(this, arguments);
    }
});

SelectFileControl = Ext.extend(Object, {
    setWorkspace: function(workspaceLabel) {
        this.workspaceLabel = workspaceLabel;        
    },

    activate: function(style) {
        if (!this.workspaceUri) {
            var control = this;
            Ext.Ajax.request({
               url: "/addama/users/whoami",
               success: function(o) {
                   var email = Ext.util.JSON.decode(o.responseText).email;
                   control.workspaceUri = "/addama/workspaces/" + control.workspaceLabel + "/" + email;
                   control.loadSelectWindow(style);
               }
            });
        } else {
            this.loadSelectWindow();
        }
    },

    loadSelectWindow: function(style) {
        if (!style) {
            style = {
                width: 800,
                height: 700,
                title: "Select File from Workspace"                
            };
        }

        style.closable = true;
        style.plain = true;
        style.src = "/addama/ui/workspaces/selectFileControl.html?workspace=" + this.workspaceUri;

        this.controlWindow = new Ext.IframeWindow(style);
        this.controlWindow.show();       
    },

    onSelectFile: function(callback) {
        this.selectFileCallback = callback;
    },

    selectFile: function(fileUri) {
        if (this.selectFileCallback) {
            this.selectFileCallback(fileUri);
        }
        if (this.controlWindow) {
            this.controlWindow.close();
        }
    }
});
