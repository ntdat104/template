package io.tcbs.template.util;

import io.tcbs.template.constants.RequestKey;
import org.slf4j.MDC;

public class ContextUtil {

    private ContextUtil() {}

    public static String getRequestId() {
        return MDC.get(RequestKey.X_REQUEST_ID);
    }
}
