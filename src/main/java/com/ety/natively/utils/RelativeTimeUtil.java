package com.ety.natively.utils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 相对时间计算器
 */
public class RelativeTimeUtil {

    public static String getRelativeTime(LocalDateTime fromDateTime, LocalDateTime toDateTime) {
        if (fromDateTime.isAfter(toDateTime)) {
            return "未来";
        }
        
        long years = ChronoUnit.YEARS.between(fromDateTime, toDateTime);
        if (years > 0) {
            if (years == 1) {
                return "1年前";
            } else if (years == 2) {
                return "2年前";
            } else {
                return years + "年前";
            }
        }

        long months = ChronoUnit.MONTHS.between(fromDateTime, toDateTime);
        if (months > 0) {
            if (months == 1) {
                return "1个月前";
            } else if (months == 6) {
                return "半年前";
            } else {
                return months + "个月前";
            }
        }

        long weeks = ChronoUnit.WEEKS.between(fromDateTime, toDateTime);
        if (weeks > 0) {
            if (weeks == 1) {
                return "1周前";
            } else {
                return weeks + "周前";
            }
        }

        long days = ChronoUnit.DAYS.between(fromDateTime, toDateTime);
        if (days > 0) {
            if (days == 1) {
                return "1天前";
            } else if (days == 2) {
                return "2天前";
            } else {
                return days + "天前";
            }
        }

        long hours = ChronoUnit.HOURS.between(fromDateTime, toDateTime);
        if (hours > 0) {
            if (hours == 1) {
                return "1小时前";
            } else {
                return hours + "小时前";
            }
        }

        long minutes = ChronoUnit.MINUTES.between(fromDateTime, toDateTime);
        if (minutes > 0) {
            if (minutes == 1) {
                return "1分钟前";
            } else {
                return minutes + "分钟前";
            }
        }

        return "刚刚";
    }
}
