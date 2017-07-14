package pt.fabm.mock;


import groovy.lang.GroovyShell;
import groovy.lang.Script;
import groovy.servlet.AbstractHttpServlet;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

public class CaptureScript {
    private static Script script;

    public static void main(String[] args) throws Exception {

        final Properties properties = new Properties();
        properties.load(new InputStreamReader(ClassLoader.getSystemResourceAsStream("config.properties")));

        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(9999);
        HttpConfiguration https = new HttpConfiguration();
        https.addCustomizer(new SecureRequestCustomizer());
        server.addConnector(connector);


        final String keystore = properties.getProperty("keystore");
        if (keystore != null) {
            SslContextFactory sslContextFactory = new SslContextFactory();
            sslContextFactory.setKeyStorePath(keystore);
            sslContextFactory.setKeyStorePassword("changeit");
            sslContextFactory.setKeyManagerPassword("changeit");
            ServerConnector sslConnector = new ServerConnector(server,
                    new SslConnectionFactory(sslContextFactory, "http/1.1"),
                    new HttpConnectionFactory(https));
            sslConnector.setPort(9998);
            server.addConnector(sslConnector);
        }

        ServletContextHandler context = new ServletContextHandler(
                ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        context.addServlet(new ServletHolder(new AbstractHttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                script.getBinding().setVariable("utils", new MockUtils(req, resp, properties));
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
                script = shell.parse(req.getReader());
                resp.getWriter().println("script registed");
            }
        }), "/setup/*");

        server.start();
        server.join();


    }
}
