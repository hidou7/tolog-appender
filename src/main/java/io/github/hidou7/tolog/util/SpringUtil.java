package io.github.hidou7.tolog.util;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class SpringUtil implements BeanFactoryPostProcessor, ApplicationContextAware {

    private static ConfigurableListableBeanFactory beanFactory;
    private static ApplicationContext applicationContext;

    public void postProcessBeanFactory(ConfigurableListableBeanFactory factory) throws BeansException {
        beanFactory = factory;
    }

    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    public static String getProperty(String key) {
        if(null == applicationContext){
            return null;
        }
        return applicationContext.getEnvironment().getProperty(key);
    }
}
