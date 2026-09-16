package io.tcbs.template.util;

import java.util.UUID;

public class UuidGeneratorUtil {

    private UuidGeneratorUtil() {}

    public static String randomUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
