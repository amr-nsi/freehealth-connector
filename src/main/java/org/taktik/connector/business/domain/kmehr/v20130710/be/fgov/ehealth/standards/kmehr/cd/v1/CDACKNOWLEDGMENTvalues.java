//
// Ce fichier a été généré par l'implémentation de référence JavaTM Architecture for XML Binding (JAXB), v2.2.8-b130911.1802 
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apportée à ce fichier sera perdue lors de la recompilation du schéma source. 
// Généré le : 2015.03.05 à 11:48:09 AM CET 
//


package org.taktik.connector.business.domain.kmehr.v20130710.be.fgov.ehealth.standards.kmehr.cd.v1;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour CD-ACKNOWLEDGMENTvalues.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * <p>
 * <pre>
 * &lt;simpleType name="CD-ACKNOWLEDGMENTvalues">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="always"/>
 *     &lt;enumeration value="never"/>
 *     &lt;enumeration value="onerror"/>
 *     &lt;enumeration value="onsuccess"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "CD-ACKNOWLEDGMENTvalues")
@XmlEnum
public enum CDACKNOWLEDGMENTvalues {

    @XmlEnumValue("always")
    ALWAYS("always"),
    @XmlEnumValue("never")
    NEVER("never"),
    @XmlEnumValue("onerror")
    ONERROR("onerror"),
    @XmlEnumValue("onsuccess")
    ONSUCCESS("onsuccess");
    private final String value;

    CDACKNOWLEDGMENTvalues(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static CDACKNOWLEDGMENTvalues fromValue(String v) {
        for (CDACKNOWLEDGMENTvalues c: CDACKNOWLEDGMENTvalues.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
