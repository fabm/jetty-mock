package pt.fabm.mock;


import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.Log;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

public class CaptureScript {
    private static String scriptSource;

    public static void main(String[] args) throws Exception {
        Server server = new Server(8083);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                scriptSource = IOUtils.toString(req.getReader());
                resp.getWriter().println("ok");
            }
        }), "/capture-script");

        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                GroovyShell shell = new GroovyShell();

                Script script = shell.parse(scriptSource);

                Binding binding = new Binding();
                binding.setVariable("request", req);
                binding.setVariable("log", Log.getLog());
                script.setBinding(binding);

                resp.getWriter().write(script.run().toString());
            }

        }), "/mock/*");


        server.start();
        server.join();

    }
}
