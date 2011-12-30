package org.systemsbiology.addama.commons.spring;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author hrovira
 */
public class PropertiesFileLoaderTest {
    private static final String KEY = "looking.for.this.key";
    private static final String VALUE = "this is the value";

    private PropertiesFileLoader correct = new PropertiesFileLoader();
    private PropertiesFileLoader missingValues = new PropertiesFileLoader();
    private PropertiesFileLoader missingFile = new PropertiesFileLoader();

    @Before
    public void setUp() {
        correct.setPropertiesFile("propertiesFileLoaderTest/correct.properties");
        correct.loaded();

        missingValues.setPropertiesFile("propertiesFileLoaderTest/missingValues.properties");
        missingValues.loaded();

        missingFile.setPropertiesFile("propertiesFileLoaderTest/missingFile.properties");
        missingFile.loaded();
    }

    @Test
    public void test_loaded() {
        assertTrue(correct.loaded());
        assertTrue(missingValues.loaded());
        assertFalse(missingFile.loaded());
    }

    @Test
    public void test_has() {
        assertTrue(correct.has(KEY));
        assertFalse(missingValues.has(KEY));
        assertFalse(missingFile.has(KEY));
    }

    @Test
    public void test_getProperty() {
        assertEquals(VALUE, correct.getProperty(KEY));
        assertNull(missingValues.getProperty(KEY));
        assertNull(missingFile.getProperty(KEY));
    }
}
