package com.studyassistant;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Util {
    /** Quote and escape a string for JSON. */
    public static String q(String s) {
        if (s == null) return "null";
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break;
                case '\t': b.append("\\t"); break;
                default: b.append(c < 32 ? ' ' : c);
            }
        }
        return b.append('"').toString();
    }

    public static String hash(String salt, String pw) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest((salt + pw).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : d) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String val(HttpServletRequest r, String name) {
        String v = r.getParameter(name);
        return v == null ? "" : v.trim();
    }

    public static void json(HttpServletResponse r, int code, String body) throws IOException {
        r.setStatus(code);
        r.setContentType("application/json;charset=UTF-8");
        r.getWriter().write(body);
    }
}
