package pt.fabm.mock;


import groovy.lang.GroovyShell;
import groovy.lang.Script;
import groovy.servlet.AbstractHttpServlet;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Properties;
import java.util.logging.Logger;

public class JettyMock {
    private Script script;
    private static Logger LOGGER = Logger.getGlobal();
    private Server server;


    public void start() throws Exception {
        final Properties properties = new Properties();
        final InputStream systemResourceAsStream = ClassLoader.getSystemResourceAsStream("config.properties");
        properties.load(new InputStreamReader(systemResourceAsStream));


        final String keystorePath = Loader.create(properties.getProperty("keystore")).resolve();

        if (keystorePath != null) {
            server = new Server();
            ServerConnector connector = new ServerConnector(server);

            int httpPort = Integer.parseInt(properties.getProperty("http.port"));
            connector.setPort(httpPort);
            server.addConnector(connector);

            HttpConfiguration https = new HttpConfiguration();
            https.addCustomizer(new SecureRequestCustomizer());
            SslContextFactory sslContextFactory = new SslContextFactory();

            sslContextFactory.setKeyStorePath(Loader.create(keystorePath).resolve());
            KeyStore ts = KeyStore.getInstance("jks");

            String pass = properties.getProperty("keystore.pass");

            LOGGER.info("Keystore file = " + Loader.create(keystorePath).resolve());

            ts.load(Loader.create(properties.getProperty("keystore")).asStream(), pass.toCharArray());
            sslContextFactory.setKeyStorePassword(pass);
            sslContextFactory.setKeyManagerPassword(pass);

            ServerConnector sslConnector = new ServerConnector(server,
                    new SslConnectionFactory(sslContextFactory, "http/1.1"),
                    new HttpConnectionFactory(https));
            int sslPort = Integer.parseInt(properties.getProperty("https.port"));
            sslConnector.setPort(sslPort);
            server.addConnector(sslConnector);
        } else {
            server = new Server(Integer.parseInt(properties.getProperty("http.port")));
        }

        server.setRequestLog((request, response) -> {
            LOGGER.info(request.getMethod()+":"+request.getPathInfo());
        });

        ServletContextHandler context = new ServletContextHandler(
                ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        context.addServlet(new ServletHolder(new AbstractHttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                script.getBinding().setVariable("utils", new MockUtils(req, resp, properties));
                script.getBinding().setVariable("logger", LOGGER);
                Object result = script.run();
                if (result != null) {
                    resp.getWriter().write(result.toString());
                }
            }
        }), "/mock/*");


        context.addServlet(new ServletHolder(new AbstractHttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                GroovyShell shell = new GroovyShell();
                StringWriter writer = new StringWriter();
                IOUtils.copy(req.getReader(), writer);
                String content = writer.toString();
                script = shell.parse(content);
                resp.getWriter().println("script registed");
            }
        }), "/setup/*");

        context.addServlet(new ServletHolder(new AbstractHttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                resp.setContentType("application/javascript");
                resp.getWriter().println("{\"hello\":\"world\"}");
            }
        }), "/hello");

        LOGGER.info("http://localhost:" + properties.getProperty("http.port") + "/hello");
        LOGGER.info("https://localhost:" + properties.getProperty("https.port") + "/hello");

        server.start();

    }

    public static void main(String[] args) throws Exception {
        final JettyMock jettyMock = new JettyMock();
        jettyMock.start();
        jettyMock.server.join();
    }

}
