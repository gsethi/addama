package org.systemsbiology.addama.commons.spring;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class PropertiesFileLoader {
    private static final Logger log = Logger.getLogger(PropertiesFileLoader.class.getName());

    private String propertiesFile;

    private Properties properties;
    private boolean lockedAndLoaded;


    public void setPropertiesFile(String propertiesFile) {
        this.propertiesFile = propertiesFile;
    }

    public String getProperty(String key) {
        if (this.properties != null) {
            return this.properties.getProperty(key);
        }
        return null;
    }

    public synchronized boolean loaded() {
        if (properties == null) {
            this.properties = new Properties();

            try {
                ClassPathResource resource = new ClassPathResource(propertiesFile);
                if (!resource.exists()) {
                    log.warning("properties file not found in classpath [" + this.propertiesFile + "]");
                    return this.lockedAndLoaded = false;
                }

                this.properties.load(resource.getInputStream());
                return this.lockedAndLoaded = true;
            } catch (IOException e) {
                log.warning("properties file not loaded [" + this.propertiesFile + "]:" + e.getMessage());
                return this.lockedAndLoaded = false;
            }
        }
        return this.lockedAndLoaded;
    }

    public boolean has(String key) {
        return this.properties != null && this.properties.containsKey(key);
    }
}
