/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 *
 */

package com.dangdang.ddframe.job.spring.namespace.parser.common;

import com.dangdang.ddframe.job.api.listener.AbstractDistributeOnceElasticJobListener;
import com.dangdang.ddframe.job.spring.namespace.parser.script.ScriptJobConfigurationDto;
import com.dangdang.ddframe.job.spring.schedule.SpringJobScheduler;
import com.google.common.base.Strings;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import java.util.List;

import static com.dangdang.ddframe.job.spring.namespace.constants.BaseJobBeanDefinitionParserTag.*;
import static com.dangdang.ddframe.job.spring.namespace.constants.ScriptJobBeanDefinitionParserTag.SCRIPT_COMMAND_LINE_ATTRIBUTE;

/**
 * 基本作业的命名空间解析器.
 * 
 * @author zhangliang
 * @author caohao
 */
public abstract class AbstractJobBeanDefinitionParser extends AbstractBeanDefinitionParser {
    
    @Override
    protected AbstractBeanDefinition parseInternal(final Element element, final ParserContext parserContext) {
        BeanDefinitionBuilder factory = BeanDefinitionBuilder.rootBeanDefinition(SpringJobScheduler.class);
        factory.setInitMethodName("init");
        factory.addConstructorArgReference(element.getAttribute(REGISTRY_CENTER_REF_ATTRIBUTE));
        factory.addConstructorArgReference(createJobConfiguration(element, parserContext));
        factory.addConstructorArgValue(createJobListeners(element));
        return factory.getBeanDefinition();
    }
    
    private String createJobConfiguration(final Element element, final ParserContext parserContext) {
        BeanDefinitionBuilder factory = BeanDefinitionBuilder.rootBeanDefinition(getJobConfigurationDTO());
        factory.addConstructorArgValue(element.getAttribute(ID_ATTRIBUTE));
        if (!getJobConfigurationDTO().isAssignableFrom(ScriptJobConfigurationDto.class)) {
            factory.addConstructorArgValue(element.getAttribute(CLASS_ATTRIBUTE));
        }
        factory.addConstructorArgValue(element.getAttribute(SHARDING_TOTAL_COUNT_ATTRIBUTE));
        factory.addConstructorArgValue(element.getAttribute(CRON_ATTRIBUTE));
        if (getJobConfigurationDTO().isAssignableFrom(ScriptJobConfigurationDto.class)) {
            factory.addConstructorArgValue(element.getAttribute(SCRIPT_COMMAND_LINE_ATTRIBUTE));
        }
        addPropertyValueIfNotEmpty(SHARDING_ITEM_PARAMETERS_ATTRIBUTE, "shardingItemParameters", element, factory);
        addPropertyValueIfNotEmpty(JOB_PARAMETER_ATTRIBUTE, "jobParameter", element, factory);
        addPropertyValueIfNotEmpty(MONITOR_EXECUTION_ATTRIBUTE, "monitorExecution", element, factory);
        addPropertyValueIfNotEmpty(MONITOR_PORT_ATTRIBUTE, "monitorPort", element, factory);
        addPropertyValueIfNotEmpty(MAX_TIME_DIFF_SECONDS_ATTRIBUTE, "maxTimeDiffSeconds", element, factory);
        addPropertyValueIfNotEmpty(FAILOVER_ATTRIBUTE, "failover", element, factory);
        addPropertyValueIfNotEmpty(MISFIRE_ATTRIBUTE, "misfire", element, factory);
        addPropertyValueIfNotEmpty(JOB_SHARDING_STRATEGY_CLASS_ATTRIBUTE, "jobShardingStrategyClass", element, factory);
        addPropertyValueIfNotEmpty(DESCRIPTION_ATTRIBUTE, "description", element, factory);
        addPropertyValueIfNotEmpty(DISABLED_ATTRIBUTE, "disabled", element, factory);
        addPropertyValueIfNotEmpty(OVERWRITE_ATTRIBUTE, "overwrite", element, factory);

        addPropertyValueIfNotEmpty(SHARDING_OFFSET_ATTRIBUTE, "shardingOffset", element, factory);
        addPropertyValueIfNotEmpty(ALARM_ATTRIBUTE, "alarm", element, factory);

        setPropertiesValue(element, factory);
        String result = element.getAttribute(ID_ATTRIBUTE) + "Conf";
        parserContext.getRegistry().registerBeanDefinition(result, factory.getBeanDefinition());
        return result;
    }
    
    protected abstract Class<? extends AbstractJobConfigurationDto> getJobConfigurationDTO();
    
    protected abstract void setPropertiesValue(final Element element, final BeanDefinitionBuilder factory);
    
    private List<BeanDefinition> createJobListeners(final Element element) {
        List<Element> listenerElements = DomUtils.getChildElementsByTagName(element, LISTENER_TAG);
        List<BeanDefinition> result = new ManagedList<>(listenerElements.size());
        for (Element each : listenerElements) {
            String className = each.getAttribute(CLASS_ATTRIBUTE);
            BeanDefinitionBuilder factory = BeanDefinitionBuilder.rootBeanDefinition(className);
            factory.setScope(BeanDefinition.SCOPE_PROTOTYPE);
            try {
                Class listenerClass = Class.forName(className);
                if (AbstractDistributeOnceElasticJobListener.class.isAssignableFrom(listenerClass)) {
                    factory.addConstructorArgValue(each.getAttribute(STARTED_TIMEOUT_MILLISECONDS_ATTRIBUTE));
                    factory.addConstructorArgValue(each.getAttribute(COMPLETED_TIMEOUT_MILLISECONDS_ATTRIBUTE));
                }
            } catch (final ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }
            result.add(factory.getBeanDefinition());
        }
        return result;
    }
    
    protected final void addPropertyValueIfNotEmpty(final String attributeName, final String propertyName, final Element element, final BeanDefinitionBuilder factory) {
        String attributeValue = element.getAttribute(attributeName);
        if (!Strings.isNullOrEmpty(attributeValue)) {
            factory.addPropertyValue(propertyName, attributeValue);
        }
    }
    
    @Override
    protected boolean shouldGenerateId() {
        return true;
    }
}
