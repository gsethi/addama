if (!console) {
  if (!window.console || !console.firebug) {
    var names = ["log", "debug", "info", "warn", "error", "assert", "dir", "dirxml",
    "group", "groupEnd", "time", "timeEnd", "count", "trace", "profile", "profileEnd"];

    window.console = {};
    for (var i = 0; i < names.length; ++i)
        window.console[names[i]] = function() {}
  }
}

function get_parameter(paramName) {
    var hu = window.location.search.substring(1);
    var gy = hu.split("&");
    for (var i = 0; i < gy.length; i++) {
        var ft = gy[i].split("=");
        if (ft[0] == paramName) {
            return ft[1];
        }
    }
    return null;
}

trackDownloadLink = function(evt, elem) {
    try {
        var trackUri = elem.href;
        if (trackUri) {
            trackUri = trackUri.replace(".", "_");
            trackUri = trackUri.replace(" ", "_");
            _gaq.push(["_trackPageview", trackUri]);
        }
    }
    catch(err) {
    }
};
