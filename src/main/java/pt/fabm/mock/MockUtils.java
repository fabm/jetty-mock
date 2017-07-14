package pt.fabm.mock;

import groovy.lang.GroovyShell;
import groovy.lang.Script;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class MockUtils {
    private final HttpServletRequest req;
    private final HttpServletResponse resp;
    private String scriptsPath;

    public MockUtils(HttpServletRequest req, HttpServletResponse resp, Properties properties) throws IOException {
        this.req = req;
        this.resp = resp;
        scriptsPath = properties.getProperty("scripts.path");
    }

    public HttpServletRequest getReq() {
        return req;
    }

    public HttpServletResponse getResp() {
        return resp;
    }

    public Script scriptFromFile(String file) throws IOException {
        Reader reader = Files.newBufferedReader(Paths.get(scriptsPath+"/"+file+".groovy"),Charset.defaultCharset());
        GroovyShell shell = new GroovyShell();
        shell.setVariable("req",req);
        shell.setVariable("res",resp);
        return shell.parse(reader);
    }
}
