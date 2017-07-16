package pt.fabm.mock;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class Loader {
    private String path;
    private final static String RESOURCE_PREFIX = "classpath:";

    public static Loader create(String path){
        return new Loader(path);
    }

    private Loader(String path) {
        this.path = path;
    }

    public String resolve() {
        if (path == null){
            return null;
        }
        if (isClassLoaderPath()){
            return ClassLoader.getSystemResource(getClassloaderPath()).toExternalForm();
        }else{
            return path;
        }
    }

    private boolean isClassLoaderPath(){
        return path.startsWith(RESOURCE_PREFIX);
    }

    private String getClassloaderPath(){
        return path.substring(RESOURCE_PREFIX.length());
    }

    public InputStream asStream() {
        if (isClassLoaderPath()) {
            return ClassLoader.getSystemResourceAsStream(getClassloaderPath());
        } else {
            try {
                return new FileInputStream(path);
            } catch (FileNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
