package io.tcbs.template.constants;

public class UrlExternal {

    private UrlExternal() {}

    public static final String TEST_THREAD = "/api/v1/thread";
    public static final String TEST_ASYNC_VIRTUAL_THREAD = "/api/v1/thread/virtual";
    public static final String TEST_ASYNC_PLATFORM_THREAD = "/api/v1/thread/platform";

    public static final String TEST_PATH = "/api/v1/test";
    public static final String TEST_V2_PATH = "/api/v2/test";
    public static final String TEST_PATH_VARIABLE = "/api/v1/test/{id}";
    public static final String TEST_REQUEST_PARAM = "/api/v1/test/request-param";
}
