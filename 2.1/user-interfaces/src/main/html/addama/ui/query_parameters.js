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
