package org.systemsbiology.addama.commons.gae;

import static com.google.apphosting.api.ApiProxy.getCurrentEnvironment;
import static org.apache.commons.lang.StringUtils.substringBeforeLast;

/**
 * @author hrovira
 */
public class Appspot {
    public static final String APP_ID;
    public static final String APPSPOT_ID;
    public static final String APPSPOT_URL;

    static {
        APPSPOT_ID = (String) getCurrentEnvironment().getAttributes().get("com.google.appengine.runtime.default_version_hostname");
        APP_ID = substringBeforeLast(APPSPOT_ID, ".appspot.com");
        APPSPOT_URL = "https://" + APPSPOT_ID;
    }

}
