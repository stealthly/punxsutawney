package ly.stealth.punxsutawney;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class HttpServer {
    private int port = 5000;
    private File jar;

    private Server server;

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public File getJar() { return jar; }
    public void setJar(File jar) { this.jar = jar; }


    public synchronized void start() throws Exception {
        if (server != null) throw new IllegalStateException("started");

        QueuedThreadPool threadPool = new QueuedThreadPool(16);
        threadPool.setName("Jetty");

        server = new Server(threadPool);
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        connector.setIdleTimeout(60 * 1000);

        ServletContextHandler handler = new ServletContextHandler();
        handler.addServlet(new ServletHolder(new Servlet()), "/");

        server.setHandler(handler);
        server.addConnector(connector);
        server.start();
    }

    public synchronized void stop() throws Exception {
        if (server == null) throw new IllegalStateException("!started");

        server.stop();
        server.join();
        server = null;
    }

    public static class JettyLog4jLogger implements org.eclipse.jetty.util.log.Logger {
        private Logger logger;

        public JettyLog4jLogger() { this(Logger.getLogger("jetty")); }
        public JettyLog4jLogger(Logger logger) { this.logger = logger; }

        public boolean isDebugEnabled() { return logger.isDebugEnabled(); }
        public void setDebugEnabled(boolean enabled) { logger.setLevel(enabled ? Level.DEBUG : Level.INFO); }

        public String getName() { return logger.getName(); }
        public JettyLog4jLogger getLogger(String name) { return new JettyLog4jLogger(Logger.getLogger(name)); }

        public void info(String s, Object... args) { logger.info(format(s, args)); }
        public void info(String s, Throwable t) { logger.info(s, t); }
        public void info(Throwable t) { logger.info("", t); }

        public void debug(String s, Object... args) { logger.debug(format(s, args)); }
        public void debug(String s, Throwable t) { logger.debug(s, t); }
        public void debug(Throwable t) { logger.debug("", t); }

        public void warn(String s, Object... args) { logger.warn(format(s, args)); }
        public void warn(String s, Throwable t) { logger.warn(s, t); }
        public void warn(Throwable t) { logger.warn("", t); }

        public void ignore(Throwable t) { logger.info("Ignored", t); }

        private static String format(String s, Object... args) {
            String result = "";
            int i = 0;

            for (String token : s.split("\\{\\}")) {
                result += token;
                if (args.length > i) result += args[i];
                i += 1;
            }

            return result;
        }
    }

    private class Servlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            String uri = request.getRequestURI();
            if (uri.startsWith("/jar")) downloadFile(jar, response);
            else response.sendError(404);
        }

        private void downloadFile(File file, HttpServletResponse response) throws IOException {
            response.setContentType("application/zip");
            response.setHeader("Content-Length", "" + file.length());
            response.setHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
            Util.copyAndClose(new FileInputStream(file), response.getOutputStream());
        }
    }
}

