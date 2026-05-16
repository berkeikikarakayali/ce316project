package com.ce316.iae.util;

import com.ce316.iae.db.JsonArrayCodec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Parses project-level argv extras stored in {@link com.ce316.iae.model.Project#getRunArgs()}.
 * Accepts JSON arrays (preferred) or legacy whitespace-separated tokens.
 */
public final class RunArgsCodec {

    private RunArgsCodec() {}

    public static List<String> parseProjectRunArgs(String stored) {
        if (stored == null || stored.isBlank()) {
            return Collections.emptyList();
        }
        String s = stored.trim();
        if (s.startsWith("[")) {
            try {
                return JsonArrayCodec.decode(s);
            } catch (IllegalArgumentException ignored) {
                return Collections.singletonList(stored);
            }
        }
        return Arrays.asList(s.split("\\s+"));
    }

    public static String formatProjectRunArgs(List<String> args) {
        if (args == null || args.isEmpty()) {
            return JsonArrayCodec.encode(List.of());
        }
        return JsonArrayCodec.encode(new ArrayList<>(args));
    }
}
