package com.studyassistant;

import java.io.IOException;
import java.sql.*;
import java.util.UUID;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/api/auth/*")
public class AuthServlet extends HttpServlet {

    private void signIn(HttpServletRequest req, HttpServletResponse res, int id, String name) throws IOException {
        HttpSession old = req.getSession(false);
        if (old != null) old.invalidate();
        HttpSession s = req.getSession(true);
        s.setAttribute("uid", id);
        s.setAttribute("name", name);
        Util.json(res, 200, "{\"id\":" + id + ",\"name\":" + Util.q(name) + "}");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute("uid") == null) {
            Util.json(res, 401, "{\"error\":\"Not signed in\"}");
            return;
        }
        Util.json(res, 200, "{\"id\":" + s.getAttribute("uid") + ",\"name\":" + Util.q((String) s.getAttribute("name")) + "}");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        req.setCharacterEncoding("UTF-8");
        String p = String.valueOf(req.getPathInfo());
        if (p.equals("/logout")) {
            HttpSession s = req.getSession(false);
            if (s != null) s.invalidate();
            Util.json(res, 200, "{}");
            return;
        }
        String email = Util.val(req, "email").toLowerCase();
        String pw = Util.val(req, "password");
        try (Connection c = DB.get()) {
            if (p.equals("/register")) {
                String name = Util.val(req, "name");
                if (name.isEmpty() || !email.contains("@") || pw.length() < 6) {
                    Util.json(res, 400, "{\"error\":\"Enter your name, a valid email and a password of at least 6 characters.\"}");
                    return;
                }
                String salt = UUID.randomUUID().toString().substring(0, 8);
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO users(name,email,pass_hash) VALUES(?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, name);
                    ps.setString(2, email);
                    ps.setString(3, salt + ":" + Util.hash(salt, pw));
                    ps.executeUpdate();
                    ResultSet k = ps.getGeneratedKeys();
                    k.next();
                    signIn(req, res, k.getInt(1), name);
                } catch (SQLException e) {
                    if (e.getErrorCode() == 1062) Util.json(res, 409, "{\"error\":\"That email is already registered. Try signing in.\"}");
                    else throw e;
                }
            } else if (p.equals("/login")) {
                try (PreparedStatement ps = c.prepareStatement("SELECT id,name,pass_hash FROM users WHERE email=?")) {
                    ps.setString(1, email);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        String[] h = rs.getString(3).split(":");
                        if (h[1].equals(Util.hash(h[0], pw))) {
                            signIn(req, res, rs.getInt(1), rs.getString(2));
                            return;
                        }
                    }
                    Util.json(res, 401, "{\"error\":\"Email or password is incorrect.\"}");
                }
            } else {
                Util.json(res, 404, "{\"error\":\"Unknown action\"}");
            }
        } catch (SQLException e) {
            Util.json(res, 500, "{\"error\":" + Util.q("Database error: " + e.getMessage()) + "}");
        }
    }
}
