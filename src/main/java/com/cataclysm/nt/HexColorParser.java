package com.cataclysm.nt;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HexColorParser {
    private static final Pattern HEX = Pattern.compile("(?i)#([0-9a-f]{6})");

    private HexColorParser() {}

    public static String convert(String input) {
        Matcher matcher = HEX.matcher(input);
        StringBuffer out = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement.toString()));
        }

        matcher.appendTail(out);
        return out.toString();
    }
}
