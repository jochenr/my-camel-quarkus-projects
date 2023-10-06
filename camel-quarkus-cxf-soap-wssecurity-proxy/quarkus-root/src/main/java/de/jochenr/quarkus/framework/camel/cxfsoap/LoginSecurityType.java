package de.jochenr.quarkus.framework.camel.cxfsoap;


public enum LoginSecurityType {
	NO_SECURITY,
	BASIC,
	SAML_SENDER_VOUCHES,
	CLIENT_CERT;
}