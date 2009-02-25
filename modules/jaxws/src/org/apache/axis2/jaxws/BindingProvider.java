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

package org.apache.axis2.jaxws;

import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.jaxws.addressing.util.EndpointReferenceUtils;
import org.apache.axis2.jaxws.binding.BindingUtils;
import org.apache.axis2.jaxws.binding.SOAPBinding;
import org.apache.axis2.jaxws.client.PropertyValidator;
import org.apache.axis2.jaxws.core.InvocationContext;
import org.apache.axis2.jaxws.core.MessageContext;
import org.apache.axis2.jaxws.description.EndpointDescription;
import org.apache.axis2.jaxws.description.ServiceDescriptionWSDL;
import org.apache.axis2.jaxws.handler.HandlerResolverImpl;
import org.apache.axis2.jaxws.i18n.Messages;
import org.apache.axis2.jaxws.spi.ServiceDelegate;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;
import javax.xml.ws.Binding;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.handler.HandlerResolver;
import javax.xml.ws.wsaddressing.W3CEndpointReference;
import java.util.Hashtable;
import java.util.Map;

public class BindingProvider implements org.apache.axis2.jaxws.spi.BindingProvider {
    private static final Log log = LogFactory.getLog(BindingProvider.class);

    protected Map<String, Object> requestContext;

    protected Map<String, Object> responseContext;

    protected EndpointDescription endpointDesc;

    protected ServiceDelegate serviceDelegate;

    private org.apache.axis2.jaxws.spi.Binding binding;

    public BindingProvider(ServiceDelegate svcDelegate,
                           EndpointDescription epDesc,
                           org.apache.axis2.addressing.EndpointReference epr,
                           String addressingNamespace,
                           WebServiceFeature... features) {
        this.endpointDesc = epDesc;
        this.serviceDelegate = svcDelegate;
        
        initialize(epr, addressingNamespace, features);
    }

    /*
     * Initialize any objects needed by the BindingProvider
     */
    private void initialize(org.apache.axis2.addressing.EndpointReference epr,
                            String addressingNamespace,
                            WebServiceFeature... features) {
        requestContext = new ValidatingClientContext();
        responseContext = new ValidatingClientContext();
        
        // Setting standard property defaults for the request context
        requestContext.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, Boolean.FALSE);
        requestContext.put(BindingProvider.SOAPACTION_USE_PROPERTY, Boolean.TRUE);
        requestContext.put(AddressingConstants.DISABLE_ADDRESSING_FOR_OUT_MESSAGES, Boolean.TRUE);
        
        // Set the endpoint address
        String endpointAddress = (epr != null ) ? epr.getAddress() : endpointDesc.getEndpointAddress();        
        if (endpointAddress != null && !"".equals(endpointAddress)) {
            requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointAddress);                
        }
        
        // JAXWS 9.2.1.1 requires that we go ahead and create the binding object
        // so we can also set the handlerchain
        binding = (org.apache.axis2.jaxws.spi.Binding) BindingUtils.createBinding(endpointDesc);
        if(log.isDebugEnabled()){
            log.debug("Lookign for Handler Resolver");
        }
        // TODO should we allow the ServiceDelegate to figure out the default handlerresolver?  Probably yes, since a client app may look for one there.
        HandlerResolver handlerResolver = null;
        if(serviceDelegate.getHandlerResolver() != null){
            if(log.isDebugEnabled()){
                log.debug("Reading default Handler Resolver ");
            }
            handlerResolver = serviceDelegate.getHandlerResolver();
        }
        else{
            handlerResolver = new HandlerResolverImpl(endpointDesc.getServiceDescription(), serviceDelegate);
            if(log.isDebugEnabled()){
                log.debug("Creating new Handler Resolver using HandlerResolverImpl");
            }
        }

        // See if the metadata from creating the service indicates that MTOM should be enabled
        if (binding instanceof SOAPBinding) {
            // MTOM can be enabled either at the ServiceDescription level (via the WSDL binding type) or
            // at the EndpointDescription level via the binding type used to create a Dispatch.
            boolean enableMTOMFromMetadata = false;
            
            // if we have an SEI for the port, then we'll use it in order to search for MTOM configuration
            if(endpointDesc.getEndpointInterfaceDescription() != null
                    &&
                    endpointDesc.getEndpointInterfaceDescription().getSEIClass() != null) {
                enableMTOMFromMetadata = endpointDesc.getServiceDescription().isMTOMEnabled(serviceDelegate, 
                                                                   endpointDesc.getEndpointInterfaceDescription().getSEIClass());
            }
            else {
                enableMTOMFromMetadata = endpointDesc.getServiceDescription().isMTOMEnabled(serviceDelegate);
            }
            if (!enableMTOMFromMetadata) {
                String bindingType = endpointDesc.getClientBindingID();
                enableMTOMFromMetadata = (bindingType.equals(SOAPBinding.SOAP11HTTP_MTOM_BINDING) || 
                                          bindingType.equals(SOAPBinding.SOAP12HTTP_MTOM_BINDING));
            }
            
            if (enableMTOMFromMetadata) {
                ((SOAPBinding) binding).setMTOMEnabled(true);
            }
        }
                
        // check for properties that need to be set on the BindingProvider
        String seiName = null;
        if(endpointDesc.getEndpointInterfaceDescription() != null 
                &&
                endpointDesc.getEndpointInterfaceDescription().getSEIClass() != null) {
            seiName = endpointDesc.getEndpointInterfaceDescription().getSEIClass().getName();
        }
        String portQNameString = endpointDesc.getPortQName().toString();
        String key = seiName + ":" + portQNameString;
        Map<String, Object> bProps = endpointDesc.getServiceDescription().getBindingProperties(serviceDelegate, key);
        if(bProps != null) {
            if(log.isDebugEnabled()) {
                log.debug("Setting binding props with size: " + bProps.size() + " on " +
                "BindingProvider RequestContext");
            }
            requestContext.putAll(bProps);
        }
        
        binding.setHandlerChain(handlerResolver.getHandlerChain(endpointDesc.getPortInfo()));
        
        //Set JAX-WS 2.1 related properties.
        try {
            binding.setAxis2EndpointReference(epr);
            binding.setAddressingNamespace(addressingNamespace);
            binding.setFeatures(features);
        }
        catch (Exception e) {
            throw ExceptionFactory.makeWebServiceException(e);
        }
    }

    public ServiceDelegate getServiceDelegate() {
        return serviceDelegate;
    }

    public EndpointDescription getEndpointDescription() {
        return endpointDesc;
    }

    public Binding getBinding() {
        return binding;
    }

    public Map<String, Object> getRequestContext() {
        return requestContext;
    }

    public Map<String, Object> getResponseContext() {
        return responseContext;
    }

    /**
     * Check for maintain session state enablement either in the
     * MessageContext.isMaintainSession() or in the ServiceContext properties.
     * 
     * @param mc
     * @param ic
     */
    protected void checkMaintainSessionState(MessageContext mc, InvocationContext ic) {
        Map<String, Object> properties = ic.getServiceClient().getServiceContext().getProperties();
        boolean bValue = false;

        if (properties != null
            && properties
                         .containsKey(javax.xml.ws.BindingProvider.SESSION_MAINTAIN_PROPERTY)) {
            bValue = (Boolean) properties
                .get(javax.xml.ws.BindingProvider.SESSION_MAINTAIN_PROPERTY);
        }
        if (mc.isMaintainSession() || bValue == true) {
            setupSessionContext(properties);
        }
    }

    /*
    * Ensure that the next request context contains the session value returned
    * from previous request
    */
    protected void setupSessionContext(Map<String, Object> properties) {
        String sessionKey = null;
        Object sessionValue = null;

        if (properties == null) {
            throw ExceptionFactory.makeWebServiceException(Messages.getMessage("NoMaintainSessionProperty"));
        } else if (properties.containsKey(HTTPConstants.HEADER_LOCATION)) {
            sessionKey = HTTPConstants.HEADER_LOCATION;
            sessionValue = properties.get(sessionKey);
            if (sessionValue != null && !"".equals(sessionValue)) {
                requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, sessionValue);
            }
        } else if (properties.containsKey(HTTPConstants.HEADER_COOKIE)) {
            sessionKey = HTTPConstants.HEADER_COOKIE;
            sessionValue = properties.get(sessionKey);
            if (sessionValue != null && !"".equals(sessionValue)) {
                requestContext.put(HTTPConstants.COOKIE_STRING, sessionValue);
            }
        } else if (properties.containsKey(HTTPConstants.HEADER_COOKIE2)) {
            sessionKey = HTTPConstants.HEADER_COOKIE2;
            sessionValue = properties.get(sessionKey);
            if (sessionValue != null && !"".equals(sessionValue)) {
                requestContext.put(HTTPConstants.COOKIE_STRING, sessionValue);
            }
        } else {
            throw ExceptionFactory
                    .makeWebServiceException(Messages.getMessage("NoMaintainSessionProperty"));
        }

        if (sessionValue == null) {
            throw ExceptionFactory.makeWebServiceException(
                    Messages.getMessage("NullValueForMaintainSessionProperty", sessionKey));
        }
    }

    /**
     * Returns a boolean value representing whether or not a SOAPAction header should be sent with
     * the request.
     */
    protected boolean useSoapAction() {
        //TODO: Add some bit of validation for this property so that we know
        // it is actually a Boolean and not a String.
        Boolean use = (Boolean)requestContext.get(BindingProvider.SOAPACTION_USE_PROPERTY);
        if (use != null) {
            if (use.booleanValue()) {
                return true;
            } else {
                return false;
            }
        } else {
            // If the value is not set, then just default to sending a SOAPAction
            return true;
        }
    }

    /*
     *  (non-Javadoc)
     * @see javax.xml.ws.BindingProvider#getEndpointReference()
     */
    public EndpointReference getEndpointReference() {
        return getEndpointReference(W3CEndpointReference.class);
    }

    /*
     *  (non-Javadoc)
     * @see javax.xml.ws.BindingProvider#getEndpointReference(java.lang.Class)
     */
    public <T extends EndpointReference> T getEndpointReference(Class<T> clazz) {
        EndpointReference jaxwsEPR = null;
        String addressingNamespace = EndpointReferenceUtils.getAddressingNamespace(clazz);
        
        try {
            org.apache.axis2.addressing.EndpointReference epr = binding.getAxis2EndpointReference();
            
            if (epr == null) {
                String address =
                    (String) requestContext.get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
                if (address == null)
                    address = endpointDesc.getEndpointAddress();
                QName service = endpointDesc.getServiceQName();
                QName port = endpointDesc.getPortQName();
                String wsdlLocation = ((ServiceDescriptionWSDL) endpointDesc.getServiceDescription()).getWSDLLocation();

                epr = EndpointReferenceUtils.createAxis2EndpointReference(address, service, port, wsdlLocation, addressingNamespace);
            }
            else if (!addressingNamespace.equals(binding.getAddressingNamespace())) {
                throw ExceptionFactory.
                   makeWebServiceException(Messages.getMessage("bindingProviderErr1",
                                                               binding.getAddressingNamespace(),
                                                               addressingNamespace));
            }

            jaxwsEPR = EndpointReferenceUtils.convertFromAxis2(epr, addressingNamespace);
        } catch (UnsupportedOperationException e) {
            throw e;
        } catch (WebServiceException e) {
            throw e;
        } catch (Exception e) {
            throw ExceptionFactory.
                makeWebServiceException(Messages.getMessage("endpointRefConstructionFailure3", 
                                                            e.toString()));
        }
        
        return clazz.cast(jaxwsEPR);
    }
    
    /*
    * An inner class used to validate properties as they are set by the client.
    */
    class ValidatingClientContext extends Hashtable<String, Object> {
        private static final long serialVersionUID = 3485112205801917858L;

        @Override
        public synchronized Object put(String key, Object value) {
            // super.put rightly throws a NullPointerException if key or value is null, so don't continue if that's the case
            if (value == null)
                return null;
            if (PropertyValidator.validate(key, value)) {
                return super.put(key, value);
            } else {
                throw ExceptionFactory.makeWebServiceException(
                        Messages.getMessage("invalidPropValue", key, value.getClass().getName(),
                                            PropertyValidator.getExpectedValue(key).getName()));
            }
        }
    }


}
