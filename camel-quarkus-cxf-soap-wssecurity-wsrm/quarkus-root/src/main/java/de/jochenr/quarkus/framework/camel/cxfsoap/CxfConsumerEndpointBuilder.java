package de.jochenr.quarkus.framework.camel.cxfsoap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.camel.CamelContext;
import org.apache.camel.component.cxf.common.DataFormat;
import org.apache.camel.component.cxf.jaxws.CxfComponent;
import org.apache.camel.component.cxf.jaxws.CxfEndpoint;
import org.apache.camel.component.cxf.transport.header.CxfHeaderFilterStrategy;
import org.apache.cxf.annotations.SchemaValidation.SchemaValidationType;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.security.SimpleAuthorizingInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.apache.cxf.ws.rm.feature.RMFeature;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.wss4j.dom.handler.WSHandlerConstants;

import de.jochenr.quarkus.framework.camel.cxfsoap.client.SamlWSConfigurationManager;
import de.jochenr.quarkus.framework.camel.cxfsoap.feature.WSRMConfigRMFeature;
import de.jochenr.quarkus.framework.camel.cxfsoap.security.SignatureEncryptionSecurityType;
import de.jochenr.quarkus.framework.camel.cxfsoap.security.SubjectCreatingSAMLPolicyInterceptor;





public class CxfConsumerEndpointBuilder {

	private final CxfEndpoint cxfEndpoint;

	private boolean allowStreaming = true;
	private DataFormat dataFormat = DataFormat.PAYLOAD;
	private boolean mtomEnabled = true;
	private Boolean wrappedStyle = true;
	private String wsdlURL = null;
	private String serviceName = null;
	private String portName = null;
	private Class<?> serviceClass = null;

	private Map<String, Object> properties = new HashMap<>();
	private List<Interceptor<? extends Message>> inInterceptors = new ArrayList<>();

	/**
	 * Bei "address" Parameter sind Hostname und Port egal, da diese vom Server überschrieben werden.
	 * Die Protokollangabe https://  ( bzw. http:// ) ist jedoch relevant
	 *
	 * @param address
	 * @param context
	 */
	private CxfConsumerEndpointBuilder(String address, CamelContext context) {
		if (context == null) {
			throw new IllegalStateException(this.getClass().getName() + ": Tried to build a CxfConsumerEndpoint with a null-CamelContext!");
		}
		CxfComponent cxfComponent = new CxfComponent(context);
		this.cxfEndpoint = new CxfEndpoint(address, cxfComponent);
	}

	public static CxfConsumerEndpointBuilder getNewInstance(String address, CamelContext context) {
		return new CxfConsumerEndpointBuilder(address, context);
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

		// ROBUST_ONEWAY nicht vergessen.., ob auf requestContext oder auf Endpunkt
		// lt. https://issues.apache.org/jira/browse/CXF-5630?focusedCommentId=13946496&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-13946496
		// sollte beides funktionieren
		// wird eigentlich nur bei OneWay Requests benötogt (damit im Fehlerfall eine entsprechende HTTP-Response erfolgt),
		// schadet aber auch nicht bei "normalen" Requests. Deshalb wird es immer gesetzt.
		this.properties.put(Message.ROBUST_ONEWAY, Boolean.TRUE.toString());

		Map<String, Object> propertiesTmp = this.cxfEndpoint.getProperties();
		if (propertiesTmp == null) {
			this.cxfEndpoint.setProperties(this.properties);
		} else {
			propertiesTmp.putAll(this.properties);
		}

		this.cxfEndpoint.getInInterceptors().addAll(this.inInterceptors);


		// damit keine Header weitergegeben werden!
		// TODO wahrscheinlich müssen wir hier einen eigenen Filter bauen bzw. mehr konfigurieren!
		CxfHeaderFilterStrategy headerFilterStrategy = new CxfHeaderFilterStrategy();
		headerFilterStrategy.setRelayHeaders(true);
		headerFilterStrategy.setRelayAllMessageHeaders(false);
		this.cxfEndpoint.setHeaderFilterStrategy(headerFilterStrategy);

		return this.cxfEndpoint;
	}

	public CxfConsumerEndpointBuilder withAllowStreaming(boolean allowStreaming) {
		this.allowStreaming = allowStreaming;
		return this;
	}

	public CxfConsumerEndpointBuilder withDataFormat(DataFormat dataFormat) {
		this.dataFormat = dataFormat;
		return this;
	}

	public CxfConsumerEndpointBuilder withMtomEnabled(boolean mtomEnabled) {
		this.mtomEnabled = mtomEnabled;
		return this;
	}

	public CxfConsumerEndpointBuilder withMtomEnabled(boolean mtomEnabled, boolean expandXOPIncludeForSignature) {
		// MTOM mit WS-Security funktioniert erst ab Apache CXF 3.2.x richtig....
		// http://coheigea.blogspot.com/2017/02/ws-security-with-mtom-support-in-apache.html
		// http://coheigea.blogspot.com/2018/05/streaming-ws-security-mtom-support-in.html
		// ab Version 3.2.x heißt die property dann nur noch "expandXOPInclude"
		// ACHTUNG: Diese Property funktioniert nicht auf der ausgehenden Seite, wenn die Security über eine Policy konfiguriert wurde,
		// als nicht, wenn automagisch (aufgrund WSDL) die "PolicyBasedWSS4J...Interceptor" eingebunden werden.
		// Nur die normalen WSS4J..Interceptoren berücksichtigen die Property

		// ab Version 3.2.10/3.3.3 gibt es das neue WS-Security configuration Tag "ws-security.expand.xop.include"
		// org.apache.cxf.ws.security.SecurityConstants.EXPAND_XOP_INCLUDE
		// das aufgrund meines Kommentars hier (http://coheigea.blogspot.com/2017/02/ws-security-with-mtom-support-in-apache.html) eingebaut wurde
		// https://github.com/apache/cxf/commit/e438cd4745c2e08997af62c2958e2d079608a0da
		// Damit sollte es auch "policy based" funktionieren.

		// TODO: diese Zeilen löschen, sobald richtige Version von CXF da ist (s. unten)
		this.properties.put("expandXOPIncludeForSignature", Boolean.toString(expandXOPIncludeForSignature)); //CXF 3.1.x
		this.properties.put("expandXOPInclude", Boolean.toString(expandXOPIncludeForSignature)); //CXF 3.2.x

		// Ab CXF Version 3.2.10/3.3.3 gibt es diese Property, die auf bei den PolicyBased WSS4J Interceptors funktioniert:
		// TODO: diese Zeile reinnehmen, sobald richtige Version von CXF da ist
//		this.properties.put(org.apache.cxf.ws.security.SecurityConstants.EXPAND_XOP_INCLUDE, Boolean.toString(expandXOPIncludeForSignature)); //CXF 3.2.10/3.3.3
		// TODO: sobald klar ist, dass wir nicht mehr zur alten version kompatibel sein müssen, gegen die Zeile darüber, die Konstatnte, tauschen!!!
		this.properties.put("ws-security.expand.xop.include", Boolean.toString(expandXOPIncludeForSignature));

		this.mtomEnabled = mtomEnabled;
		return this;
	}

	public CxfConsumerEndpointBuilder withWrappedStyle(Boolean wrappedStyle) {
		this.wrappedStyle = wrappedStyle;
		return this;
	}

	public CxfConsumerEndpointBuilder withWsdlURL(String wsdlURL) {
		this.wsdlURL = wsdlURL;
		return this;
	}

	public CxfConsumerEndpointBuilder withWsdlService(String serviceName) {
		this.serviceName = serviceName;
		return this;
	}

	public CxfConsumerEndpointBuilder withWsdlPort(String portName) {
		this.portName = portName;
		return this;
	}

	public CxfConsumerEndpointBuilder withServiceClass(Class<?> serviceClass) {
		this.serviceClass = serviceClass;
		return this;
	}

	/**
	 * Schaltet die Schema-Validierung von Apache CXF ein bzw. aus.
	 * Alternativ kann auch über die Validator Kompomenete von Camel validiert werden. Z.B. wenn man die
	 * ungültige Nachricht noch loggen, archivieren oder anderweitig verarbeiten möchte.
	 * Siehe: https://stackoverflow.com/questions/42561870/enable-xml-validation-in-cxf-soap-endpoint-inside-camel-route
	 *
	 * @param enabled
	 * @return
	 */
	public CxfConsumerEndpointBuilder withSchemaValidationEnabled(boolean enabled) {
		this.properties.put(Message.SCHEMA_VALIDATION_ENABLED, Boolean.toString(enabled));
		return this;
	}

	/**
	 * Schaltet die Schema-Validierung von Apache CXF ein bzw. aus.
	 * Alternativ kann auch über die Validator Kompomenete von Camel validiert werden. Z.B. wenn man die
	 * ungültige Nachricht noch loggen, archivieren oder anderweitig verarbeiten möchte.
	 * Siehe: https://stackoverflow.com/questions/42561870/enable-xml-validation-in-cxf-soap-endpoint-inside-camel-route
	 *
	 * Der zweite Parameter erlaubt einzustellen in welche Richtung die validierung durchgeführt wird (Default ist BOTH)
	 *
	 * @param enabled
	 * @param schemaValidationType
	 * @return
	 */
	public CxfConsumerEndpointBuilder withSchemaValidationEnabled(boolean enabled, SchemaValidationType schemaValidationType) {
		this.withSchemaValidationEnabled(enabled);
		this.properties.put(Message.SCHEMA_VALIDATION_TYPE, schemaValidationType.toString());
		return this;
	}

	/**
	 *
	 * @param securityType
	 * @param rolesAllowed (String in dem die Rollen mit Leerzeichen getrennt übergeben werden!)
	 * @return
	 */
	public CxfConsumerEndpointBuilder withLoginSecurity(LoginSecurityType securityType, String securityDomainName, String rolesAllowed) {

		switch (securityType) {
		case SAML_SENDER_VOUCHES:

			// Authentication
			SubjectCreatingSAMLPolicyInterceptor elytronSAMLAuthPolicyInterceptor = new SubjectCreatingSAMLPolicyInterceptor();
//			elytronSAMLAuthPolicyInterceptor.setElytronSecurityDomainName(securityDomainName);
			this.inInterceptors.add(elytronSAMLAuthPolicyInterceptor);



			// bei "standard" Webserice Applikationen würde der Teil über die "jaxws-endpoint-config.xml" konfiguriert...
			Properties samlProperties = SamlWSConfigurationManager.getWss4jProperties();
			this.properties.put(SecurityConstants.SIGNATURE_PROPERTIES, samlProperties);

			// The user's name for signature. It is used as the alias name in the keystore....
			// würde genutzt werden, um Wert in Konfigurations-Property Datei zu überschreiben
//			this.properties.put(SecurityConstants.SIGNATURE_USERNAME, "jbentw_saml");//TODO: braucht man wohl nicht...

			// Braucht man den Callback Handler noch lange?
			// https://issues.apache.org/jira/browse/CXF-6400
			// ServiceKeystoreCallbackHandler serviceKeystoreCallbackHandler = new ServiceKeystoreCallbackHandler();
			// this.properties.put(SecurityConstants.CALLBACK_HANDLER, serviceKeystoreCallbackHandler);


			//TODO: weitere Ideen zum Testen....
//			this.properties.put(org.apache.cxf.ws.security.SecurityConstants.IS_BSP_COMPLIANT, "true");
			this.properties.put(org.apache.cxf.ws.security.SecurityConstants.STORE_BYTES_IN_ATTACHMENT, "true");
			// hat leider nicht auf Anhieb funktioniert;-(  lohnt aber auch nur für sehr große Messages
//			this.properties.put(org.apache.cxf.ws.security.SecurityConstants.ENABLE_STREAMING_SECURITY, "true");

			break;
		case CLIENT_CERT:

//			// Authentication
//			SubjectCreatingClientCertPolicyInterceptor elytronClientCertAuthInterceptor = new SubjectCreatingClientCertPolicyInterceptor();
//			elytronClientCertAuthInterceptor.setElytronSecurityDomainName(securityDomainName);
//			this.inInterceptors.add(elytronClientCertAuthInterceptor);

			break;
		case BASIC:

//			// Authentication
//			SubjectCreatingBasicAuthPolicyInterceptor elytronBasicAuthInterceptor = new SubjectCreatingBasicAuthPolicyInterceptor();
//			elytronBasicAuthInterceptor.setElytronSecurityDomainName(securityDomainName);
//			this.inInterceptors.add(elytronBasicAuthInterceptor);

			break;
		case NO_SECURITY:
			// Absichtlich keine Security
			break;
		default:
			throw new IllegalStateException("Security wurde nicht konfiguriert!");
		}


		if ( ! LoginSecurityType.NO_SECURITY.equals(securityType)) {
			// Authorization
			SimpleAuthorizingInterceptor authorizingInterceptor = new SimpleAuthorizingInterceptor();
	        authorizingInterceptor.setAllowAnonymousUsers(false);
	        // geht nicht...da wird immer die Methode "invoke" genommen?!?!?
	//			        Map<String, String> methodRolesMap = new HashMap<>(1);
	//			        methodRolesMap.put("isAlive", "LProfiXXX");
	//			        methodRolesMap.put("abrufEinstellen", "LProfiXXX");
	//			        authorizingInterceptor.setMethodRolesMap(methodRolesMap);
	        authorizingInterceptor.setGlobalRoles(rolesAllowed);
	        inInterceptors.add(authorizingInterceptor);
		}

		return this;
	}


	public CxfConsumerEndpointBuilder withSignatureEncryptionSecurity(SignatureEncryptionSecurityType securityType, String pathToProperties) {

		// siehe:
		// https://docs.jboss.org/jbossws/5.1.9.Final/sid-3866738.html#sid-3866795
		// https://glenmazza.net/blog/entry/cxf-x509-profile
		// https://wso2.com/library/255/

		// Braucht man den Callback Handler noch lange?
		// https://issues.apache.org/jira/browse/CXF-6400
		// KeystorePasswordCallbackHandler callbackHandler = new KeystorePasswordCallbackHandler();

		/*
		Properties signatureEncryptionProperties = this.configHelper.loadCommonProperties(pathToProperties);

		String keystoreAliasKey = Merlin.PREFIX + Merlin.KEYSTORE_ALIAS;
		String keystorePrivatePasswordKey = Merlin.PREFIX + Merlin.KEYSTORE_PRIVATE_PASSWORD;
		String alias = (String) signatureEncryptionProperties.get(keystoreAliasKey);
		String password = (String) signatureEncryptionProperties.get(keystorePrivatePasswordKey);
		callbackHandler.setAliasPassword(alias, password);
		this.properties.put(SecurityConstants.CALLBACK_HANDLER, callbackHandler);
		*/

		switch (securityType) {
			case 	SIGNATURE_ONLY:
				this.properties.put(SecurityConstants.SIGNATURE_PROPERTIES, pathToProperties);
				// The user's name for signature. It is used as the alias name in the keystore....
				// würde genutzt werden, um Wert in Konfigurations-Property Datei zu überschreiben
				// this.properties.put(SecurityConstants.SIGNATURE_USERNAME, "MeinAliasName");
				break;
			case 	SIGNATURE_AND_ENCRYPTION:

				this.properties.put(SecurityConstants.SIGNATURE_PROPERTIES, pathToProperties);
				this.properties.put(SecurityConstants.ENCRYPT_PROPERTIES, pathToProperties);

				/*
				 * In some real world scenarios though, the same server might be expected to be able to deal with
				 * (including decrypting and encrypting) messages coming from and being sent to multiple clients.
				 * Apache CXF supports that through the useReqSigCert value.
				 *
				 * When this value is used instead of a specific client key alias, it tells the service to use the
				 * same key that was used to sign the SOAP request. This allows the service to handle any client whose
				 * public key is in the service's truststore.
				 *
				 */
				this.properties.put(SecurityConstants.ENCRYPT_USERNAME, WSHandlerConstants.USE_REQ_SIG_CERT);

				break;
			default:
				break;
		}

		return this;
	}


	public CxfConsumerEndpointBuilder withWSAddressing() {

		List<Feature> features = this.cxfEndpoint.getFeatures();

		if (!featureAlreadyAdded(WSAddressingFeature.class, features)) {
			WSAddressingFeature addressing = new WSAddressingFeature();
			addressing.setAddressingRequired(true);
			addressing.setUsingAddressingAdvisory(true);

			features.add(addressing);
		}
		return this;
	}

	public CxfConsumerEndpointBuilder withWSRM() {

		// laut https://issues.apache.org/jira/browse/CXF-4091
		// sollte ein Call durch die oben gesetzte "Message.ROBUST_ONEWAY" Property schon synchron sein.
		// Diese Property scheint aber nicht ausrechend zu sein, wenn CXF aud Camel heraus verwendet wird:
		// hier ist beschrieben, dass man "robust" UND "synchron" setzen muss....
		// https://grokbase.com/t/camel/users/12cc7rc4w5/error-handling-when-using-camel-cxf

		// Camel würde im WS-RM Protokollablauf nur im CreateSequence den Fehler mitbekommen, nicht mehr beim eigentlichen "Business-Call"
		this.cxfEndpoint.setSynchronous(true);

		List<Feature> features = this.cxfEndpoint.getFeatures();

		//WSRM benötigt das WSAddressing Feature
		if (!featureAlreadyAdded(WSAddressingFeature.class, features)) {
			this.withWSAddressing();
		}

		if (!featureAlreadyAdded(RMFeature.class, features)) {
			RMFeature reliableMessagingFeature = new WSRMConfigRMFeature();
			features.add(reliableMessagingFeature);
		}
		return this;
	}



	protected boolean featureAlreadyAdded(Class<? extends Object> clazz, Collection<Feature> features) {
		for (Feature feature : features) {
			if (clazz.isInstance(feature))
				return true;
		}
		return false;
	}

}
