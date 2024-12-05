package fr.univparis1.tomcat;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.catalina.Session;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.session.StandardSession;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public class EarlySessionsUnloadManager extends StandardManager {
    private final Log log = LogFactory.getLog(EarlySessionsUnloadManager.class);

    private boolean early_unloaded = false;

    public void unload() throws IOException {
        if (early_unloaded) {
            log.debug("Not saving sessions again (since there could be a concurrency issue with another tomcat reading the sessions file) (" + getContext().getBaseName() + ")");
        } else {
            super.unload();
        }
    }

    // redo StandardManager.unload(), but do NOT expire sessions
    public void early_unload() throws IOException {
        var file = file();
       
        try (FileOutputStream fos = new FileOutputStream(file.getAbsolutePath());
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                ObjectOutputStream oos = new ObjectOutputStream(bos)) {

            synchronized (sessions) {
                if (log.isTraceEnabled()) {
                    log.trace("Unloading " + sessions.size() + " sessions to " + file + " (" + getContext().getBaseName() + ")");
                }
                // write the number of active sessions, followed by the details
                oos.writeObject(Integer.valueOf(sessions.size()));
                for (Session s : sessions.values()) {
                    StandardSession session = (StandardSession) s;
                    session.passivate();
                    session.writeObjectData(oos);
                }
            }
            early_unloaded = true;
        }
    }

    static Map<String, String> pathname2context = new HashMap<>();

    protected File file() {
        if (pathname == null) throw new RuntimeException("pathname is mandatory for EarlySessionsUnloadManager");

        var file = new File(pathname);
        if (file.isAbsolute()) {
            var contextName = getContext().getBaseName();
            if (file.isDirectory()) {
                // new feature: allow putting SESSIONS.ser anywhere you want (otherwise it must be in WorkDir)
                return new File(file, "SESSIONS-" + contextName + ".ser");
            } else {
                var prev = pathname2context.get(pathname);
                if (prev != null && !prev.equals(contextName)) {
                    log.error("You can not share same absolute \"pathname\" for multiple <Manager>s (found same \"pathname\" for " + contextName + " and " + prev + ")");
                }
                pathname2context.put(pathname, contextName);
            }
        }
        return super.file();
    }
}