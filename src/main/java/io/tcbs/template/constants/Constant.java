package io.tcbs.template.constants;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Constant {

    private Constant() {}

    public static final String SYSTEM = "system";
    public static final String SERVICE_CODE = "template";
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    public static final ZoneId ZONE_ID = ZoneId.of("Asia/Ho_Chi_Minh");
}
