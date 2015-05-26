package ly.stealth.punxsutawney;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.log4j.*;

import java.io.File;
import java.net.URI;

import static java.lang.System.err;
import static java.lang.System.out;

public class Cli {
    public static void main(String... args) throws Exception {
        initLogging();

        try { run(args); }
        catch (Error e) {
            out.println();
            err.println("Error: " + Util.uncapitalize(e.getMessage()));
            System.exit(1);
        }
    }

    private static void run(String... args) throws Exception {
        Marathon.App app = new Marathon.App();

        OptionParser parser = new OptionParser();
        parser.accepts("marathon", "marathon url (http://master:8080)").withRequiredArg().required().ofType(String.class);
        parser.accepts("listen", "listen url to download jar (http://192.168.3.1:5000)").withRequiredArg().required().ofType(String.class);

        parser.accepts("id", "app id").withOptionalArg().ofType(String.class).defaultsTo(app.id);
        parser.accepts("instances", "number of servers").withOptionalArg().ofType(Integer.class).defaultsTo(app.instances);
        parser.accepts("cpus", "amount of cpu to use").withOptionalArg().ofType(Double.class).defaultsTo(app.cpus);
        parser.accepts("mem", "amount of memory to use").withOptionalArg().ofType(Integer.class).defaultsTo(app.mem);
        parser.accepts("help", "show this help");

        OptionSet options;
        try {
            options = parser.parse(args);
        } catch (OptionException e) {
            parser.printHelpOn(out);
            throw new Error(e.getMessage());
        }

        if (options.has("help")) {
            parser.printHelpOn(out);
            return;
        }

        Marathon.url = (String) options.valueOf("marathon");
        String listen = (String) options.valueOf("listen");

        app.uris.add(listen + "/jar/punxsutawney.jar");
        app.id = (String) options.valueOf("id");
        app.instances = (int) options.valueOf("instances");

        app.cpus = (double) options.valueOf("cpus");
        app.mem = (int) options.valueOf("mem");
        app.cmd = "while true; do echo 'Hello World'; sleep 1; done";


        HttpServer httpServer = new HttpServer();
        httpServer.setPort(new URI(listen).getPort());
        httpServer.setJar(findJar());
        httpServer.start();

        if (Marathon.hasApp(app.id))
            throw new Error("App \"" + app.id + "\" is already running");

        out.println("Starting app \"" + app.id + "\" ...");
        Marathon.startApp(app);

        Thread.sleep(60000);

        out.println("Stopping app \"" + app.id + "\" ...");
        Marathon.stopApp(app.id);
        httpServer.stop();
    }

    private static void initLogging() {
        System.setProperty("org.eclipse.jetty.util.log.class", HttpServer.JettyLog4jLogger.class.getName());
        BasicConfigurator.resetConfiguration();

        Logger root = Logger.getRootLogger();
        root.setLevel(Level.INFO);

        Logger.getLogger("org.eclipse.jetty").setLevel(Level.WARN);
        Logger.getLogger("jetty").setLevel(Level.WARN);

        PatternLayout layout = new PatternLayout("%d [%t] %-5p %c %x - %m%n");
        root.addAppender(new ConsoleAppender(layout));
    }

    @SuppressWarnings("ConstantConditions")
    private static File findJar() {
        String mask = "punxsutawney.*\\.jar";

        for (File file : new File(".").listFiles())
            if (file.getName().matches(mask)) return file;

        throw new IllegalStateException("No " + mask + " found in . folder");
    }

    public static class Error extends java.lang.Error {
        public Error(String message) { super(message); }
    }
}
