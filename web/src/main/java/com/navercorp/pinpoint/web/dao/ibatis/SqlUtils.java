package com.navercorp.pinpoint.web.dao.ibatis;

import java.util.StringTokenizer;

/**
 * query 처리용 util 클래스
 *
 * @author Web Platform Development Lab
 * @author emeroad
 * @since 1.7.4
 */
public final class SqlUtils {
    private SqlUtils() {
    }

    /**
     * query의 빈줄<code>"&nbsp;&#92;t&#92;n&#92;r&#92;f"</code>을 모두 제거하여 한줄로 만든다.
     * @param original 원본 query
     * @return 빈줄이 제거된 query
     */
    public static String removeBreakingWhitespace(String original) {
        StringTokenizer whitespaceStripper = new StringTokenizer(original);
        StringBuilder builder = new StringBuilder();
        while (whitespaceStripper.hasMoreTokens()) {
            builder.append(whitespaceStripper.nextToken());
            builder.append(" ");
        }
        return builder.toString();
    }
}
