package camel_quarkus.jochenr.de.cxf_soap.contactservice;

import java.net.URL;
import javax.xml.namespace.QName;
import jakarta.xml.ws.WebEndpoint;
import jakarta.xml.ws.WebServiceClient;
import jakarta.xml.ws.WebServiceFeature;
import jakarta.xml.ws.Service;

/**
 * This class was generated by Apache CXF 4.0.2
 * 2023-09-22T08:12:26.530+02:00
 * Generated source version: 4.0.2
 *
 */
@WebServiceClient(name = "ContactService",
                  wsdlLocation = "classpath:wsdl/ContactService.wsdl",
                  targetNamespace = "http://de.jochenr.camel-quarkus/cxf-soap/ContactService")
public class ContactService extends Service {

    public final static URL WSDL_LOCATION;

    public final static QName SERVICE = new QName("http://de.jochenr.camel-quarkus/cxf-soap/ContactService", "ContactService");
    public final static QName ContactServicePort = new QName("http://de.jochenr.camel-quarkus/cxf-soap/ContactService", "ContactServicePort");
    static {
        URL url = ContactService.class.getClassLoader().getResource("wsdl/ContactService.wsdl");
        if (url == null) {
            java.util.logging.Logger.getLogger(ContactService.class.getName())
                .log(java.util.logging.Level.INFO,
                     "Can not initialize the default wsdl from {0}", "classpath:wsdl/ContactService.wsdl");
        }
        WSDL_LOCATION = url;
    }

    public ContactService(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public ContactService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public ContactService() {
        super(WSDL_LOCATION, SERVICE);
    }

    public ContactService(WebServiceFeature ... features) {
        super(WSDL_LOCATION, SERVICE, features);
    }

    public ContactService(URL wsdlLocation, WebServiceFeature ... features) {
        super(wsdlLocation, SERVICE, features);
    }

    public ContactService(URL wsdlLocation, QName serviceName, WebServiceFeature ... features) {
        super(wsdlLocation, serviceName, features);
    }




    /**
     *
     * @return
     *     returns ContactWS
     */
    @WebEndpoint(name = "ContactServicePort")
    public ContactWS getContactServicePort() {
        return super.getPort(ContactServicePort, ContactWS.class);
    }

    /**
     *
     * @param features
     *     A list of {@link jakarta.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns ContactWS
     */
    @WebEndpoint(name = "ContactServicePort")
    public ContactWS getContactServicePort(WebServiceFeature... features) {
        return super.getPort(ContactServicePort, ContactWS.class, features);
    }

}
