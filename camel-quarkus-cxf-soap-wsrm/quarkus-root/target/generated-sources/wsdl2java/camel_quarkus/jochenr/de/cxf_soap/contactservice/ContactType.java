
package camel_quarkus.jochenr.de.cxf_soap.contactservice;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse für ContactType.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * <pre>{@code
 * <simpleType name="ContactType">
 *   <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     <enumeration value="PERSONAL"/>
 *     <enumeration value="WORK"/>
 *     <enumeration value="OTHER"/>
 *   </restriction>
 * </simpleType>
 * }</pre>
 * 
 */
@XmlType(name = "ContactType")
@XmlEnum
public enum ContactType {

    PERSONAL,
    WORK,
    OTHER;

    public String value() {
        return name();
    }

    public static ContactType fromValue(String v) {
        return valueOf(v);
    }

}
