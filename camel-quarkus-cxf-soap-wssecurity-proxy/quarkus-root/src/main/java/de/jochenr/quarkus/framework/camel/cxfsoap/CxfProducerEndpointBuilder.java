package de.jochenr.quarkus.framework.camel.cxfsoap;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.camel.CamelContext;
import org.apache.camel.component.cxf.common.DataFormat;
import org.apache.camel.component.cxf.jaxws.CxfComponent;
import org.apache.camel.component.cxf.jaxws.CxfConfigurer;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.component.cxf.transport.header.CxfHeaderFilterStrategy;
import org.apache.camel.support.jsse.FilterParameters;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextClientParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.apache.cxf.Bus;
import org.apache.cxf.annotations.SchemaValidation.SchemaValidationType;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.frontend.AbstractWSDLBasedEndpointFactory;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.rt.security.SecurityConstants;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.https.httpclient.DefaultHostnameVerifier;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.transports.http.configuration.ProxyServerType;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.apache.cxf.ws.rm.feature.RMFeature;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.wss4j.common.crypto.Merlin;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import de.jochenr.quarkus.framework.ConfigHelper;
import de.jochenr.quarkus.framework.camel.cxfsoap.client.EnumClientConfig;
import de.jochenr.quarkus.framework.camel.cxfsoap.client.SamlWSConfigurationManager;
import de.jochenr.quarkus.framework.camel.cxfsoap.feature.WSRMConfigRMFeature;
import de.jochenr.quarkus.framework.camel.cxfsoap.security.CxfProducerSAMLSenderVouchesOutInterceptor;
import de.jochenr.quarkus.framework.camel.cxfsoap.security.KeystorePasswordCallbackHandler;
import de.jochenr.quarkus.framework.camel.cxfsoap.security.SignatureEncryptionSecurityType;

public class CxfProducerEndpointBuilder {

	protected static final int CLIENT_RECEIVE_TIMEOUT_DEFAULT = 300000;
	protected static final int CLIENT_CONNECTION_TIMEOUT_DEFAULT = 120000;

	private final CxfEndpoint cxfEndpoint;

	private Properties clientConfigurationProperties = null;

	private String httpProxyServer = null;
	private Integer httpProxyPort = null;
	private long receiveTimeout = CLIENT_RECEIVE_TIMEOUT_DEFAULT;
	private long connectionTimeout = CLIENT_CONNECTION_TIMEOUT_DEFAULT;

	private boolean allowStreaming = true;
	private DataFormat dataFormat = DataFormat.PAYLOAD;
	private boolean mtomEnabled = true;
	private Boolean wrappedStyle = true;
	private String wsdlURL = null;
	private String serviceName = null;
	private String portName = null;
	private Class<?> serviceClass = null;

	private Map<String, Object> properties = new HashMap<>();
	private List<Interceptor<? extends Message>> outInterceptors = new ArrayList<>();

	private CxfProducerEndpointBuilder(String cxfClientPropsPrefix, CamelContext context) {
		if (context == null) {
			throw new IllegalStateException(
					this.getClass().getName() + ": Tried to build a CxfProducerEndpoint with a null-CamelContext!");
		}

		Config cfg = ConfigProvider.getConfig();
		String address = cfg.getValue(cxfClientPropsPrefix + "." + EnumClientConfig.ADDRESS.getKey(), String.class);

		this.clientConfigurationProperties = ConfigHelper.loadPropertiesWithPrefix(cxfClientPropsPrefix);

		CxfComponent cxfComponent = new CxfComponent(context);
		cxfEndpoint = new CxfEndpoint(address, cxfComponent);
	}

	public static CxfProducerEndpointBuilder getNewInstance(String address, CamelContext context) {
		return new CxfProducerEndpointBuilder(address, context);
	}

	

	public CxfEndpoint build() {

		this.cxfEndpoint.setAllowStreaming(this.allowStreaming);
		this.cxfEndpoint.setDataFormat(this.dataFormat);
		this.cxfEndpoint.setMtomEnabled(this.mtomEnabled);
		this.cxfEndpoint.setWrappedStyle(this.wrappedStyle);
		this.cxfEndpoint.setWsdlURL(this.wsdlURL);
		this.cxfEndpoint.setServiceName(this.serviceName);
		this.cxfEndpoint.setPortName(this.portName);
		this.cxfEndpoint.setServiceClass(this.serviceClass);

		// damit keine Header (nach draußen) weitergegeben werden!
		// TODO wahrscheinlich müssen wir hier einen eigenen Filter bauen bzw. mehr
		// konfigurieren!
		CxfHeaderFilterStrategy headerFilterStrategy = new CxfHeaderFilterStrategy();
		headerFilterStrategy.setRelayHeaders(true);
		headerFilterStrategy.setRelayAllMessageHeaders(false);
		this.cxfEndpoint.setHeaderFilterStrategy(headerFilterStrategy);

		// ROBUST_ONEWAY nicht vergessen.., ob auf requestContext oder auf Endpunkt
		// lt.
		// https://issues.apache.org/jira/browse/CXF-5630?focusedCommentId=13946496&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-13946496
		// sollte beides funktionieren
		// wird eigentlich nur bei OneWay Requests benötogt (damit im Fehlerfall eine
		// entsprechende HTTP-Response erfolgt),
		// schadet aber auch nicht bei "normalen" Requests. Deshalb wird es immer
		// gesetzt.
		this.properties.put(Message.ROBUST_ONEWAY, Boolean.TRUE.toString());

		// Timeouts konfigurieren und Features initialisieren
		this.cxfEndpoint.setCxfConfigurer(new CxfConfigurer() {

			@Override
			public void configure(AbstractWSDLBasedEndpointFactory factoryBean) {
				// TODO Auto-generated method stub
			}

			@Override
			public void configureClient(Client client) {
				HTTPConduit httpConduit = (HTTPConduit) client.getConduit();
				HTTPClientPolicy httpClientPolicy = httpConduit.getClient();
				if (httpClientPolicy == null) {
					httpClientPolicy = new HTTPClientPolicy();
				}
				httpClientPolicy.setConnectionTimeout(CxfProducerEndpointBuilder.this.connectionTimeout);
				httpClientPolicy.setReceiveTimeout(CxfProducerEndpointBuilder.this.receiveTimeout);

				if (CxfProducerEndpointBuilder.this.httpProxyServer != null
						&& CxfProducerEndpointBuilder.this.httpProxyPort != null) {
					httpClientPolicy.setProxyServer(CxfProducerEndpointBuilder.this.httpProxyServer);
					httpClientPolicy.setProxyServerPort(CxfProducerEndpointBuilder.this.httpProxyPort);
					httpClientPolicy.setProxyServerType(ProxyServerType.HTTP);
				}

				// FIXME: remove if HTTP/2 compatibility issue is fixed
				// WORKAROUND-START
				Config cfg = ConfigProvider.getConfig();
				Optional<String> optExplicitHttpVersion = cfg.getOptionalValue("cxf.client.explicit.http.version", String.class);
				if (optExplicitHttpVersion.isPresent()) {
					httpClientPolicy.setVersion(optExplicitHttpVersion.get());
				}
				// WORKAROUND-END


				// // Workaround for making https call against "localhost"
				// if (isLocalhost(httpConduit)) {
				// 	TrustManager[] trustAllCerts = new TrustManager[]{};
				// 	TLSClientParameters tlsCP = new TLSClientParameters();
				// 	tlsCP.setDisableCNCheck(true);
				// 	tlsCP.setHostnameVerifier(new NoopHostnameVerifier());
				// 	tlsCP.setTrustManagers(trustAllCerts);
				// 	httpConduit.setTlsClientParameters(tlsCP);
				//  }

				httpConduit.setClient(httpClientPolicy);

				// FIXME: sind features ohne diesen Code enabled??
				// List<Feature> features = this.cxfEndpoint.getFeatures();
				Bus bus = client.getBus();
				Collection<Feature> features = bus.getFeatures();

				for (Feature feature : features) {
					feature.initialize(client, bus);
				}
			}

			@Override
			public void configureServer(Server server) {
				throw new IllegalStateException(
						"In \"" + this.getClass().getName() + "\" kann nur ein Client konfiguriert werden!");
			}

		});

		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }

        } };

		 /* SSL specific settings start*/
		SSLContextParameters sslContextParameters = new SSLContextParameters();
		TrustManagersParameters trustManagersParameters = new TrustManagersParameters();
		trustManagersParameters.setTrustManager(trustAllCerts[0]);
		sslContextParameters.setTrustManagers(trustManagersParameters);
		/* SSL specific settings end*/

		this.cxfEndpoint.setSslContextParameters(sslContextParameters);
		this.cxfEndpoint.setHostnameVerifier(new NoopHostnameVerifier());

		Map<String, Object> propertiesTmp = this.cxfEndpoint.getProperties();
		if (propertiesTmp == null) {
			this.cxfEndpoint.setProperties(this.properties);
		} else {
			propertiesTmp.putAll(this.properties);
		}

		this.cxfEndpoint.getOutInterceptors().addAll(this.outInterceptors);

		return this.cxfEndpoint;
	}

	public CxfProducerEndpointBuilder withAllowStreaming(boolean allowStreaming) {
		this.allowStreaming = allowStreaming;
		return this;
	}

	public CxfProducerEndpointBuilder withConnectionTimeout(long connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
		return this;
	}

	public CxfProducerEndpointBuilder withDataFormat(DataFormat dataFormat) {
		this.dataFormat = dataFormat;
		return this;
	}

	public CxfProducerEndpointBuilder withMtomEnabled(boolean mtomEnabled) {
		this.mtomEnabled = mtomEnabled;
		return this;
	}

	public CxfProducerEndpointBuilder withMtomEnabled(boolean mtomEnabled, boolean expandXOPIncludeForSignature) {

		// MTOM mit WS-Security funktioniert erst ab Apache CXF 3.2.x richtig....
		// http://coheigea.blogspot.com/2017/02/ws-security-with-mtom-support-in-apache.html
		// http://coheigea.blogspot.com/2018/05/streaming-ws-security-mtom-support-in.html
		// ab Version 3.2.x heißt die property dann nur noch "expandXOPInclude"
		// ACHTUNG: Diese Property funktioniert nicht auf der ausgehenden Seite, wenn
		// die Security über eine Policy konfiguriert wurde,
		// als nicht, wenn automagisch (aufgrund WSDL) die
		// "PolicyBasedWSS4J...Interceptor" eingebunden werden.
		// Nur die normalen WSS4J..Interceptoren berücksichtigen die Property

		// ab Version 3.2.10/3.3.3 gibt es das neue WS-Security configuration Tag
		// "ws-security.expand.xop.include"
		// org.apache.cxf.ws.security.SecurityConstants.EXPAND_XOP_INCLUDE
		// das aufgrund meines Kommentars hier
		// (http://coheigea.blogspot.com/2017/02/ws-security-with-mtom-support-in-apache.html)
		// eingebaut wurde
		// https://github.com/apache/cxf/commit/e438cd4745c2e08997af62c2958e2d079608a0da
		// Damit sollte es auch "policy based" funktionieren.

		// TODO: diese Zeilen löschen, sobald richtige Version von CXF da ist (s. unten)
		this.properties.put("expandXOPIncludeForSignature", Boolean.toString(expandXOPIncludeForSignature)); // CXF
																												// 3.1.x
		this.properties.put("expandXOPInclude", Boolean.toString(expandXOPIncludeForSignature)); // CXF 3.2.x

		// Ab CXF Version 3.2.10/3.3.3 gibt es diese Property, die auf bei den
		// PolicyBased WSS4J Interceptors funktioniert:
		// TODO: diese Zeile reinnehmen, sobald richtige Version von CXF da ist
		// this.properties.put(org.apache.cxf.ws.security.SecurityConstants.EXPAND_XOP_INCLUDE,
		// Boolean.toString(expandXOPIncludeForSignature)); //CXF 3.2.10/3.3.3
		// TODO: sobald klar ist, dass wir nicht mehr zur alten version kompatibel sein
		// müssen, gegen die Zeile darüber, die Konstatnte, tauschen!!!
		this.properties.put("ws-security.expand.xop.include", Boolean.toString(expandXOPIncludeForSignature));

		this.mtomEnabled = mtomEnabled;
		return this;
	}

	public CxfProducerEndpointBuilder withReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
		return this;
	}

	public CxfProducerEndpointBuilder withWrappedStyle(Boolean wrappedStyle) {
		this.wrappedStyle = wrappedStyle;
		return this;
	}

	public CxfProducerEndpointBuilder withWsdlURL(String wsdlURL) {
		this.wsdlURL = wsdlURL;
		return this;
	}

	public CxfProducerEndpointBuilder withWsdlService(String serviceName) {
		this.serviceName = serviceName;
		return this;
	}

	public CxfProducerEndpointBuilder withWsdlPort(String portName) {
		this.portName = portName;
		return this;
	}

	public CxfProducerEndpointBuilder withServiceClass(Class<?> serviceClass) {
		this.serviceClass = serviceClass;
		return this;
	}

	/**
	 * Schaltet die Schema-Validierung von Apache CXF ein bzw. aus.
	 * An einem Producer Endpoint sorgt dies dafür, dass eine "kaputte" Soap-Message
	 * gar nicht verschickt würde.
	 *
	 * @param enabled
	 * @return
	 */
	public CxfProducerEndpointBuilder withSchemaValidationEnabled(boolean enabled) {
		this.properties.put(Message.SCHEMA_VALIDATION_ENABLED, Boolean.toString(enabled));
		return this;
	}

	/**
	 * Schaltet die Schema-Validierung von Apache CXF ein bzw. aus.
	 * An einem Producer Endpoint sorgt dies dafür, dass eine "kaputte" Soap-Message
	 * gar nicht verschickt würde.
	 *
	 * Der zweite Parameter erlaubt einzustellen in welche Richtung die validierung
	 * durchgeführt wird (Default ist BOTH)
	 *
	 * @param enabled
	 * @param schemaValidationType
	 * @return
	 */
	public CxfProducerEndpointBuilder withSchemaValidationEnabled(boolean enabled,
			SchemaValidationType schemaValidationType) {
		this.withSchemaValidationEnabled(enabled);
		this.properties.put(Message.SCHEMA_VALIDATION_TYPE, schemaValidationType.toString());
		return this;
	}

	/**
	 *
	 * @param securityType
	 * @param rolesAllowed
	 *                     (String in dem die Rollen mit Leerzeichen getrennt
	 *                     übergeben werden!)
	 * @return
	 */
	public CxfProducerEndpointBuilder withLoginSecurity(LoginSecurityType securityType,
			String... pathToConfigurationPropertiesFile) {

		switch (securityType) {
			case SAML_SENDER_VOUCHES:

				// bei "standard" Webserice Applikationen würde der Teil über die
				// "jaxws-endpoint-config.xml" konfiguriert...
				Properties samlProperties = SamlWSConfigurationManager.getWss4jProperties();
				this.properties.put(SecurityConstants.SIGNATURE_PROPERTIES, samlProperties);

				// TODO: weitere Ideen zum Testen....
				// this.properties.put(org.apache.cxf.ws.security.SecurityConstants.IS_BSP_COMPLIANT,
				// "true");
				this.properties.put(org.apache.cxf.ws.security.SecurityConstants.STORE_BYTES_IN_ATTACHMENT, "true");
				// hat leider nicht auf Anhieb funktioniert;-( lohnt aber auch nur für sehr große Messages
				// this.properties.put(org.apache.cxf.ws.security.SecurityConstants.ENABLE_STREAMING_SECURITY, "true");

				CxfProducerSAMLSenderVouchesOutInterceptor<Message> cxfProducerOutInterceptor = new CxfProducerSAMLSenderVouchesOutInterceptor<Message>();
				this.outInterceptors.add(cxfProducerOutInterceptor);

				break;
			case CLIENT_CERT:

				SSLContextParameters sslContextParameters = new SSLContextParameters();

				String keystoreName = (String) this.clientConfigurationProperties.get(EnumClientConfig.KEYSTORE_NAME.getKey());
				String keystorePassword = (String) this.clientConfigurationProperties.get(EnumClientConfig.KEYSTORE_PASSWORD.getKey());

				KeyStoreParameters ksp = new KeyStoreParameters();
				ksp.setResource(keystoreName);
				ksp.setPassword(keystorePassword);

				KeyManagersParameters kmp = new KeyManagersParameters();
				kmp.setKeyStore(ksp);
				kmp.setKeyPassword(keystorePassword); // keystorePassword == keyPassword ....

				FilterParameters filter = new FilterParameters();
				filter.getInclude().add(".*");

				SSLContextClientParameters sccp = new SSLContextClientParameters();
				sccp.setCipherSuitesFilter(filter);

				sslContextParameters.setClientParameters(sccp);
				sslContextParameters.setKeyManagers(kmp);

				this.cxfEndpoint.setSslContextParameters(sslContextParameters);

				this.cxfEndpoint.setSslContextParameters(sslContextParameters);

				// Ausnahme für localhost, da wir für localhost kein Zertifikat haben
				HostnameVerifier hostnameVerifier = new DefaultHostnameVerifier();
				try {
					URL url = new URL(this.cxfEndpoint.getPublishedEndpointUrl());
					String hostname = url.getHost();
					if (ConfigHelper.isLocalhost(hostname)) {
						hostnameVerifier = new HostnameVerifier() {
							// essentially turns hostname verification off
							@Override
							public boolean verify(String hostname, SSLSession session) {
								return true;
							}
						};
					}
				} catch (MalformedURLException e) {
				}
				this.cxfEndpoint.setHostnameVerifier(hostnameVerifier);

				break;
			case BASIC:

				String username = (String) this.clientConfigurationProperties.get(EnumClientConfig.USERNAME.getKey());
				String password = (String) this.clientConfigurationProperties.get(EnumClientConfig.PASSWORD.getKey());

				this.cxfEndpoint.setUsername(username);
				this.cxfEndpoint.setPassword(password);

				break;
			case NO_SECURITY:

				// Absichtlich keine Security
				break;
			default:
				throw new IllegalStateException("Security wurde nicht konfiguriert!");
		}

		return this;
	}

	public CxfProducerEndpointBuilder withSignatureEncryptionSecurity(SignatureEncryptionSecurityType securityType) {

		// siehe:
		// https://docs.jboss.org/jbossws/5.1.9.Final/sid-3866738.html#sid-3866795
		// https://glenmazza.net/blog/entry/cxf-x509-profile
		// https://wso2.com/library/255/

		// Braucht man den Callback Handler noch lange?
		// https://issues.apache.org/jira/browse/CXF-6400
		KeystorePasswordCallbackHandler callbackHandler = new KeystorePasswordCallbackHandler();

		String keystoreAliasKey = Merlin.PREFIX + Merlin.KEYSTORE_ALIAS;
		String keystorePrivatePasswordKey = Merlin.PREFIX + Merlin.KEYSTORE_PRIVATE_PASSWORD;
		String alias = (String) this.clientConfigurationProperties.get(keystoreAliasKey);
		String password = (String) this.clientConfigurationProperties.get(keystorePrivatePasswordKey);
		callbackHandler.setAliasPassword(alias, password);
		this.properties.put(SecurityConstants.CALLBACK_HANDLER, callbackHandler);

		switch (securityType) {
			case SIGNATURE_ONLY:
				this.properties.put(SecurityConstants.SIGNATURE_PROPERTIES, this.clientConfigurationProperties);
				// The user's name for signature. It is used as the alias name in the
				// keystore....
				// würde genutzt werden, um Wert in Konfigurations-Property Datei zu
				// überschreiben
				// this.properties.put(SecurityConstants.SIGNATURE_USERNAME, alias);
				break;
			case SIGNATURE_AND_ENCRYPTION:
				this.properties.put(SecurityConstants.SIGNATURE_PROPERTIES, this.clientConfigurationProperties);
				this.properties.put(SecurityConstants.ENCRYPT_PROPERTIES, this.clientConfigurationProperties);
				String aliasOfPublicServerCert = (String) this.clientConfigurationProperties
						.get(EnumClientConfig.ALIAS_OF_PUBLIC_SERVER_CERT_FOR_ENCRYPTION.getKey());
				this.properties.put(SecurityConstants.ENCRYPT_USERNAME, aliasOfPublicServerCert);
				break;
			default:
				break;
		}

		return this;
	}

	public CxfProducerEndpointBuilder withWSAddressing() {

		List<Feature> features = this.cxfEndpoint.getFeatures();

		if (!featureAlreadyAdded(WSAddressingFeature.class, features)) {
			WSAddressingFeature addressing = new WSAddressingFeature();
			addressing.setAddressingRequired(true);
			addressing.setUsingAddressingAdvisory(true);

			features.add(addressing);
		}
		return this;
	}

	public CxfProducerEndpointBuilder withWSRM() {

		// laut https://issues.apache.org/jira/browse/CXF-4091
		// sollte ein Call durch die oben gesetzte "Message.ROBUST_ONEWAY" Property
		// schon synchron sein.
		// Diese Property scheint aber nicht ausrechend zu sein, wenn CXF aud Camel
		// heraus verwendet wird:
		// hier ist beschrieben, dass man "robust" UND "synchron" setzen muss....
		// https://grokbase.com/t/camel/users/12cc7rc4w5/error-handling-when-using-camel-cxf

		// Camel würde im WS-RM Protokollablauf nur im CreateSequence den Fehler
		// mitbekommen, nicht mehr beim eigentlichen "Business-Call"
		this.cxfEndpoint.setSynchronous(true);

		List<Feature> features = this.cxfEndpoint.getFeatures();

		// WSRM benötigt das WSAddressing Feature
		if (!featureAlreadyAdded(WSAddressingFeature.class, features)) {
			this.withWSAddressing();
		}

		if (!featureAlreadyAdded(RMFeature.class, features)) {
			RMFeature reliableMessagingFeature = new WSRMConfigRMFeature();
			features.add(reliableMessagingFeature);
		}
		return this;
	}

	public CxfProducerEndpointBuilder withHttpProxy(String proxyServer, Integer proxyPort) {

		this.httpProxyServer = proxyServer;
		this.httpProxyPort = proxyPort;

		return this;
	}

	protected boolean featureAlreadyAdded(Class<? extends Object> clazz, Collection<Feature> features) {
		for (Feature feature : features) {
			if (clazz.isInstance(feature))
				return true;
		}
		return false;
	}

	protected static boolean isLocalhost(HTTPConduit httpConduit) {
		boolean result = false;
  
		try {
		   String address = httpConduit.getAddress();
		   URL url = new URL(address);
		   String host = url.getHost();
		   if (isLocalhost(host)) {
			  result = true;
		   }
		} catch (MalformedURLException var5) {
		}
  
		return result;
	 }

	 protected static boolean isLocalhost(String hostname) {
		return "localhost".equalsIgnoreCase(hostname) || "127.0.0.1".equals(hostname);
	 }

}
