import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import pt.fabm.mock.JettyMock;
import pt.fabm.mock.Loader;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.security.KeyStore;
import java.util.Properties;
import java.util.logging.Logger;

public class TestDynamicMock {
    private static final Logger LOGGER = Logger.getGlobal();
    private static final boolean START_SERVER = true;
    private static WebTarget webTarget;

    private static WebTarget createClient() throws Exception {
        if (webTarget != null) {
            return webTarget;
        }
        if (START_SERVER) {
            final JettyMock jettyMock = new JettyMock();
            jettyMock.start();
        }
        Properties properties = new Properties();
        properties.load(TestDynamicMock.class.getResourceAsStream("/config.properties"));

        KeyStore keyStore = KeyStore.getInstance("jks");
        final String pass = properties.getProperty("keystore.pass");
        keyStore.load(
                Loader.create(properties.getProperty("keystore")).asStream(),
                pass.toCharArray()
        );


        Gson gson = new Gson();
        Client client = ClientBuilder.newBuilder()
                //with the hostnameVerifier returning always true the CN is never checked
                .hostnameVerifier((name, sslSession) -> true)
                .keyStore(keyStore, pass)
                .trustStore(keyStore)
                .register(new MessageBodyWriter<String>() {
                    @Override
                    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
                        return true;
                    }

                    @Override
                    public long getSize(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
                        return s.length();
                    }

                    @Override
                    public void writeTo(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
                        entityStream.write(s.getBytes());
                        entityStream.flush();

                    }
                })
                .register(new MessageBodyReader<Object>() {
                    @Override
                    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
                        return true;
                    }

                    @Override
                    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
                        if (JsonElement.class.equals(type)) {
                            JsonParser jsonParser = new JsonParser();
                            StringWriter writer = new StringWriter();
                            IOUtils.copy(new InputStreamReader(entityStream), writer);
                            return jsonParser.parse(writer.toString());
                        } else if (String.class.equals(type)) {
                            StringWriter writer = new StringWriter();
                            IOUtils.copy(new InputStreamReader(entityStream), writer);
                            return writer.toString();
                        }
                        return gson.fromJson(new InputStreamReader(entityStream), type);
                    }
                })
                .build();

        final String uri = "https://localhost:" + properties.getProperty("https.port");
        LOGGER.info("URI:" + uri);
        webTarget = client.target(uri);
        return webTarget;
    }

    @Test
    public void helloTest() throws Exception {
        WebTarget target = createClient().path("/hello");
        Pojo pojo = target.request().buildGet().invoke(Pojo.class);
        Assert.assertEquals("world", pojo.getHello());
    }

    @Test
    public void mockSetupAndTest() throws Exception {
        WebTarget target = createClient();

        target.path("/setup")
                .request()
                .buildPost(Entity.entity("utils.load('loginJson').run()", MediaType.TEXT_PLAIN)).submit(String.class)
                .get();

        JsonElement element = target.path("/mock")
                .request()
                .buildGet()
                .invoke(JsonElement.class);

        String message = element
                .getAsJsonObject().get("response")
                .getAsJsonObject().get("message")
                .getAsString();

        Assert.assertEquals("fail", message);


        element = target.path("/mock")
                .queryParam("login", "my-name")
                .queryParam("pass", "my-pass")
                .request()
                .buildGet()
                .invoke(JsonElement.class);

        message = element
                .getAsJsonObject().get("response")
                .getAsJsonObject().get("message")
                .getAsString();

        Assert.assertEquals("successful", message);

    }

}
