package io.tcbs.template.util;

import io.tcbs.template.enums.ErrorCode;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.context.i18n.LocaleContextHolder;

public class MessagesUtil {

    private static final String BUNDLE_BASE_NAME = "i18n/errors";

    private MessagesUtil() {}

    public static String getMessage(ErrorCode errorCode, Object... var2) {
        return getMessage(String.valueOf(errorCode.getCode()), var2);
    }

    public static String getMessage(String code, Object... var2) {
        String message;
        try {
            // 1. Tự động lấy Locale của request hiện tại từ Header Accept-Language
            Locale currentLocale = LocaleContextHolder.getLocale();

            // 2. Load bundle động theo Locale của Request
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, currentLocale);

            message = bundle.getString(code);
        } catch (MissingResourceException ex) {
            message = code;
        }
        FormattingTuple formattingTuple = MessageFormatter.arrayFormat(message, var2);
        return formattingTuple.getMessage();
    }
}
