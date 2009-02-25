/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package javax.xml.ws.wsaddressing;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.namespace.QName;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.WebServiceException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Java class for EndpointReferenceType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="EndpointReferenceType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Address" type="{http://www.w3.org/2005/08/addressing}AttributedURIType"/>
 *         &lt;element ref="{http://www.w3.org/2005/08/addressing}ReferenceParameters" minOccurs="0"/>
 *         &lt;element ref="{http://www.w3.org/2005/08/addressing}Metadata" minOccurs="0"/>
 *         &lt;any/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlRootElement(name = "EndpointReference", namespace = W3CEndpointReference.NS)
@XmlType(name = "EndpointReferenceType", namespace = W3CEndpointReference.NS)
public final class W3CEndpointReference extends EndpointReference {
    protected static final String NS = "http://www.w3.org/2005/08/addressing";
    private static JAXBContext jaxbContext;
    
    @XmlElement(name = "Address", namespace = NS, required = true)
    private AttributedURIType address;
    @XmlElement(name = "ReferenceParameters", namespace = NS)
    private ReferenceParametersType referenceParameters;
    @XmlElement(name = "Metadata", namespace = NS)
    private MetadataType metadata;
    @XmlAnyElement(lax = true)
    private List<Object> any;
    @XmlAnyAttribute
    private Map<QName, String> otherAttributes = new HashMap<QName, String>();
    
    static {
        try { 
            jaxbContext = JAXBContext.newInstance(W3CEndpointReference.class);
        }
        catch (Exception e) {
            //TODO NLS enable
            throw new WebServiceException("JAXBContext creation failed.", e);
        }
    }

    protected W3CEndpointReference() {
    }
    
    public W3CEndpointReference(Source eprInfoset) {
        super();
        
        try {
            Unmarshaller um = jaxbContext.createUnmarshaller();
            W3CEndpointReference w3cEPR = (W3CEndpointReference) um.unmarshal(eprInfoset);
            
            address = w3cEPR.address;
            referenceParameters = w3cEPR.referenceParameters;
            metadata = w3cEPR.metadata;
            any = w3cEPR.any;
            otherAttributes.putAll(w3cEPR.otherAttributes);
        }
        catch (Exception e) {
            //TODO NLS enable.
            throw new WebServiceException("Unable to create W3C endpoint reference.", e);
        }
    }
    
    @Override
    public void writeTo(Result result) {
        if (result == null) {
            //TODO NLS enable
            throw new IllegalArgumentException("Null is not allowed.");
        }
        
        try {
            Marshaller m = jaxbContext.createMarshaller();
            m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            m.marshal(this, result);
        }
        catch (Exception e) {
            //TODO NLS enable
            throw new WebServiceException("writeTo failure.", e);
        }
    }

    /**
     * <p>Java class for AttributedURIType complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType name="AttributedURIType">
     *   &lt;simpleContent>
     *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>anyURI">
     *     &lt;/extension>
     *   &lt;/simpleContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "AttributedURIType", propOrder = {
        "value"
    })
    private static class AttributedURIType {

        @XmlValue
        @XmlSchemaType(name = "anyURI")
        protected String value;
        @XmlAnyAttribute
        private Map<QName, String> otherAttributes = new HashMap<QName, String>();
        
        public AttributedURIType() {
        }
    }

    /**
     * <p>Java class for ReferenceParametersType complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType name="ReferenceParametersType">
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;any/>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "ReferenceParametersType", propOrder = {
        "any"
    })
    private static class ReferenceParametersType {

        @XmlAnyElement(lax = true)
        protected List<Object> any;
        @XmlAnyAttribute
        private Map<QName, String> otherAttributes = new HashMap<QName, String>();
        
        public ReferenceParametersType() {
        }
    }

    /**
     * <p>Java class for MetadataType complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType name="MetadataType">
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;any/>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "MetadataType", propOrder = {
        "any"
    })
    private static class MetadataType {

        @XmlAnyElement(lax = true)
        protected List<Object> any;
        @XmlAnyAttribute
        private Map<QName, String> otherAttributes = new HashMap<QName, String>();
        
        public MetadataType() {
        }
    }
}
