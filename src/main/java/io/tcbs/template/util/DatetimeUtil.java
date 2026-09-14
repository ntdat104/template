package io.tcbs.template.util;

import io.tcbs.template.constants.Constant;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import org.springframework.context.i18n.LocaleContextHolder;

public class DatetimeUtil {

    public static final DateTimeFormatter DEFAULT_FORMATTER = Constant.DATE_TIME_FORMATTER;
    public static final ZoneId DEFAULT_ZONE_ID = Constant.ZONE_ID;

    private DatetimeUtil() {}

    public static ZonedDateTime toDefaultZone(Instant instant) {
        return toZonedDateTime(instant, DEFAULT_ZONE_ID);
    }

    public static ZonedDateTime toZonedDateTime(Instant instant, ZoneId zoneId) {
        if (instant == null) return null;
        return instant.atZone(Objects.requireNonNullElse(zoneId, DEFAULT_ZONE_ID));
    }

    public static String formatNow() {
        return format(Instant.now());
    }

    public static String format(Instant instant) {
        return format(instant, DEFAULT_FORMATTER, DEFAULT_ZONE_ID);
    }

    public static String format(Instant instant, DateTimeFormatter formatter) {
        return format(instant, formatter, DEFAULT_ZONE_ID);
    }

    public static String format(Instant instant, DateTimeFormatter formatter, ZoneId zoneId) {
        if (instant == null) return null;
        DateTimeFormatter fmt = Objects.requireNonNullElse(formatter, DEFAULT_FORMATTER);
        ZoneId zone = Objects.requireNonNullElse(zoneId, DEFAULT_ZONE_ID);
        return instant.atZone(zone).format(fmt);
    }

    public static Long toEpochMilli(Instant instant) {
        return instant != null ? instant.toEpochMilli() : null;
    }

    public static String toTimeAgo(Long epochMilli) {
        return toTimeAgo(toInstant(epochMilli));
    }

    public static String toTimeAgo(Long epochMilli, Locale locale) {
        return toTimeAgo(toInstant(epochMilli), locale);
    }

    public static Instant toInstant(Long epochMilli) {
        return epochMilli != null ? Instant.ofEpochMilli(epochMilli) : null;
    }

    public static String toTimeAgo(Instant instant) {
        Locale currentLocale = LocaleContextHolder.getLocale();
        return toTimeAgo(instant, Instant.now(), currentLocale);
    }

    public static String toTimeAgo(Instant instant, Locale locale) {
        return toTimeAgo(instant, Instant.now(), locale);
    }

    public static String toTimeAgo(Instant instant, Instant relativeTo, Locale locale) {
        if (instant == null) return null;
        if (relativeTo == null) relativeTo = Instant.now();
        if (locale == null) locale = LocaleContextHolder.getLocale();

        boolean isVi = "vi".equalsIgnoreCase(locale.getLanguage());
        Duration duration = Duration.between(instant, relativeTo);
        long seconds = duration.getSeconds();

        // 1. Xử lý thời gian trong tương lai (Future)
        if (seconds < 0) {
            long absSeconds = Math.abs(seconds);
            if (absSeconds < 60) return isVi ? "vừa xong" : "just now";
            if (absSeconds < 3600) return isVi ? absSeconds / 60 + " phút nữa" : "in " + absSeconds / 60 + " minutes";
            if (absSeconds < 86400) return isVi ? absSeconds / 3600 + " giờ nữa" : "in " + absSeconds / 3600 + " hours";
            return isVi ? absSeconds / 86400 + " ngày nữa" : "in " + absSeconds / 86400 + " days";
        }

        // 2. Xử lý thời gian trong quá khứ (Past)
        if (seconds < 30) {
            return isVi ? "vừa xong" : "just now";
        }
        if (seconds < 60) {
            return isVi ? seconds + " giây trước" : seconds + " seconds ago";
        }

        long minutes = seconds / 60;
        if (minutes < 60) {
            return isVi ? minutes + " phút trước" : minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        }

        long hours = minutes / 60;
        if (hours < 24) {
            return isVi ? hours + " giờ trước" : hours + (hours == 1 ? " hour ago" : " hours ago");
        }

        long days = hours / 24;
        if (days < 30) {
            return isVi ? days + " ngày trước" : days + (days == 1 ? " day ago" : " days ago");
        }

        long months = days / 30;
        if (months < 12) {
            return isVi ? months + " tháng trước" : months + (months == 1 ? " month ago" : " months ago");
        }

        long years = days / 365;
        return isVi ? years + " năm trước" : years + (years == 1 ? " year ago" : " years ago");
    }
}
