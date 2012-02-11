package org.systemsbiology.addama.services.execution.jobs;

import org.json.JSONException;
import org.json.JSONObject;
import org.systemsbiology.addama.jsonconfig.Mapping;
import org.systemsbiology.addama.jsonconfig.MappingsHandler;
import org.systemsbiology.addama.jsonconfig.impls.MappingPropertyByIdContainer;

import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import static java.lang.Integer.parseInt;

/**
 * @author hrovira
 */
public class ReturnCodesMappingsHandler extends MappingPropertyByIdContainer<ReturnCodes> implements MappingsHandler {
    private static final Logger log = Logger.getLogger(ReturnCodesMappingsHandler.class.getName());

    public ReturnCodesMappingsHandler(Map<String, ReturnCodes> returnCodesByUri) {
        super(returnCodesByUri);
    }

    public void handle(Mapping mapping) throws Exception {
        try {
            JSONObject item = mapping.JSON();
            JSONObject returnCodes = item.getJSONObject("returnCodes");
            ReturnCodes rcs = new ReturnCodes(returnCodes.optInt("success", 0));
            if (returnCodes.has("unknownReason")) {
                rcs.setUnknownReason(returnCodes.getString("unknownReason"));
            }
            addReasonsByErrorCode(rcs, returnCodes);
            addValue(mapping, rcs);
        } catch (Exception e) {
            log.warning(e.getMessage());
        }
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
