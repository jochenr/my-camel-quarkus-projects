/**
 *
 */
package de.jochenr.quarkus.framework.camel.cxfsoap.client;

public enum EnumClientConfig {

	// Die URL für den Webservice-Aufruf (nicht für den Zugriff auf die WSDL)
	ADDRESS("address"),
	// für Client-Cert und für SAML Authentifizierung
	KEYSTORE_NAME("keystoreName"),
	// für Client-Cert und für SAML Authentifizierung
	KEYSTORE_PASSWORD("keystorePassword"),
	//für HTTP-Basic Authentifizierung
	USERNAME("username"),
	//für HTTP-Basic Authentifizierung
	PASSWORD("password"),
	// ReceiveTimeout: Specifies the amount of time, in milliseconds, that the client will wait for a response before it times out.
	CLIENT_RECEIVE_TIMEOUT("client_receive_timeout"),
	// ConnectionTimeout: Specifies the amount of time, in milliseconds, that the client will attempt to establish a connection before it times out.
	CLIENT_CONNECTION_TIMEOUT("client_connection_timeout"),
	// darüber kann die WS-Security Signatur ein- bzw. ausgeschaltet werden. Erlaubte Werte: true/false
	USE_WS_SECURITY_SIGNATURE("useWsSecuritySignature"),
	// darüber kann die WS-Security Verschlüsselung ein- bzw. ausgeschaltet werden. Erlaubte Werte: true/false
	USE_WS_SECURITY_ENCRYPTION("useWsSecurityEncryption"),
	// Der Alias des öffentlichen Zertifikats eines Service, das für die Verschlüsselung genutzt werden soll
	// die anderen Keys/Konstanten für die Konfiguration sind in der Klasse "org.apache.wss4j.common.crypto.Merlin"
	ALIAS_OF_PUBLIC_SERVER_CERT_FOR_ENCRYPTION("aliasOfPublicServerCertForEncryption");

	private String key;

	private EnumClientConfig(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}


}
