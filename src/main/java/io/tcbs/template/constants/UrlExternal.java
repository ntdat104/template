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

    public static final String TEST_CAFFEIN_CACHE_PUT = "/api/v1/caffein/put";
    public static final String TEST_CAFFEIN_CACHE_GET_IF_PRESENT = "/api/v1/caffein/get-if-present";
    public static final String TEST_CAFFEIN_CACHE_GET = "/api/v1/caffein/get";
    public static final String TEST_CAFFEIN_CACHE_INVALIDATE = "/api/v1/caffein/invalidate";

    public static final String TEST_RANDOMUSER_GENERATE = "/api/v1/randomuser/generate";

    public static final String TEST_SYSTEM_CONFIG_PATH = "/api/v1/system/config";

    public static final String TEST_REDIS_PATH = "/api/v1/redis/{id}";
    public static final String TEST_REDIS_MANUAL_PATH = "/api/v1/redis/manual/{id}";
    public static final String TEST_REDIS_DEMO_PATH = "/api/v1/redis/demo";

    public static final String TEST_SEMAPHORE_LOCAL_PATH = "/api/v1/semaphore/local/{orderId}";
    public static final String TEST_SEMAPHORE_LOCAL_FALLBACK_PATH = "/api/v1/semaphore/local/fallback/{orderId}";
    public static final String TEST_SEMAPHORE_LOCAL_NOTIFY_PATH = "/api/v1/semaphore/local/notify/{userId}";
    public static final String TEST_SEMAPHORE_LOCAL_BURST_PATH = "/api/v1/semaphore/local/burst";

    public static final String TEST_SEMAPHORE_DISTRIBUTED_PATH = "/api/v1/semaphore/distributed/{orderId}";
    public static final String TEST_SEMAPHORE_DISTRIBUTED_BURST_PATH = "/api/v1/semaphore/distributed/burst";
    public static final String TEST_SEMAPHORE_DISTRIBUTED_BLOCKING_PATH = "/api/v1/semaphore/distributed/blocking/{orderId}";
    public static final String TEST_SEMAPHORE_DISTRIBUTED_BLOCKING_BURST_PATH = "/api/v1/semaphore/distributed/blocking/burst";
}
