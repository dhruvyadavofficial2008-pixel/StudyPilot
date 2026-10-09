package com.studyassistant;

import java.io.IOException;
import java.sql.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

/** Notes, flashcards and progress stats for the signed-in student. */
@WebServlet({"/api/notes", "/api/cards", "/api/stats"})
public class StudyServlet extends HttpServlet {

    private Integer uid(HttpServletRequest r) {
        HttpSession s = r.getSession(false);
        return s == null ? null : (Integer) s.getAttribute("uid");
    }

    private int count(Connection c, String sql, int u) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, u);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        Integer u = uid(req);
        if (u == null) { Util.json(res, 401, "{\"error\":\"Please sign in.\"}"); return; }
        String p = req.getServletPath();
        try (Connection c = DB.get()) {
            StringBuilder out = new StringBuilder();
            if (p.endsWith("stats")) {
                out.append("{\"notes\":").append(count(c, "SELECT COUNT(*) FROM notes WHERE user_id=?", u))
                   .append(",\"cards\":").append(count(c, "SELECT COUNT(*) FROM cards WHERE user_id=?", u))
                   .append(",\"known\":").append(count(c, "SELECT COUNT(*) FROM cards WHERE user_id=? AND known=1", u))
                   .append("}");
            } else {
                boolean notes = p.endsWith("notes");
                String sql = notes
                    ? "SELECT id,subject,title,content,DATE_FORMAT(created_at,'%d %b %Y') FROM notes WHERE user_id=? AND (title LIKE ? OR subject LIKE ? OR content LIKE ?) ORDER BY id DESC"
                    : "SELECT id,subject,question,answer,known FROM cards WHERE user_id=? AND (question LIKE ? OR subject LIKE ? OR answer LIKE ?) ORDER BY id DESC";
                String q = "%" + Util.val(req, "q") + "%";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, u);
                    for (int i = 2; i <= 4; i++) ps.setString(i, q);
                    ResultSet rs = ps.executeQuery();
                    out.append("[");
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) out.append(",");
                        first = false;
                        out.append("{\"id\":").append(rs.getInt(1))
                           .append(",\"subject\":").append(Util.q(rs.getString(2)))
                           .append(notes ? ",\"title\":" : ",\"question\":").append(Util.q(rs.getString(3)))
                           .append(notes ? ",\"content\":" : ",\"answer\":").append(Util.q(rs.getString(4)))
                           .append(notes ? ",\"date\":" + Util.q(rs.getString(5)) : ",\"known\":" + (rs.getInt(5) == 1))
                           .append("}");
                    }
                    out.append("]");
                }
            }
            Util.json(res, 200, out.toString());
        } catch (SQLException e) {
            Util.json(res, 500, "{\"error\":" + Util.q("Database error: " + e.getMessage()) + "}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        req.setCharacterEncoding("UTF-8");
        Integer u = uid(req);
        if (u == null) { Util.json(res, 401, "{\"error\":\"Please sign in.\"}"); return; }
        String p = req.getServletPath();
        boolean notes = p.endsWith("notes");
        String table = notes ? "notes" : "cards";
        String a = Util.val(req, "action");
        try (Connection c = DB.get()) {
            if (a.equals("add")) {
                String x = Util.val(req, notes ? "title" : "question");
                String y = Util.val(req, notes ? "content" : "answer");
                if (x.isEmpty() || y.isEmpty()) { Util.json(res, 400, "{\"error\":\"Please fill in both fields.\"}"); return; }
                String sql = notes
                    ? "INSERT INTO notes(user_id,subject,title,content) VALUES(?,?,?,?)"
                    : "INSERT INTO cards(user_id,subject,question,answer) VALUES(?,?,?,?)";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, u);
                    ps.setString(2, Util.val(req, "subject"));
                    ps.setString(3, x);
                    ps.setString(4, y);
                    ps.executeUpdate();
                }
            } else if (a.equals("delete")) {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM " + table + " WHERE id=? AND user_id=?")) {
                    ps.setInt(1, Integer.parseInt(Util.val(req, "id")));
                    ps.setInt(2, u);
                    ps.executeUpdate();
                }
            } else if (a.equals("known") && !notes) {
                try (PreparedStatement ps = c.prepareStatement("UPDATE cards SET known=? WHERE id=? AND user_id=?")) {
                    ps.setInt(1, "1".equals(Util.val(req, "value")) ? 1 : 0);
                    ps.setInt(2, Integer.parseInt(Util.val(req, "id")));
                    ps.setInt(3, u);
                    ps.executeUpdate();
                }
            } else {
                Util.json(res, 400, "{\"error\":\"Unknown action\"}");
                return;
            }
            Util.json(res, 200, "{\"ok\":true}");
        } catch (NumberFormatException e) {
            Util.json(res, 400, "{\"error\":\"Bad id\"}");
        } catch (SQLException e) {
            Util.json(res, 500, "{\"error\":" + Util.q("Database error: " + e.getMessage()) + "}");
        }
    }
}
