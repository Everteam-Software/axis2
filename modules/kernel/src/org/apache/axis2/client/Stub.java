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


package org.apache.axis2.client;

import org.apache.axiom.om.*;
import org.apache.axiom.soap.*;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.i18n.Messages;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.httpclient.Header;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Base class for generated client stubs. This defines several client API
 * (<code>public</code>) methods shared between all types of stubs, along with
 * some <code>protected</code> methods intended for use by the actual stub
 * implementation code. The client API method names start with a leading
 * underscore character to avoid conflicts with actual implementation methods.
 */
public abstract class Stub {

    protected AxisService _service;
    protected ArrayList modules = new ArrayList();


    protected ServiceClient _serviceClient;

    /**
     * Get service client implementation used by this stub.
     *
     * @return service client
     */
    public ServiceClient _getServiceClient() {
        return _serviceClient;
    }

    /**
     * Set service client implementation used by this stub. Once set, the
     * service client is owned by this stub and will automatically be removed
     * from the configuration when use of the stub is done.
     *
     * @param _serviceClient
     */
    public void _setServiceClient(ServiceClient _serviceClient) {
        this._serviceClient = _serviceClient;
    }

    /**
     * Create a SOAP message envelope using the supplied options.
     * TODO generated stub code should use this method, or similar method taking
     * an operation client
     *
     * @param options
     * @return generated
     * @throws SOAPProcessingException
     */
    protected static SOAPEnvelope createEnvelope(Options options) throws SOAPProcessingException {
        return getFactory(options.getSoapVersionURI()).getDefaultEnvelope();
    }

    /**
     * Get Axiom factory appropriate to selected SOAP version.
     *
     * @param soapVersionURI
     * @return factory
     */
    protected static SOAPFactory getFactory(String soapVersionURI) {

        if (SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI.equals(soapVersionURI)) {
            return OMAbstractFactory.getSOAP11Factory();
        } else if (SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI.equals(soapVersionURI)) {
            return OMAbstractFactory.getSOAP12Factory();
        } else {
            throw new RuntimeException(Messages
                    .getMessage("unknownsoapversion"));
        }
    }

    /**
     * Finalize method called by garbage collection. This is overridden to
     * support cleanup of any associated resources.
     *
     * @throws Throwable
     */
    protected void finalize() throws Throwable {
        super.finalize();
        cleanup();
    }

    /**
     * Cleanup associated resources. This removes the axis service from the
     * configuration.
     *
     * @throws AxisFault
     */
    public void cleanup() throws AxisFault {
        _service.getAxisConfiguration().removeService(_service.getName());
    }

    /**
     * sets the epr of the service client to given value
     *
     * @param address
     */

    protected void setServiceClientEPR(String address) {
        EndpointReference toEPRFromServiceClient = _serviceClient.getOptions().getTo();
        toEPRFromServiceClient.setAddress(address);
    }

    /**
     * add an http header with name and value to message context
     *
     * @param messageContext
     * @param name
     * @param value
     */
    protected void addHttpHeader(MessageContext messageContext,
                                 String name,
                                 String value) {
        java.lang.Object headersObj = messageContext.getProperty(HTTPConstants.HTTP_HEADERS);
        if (headersObj == null) {
            headersObj = new java.util.ArrayList();
        }
        java.util.List headers = (java.util.List) headersObj;
        Header header = new Header();
        header.setName(name);
        header.setValue(value);
        headers.add(header);
        messageContext.setProperty(HTTPConstants.HTTP_HEADERS, headers);
    }

    /**
     * sets the propertykey and propertyValue as a pair to operation client
     *
     * @param operationClient
     * @param propertyKey
     * @param propertyValue
     */

    protected void addPropertyToOperationClient(OperationClient operationClient,
                                                String propertyKey,
                                                Object propertyValue) {
        operationClient.getOptions().setProperty(propertyKey, propertyValue);
    }

    protected void addPropertyToOperationClient(OperationClient operationClient,
                                                String propertyKey,
                                                boolean value) {
        addPropertyToOperationClient(operationClient, propertyKey, new Boolean(value));
    }

    protected void addPropertyToOperationClient(OperationClient operationClient,
                                                String propertyKey,
                                                int value) {
        addPropertyToOperationClient(operationClient, propertyKey, new Integer(value));
    }

    protected void setMustUnderstand(OMElement headerElement, OMNamespace omNamespace) {
        OMFactory omFactory = OMAbstractFactory.getOMFactory();
        OMAttribute mustUnderstandAttribute =
                omFactory.createOMAttribute(SOAP12Constants.ATTR_MUSTUNDERSTAND, omNamespace,
                                            "true");
        headerElement.addAttribute(mustUnderstandAttribute);
    }

    protected void addHeader(OMElement omElementToadd,
                             SOAPEnvelope envelop,
                             boolean mustUnderstand){
        SOAPHeaderBlock soapHeaderBlock =
                envelop.getHeader().addHeaderBlock(omElementToadd.getLocalName(),omElementToadd.getNamespace());
        soapHeaderBlock.setMustUnderstand(mustUnderstand);
        OMNode omNode = null;
        for (Iterator iter = omElementToadd.getChildren(); iter.hasNext();){
             omNode = (OMNode) iter.next();
             soapHeaderBlock.addChild(omNode);
        }

    }

    protected void addHeader(OMElement omElementToadd,
                             SOAPEnvelope envelop){
        addHeader(omElementToadd,envelop,false);
    }

}
