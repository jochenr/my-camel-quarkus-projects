/**
 *
 */
package de.jochenr.quarkus.framework.camel.cxfsoap.client;

import java.util.Properties;

import org.eclipse.microprofile.config.ConfigProvider;

import de.jochenr.quarkus.framework.ConfigHelper;



public class SamlWSConfigurationManager {

	private static final String SAML_ISSUER_KEY = "de.jochenr.ws.client.security.saml.issuer";

	private static Properties samlSignatureKeystoreProperties;


	public static Properties getWss4jProperties() {
		if (samlSignatureKeystoreProperties == null) {
			synchronized (SamlWSConfigurationManager.class) {
				samlSignatureKeystoreProperties = ConfigHelper.loadPropertiesWithPrefix("org.apache.wss4j");
			}
		}
		return samlSignatureKeystoreProperties;
	}

	public static String getSamlIssuer() {
		String issuer = ConfigProvider.getConfig().getValue(SamlWSConfigurationManager.SAML_ISSUER_KEY, String.class);
		return issuer;
	}

}
