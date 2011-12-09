package org.systemsbiology.addama.commons.gae;

import static com.google.apphosting.api.ApiProxy.getCurrentEnvironment;

/**
 * @author hrovira
 */
public class Appspot {
    public static final String APP_ID;
    public static final String APPSPOT_ID;
    public static final String APPSPOT_URL;

    static {
        APP_ID = (String) getCurrentEnvironment().getAttributes().get("com.google.appengine.runtime.default_version_hostname");
        APPSPOT_ID = APP_ID + ".appspot.com";
        APPSPOT_URL = "https://" + APPSPOT_ID;
    }

}
