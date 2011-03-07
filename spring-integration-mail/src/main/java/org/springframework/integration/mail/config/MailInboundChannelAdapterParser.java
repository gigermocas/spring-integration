/*
 * Copyright 2002-2011 the original author or authors.
 *
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
 */

package org.springframework.integration.mail.config;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the &lt;inbound-channel-adapter&gt; element of Spring Integration's 'mail' namespace. 
 * 
 * @author Jonas Partner
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class MailInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	private static final String BASE_PACKAGE = "org.springframework.integration.mail";


	@Override
	protected BeanMetadataElement parseSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				BASE_PACKAGE + ".MailReceivingMessageSource");
		builder.addConstructorArgValue(this.parseMailReceiver(element, parserContext));
		return builder.getBeanDefinition();
	}

	private BeanDefinition parseMailReceiver(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder receiverBuilder = BeanDefinitionBuilder.genericBeanDefinition(
				BASE_PACKAGE + ".config.MailReceiverFactoryBean");
		Object source = parserContext.extractSource(element);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(receiverBuilder, element, "store-uri");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(receiverBuilder, element, "protocol");
		String session = element.getAttribute("session");
		if (StringUtils.hasText(session)) {
			if (element.hasAttribute("java-mail-properties") || element.hasAttribute("authenticator")) {
				parserContext.getReaderContext().error("Neither 'java-mail-properties' nor 'authenticator' " +
						"references are allowed when a 'session' reference has been provided.", source);
			}
			receiverBuilder.addPropertyReference("session", session);
		}
		else {
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(receiverBuilder, element, "java-mail-properties");
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(receiverBuilder, element, "authenticator");
		}
		String maxFetchSize = element.getAttribute("max-fetch-size");
		if (StringUtils.hasText(maxFetchSize)) {
			receiverBuilder.addPropertyValue("maxFetchSize", maxFetchSize);
		}
		else {
			Element pollerElement = DomUtils.getChildElementByTagName(element, "poller");
			if (pollerElement != null) {
				String mmpp = pollerElement.getAttribute("max-messages-per-poll");
				if (StringUtils.hasText(mmpp)) {
					receiverBuilder.addPropertyValue("maxFetchSize", mmpp);
				}
			}
		}
		receiverBuilder.addPropertyValue("shouldDeleteMessages", element.getAttribute("should-delete-messages"));
		String markAsRead = element.getAttribute("should-mark-messages-as-read");
		if (StringUtils.hasText(markAsRead)){
			receiverBuilder.addPropertyValue("shouldMarkMessagesAsRead", markAsRead);
		}
		
		String selectorExpression = element.getAttribute("message-matcher-expression");
		
		RootBeanDefinition expressionDef = null;
		if (StringUtils.hasText(selectorExpression)){
			expressionDef = new RootBeanDefinition("org.springframework.integration.config.ExpressionFactoryBean");
			expressionDef.getConstructorArgumentValues().addGenericArgumentValue(selectorExpression);
			receiverBuilder.addPropertyValue("selectorExpression", expressionDef);
		}
		
		return receiverBuilder.getBeanDefinition();
	}

}
