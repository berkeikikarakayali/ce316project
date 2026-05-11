package com.ce316.iae.db;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal codec for the {@code List<String>} columns stored as JSON TEXT
 * (compile_args, run_args, diff_lines). Keeps the runtime free of an
 * external JSON dependency. Only supports a flat array of strings.
 */
public final class JsonArrayCodec {

    private JsonArrayCodec() {}

    public static String encode(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder(items.size() * 8);
        sb.append('[');
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(escape(items.get(i))).append('"');
        }
        sb.append(']');
        return sb.toString();
    }

    public static List<String> decode(String json) {
        List<String> out = new ArrayList<>();
        if (json == null) return out;
        String s = json.trim();
        if (s.isEmpty() || s.equals("[]")) return out;
        if (s.charAt(0) != '[' || s.charAt(s.length() - 1) != ']') {
            throw new IllegalArgumentException("Not a JSON array: " + json);
        }
        int i = 1;
        int end = s.length() - 1;
        while (i < end) {
            while (i < end && (s.charAt(i) == ' ' || s.charAt(i) == ',')) i++;
            if (i >= end) break;
            if (s.charAt(i) != '"') {
                throw new IllegalArgumentException("Expected '\"' at position " + i + " in: " + json);
            }
            i++;
            StringBuilder val = new StringBuilder();
            while (i < end && s.charAt(i) != '"') {
                char c = s.charAt(i);
                if (c == '\\' && i + 1 < end) {
                    char next = s.charAt(i + 1);
                    switch (next) {
                        case '"':  val.append('"');  break;
                        case '\\': val.append('\\'); break;
                        case 'n':  val.append('\n'); break;
                        case 't':  val.append('\t'); break;
                        case 'r':  val.append('\r'); break;
                        default:   val.append(next);
                    }
                    i += 2;
                } else {
                    val.append(c);
                    i++;
                }
            }
            out.add(val.toString());
            i++;
        }
        return out;
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 4);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\t': sb.append("\\t");  break;
                case '\r': sb.append("\\r");  break;
                default:   sb.append(c);
            }
        }
        return sb.toString();
    }
}
