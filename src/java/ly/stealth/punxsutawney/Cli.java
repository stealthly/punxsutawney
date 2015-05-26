package ly.stealth.punxsutawney;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.log4j.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

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
        Options options = new Options(args);
        out.println("Options:\n" + options);

        Marathon.url = "" + options.marathon;

        Marathon.App app = new Marathon.App();
        app.uris.add(options.listen + "/jar/punxsutawney.jar");
        app.id = options.id;
        app.instances = options.instances;

        app.cpus = options.cpus;
        app.mem = options.mem;
        app.cmd = "while true; do echo 'Hello World'; sleep 1; done";

        HttpServer httpServer = new HttpServer();
        httpServer.setPort(options.listen.getPort());
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

    private static class Options {
        public URL marathon;
        public URL listen;

        public String id;
        public int instances;

        public double cpus;
        public int mem;

        Options(String... args) {
            Marathon.App app = new Marathon.App();

            OptionParser parser = new OptionParser();
            parser.accepts("marathon", "marathon url (http://master:8080)").withRequiredArg().ofType(String.class);
            parser.accepts("listen", "listen url to download jar (http://192.168.3.1:5000)").withRequiredArg().ofType(String.class);

            parser.accepts("id", "app id").withOptionalArg().ofType(String.class).defaultsTo(app.id);
            parser.accepts("instances", "number of servers").withOptionalArg().ofType(Integer.class).defaultsTo(app.instances);
            parser.accepts("cpus", "amount of cpu to use").withOptionalArg().ofType(Double.class).defaultsTo(app.cpus);
            parser.accepts("mem", "amount of memory to use").withOptionalArg().ofType(Integer.class).defaultsTo(app.mem);

            OptionSet options;
            try {
                options = parser.parse(args);
            } catch (OptionException e) {
                try { parser.printHelpOn(out); } catch (IOException ignore) {}
                throw new Error(e.getMessage());
            }

            Properties props = new Properties();

            File file = new File("punxsutawney.properties");
            if (file.exists())
                try (InputStream stream = new FileInputStream(file)) {
                    props.load(stream);
                } catch (IOException e) {
                    throw new Error(e.getMessage());
                }

            String marathonValue = (String)options.valueOf("marathon");
            if (marathonValue == null) marathonValue = props.getProperty("marathon");
            if (marathonValue == null) throw new Error("Undefined marathon option");
            try { marathon = new URL(marathonValue); }
            catch (MalformedURLException e) { throw new Error("Invalid marathon value: " + marathonValue); }

            String listenValue = (String)options.valueOf("listen");
            if (listenValue == null) listenValue = props.getProperty("listen");
            if (listenValue == null) throw new Error("Undefined listen option");
            try { listen = new URL(listenValue); }
            catch (MalformedURLException e) { throw new Error("Invalid listen value: " + listenValue); }

            id = (String)options.valueOf("id");
            instances = (Integer)options.valueOf("instances");

            cpus = (double)options.valueOf("cpus");
            mem = (int)options.valueOf("mem");
        }

        public String toString() {
            String s = "";
            s += "marathon:" + marathon + ", listen:" + listen;
            s += "\nid:" + id + ", instances:" + instances + ", cpus:" + cpus+ ", mem:" + mem;
            return s;
        }
    }

    private static class Error extends java.lang.Error {
        public Error(String message) { super(message); }
    }
}
