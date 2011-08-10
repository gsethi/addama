package org.systemsbiology.addama.commons.spring;

import static org.junit.Assert.*;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.systemsbiology.addama.commons.spring.mock.MockBean;

import java.util.Map;

/**
 * @author hrovira
 */
public class BootstrapTest {
    @Test
    public void single() {
        ApplicationContext appCtx = Bootstrap.loadApplicationContext("test-bootstrap.xml");
        assertEquals(3, appCtx.getBeanDefinitionCount());
        assertCoolBeans(appCtx);
    }

    @Test
    public void multiple() {
        ApplicationContext appCtx = Bootstrap.loadApplicationContext("test-bootstrap.xml", "test-bootstrap-2.xml");
        assertEquals(6, appCtx.getBeanDefinitionCount());
        assertCoolBeans(appCtx);
    }

    @Test
    public void duplicate() {
        ApplicationContext appCtx = Bootstrap.loadApplicationContext("test-bootstrap.xml", "test-bootstrap.xml");
        assertEquals(3, appCtx.getBeanDefinitionCount());
        assertCoolBeans(appCtx);
    }

    @Test
    public void empty() {
        ApplicationContext appCtx = Bootstrap.loadApplicationContext("");
        assertEquals(0, appCtx.getBeanDefinitionCount());
    }

    @Test
    public void none() {
        ApplicationContext appCtx = Bootstrap.loadApplicationContext();
        assertEquals(0, appCtx.getBeanDefinitionCount());
    }

    @Test
    public void nullInput() {
        ApplicationContext appCtx = Bootstrap.loadApplicationContext(null);
        assertEquals(0, appCtx.getBeanDefinitionCount());
    }

    /*
     * Private Methods
     */
    private void assertCoolBeans(ApplicationContext appCtx) {
        Map<String, MockBean> map = (Map<String, MockBean>) appCtx.getBeansOfType(MockBean.class);
        assertFalse(map.isEmpty());

        for (Map.Entry<String, MockBean> entry : map.entrySet()) {
            MockBean mockBean = entry.getValue();
            assertNotNull(mockBean);
            assertEquals(entry.getKey(), mockBean.getBeanName());
        }
    }
}
