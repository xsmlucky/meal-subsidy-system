package com.mealsubsidy.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public final class BizNoUtil {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private BizNoUtil() {
    }

    public static String orderNo() {
        return "MO" + FMT.format(LocalDateTime.now()) + random4();
    }

    public static String logNo() {
        return "AL" + FMT.format(LocalDateTime.now()) + random4();
    }

    public static String refundNo() {
        return "RF" + FMT.format(LocalDateTime.now()) + random4();
    }

    private static String random4() {
        return String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
}
