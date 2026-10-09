# StudyPilot – Smart Study Assistant (Java Servlets + MySQL on WampServer)

## 1. Database (WampServer)
1. Start WampServer until the tray icon is **green**.
2. Open http://localhost/phpmyadmin (user `root`, empty password).
3. Go to **Import** and choose `database/schema.sql`, or paste it in the SQL tab and run it.

## 2. Open in NetBeans
1. File > Open Project > select the `SmartStudyAssistant` folder (NetBeans recognises the Maven project).
2. Make sure a server is registered: Tools > Servers > Add Server > **Apache Tomcat 9** (javax.servlet; do NOT use Tomcat 10+).
3. Right-click the project > Properties > Run > Server = Tomcat 9.
4. If your WAMP MySQL is not on port 3306 or has a password, edit `src/main/java/com/studyassistant/DB.java`.
5. Right-click the project > **Clean and Build**, then **Run**.
6. Open http://localhost:8080/StudyAssistant/

## 3. Deploy
Build produces `target/StudyAssistant.war`. Copy it to the `webapps` folder of any Tomcat 9 server that can reach the MySQL database.

## Notes
- Passwords are salted SHA-256 hashes. For production, switch to bcrypt/Argon2 and use HTTPS.
- Create a dedicated MySQL user instead of `root` before putting this online.
