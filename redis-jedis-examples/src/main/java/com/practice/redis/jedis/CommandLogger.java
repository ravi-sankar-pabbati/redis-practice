package com.practice.redis.jedis;

/**
 * Prints each Redis command next to its result, in the style of {@code redis-cli}:
 *
 * <pre>
 * redis&gt; SET demo:greeting "Hello"                  -&gt; OK
 * </pre>
 *
 * <p>{@link #log} returns the result it was given, so you can wrap a Jedis call in it
 * and still use the value: {@code String v = log("GET k", jedis.get("k"));}
 */
public final class CommandLogger {

    private CommandLogger() {
    }

    /** Prints a section header, e.g. "=== Strings ===". */
    public static void section(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    /** Prints the command and its result, then returns the result unchanged. */
    public static <T> T log(String command, T result) {
        System.out.printf("redis> %-50s -> %s%n", command, format(result));
        return result;
    }

    /** Prints a free-form note, used for explanations between commands. */
    public static void note(String text) {
        System.out.println("       # " + text);
    }

    private static String format(Object result) {
        if (result == null) {
            return "(nil)";  // redis-cli shows a missing value as (nil)
        }
        if (result instanceof String s && !"OK".equals(s) && !"PONG".equals(s)) {
            return "\"" + s + "\"";
        }
        return String.valueOf(result);
    }
}
