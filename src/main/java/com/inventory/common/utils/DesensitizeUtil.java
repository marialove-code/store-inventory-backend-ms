package com.inventory.common.utils;

public class DesensitizeUtil {

    /**
     * 手机号脱敏
     */
    public static String mobile(String phone) {

        if (phone == null || phone.length() != 11) {
            return phone;
        }

        return phone.substring(0, 3)
                + "****"
                + phone.substring(7);
    }
}