
package camel_quarkus.jochenr.de.cxf_soap.contactservice;

import javax.xml.namespace.QName;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the camel_quarkus.jochenr.de.cxf_soap.contactservice package. 
 * <p>An ObjectFactory allows you to programmatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private static final QName _AddContact_QNAME = new QName("http://de.jochenr.camel-quarkus/cxf-soap/ContactService", "addContact");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: camel_quarkus.jochenr.de.cxf_soap.contactservice
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link AddContact }
     * 
     * @return
     *     the new instance of {@link AddContact }
     */
    public AddContact createAddContact() {
        return new AddContact();
    }

    /**
     * Create an instance of {@link Contact }
     * 
     * @return
     *     the new instance of {@link Contact }
     */
    public Contact createContact() {
        return new Contact();
    }

    /**
     * Create an instance of {@link Address }
     * 
     * @return
     *     the new instance of {@link Address }
     */
    public Address createAddress() {
        return new Address();
    }

    /**
     * Create an instance of {@link Contacts }
     * 
     * @return
     *     the new instance of {@link Contacts }
     */
    public Contacts createContacts() {
        return new Contacts();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AddContact }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link AddContact }{@code >}
     */
    @XmlElementDecl(namespace = "http://de.jochenr.camel-quarkus/cxf-soap/ContactService", name = "addContact")
    public JAXBElement<AddContact> createAddContact(AddContact value) {
        return new JAXBElement<>(_AddContact_QNAME, AddContact.class, null, value);
    }

}
