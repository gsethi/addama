package org.systemsbiology.addama.services.execution.jobs;

import org.json.JSONException;
import org.json.JSONObject;
import org.systemsbiology.addama.jsonconfig.impls.GenericMapJsonConfigHandler;

import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import static java.lang.Integer.parseInt;

/**
 * @author hrovira
 */
public class ReturnCodesConfigHandler extends GenericMapJsonConfigHandler<ReturnCodes> {
    private static final Logger log = Logger.getLogger(ReturnCodesConfigHandler.class.getName());

    public ReturnCodesConfigHandler(Map<String, ReturnCodes> returnCodesByUri) {
        super(returnCodesByUri, "returnCodes");
    }

    public ReturnCodes getSpecific(JSONObject item) throws Exception {
        try {
            JSONObject returnCodes = item.getJSONObject("returnCodes");
            ReturnCodes rcs = new ReturnCodes(returnCodes.optInt("success", 0));
            if (returnCodes.has("unknownReason")) {
                rcs.setUnknownReason(returnCodes.getString("unknownReason"));
            }
            addReasonsByErrorCode(rcs, returnCodes);
            return rcs;
        } catch (Exception e) {
            log.warning(e.getMessage());
        }
        return null;
    }

    private void addReasonsByErrorCode(ReturnCodes rcs, JSONObject returnCodes) throws JSONException {
        if (returnCodes.has("errors")) {
            JSONObject errors = returnCodes.getJSONObject("errors");
            Iterator itr = errors.keys();
            while (itr.hasNext()) {
                String code = (String) itr.next();

                rcs.addReasonByErrorCode(parseInt(code), errors.getString(code));
            }
        }
    }
}
