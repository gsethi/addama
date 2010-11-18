package org.systemsbiology.addama.commons.jcrstart;

import org.apache.commons.lang.StringUtils;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.ArrayList;

/**
 * @author hrovira
 */
public class SpringBootstrap {
    public static void main(String[] args) {
        ArrayList<String> list = new ArrayList<String>();
        if (args != null) {
            for (String arg : args) {
                if (!StringUtils.isEmpty(arg)) {
                    list.add(arg);
                }
            }
        }

        if (!list.contains("jcrstart.xml")) {
            list.add("jcrstart.xml");
        }
        
        new ClassPathXmlApplicationContext(list.toArray(new String[list.size()]));
    }
}
