package org.systemsbiology.addama.commons.spring.mock;

import org.springframework.beans.factory.BeanNameAware;

/**
 * @author hrovira
 */
public class MockBean implements BeanNameAware {
    private String beanName;

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }
}
