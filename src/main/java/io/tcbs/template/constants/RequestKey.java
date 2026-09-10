package io.tcbs.template.constants;

public class RequestKey {

    private RequestKey() {}

    // Đăng nhập vào Merchant Site để tạo mã. (VD: b7bdf002-4948-44d2-99d1-99c8c81c3f47)
    public static final String X_CLIENT_ID = "X-Client-ID";

    // Thời gian tạo đơn hàng (miliseconds) (VD: 1788862690664)
    public static final String X_TIMESTAMP = "X-Timestamp";

    // Sử dụng với mỗi request từ merchant, có giá trị sử dụng duy nhất trong vòng 2h tính từ thời
    // điểm hệ thống nhận được request. (VD: 00a81e60-2684-4cf9-878d-f37559213059)
    public static final String X_NONCE = "X-Nonce";

    // Dữ liệu xác thực của đơn hàng. (VD:
    // pKD58qVfyFYe2nqUCa2oZFQZp74r2YT6yugohQJxIcK5XdLP+FbKijuiHQah9+cCD1y/XvJ9Rb35Cjkoz12pcpQlGA2xTJz+F4VKaWmI33+dh3As8eZugAMtGfrBGZooyvWI/JQJgtU+DM9JhxR02MJjO56w/AUqLzuPaIbhkJYOexNNs/Ad9GA2X2dJdpDs7WW37MN5jUgbv1BM1l+fLOiGCCj/If9hiWkNZM+EBYLoKe7xlrwp6H3FR4ubrt125/xFgpujZVIh4MInf7dStf5g+MjDV1H99im3ZbeWFUYsNGzFY/JQOkmKQwvNFhPoOi2GNJw4ZJa5XjhYAbS/IA==)
    public static final String X_SIGNATURE = "X-Signature";

    // ID của request từ đối tác. (VD: 9ef0ad2bd759450295236a1424ba6033)
    public static final String X_REQUEST_ID = "X-Request-ID";

    public static final String REQUEST_ID = "request_id";
    public static final String SERVICE_NAME = "service_name";
    public static final String SERVICE_VERSION = "service_version";
    public static final String START_TIME = "start_time";
    public static final String END_TIME = "end_time";
    public static final String EXECUTION_TIME = "execution_time";
    public static final String METHOD = "method";
    public static final String PATH = "path";
    public static final String QUERY_STRING = "query_string";
    public static final String REQUEST_PARAMS = "request_params";
    public static final String CODE_FILE = "code_file";
    public static final String MESSAGE_TYPE = "message_type";
    public static final String REQUEST = "request";
    public static final String RESPONSE = "response";
    public static final String API_KEY = "api_key";
    public static final String API_SECRET = "api_secret";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String SIGNATURE = "signature";
    public static final String STATUS = "status";
    public static final String CLIENT_IP = "client_ip";
    public static final String USER_AGENT = "user_agent";
    public static final String CODE = "code";
    public static final String MESSAGE = "message";
    public static final String TIMESTAMP = "timestamp";
    public static final String DATETIME = "datetime";
    public static final String REQUEST_BODY = "request_body";
    public static final String RESPONSE_BODY = "response_body";
    public static final String ERRORS = "errors";
    public static final String HEADERS = "headers";
    public static final String REQUEST_PARAMETERS = "request_parameters";
    public static final String URI = "uri";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String X_API_KEY = "x-api-key";
    public static final String X_API_SECRET = "x-api-secret";
    public static final String HOST = "host";
    public static final String JOB_ID = "job_id";
    public static final String JOB_TIME = "job_time";
    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String WHITE_LABEL = "whiteLabel";
    public static final String CHANNEL = "channel";
    public static final String GRANT_TYPE = "grant_type";
    public static final String IS_SERVER_REQUEST_ID = "is_server_request_id";
    public static final String X_SERIAL_NUMBER = "x-serial-number";
    public static final String CHUBBLIFE_APP_ID = "app_id";
    public static final String CHUBBLIFE_APP_KEY = "app_key";
    public static final String CHUBBLIFE_API_VERSION = "api_version";
    public static final String RESOURCE = "resource";
    public static final String CHUBBLIFE_AUTHORIZATION = "Authorization";
    public static final String SERIAL_DEVICE = "serial_device";
}
