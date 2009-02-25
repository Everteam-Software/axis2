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
package org.apache.axis2.jaxws.description.impl;

import static org.apache.axis2.jaxws.description.builder.MDQConstants.RETURN_TYPE_FUTURE;
import static org.apache.axis2.jaxws.description.builder.MDQConstants.RETURN_TYPE_RESPONSE;

import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.jaxws.ClientConfigurationFactory;
import org.apache.axis2.jaxws.ExceptionFactory;
import org.apache.axis2.jaxws.description.DescriptionFactory;
import org.apache.axis2.jaxws.description.EndpointDescription;
import org.apache.axis2.jaxws.description.EndpointInterfaceDescription;
import org.apache.axis2.jaxws.description.ServiceDescription;
import org.apache.axis2.jaxws.description.ServiceDescriptionJava;
import org.apache.axis2.jaxws.description.ServiceDescriptionWSDL;
import org.apache.axis2.jaxws.description.ServiceRuntimeDescription;
import org.apache.axis2.jaxws.description.builder.DescriptionBuilderComposite;
import org.apache.axis2.jaxws.description.builder.MDQConstants;
import org.apache.axis2.jaxws.description.builder.MethodDescriptionComposite;
import org.apache.axis2.jaxws.description.builder.ParameterDescriptionComposite;
import org.apache.axis2.jaxws.description.xml.handler.HandlerChainsType;
import org.apache.axis2.jaxws.i18n.Messages;
import org.apache.axis2.jaxws.util.WSDL4JWrapper;
import org.apache.axis2.jaxws.util.WSDLWrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jws.HandlerChain;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.ws.soap.SOAPBinding;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/** @see ../ServiceDescription */
class ServiceDescriptionImpl
        implements ServiceDescription, ServiceDescriptionWSDL, ServiceDescriptionJava {
    private ClientConfigurationFactory clientConfigFactory;
    private ConfigurationContext configContext;

    private URL wsdlURL;
    private QName serviceQName;

    // Only ONE of the following will be set in a ServiceDescription, depending on whether this Description
    // was created from a service-requester or service-provider flow. 
    private Class serviceClass;         // A service-requester generated service or generic service class

    // TODO: Possibly remove Definition and delegate to the Defn on the AxisSerivce set as a paramater by WSDLtoAxisServicBuilder?
    private WSDLWrapper wsdlWrapper;
    private WSDLWrapper generatedWsdlWrapper;
    
    //ANNOTATION: @HandlerChain
    private HandlerChain handlerChainAnnotation;
    private HandlerChainsType handlerChainsType;

    private Map<QName, EndpointDescription> endpointDescriptions =
            new HashMap<QName, EndpointDescription>();

    private static final Log log = LogFactory.getLog(ServiceDescriptionImpl.class);

    private HashMap<String, DescriptionBuilderComposite> dbcMap = null;

    private DescriptionBuilderComposite composite = null;
    private boolean isServerSide = false;

//  RUNTIME INFORMATION
    Map<String, ServiceRuntimeDescription> runtimeDescMap =
            Collections.synchronizedMap(new HashMap<String, ServiceRuntimeDescription>());


    /**
     * This is (currently) the client-side-only constructor Construct a service description hierachy
     * based on WSDL (may be null), the Service class, and a service QName.
     *
     * @param wsdlURL      The WSDL file (this may be null).
     * @param serviceQName The name of the service in the WSDL.  This can not be null since a
     *                     javax.xml.ws.Service can not be created with a null service QName.
     * @param serviceClass The JAX-WS service class.  This could be an instance of
     *                     javax.xml.ws.Service or a generated service subclass thereof.  This will
     *                     not be null.
     */
    ServiceDescriptionImpl(URL wsdlURL, QName serviceQName, Class serviceClass) {
        if (serviceQName == null) {
            throw ExceptionFactory.makeWebServiceException(Messages.getMessage("serviceDescErr0"));
        }
        if (serviceClass == null) {
            throw ExceptionFactory
                    .makeWebServiceException(Messages.getMessage("serviceDescErr1", "null"));
        }
        if (!javax.xml.ws.Service.class.isAssignableFrom(serviceClass)) {
            throw ExceptionFactory.makeWebServiceException(
                    Messages.getMessage("serviceDescErr1", serviceClass.getName()));
        }

        // TODO: On the client side, we should not support partial WSDL; i.e. if the WSDL is specified it must be
        //       complete and must contain the ServiceQName.  This is how the Sun RI behaves on the client.
        //       When this is fixed, the check in ServiceDelegate(URL, QName, Class) should be removed
        this.wsdlURL = wsdlURL;
        // TODO: The serviceQName needs to be verified between the argument/WSDL/Annotation
        this.serviceQName = serviceQName;
        this.serviceClass = serviceClass;

        setupWsdlDefinition();
    }

    /**
     * This is (currently) the service-provider-side-only constructor. Create a service Description
     * based on a service implementation class
     *
     * @param serviceImplClass
     */
    ServiceDescriptionImpl(Class serviceImplClass, AxisService axisService) {
        isServerSide = true;
        // Create the EndpointDescription hierachy from the service impl annotations; Since the PortQName is null, 
        // it will be set to the annotation value.
        EndpointDescriptionImpl endpointDescription =
                new EndpointDescriptionImpl(serviceImplClass, null, axisService, this);
        addEndpointDescription(endpointDescription);

        // TODO: The ServiceQName instance variable should be set based on annotation or default
    }

    /**
     * This is (currently) the service-provider-side-only constructor. Create a service Description
     * based on a service implementation class
     *
     * @param serviceImplClass
     */
    ServiceDescriptionImpl(
            HashMap<String, DescriptionBuilderComposite> dbcMap,
            DescriptionBuilderComposite composite) {
        this.composite = composite;

        String serviceImplName = this.composite.getClassName();

        this.dbcMap = dbcMap;
        //TODO: How to we get this when called from server side, create here for now
        //REVIEW: The value being set here is used later in validation checking to
        //        validation that should occur separately on server and client. If
        //        at some point this constructor is ever called by the client side,
        //        then we'll have to get smarter about how we determine server/client
        //        validation
        this.isServerSide = true;

        //capture the WSDL, if there is any...to be used for later processing
        setupWsdlDefinition();

        // Do a first pass validation for this DescriptionBuilderComposite.
        // This is not intended to be a full integrity check, but rather a fail-fast mechanism
        // TODO: Refactor this to a seperate validator class?
        validateDBCLIntegrity();

        // The ServiceQName instance variable is set based on annotation or default
        // It will be set by the EndpointDescriptionImpl since it is the one that knows
        // how to process the annotations and the defaults.
        //TODO: When we get this, need to consider verifying service name between WSDL
        //      and annotations, so

        // Create the EndpointDescription hierachy from the service impl annotations; Since the PortQName is null, 
        // it will be set to the annotation value.
        //EndpointDescription endpointDescription = new EndpointDescription(null, this, serviceImplName);
        EndpointDescriptionImpl endpointDescription =
                new EndpointDescriptionImpl(this, serviceImplName);
        addEndpointDescription(endpointDescription);
    }

    /*=======================================================================*/
    /*=======================================================================*/
    // START of public accessor methods

    /**
     * Update or create an EndpointDescription. Updates to existing EndpointDescriptons will be
     * based on the SEI class and its annotations.  Both declared ports and dynamic ports can be
     * updated.  A declared port is one that is defined (e.g. in WSDL or via annotations); a dyamic
     * port is one that is not defined (e.g. not via WSDL or annotations) and has been added via
     * Serivce.addPort.
     * <p/>
     * Notes on how an EndpointDescription can be updated or created: 1) Service.createDispatch can
     * create a Dispatch client for either a declared or dynamic port 2) Note that creating a
     * Dispatch does not associate an SEI with an endpoint 3) Service.getPort will associate an SEI
     * with a port 4) A getPort on an endpoint which was originally created for a Distpatch will
     * update that EndpointDescription with the SEI provided on the getPort 5) Service.getPort can
     * not be called on a dynamic port (per the JAX-WS spec) 6) Service.addPort can not be called
     * for a declared port
     *
     * @param sei        This will be non-null if the update is of type GET_PORT; it will be null if
     *                   the update is ADD_PORT or CREATE_DISPATCH
     * @param portQName
     * @param updateType Indicates what is causing the update GET_PORT is an attempt to get a
     *                   declared SEI-based port ADD_PORT is an attempt to add a previously
     *                   non-existent dynamic port CREATE_DISPATCH is an attempt to create a
     *                   Dispatch-based client to either a declared port or a pre-existing dynamic
     *                   port.
     */

    EndpointDescription updateEndpointDescription(Class sei, QName portQName,
                                                  DescriptionFactory.UpdateType updateType) {

        EndpointDescriptionImpl endpointDescription = getEndpointDescriptionImpl(portQName);
        boolean isPortDeclared = isPortDeclared(portQName);

        switch (updateType) {

            case ADD_PORT:
                // Port must NOT be declared (e.g. can not already exist in WSDL)
                // If an EndpointDesc doesn't exist; create it as long as it doesn't exist in the WSDL
                // TODO: This test can be simplified once isPortDeclared(QName) understands annotations and WSDL as ways to declare a port.
                if (DescriptionUtils.isEmpty(portQName)) {
                    throw ExceptionFactory
                            .makeWebServiceException(Messages.getMessage("addPortErr2"));
                }
                if (getWSDLWrapper() != null && isPortDeclared) {
                    // TODO: RAS & NLS
                    throw ExceptionFactory.makeWebServiceException(
                            Messages.getMessage("addPortDup", portQName.toString()));
                } else if (endpointDescription == null) {
                    // Use the SEI Class and its annotations to finish creating the Description hierachy.  Note that EndpointInterface, Operations, Parameters, etc.
                    // are not created for dynamic ports.  It would be an error to later do a getPort against a dynamic port (per the JAX-WS spec)
                    endpointDescription = new EndpointDescriptionImpl(sei, portQName, true, this);
                    addEndpointDescription(endpointDescription);
                } else {
                    // All error check above passed, the EndpointDescription already exists and needs no updating
                }
                break;

            case GET_PORT:
                
                // try to find existing endpointDesc by SEI class if portQName was not specified 
                if (endpointDescription == null && portQName == null && sei != null) {
                    endpointDescription = getEndpointDescriptionImpl(sei);
                }
                
                // If an endpointDesc doesn't exist, and the port exists in the WSDL, create it
                // If an endpointDesc already exists and has an associated SEI already, make sure they match
                // If an endpointDesc already exists and was created for Dispatch (no SEI), update that with the SEI provided on the getPort

                // Port must be declared (e.g. in WSDL or via annotations)
                // TODO: Once isPortDeclared understands annotations and not just WSDL, the 2nd part of this check can possibly be removed.
                //       Although consider the check below that updates an existing EndpointDescritpion with an SEI.
                if (!isPortDeclared ||
                        (endpointDescription != null && endpointDescription.isDynamicPort())) {
                    // This guards against the case where an addPort was done previously and now a getPort is done on it.
                    // TODO: RAS & NLS
                    throw ExceptionFactory.makeWebServiceException(
                        "ServiceDescription.updateEndpointDescription: Can not do a getPort on a port added via addPort().  PortQN: " +
                        (portQName != null ? portQName.toString() : "not specified"));
                } else if (sei == null) {
                    // TODO: RAS & NLS
                    throw ExceptionFactory.makeWebServiceException(
                        "ServiceDescription.updateEndpointDescription: Can not do a getPort with a null SEI.  PortQN: " +
                        (portQName != null ? portQName.toString() : "not specified"));
                } else if (endpointDescription == null) {
                    // Use the SEI Class and its annotations to finish creating the Description hierachy: Endpoint, EndpointInterface, Operations, Parameters, etc.
                    // TODO: Need to create the Axis Description objects after we have all the config info (i.e. from this SEI)
                    endpointDescription = new EndpointDescriptionImpl(sei, portQName, this);
                    addEndpointDescription(endpointDescription);
                    /*
                     * We must reset the service runtime description after adding a new endpoint
                     * since the runtime description information could have references to old
                     * information. See MarshalServiceRuntimeDescriptionFactory and 
                     * MarshalServiceRuntimeDescription.
                     */
                    resetServiceRuntimeDescription();
                } else
                if (getEndpointSEI(portQName) == null && !endpointDescription.isDynamicPort()) {
                    // Existing endpointDesc from a declared port needs to be updated with an SEI
                    // Note that an EndpointDescritption created from an addPort (i.e. a dynamic port) can not do this.
                    endpointDescription.updateWithSEI(sei);
                } else if (getEndpointSEI(portQName) != sei) {
                    // TODO: RAS & NLS
                    throw ExceptionFactory.makeWebServiceException(
                            "ServiceDescription.updateEndpointDescription: Can't do a getPort() specifiying a different SEI than the previous getPort().  PortQN: "
                                    + portQName + "; current SEI: " + sei + "; previous SEI: " +
                                    getEndpointSEI(portQName));
                } else {
                    // All error check above passed, the EndpointDescription already exists and needs no updating
                }
                break;

            case CREATE_DISPATCH:
                // Port may or may not exist in WSDL.
                // If an endpointDesc doesn't exist and it is in the WSDL, it can be created
                // Otherwise, it is an error.
                if (DescriptionUtils.isEmpty(portQName)) {
                    throw ExceptionFactory
                            .makeWebServiceException(Messages.getMessage("createDispatchFail0"));
                } else if (endpointDescription != null) {
                    // The EndpoingDescription already exists; nothing needs to be done
                } else if (sei != null) {
                    // The Dispatch should not have an SEI associated with it on the update call.
                    // REVIEW: Is this a valid check?
                    throw ExceptionFactory.makeWebServiceException(
                            "ServiceDescription.updateEndpointDescription: Can not specify an SEI when creating a Dispatch. PortQN: " +
                                    portQName);
                } else if (getWSDLWrapper() != null && isPortDeclared) {
                    // EndpointDescription doesn't exist and this is a declared Port, so create one
                    // Use the SEI Class and its annotations to finish creating the Description hierachy.  Note that EndpointInterface, Operations, Parameters, etc.
                    // are not created for Dipsatch-based ports, but might be updated later if a getPort is done against the same declared port.
                    // TODO: Need to create the Axis Description objects after we have all the config info (i.e. from this SEI)
                    endpointDescription = new EndpointDescriptionImpl(sei, portQName, this);
                    addEndpointDescription(endpointDescription);
                } else {
                    // The port is not a declared port and it does not have an EndpointDescription, meaning an addPort has not been done for it
                    // This is an error.
                    throw ExceptionFactory.makeWebServiceException(
                            Messages.getMessage("createDispatchFail1", portQName.toString()));
                }
                break;
        }
        return endpointDescription;
    }

    private Class getEndpointSEI(QName portQName) {
        Class endpointSEI = null;
        EndpointDescription endpointDesc = getEndpointDescription(portQName);
        if (endpointDesc != null) {
            EndpointInterfaceDescription endpointInterfaceDesc = 
                endpointDesc.getEndpointInterfaceDescription();
            if (endpointInterfaceDesc != null ) {
                endpointSEI = endpointInterfaceDesc.getSEIClass();
            }
        }
        return endpointSEI;
    }

    private boolean isPortDeclared(QName portQName) {
        // TODO: This needs to account for declaration of the port via annotations in addition to just WSDL
        // TODO: Add logic to check the portQN namespace against the WSDL Definition NS
        boolean portIsDeclared = false;
        if (!DescriptionUtils.isEmpty(portQName)) {
            if (getWSDLWrapper() != null) {
                Definition wsdlDefn = getWSDLWrapper().getDefinition();
                Service wsdlService = wsdlDefn.getService(serviceQName);
                Port wsdlPort = wsdlService.getPort(portQName.getLocalPart());
                portIsDeclared = (wsdlPort != null);
            } else {
                // TODO: Add logic to determine if port is declared via annotations when no WSDL is present.  For now, we have to assume it is declared 
                // so getPort(...) and createDispatch(...) calls work when there is no WSDL.
                portIsDeclared = true;
            }
        } else {
            // PortQName is null, so the runtime gets to choose which one to use.  Since there's no WSDL
            // we'll use annotations, so it is implicitly declared
            portIsDeclared = true;
        }
        return portIsDeclared;
    }

    /* (non-Javadoc)
    * @see org.apache.axis2.jaxws.description.ServiceDescription#getEndpointDescriptions()
    */
    public EndpointDescription[] getEndpointDescriptions() {
        return endpointDescriptions.values().toArray(new EndpointDescriptionImpl[0]);
    }

    public Collection<EndpointDescription> getEndpointDescriptions_AsCollection() {
        return endpointDescriptions.values();
    }

    /* (non-Javadoc)
    * @see org.apache.axis2.jaxws.description.ServiceDescription#getEndpointDescription(javax.xml.namespace.QName)
    */
    public EndpointDescription getEndpointDescription(QName portQName) {
        EndpointDescription returnDesc = null;
        if (!DescriptionUtils.isEmpty(portQName)) {
            returnDesc = endpointDescriptions.get(portQName);
        }
        return returnDesc;
    }

    EndpointDescriptionImpl getEndpointDescriptionImpl(QName portQName) {
        return (EndpointDescriptionImpl)getEndpointDescription(portQName);
    }
    
    EndpointDescriptionImpl getEndpointDescriptionImpl(Class seiClass) {
        for (EndpointDescription endpointDescription : endpointDescriptions.values()) {
            EndpointInterfaceDescription endpointInterfaceDesc =
                    endpointDescription.getEndpointInterfaceDescription();
            // Note that Dispatch endpoints will not have an endpointInterface because the do not have an associated SEI
            if (endpointInterfaceDesc != null) {
                Class endpointSEIClass = endpointInterfaceDesc.getSEIClass();
                if (endpointSEIClass != null && endpointSEIClass.equals(seiClass)) {
                    return (EndpointDescriptionImpl)endpointDescription;
                }
            }
        }
        return null;
    }

    DescriptionBuilderComposite getDescriptionBuilderComposite() {
        return composite;
    }

    /* (non-Javadoc)
     * @see org.apache.axis2.jaxws.description.ServiceDescription#getEndpointDescription(java.lang.Class)
     */
    public EndpointDescription[] getEndpointDescription(Class seiClass) {
        EndpointDescription[] returnEndpointDesc = null;
        ArrayList<EndpointDescriptionImpl> matchingEndpoints =
                new ArrayList<EndpointDescriptionImpl>();
        for (EndpointDescription endpointDescription : endpointDescriptions.values()) {
            EndpointInterfaceDescription endpointInterfaceDesc =
                    endpointDescription.getEndpointInterfaceDescription();
            // Note that Dispatch endpoints will not have an endpointInterface because the do not have an associated SEI
            if (endpointInterfaceDesc != null) {
                Class endpointSEIClass = endpointInterfaceDesc.getSEIClass();
                if (endpointSEIClass != null && endpointSEIClass.equals(seiClass)) {
                    matchingEndpoints.add((EndpointDescriptionImpl)endpointDescription);
                }
            }
        }
        if (matchingEndpoints.size() > 0) {
            returnEndpointDesc = matchingEndpoints.toArray(new EndpointDescriptionImpl[0]);
        }
        return returnEndpointDesc;
    }

    /*
    * @return True - if we are processing with the DBC List instead of reflection
    */
    boolean isDBCMap() {
        if (dbcMap == null)
            return false;
        else
            return true;
    }

    // END of public accessor methods
    /*=======================================================================*/
    /*=======================================================================*/
    private void addEndpointDescription(EndpointDescriptionImpl endpoint) {
        endpointDescriptions.put(endpoint.getPortQName(), endpoint);
    }

    private void setupWsdlDefinition() {
        // Note that there may be no WSDL provided, for example when called from 
        // Service.create(QName serviceName).

        if (isDBCMap()) {

            //  Currently, there is a bug which allows the wsdlDefinition to be placed
            //  on either the impl class composite or the sei composite, or both. We need to
            //  look in both places and find the correct one, if it exists.

            if (((composite.getWebServiceAnnot() != null) &&
                    DescriptionUtils.isEmpty(composite.getWebServiceAnnot().endpointInterface()))
                    ||
                    (!(composite.getWebServiceProviderAnnot() == null))) {
                //This is either an implicit SEI, or a WebService Provider
                if (composite.getWsdlDefinition() != null) {
                    this.wsdlURL = composite.getWsdlURL();

                    try {
                        this.wsdlWrapper = new WSDL4JWrapper(this.wsdlURL,
                                                             composite.getWsdlDefinition());
                    } catch (WSDLException e) {
                        throw ExceptionFactory.makeWebServiceException(
                                Messages.getMessage("wsdlException", e.getMessage()), e);
                    }
                }

            } else if (composite.getWebServiceAnnot() != null) {
                //This impl class specifies an SEI...this is a special case. There is a bug
                //in the tooling that allows for the wsdllocation to be specifed on either the
                //impl. class, or the SEI, or both. So, we need to look for the wsdl as follows:
                //          1. If the Wsdl exists on the SEI, then check for it on the impl.
                //          2. If it is not found in either location, in that order, then generate

                DescriptionBuilderComposite seic =
                        getDBCMap().get(composite.getWebServiceAnnot().endpointInterface());

                try {
                    if (seic.getWsdlDefinition() != null) {
                        //set the sdimpl from the SEI composite
                        this.wsdlURL = seic.getWsdlURL();
                        this.wsdlWrapper =
                                new WSDL4JWrapper(seic.getWsdlURL(), seic.getWsdlDefinition());
                    } else if (composite.getWsdlDefinition() != null) {
                        //set the sdimpl from the impl. class composite
                        this.wsdlURL = composite.getWsdlURL();
                        this.wsdlWrapper = new WSDL4JWrapper(composite.getWsdlURL(),
                                                             composite.getWsdlDefinition());
                    }
                } catch (WSDLException e) {
                    throw ExceptionFactory.makeWebServiceException(
                            Messages.getMessage("wsdlException", e.getMessage()), e);
                }
            }

            //Deprecate this code block when MDQ is fully integrated
        } else if (wsdlURL != null) {
            try {
                this.wsdlWrapper = new WSDL4JWrapper(this.wsdlURL);
            }
            catch (FileNotFoundException e) {
                throw ExceptionFactory.makeWebServiceException(
                        Messages.getMessage("wsdlNotFoundErr", e.getMessage()), e);
            }
            catch (UnknownHostException e) {
                throw ExceptionFactory.makeWebServiceException(
                        Messages.getMessage("unknownHost", e.getMessage()), e);
            }
            catch (ConnectException e) {
                throw ExceptionFactory.makeWebServiceException(
                        Messages.getMessage("connectionRefused", e.getMessage()), e);
            }
            catch(IOException e) {
                throw ExceptionFactory.makeWebServiceException(Messages.getMessage("urlStream", e.getMessage()), e);
            }
            catch (WSDLException e) {
                throw ExceptionFactory.makeWebServiceException(
                        Messages.getMessage("wsdlException", e.getMessage()), e);
            }
        }
    }

    // TODO: Remove these and replace with appropraite get* methods for WSDL information
    /* (non-Javadoc)
     * @see org.apache.axis2.jaxws.description.ServiceDescriptionWSDL#getWSDLWrapper()
     */
    public WSDLWrapper getWSDLWrapper() {
        return wsdlWrapper;
    }

    /* (non-Javadoc)
    * @see org.apache.axis2.jaxws.description.ServiceDescriptionWSDL#getWSDLLocation()
    */
    public URL getWSDLLocation() {
        return wsdlURL;
    }

    /**
     * TODO: This method should be replaced with specific methods for getWSDLGenerated... similar to
     * how getWsdlWrapper should be replaced.
     */
    public WSDLWrapper getGeneratedWsdlWrapper() {
        return this.generatedWsdlWrapper;
    }

    void setAxisConfigContext(ConfigurationContext config) {
        this.configContext = config;
    }
    
    /* (non-Javadoc)
    * @see org.apache.axis2.jaxws.description.ServiceDescription#getAxisConfigContext()
    */
    public ConfigurationContext getAxisConfigContext() {
        if (configContext == null) {
            configContext = getClientConfigurationFactory().getClientConfigurationContext();
        }
        return configContext;

    }

    ClientConfigurationFactory getClientConfigurationFactory() {

        if (clientConfigFactory == null) {
            clientConfigFactory = DescriptionFactory.createClientConfigurationFactory();
        }
        return clientConfigFactory;
    }

    /* (non-Javadoc)
    * @see org.apache.axis2.jaxws.description.ServiceDescription#getServiceClient(javax.xml.namespace.QName)
    */
    public ServiceClient getServiceClient(QName portQName) {
        ServiceClient returnServiceClient = null;
        if (!DescriptionUtils.isEmpty(portQName)) {
            EndpointDescription endpointDesc = getEndpointDescription(portQName);
            if (endpointDesc != null) {
                returnServiceClient = endpointDesc.getServiceClient();
            }
            else {
                // Couldn't find Endpoint Description for port QName
                if (log.isDebugEnabled()) {
                    log.debug("Could not find portQName: " + portQName 
                            + " under ServiceDescription: " + toString());
                }
            }
        }
        else {
            // PortQName is empty
            if (log.isDebugEnabled()) {
                log.debug("PortQName agrument is invalid; it can not be null or an empty string: " + portQName);
            }
        }
        
        return returnServiceClient;
    }

    /* (non-Javadoc)
    * @see org.apache.axis2.jaxws.description.ServiceDescription#getServiceQName()
    */
    public QName getServiceQName() {
        //It is assumed that this will always be set in the constructor rather than
        //built up from the class or DBC
        return serviceQName;
    }

    void setServiceQName(QName theName) {
        serviceQName = theName;
    }


    public boolean isServerSide() {
        return isServerSide;
    }

    HashMap<String, DescriptionBuilderComposite> getDBCMap() {
        return dbcMap;
    }

    void setGeneratedWsdlWrapper(WSDL4JWrapper wrapper) {
        this.generatedWsdlWrapper = wrapper;
    }

    void setWsdlWrapper(WSDL4JWrapper wrapper) {
        this.wsdlWrapper = wrapper;
    }

    private void validateDBCLIntegrity() {

        //First, check the integrity of this input composite
        //and retrieve
        //the composite that represents this impl

//TODO: Currently, we are calling this method on the DBC. However, the DBC
//will eventually need access to to the whole DBC map to do proper validation.
//We don't want to pass the map of DBC's back into a single DBC.
//So, for starters, this method and all the privates that it calls should be 
// moved to here. At some point, we should consider using a new class that we
//can implement scenarios of, like validateServiceImpl implements validator

        try {
            validateIntegrity();
        }
        catch (Exception ex) {
            if (log.isDebugEnabled()) {
                log.debug("Validation phase 1 failure: " + ex.toString(), ex);
                log.debug("Failing composite: " + composite.toString());
            }
            throw ExceptionFactory.makeWebServiceException("Validation Exception " + ex, ex);
        }
    }

    /*
      * Validates the integrity of an impl. class. This should not be called directly for an SEI composite
      */
    void validateIntegrity() {
        //TODO: Consider moving this to a utils area, do we really want a public
        //      method that checks integrity...possibly

        //In General, this integrity checker should do gross level checking
        //It should not be setting spec-defined default values, but can look
        //at things like empty strings or null values

        //TODO: This method will validate the integrity of this object. Basically, if
        //consumer set this up improperly, then we should fail fast, should consider placing
        //this method in a utils class within the 'description' package

        //Verify that, if this implements a strongly typed provider interface, that it
        // also contain a WebServiceProvider annotation per JAXWS Sec. 5.1
        Iterator<String> iter =
                composite.getInterfacesList().iterator();

        // Remember if we've validated the Provider interface.  Later we'll make sure that if we have an 
        // WebServiceProvider annotation, we found a valid interface here.
        boolean providerInterfaceValid = false;
        while (iter.hasNext()) {
            String interfaceString = iter.next();
            if (interfaceString.equals(MDQConstants.PROVIDER_SOURCE)
                    || interfaceString.equals(MDQConstants.PROVIDER_SOAP)
                    || interfaceString.equals(MDQConstants.PROVIDER_DATASOURCE)
                    || interfaceString.equals(MDQConstants.PROVIDER_STRING)) {
                providerInterfaceValid = true;
                //This is a provider based endpoint, make sure the annotation exists
                if (composite.getWebServiceProviderAnnot() == null) {
                    // TODO: RAS/NLS
                    throw ExceptionFactory.makeWebServiceException(
                            "Validation error: This is a Provider based endpoint that does not contain a WebServiceProvider annotation.  Provider class: " +
                                    composite.getClassName());
                }
            }
        }

        //Verify that WebService and WebServiceProvider are not both specified
        //per JAXWS - Sec. 7.7
        if (composite.getWebServiceAnnot() != null &&
                composite.getWebServiceProviderAnnot() != null) {
            // TODO: RAS/NLS
            throw ExceptionFactory.makeWebServiceException(
                    "Validation error: WebService annotation and WebServiceProvider annotation cannot coexist.  Implementation class: " +
                            composite.getClassName());
        }

        if (composite.getWebServiceProviderAnnot() != null) {
            if (!providerInterfaceValid) {
                // TODO: RAS/NLS
                throw ExceptionFactory.makeWebServiceException(
                        "Validation error: This is a Provider that does not specify a valid Provider interface.   Implementation class: " +
                                composite.getClassName());
            }
            // There must be a public default constructor per JAXWS - Sec 5.1
            if (!validateDefaultConstructor()) {
                // TODO: RAS/NLS
                throw ExceptionFactory.makeWebServiceException(
                        "Validation error: Provider must have a public default constructor.  Implementation class: " +
                                composite.getClassName());
            }
            // There must be an invoke method per JAXWS - Sec 5.1.1
            if (!validateInvokeMethod()) {
                // TODO: RAS/NLS
                throw ExceptionFactory.makeWebServiceException(
                        "Validation error: Provider must have a public invoke method.  Implementation class: " +
                                composite.getClassName());
            }

            //If ServiceMode annotation specifies 'payload', then make sure that it is not typed with
            // SOAPMessage or DataSource
            validateProviderInterfaces();

        } else if (composite.getWebServiceAnnot() != null) {

            if (composite.getServiceModeAnnot() != null) {
                // TODO: RAS/NLS
                throw ExceptionFactory.makeWebServiceException(
                        "Validation error: ServiceMode annotation can only be specified for WebServiceProvider.   Implementation class: " +
                                composite.getClassName());
            }

            //TODO: hmmm, will we ever actually validate an interface directly...don't think so
            if (!composite.isInterface()) {
                // TODO: Validate on the class that this.classModifiers Array does not contain the strings
                //        FINAL or ABSTRACT, but does contain PUBLIC
                // TODO: Validate on the class that a public constructor exists
                // TODO: Validate on the class that a finalize() method does not exist
                if (!DescriptionUtils.isEmpty(composite.getWebServiceAnnot().wsdlLocation())) {
                    if (composite.getWsdlDefinition() == null && composite.getWsdlURL() == null) {
                        // TODO: RAS/NLS
                        throw ExceptionFactory.makeWebServiceException(
                                "Validation error: cannot find WSDL Definition specified by this WebService annotation. Implementation class: "
                                        + composite.getClassName() + "; WSDL location: " +
                                        composite.getWebServiceAnnot().wsdlLocation());
                    }
                }

                //		setWebServiceAnnotDefaults(true=impl); Must happen before we start checking annot
                if (!DescriptionUtils.isEmpty(composite.getWebServiceAnnot().endpointInterface())) {

                    DescriptionBuilderComposite seic =
                            dbcMap.get(composite.getWebServiceAnnot().endpointInterface());

                    //Verify that we can find the SEI in the composite list
                    if (seic == null) {
                        // TODO: RAS/NLS
                        throw ExceptionFactory.makeWebServiceException(
                                "Validation error: cannot find SEI specified by the WebService.endpointInterface.  Implementaiton class: "
                                        + composite.getClassName() + "; EndpointInterface: " +
                                        composite.getWebServiceAnnot().endpointInterface());
                    }

                    // Verify that the only class annotations are WebService and HandlerChain
                    // (per JSR181 Sec. 3.1).  Note that this applies to JSR-181 annotations; the restriction
                    // does not apply to JSR-224 annotations such as BindingType
                    if (composite.getSoapBindingAnnot() != null
                            || composite.getWebFaultAnnot() != null
                            || composite.getWebServiceClientAnnot() != null
                            || composite.getWebServiceContextAnnot() != null
                            || !composite.getAllWebServiceRefAnnots().isEmpty()
                            ) {
                        // TODO: RAS/NLS
                        throw ExceptionFactory.makeWebServiceException(
                                "Validation error: invalid annotations specified when WebService annotation specifies an endpoint interface.  Implemntation class:  "
                                        + composite.getClassName());
                    }

                    //Verify that WebService annotation does not contain a name attribute
                    //(per JSR181 Sec. 3.1)
                    if (!DescriptionUtils.isEmpty(composite.getWebServiceAnnot().name())) {
                        // TODO: RAS/NLS
                        throw ExceptionFactory.makeWebServiceException(
                                "Validation error: WebService.name must not be specified when the bean specifies an endpoint interface.  Implentation class: "
                                        + composite.getClassName() + "; WebService.name: " +
                                        composite.getWebServiceAnnot().name());
                    }

                    validateSEI(seic);
                    //Verify that that this implementation class implements all methods in the interface
                    validateImplementation(seic);

                    //Verify that this impl. class does not contain any @WebMethod annotations
                    if (webMethodAnnotationsExist()) {
                        // TODO: RAS/NLS
                        throw ExceptionFactory.makeWebServiceException(
                                "Validation error: WebMethod annotations cannot exist on implentation when WebService.endpointInterface is set.  Implementation class: " +
                                        composite.getClassName());
                    }


                } else { //this is an implicit SEI (i.e. impl w/out endpointInterface

                    
                    checkImplicitSEIAgainstWSDL();
                    //	TODO:	Call ValidateWebMethodAnnots()
                    //			- this method will check that all methods are public - ???
                    //
                }
            } else { //this is an interface...we should not be processing interfaces here
                // TODO: RAS/NLS
                throw ExceptionFactory.makeWebServiceException(
                        "Validation error: Improper usage: cannot invoke this method with an interface.  Implementation class: "
                                + composite.getClassName());
            }

            // Verify that the SOAPBinding annotations are supported.
            //REVIEW: The assumption here is that SOAPBinding annotation can be place on an impl. class
            //        (implicit SEI) or a WebServiceProvider
            if (composite.getSoapBindingAnnot() != null) {
                if (composite.getSoapBindingAnnot().use() == javax.jws.soap.SOAPBinding.Use.ENCODED) {
                    throw ExceptionFactory.makeWebServiceException("Validation error: Unsupported SOAPBinding annotation value. The ENCODED setting is not supported for SOAPBinding.Use. Implementation class: " 
                                +composite.getClassName());  
                }
            }
            
            //TODO: don't think this is necessary
            checkMethodsAgainstWSDL();
        }
    }

    /**
     * Validate there is an invoke method on the composite.
     *
     * @return
     */
    private boolean validateInvokeMethod() {
        boolean validInvokeMethod = false;
        List<MethodDescriptionComposite> invokeMethodList =
                composite.getMethodDescriptionComposite("invoke");
        if (invokeMethodList != null && !invokeMethodList.isEmpty()) {
            validInvokeMethod = true;
        }
        return validInvokeMethod;
    }

    /**
     * Validate that, if using PAYLOAD mode, then interfaces list cannot contain SOAPMessage or
     * DataSource
     *
     * @return
     */
    private void validateProviderInterfaces() {

        // Default for ServiceMode is 'PAYLOAD'. So, if it is specified  (explicitly or
        // implicitly) then verify that we are not implementing improper interfaces)
        if ((composite.getServiceModeAnnot() == null)
                || composite.getServiceModeAnnot().value() == javax.xml.ws.Service.Mode.PAYLOAD) {

            Iterator<String> iter = composite.getInterfacesList().iterator();

            while (iter.hasNext()) {
                String interfaceString = iter.next();
                if (interfaceString.equals(MDQConstants.PROVIDER_SOAP)
                        || interfaceString.equals(MDQConstants.PROVIDER_DATASOURCE)) {

                    throw ExceptionFactory
                            .makeWebServiceException(
                                    "Validation error: SOAPMessage and DataSource objects cannot be used when ServiceMode specifies PAYLOAD. Implementation class: "
                                            + composite.getClassName());
                }
            }

        } else {
            // We are in MESSAGE mode
            // Conformance: JAXWS Spec.- Sec. 4.3 (javax.activation.DataSource)

            // REVIEW: Should the provider interface validation be moved to post-construction validation, 
            // since it seems that the logic to understand the default values for binding type 
            // (see comment below) should be left to the creation of the Description objects.
            String bindingType = null;
            if (composite.getBindingTypeAnnot() != null) {
                bindingType = composite.getBindingTypeAnnot().value();
            }

            Iterator<String> iter = composite.getInterfacesList().iterator();

            while (iter.hasNext()) {
                String interfaceString = iter.next();

                if (interfaceString.equals(MDQConstants.PROVIDER_SOAP)) {

                    // Make sure BindingType is SOAP/HTTP with SOAPMessage
                    // object, Default for Binding Type is SOAP/HTTP
                    if (!DescriptionUtils.isEmpty(bindingType)
                            && !bindingType
                            .equals(SOAPBinding.SOAP11HTTP_BINDING)
                            && !bindingType
                            .equals(SOAPBinding.SOAP11HTTP_MTOM_BINDING)
                            && !bindingType
                            .equals(SOAPBinding.SOAP12HTTP_BINDING)
                            && !bindingType
                            .equals(SOAPBinding.SOAP12HTTP_MTOM_BINDING))

                        throw ExceptionFactory
                                .makeWebServiceException(
                                        "Validation error: SOAPMessage objects cannot be used with HTTP binding type. Implementation class: "
                                                + composite.getClassName());

                } else if (interfaceString
                        .equals(MDQConstants.PROVIDER_DATASOURCE)) {

                    // Make sure BindingType is XML/HTTP with DataSource object
                    if (DescriptionUtils.isEmpty(bindingType)
                            || !bindingType
                            .equals(javax.xml.ws.http.HTTPBinding.HTTP_BINDING))

                        throw ExceptionFactory
                                .makeWebServiceException(
                                        "Validation error: DataSource objects must be used with HTTP binding type. Implementation class: "
                                                + composite.getClassName());
                }
            }
        }
    }


    /**
     * Validate there is a default no-argument constructor on the composite.
     *
     * @return
     */
    private boolean validateDefaultConstructor() {
        boolean validDefaultCtor = false;
        List<MethodDescriptionComposite> constructorList =
                composite.getMethodDescriptionComposite("<init>");
        if (constructorList != null && !constructorList.isEmpty()) {
            // There are public constructors; make sure there is one that takes no arguments.
            for (MethodDescriptionComposite checkCtor : constructorList) {
                List<ParameterDescriptionComposite> paramList =
                        checkCtor.getParameterDescriptionCompositeList();
                if (paramList == null || paramList.isEmpty()) {
                    validDefaultCtor = true;
                    break;
                }
            }
        }

        return validDefaultCtor;
    }

    private void validateImplementation(DescriptionBuilderComposite seic) {
        /*
           *	Verify that an impl class implements all the methods of the SEI. We
           *  have to verify this because an impl class is not required to actually use
           *  the 'implements' clause. So, if it doesn't, the Java compiler won't
           *	catch it. Don't need to worry about chaining because only one EndpointInterface
           *  can be specified, and the SEI cannot specify an EndpointInterface, so the Java
           *	compiler will take care of everything else.
           */

        HashMap<String, MethodDescriptionComposite> compositeHashMap = 
            new HashMap<String, MethodDescriptionComposite>();
        Iterator<MethodDescriptionComposite> compIterator =
                composite.getMethodDescriptionsList().iterator();
        while (compIterator.hasNext()) {
            MethodDescriptionComposite mdc = compIterator.next();
            compositeHashMap.put(mdc.getMethodName(), mdc);
        }
        // Add methods declared in the implementation's superclass
        addSuperClassMethods(compositeHashMap, composite);

        HashMap<String, MethodDescriptionComposite> seiMethodHashMap = 
            new HashMap<String, MethodDescriptionComposite>();
        Iterator<MethodDescriptionComposite> seiMethodIterator =
                seic.getMethodDescriptionsList().iterator();
        while (seiMethodIterator.hasNext()) {
            MethodDescriptionComposite mdc = seiMethodIterator.next();
            seiMethodHashMap.put(mdc.getMethodName(), mdc);
        }
        // Add any methods declared in superinterfaces of the SEI
        addSuperClassMethods(seiMethodHashMap, seic);

        // Make sure all the methods in the SEI (including any inherited from superinterfaces) are
        // implemented by the bean (including inherited methods on the bean).
        Iterator<MethodDescriptionComposite> verifySEIIterator =
                seiMethodHashMap.values().iterator();
        while (verifySEIIterator.hasNext()) {
            MethodDescriptionComposite mdc = verifySEIIterator.next();
            // TODO: This does not take into consideration overloaded java methods!
            MethodDescriptionComposite implMDC = compositeHashMap.get(mdc.getMethodName());
            
            if (implMDC == null) {
                // TODO: RAS/NLS
                throw ExceptionFactory.makeWebServiceException(
                        "Validation error: Implementation subclass does not implement method on specified interface.  Implementation class: "
                                + composite.getClassName() + "; missing method name: " +
                                mdc.getMethodName() + "; endpointInterface: " +
                                seic.getClassName());
            } else {
                //At least we found it, now make sure that signatures match up
                
                //Check for exception and signature matching
                validateMethodExceptions(mdc, implMDC, seic.getClassName());
                validateMethodReturnValue(mdc, implMDC, seic.getClassName());
                validateMethodParameters(mdc, implMDC, seic.getClassName());
            }
        }
    }

    private void validateMethodParameters(MethodDescriptionComposite seiMDC,
        MethodDescriptionComposite implMDC, String className) {
        List<ParameterDescriptionComposite> seiPDCList = seiMDC
            .getParameterDescriptionCompositeList();
        List<ParameterDescriptionComposite> implPDCList = implMDC
            .getParameterDescriptionCompositeList();
        if ((seiPDCList == null || seiPDCList.isEmpty())
            && (implPDCList == null || implPDCList.isEmpty())) {
            // There are no parameters on the SEI or the impl; all is well
        } else if ((seiPDCList == null || seiPDCList.isEmpty())
            && !(implPDCList == null || implPDCList.isEmpty())) {
            String message = "Validation error: SEI indicates no parameters but implementation method specifies parameters: "
                + implPDCList
                + "; Implementation class: "
                + composite.getClassName()
                + "; Method name: " + seiMDC.getMethodName() + "; Endpoint Interface: " + className;
            throw ExceptionFactory.makeWebServiceException(message);
        } else if ((seiPDCList != null && !seiPDCList.isEmpty())
            && !(implPDCList != null && !implPDCList.isEmpty())) {
            String message = "Validation error: SEI indicates parameters " + seiPDCList
                + " but implementation method specifies no parameters; Implementation class: "
                + composite.getClassName() + "; Method name: " + seiMDC.getMethodName()
                + "; Endpoint Interface: " + className;
            throw ExceptionFactory.makeWebServiceException(message);
        } else if (seiPDCList.size() != implPDCList.size()) {
            String message = "Validation error: The number of parameters on the SEI method ("
                + seiPDCList.size()
                + ") does not match the number of parameters on the implementation ( "
                + implPDCList.size() + "); Implementation class: " + composite.getClassName()
                + "; Method name: " + seiMDC.getMethodName() + "; Endpoint Interface: " + className;
            throw ExceptionFactory.makeWebServiceException(message);

        } else {
            // Make sure the order and type of parameters match
            // REVIEW: This checks for strict equality of the fully qualified
            // type. It does not
            // take into consideration object hierachy. For example foo(Animal)
            // will not equal bar(Zebra)
            boolean parametersMatch = true;
            String failingMessage = null;
            for (int paramNumber = 0; paramNumber < seiPDCList.size(); paramNumber++) {
                String seiParamType = seiPDCList.get(paramNumber).getParameterType();
                String implParamType = implPDCList.get(paramNumber).getParameterType();
                if (!seiParamType.equals(implParamType)) {
                    parametersMatch = false;
                    failingMessage = "Validation error: SEI and implementation parameters do not match.  Parameter number "
                        + paramNumber
                        + " on the SEI is "
                        + seiParamType
                        + "; on the implementation it is "
                        + implParamType
                        + "; Implementation class: "
                        + composite.getClassName()
                        + "; Method name: "
                        + seiMDC.getMethodName() + "; Endpoint Interface: " + className;
                    break;
                }
            }
            if (!parametersMatch) {
                throw ExceptionFactory.makeWebServiceException(failingMessage);
            }
        }
    }

    private void validateMethodReturnValue(MethodDescriptionComposite seiMDC,
        MethodDescriptionComposite implMDC, String className) {
        String seiReturnValue = seiMDC.getReturnType();
        String implReturnValue = implMDC.getReturnType();

        if (seiReturnValue == null && implReturnValue == null) {
            // Neither specify a return value; all is well
        } else if (seiReturnValue == null && implReturnValue != null) {
            String message = "Validation error: SEI indicates no return value but implementation method specifies return value: "
                + implReturnValue
                + "; Implementation class: "
                + composite.getClassName()
                + "; Method name: " + seiMDC.getMethodName() + "; Endpoint Interface: " + className;
            throw ExceptionFactory.makeWebServiceException(message);
        } else if (seiReturnValue != null && implReturnValue == null) {
            String message = "Validation error: SEI indicates return value " + seiReturnValue
                + " but implementation method specifies no return value; Implementation class: "
                + composite.getClassName() + "; Method name: " + seiMDC.getMethodName()
                + "; Endpoint Interface: " + className;
            throw ExceptionFactory.makeWebServiceException(message);
        } else if (!seiReturnValue.equals(implReturnValue)) {
            String message = "Validation error: SEI return value " + seiReturnValue
                + " does not match implementation method return value " + implReturnValue
                + "; Implementation class: " + composite.getClassName() + "; Method name: "
                + seiMDC.getMethodName() + "; Endpoint Interface: " + className;
            throw ExceptionFactory.makeWebServiceException(message);
        }

    }

    private void validateMethodExceptions ( MethodDescriptionComposite seiMDC, 
        MethodDescriptionComposite implMDC,
        String className) {

        String[] seiExceptions = seiMDC.getExceptions();
        String[] implExceptions = implMDC.getExceptions();

        // An impl can choose to throw fewer checked exceptions than declared on the SEI, but not more.
        // This is analagous to the Java rules for interfaces.
        if (seiExceptions == null) {
            if (implExceptions == null) {
                return;
            } else {
                // SEI delcares no checked exceptions, but the implementation has checked exceptions, which is an error
                throw ExceptionFactory.makeWebServiceException("Validation error: Implementation method signature has more checked exceptions than SEI method signature (0): Implementation class: "
                    + composite.getClassName() 
                    + "; method name: " + seiMDC.getMethodName() 
                    + "; endpointInterface: " + className);
            }
        } else if (implExceptions == null) {
            // Implementation throws fewer checked exceptions than SEI, which is OK.
            return;
        }
        
        // Check the list length; An implementation can not declare more exceptions than the SEI
        if (seiExceptions.length < implExceptions.length) {
            throw ExceptionFactory.makeWebServiceException("Validation error: Implementation method signature has more checked exceptions ("
                + implExceptions.length + ") than SEI method signature ("
                + seiExceptions.length + "): Implementation class: "
                + composite.getClassName() 
                + "; method name: " + seiMDC.getMethodName() 
                + "; endpointInterface: " + className);
        }
        
        // Make sure that each checked exception declared by the 
        // implementation is on the SEI also
        if (implExceptions.length > 0) {                
            for (String implException : implExceptions) {
                boolean foundIt = false;
                if (seiExceptions.length > 0) {         
                    for (String seiException : seiExceptions) {
                        if (seiException.equals(implException)) {
                            foundIt = true;
                            break;
                        }
                    }
                }
                
                if (!foundIt) {
                    throw ExceptionFactory.makeWebServiceException("Validation error: Implementation method signature throws exception " 
                        + implException + "which is not declared on the SEI method signature: Implementation class: "
                        + composite.getClassName() 
                        + "; method name: " + seiMDC.getMethodName() 
                        + "; endpointInterface: " + className);
                }
            }
        }

    }

    /**
     * Adds any methods declared in superclasses to the HashMap.  The hierachy starting with the DBC
     * will be walked up recursively, adding methods from each parent DBC encountered.
     * <p/>
     * Note that this can be used for either classes or interfaces.
     *
     * @param methodMap
     * @param dbc
     */
    private void addSuperClassMethods(HashMap methodMap, DescriptionBuilderComposite dbc) {
        DescriptionBuilderComposite superDBC = dbcMap.get(dbc.getSuperClassName());
        if (superDBC != null) {
            Iterator<MethodDescriptionComposite> mIter =
                    superDBC.getMethodDescriptionsList().iterator();
            while (mIter.hasNext()) {
                MethodDescriptionComposite mdc = mIter.next();
                methodMap.put(mdc.getMethodName(), mdc);
            }
            addSuperClassMethods(methodMap, superDBC);
        }
    }


    /*
      * This method verifies that, if there are any WebMethod with exclude == false, then
      * make sure that we find all of those methods represented in the wsdl. However, if
      * there are no exclusions == false, or there are no WebMethod annotations, then verify
      * that all the public methods are in the wsdl
      */
    private void checkMethodsAgainstWSDL() {
//Verify that, for ImplicitSEI, that all methods that should exist(if one false found, then
//only look for WebMethods w/ False, else take all public methods but ignore those with
//exclude == true
        if (webMethodAnnotationsExist()) {
            if (DescriptionUtils.falseExclusionsExist(composite))
                verifyFalseExclusionsWithWSDL();
            else
                verifyPublicMethodsWithWSDL();
        } else {
            verifyPublicMethodsWithWSDL();
        }
    }

    private void checkImplicitSEIAgainstWSDL() {

        //TODO: If there is a WSDL, then verify that all WebMethods on this class and in the
        //		superclasses chain are represented in the WSDL...Look at logic below to make
        //		sure this really happening


        if (webMethodAnnotationsExist()) {
            if (DescriptionUtils.falseExclusionsExist(composite))
                verifyFalseExclusionsWithWSDL();
            else
                verifyPublicMethodsWithWSDL();
        } else {
            verifyPublicMethodsWithWSDL();
        }

    }

    private void checkSEIAgainstWSDL() {
        //TODO: Place logic here to verify that each publicMethod with WebMethod annot
        //      is contained in the WSDL (If there is a WSDL) If we find
        //	    a WebMethod annotation, use its values for looking in the WSDL

    }

    private void validateSEI(DescriptionBuilderComposite seic) {

        //TODO: Validate SEI superclasses -- hmmm, may be doing this below
        //
        if (seic.getWebServiceAnnot() == null) {
            // TODO: RAS & NLS
            throw ExceptionFactory.makeWebServiceException(
                    "Validation error: SEI does not contain a WebService annotation.  Implementation class: "
                            + composite.getClassName() + "; SEI: " + seic.getClassName());
        }
        if (!seic.getWebServiceAnnot().endpointInterface().equals("")) {
            // TODO: RAS & NLS
            throw ExceptionFactory.makeWebServiceException(
                    "Validation error: SEI must not set a value for @WebService.endpointInterface.  Implementation class: "
                            + composite.getClassName() + "; SEI: " + seic.getClassName()
                            + "; Invalid endpointInterface value: " +
                            seic.getWebServiceAnnot().endpointInterface());
        }
        // Verify that the SOAPBinding annotations are supported.
        if (seic.getSoapBindingAnnot() != null) {
        if (seic.getSoapBindingAnnot().use() == javax.jws.soap.SOAPBinding.Use.ENCODED) {
               throw ExceptionFactory.makeWebServiceException("Validation error: Unsupported SOAPBinding annotation value. The ENCODED setting is not supported for SOAPBinding.Use. Implementation class: " 
                        +seic.getClassName());  
        }
       }

        checkSEIAgainstWSDL();

        //TODO: More validation here

        //TODO: Make sure we don't find any WebMethod annotations with exclude == true
        //		anywhere in the superclasses chain

        //TODO: Check that all WebMethod annotations in the superclass chain are represented in
        //		WSDL, assuming there is WSDL

        //TODO:	Validate that the interface is public

        //		Call ValidateWebMethodAnnots()
        //

        //This will perform validation for all methods, regardless of WebMethod annotations
        //It is called for the SEI, and an impl. class that does not specify an endpointInterface
        validateMethods(seic.getMethodDescriptionsList());
    }

    /** @return Returns TRUE if we find just one WebMethod Annotation */
    private boolean webMethodAnnotationsExist() {
        MethodDescriptionComposite mdc = null;
        Iterator<MethodDescriptionComposite> iter =
                composite.getMethodDescriptionsList().iterator();

        while (iter.hasNext()) {
            mdc = iter.next();

            if (mdc.getWebMethodAnnot() != null)
                return true;
        }

        return false;
    }

    private void verifyFalseExclusionsWithWSDL() {
        //TODO: Place logic here to verify that each exclude==false WebMethod annot we find
        //      is contained in the WSDL
    }

    private void verifyPublicMethodsWithWSDL() {
        //TODO: Place logic here to verify that each publicMethod with no WebMethod annot
        //      is contained in the WSDL

    }


    private void validateMethods(List<MethodDescriptionComposite> mdcList) {
        if (mdcList != null && !mdcList.isEmpty()) {
            for (MethodDescriptionComposite mdc : mdcList) {
                String returnType = mdc.getReturnType();
                if (returnType != null
                                && (returnType.equals(RETURN_TYPE_FUTURE) || returnType
                                                .equals(RETURN_TYPE_RESPONSE))) {
                    throw ExceptionFactory.makeWebServiceException(Messages
                                    .getMessage("serverSideAsync", mdc.getDeclaringClass(), mdc
                                                    .getMethodName()));
                }
            }
        }
        // TODO: Fill this out to validate all MethodDescriptionComposite (and
        // their inclusive
        //      annotations on this SEI (SEI is assumed here)
        //check oneway
        //

        //This could be an SEI, or an impl. class that doesn' specify an EndpointInterface (so, it
        //is implicitly an SEI...need to consider this
        //

        //TODO: Verify that, if this is an interface...that there are no Methods with WebMethod
        //      annotations that contain exclude == true

        //TODO: Verify that, if a SOAPBinding annotation exists, that its style be set to
        //      only DOCUMENT JSR181-Sec 4.7.1

    }

    private void validateWSDLOperations() {
        //Verifies that all operations on the wsdl are found in the impl/sei class
    }

    public boolean isWSDLSpecified() {
        boolean wsdlSpecified = false;
        if (getWSDLWrapper() != null) {
            wsdlSpecified = (getWSDLWrapper().getDefinition() != null);
        }
        return wsdlSpecified;
    }

    
    // ===========================================
    // ANNOTATION: HandlerChain
    // ===========================================

    /**
     * Returns a schema derived java class containing the the handler configuration filel
     *  
     * @return HandlerChainsType This is the top-level element for the Handler configuration file
     * 
     */
    public HandlerChainsType getHandlerChain() {

        if (handlerChainsType == null) {

            getAnnoHandlerChainAnnotation();
            if (handlerChainAnnotation != null) {

                String handlerFileName = handlerChainAnnotation.file();

                // TODO RAS & NLS
                if (log.isDebugEnabled()) {
                    if (composite != null) {
                        log.debug("EndpointDescriptionImpl.getHandlerChain: fileName: "
                                + handlerFileName + " className: " + composite.getClassName());
                    }
                    else {
                        log.debug("EndpointDescriptionImpl.getHandlerChain: fileName: "
                                + handlerFileName + " className: " + serviceClass.getName());
                    }
                }

                String className =
                        (composite != null) ? composite.getClassName() : serviceClass.getName();

                ClassLoader classLoader =
                        (composite != null) ? composite.getClassLoader() : this.getClass()
                                                                               .getClassLoader();

                InputStream is =
                        DescriptionUtils.openHandlerConfigStream(handlerFileName,
                                                                 className,
                                                                 classLoader);

                try {

                    // All the classes we need should be part of this package
                    JAXBContext jc =
                            JAXBContext.newInstance("org.apache.axis2.jaxws.description.xml.handler",
                                                    this.getClass().getClassLoader());

                    Unmarshaller u = jc.createUnmarshaller();

                    JAXBElement<?> o = (JAXBElement<?>) u.unmarshal(is);
                    handlerChainsType = (HandlerChainsType) o.getValue();

                } catch (Exception e) {
                    throw ExceptionFactory.makeWebServiceException("EndpointDescriptionImpl: getHandlerChain: thrown when attempting to unmarshall JAXB content");
                }
            }
        }
        return handlerChainsType;
    }

    
    /*
     * This is a client side only method. The generated service class may contain 
     * handler chain annotations
     */
    public HandlerChain getAnnoHandlerChainAnnotation() {
        if (this.handlerChainAnnotation == null) {
                if (serviceClass != null) {
                    handlerChainAnnotation =
                            (HandlerChain) serviceClass.getAnnotation(HandlerChain.class);
            }
        }

        return handlerChainAnnotation;
    }
    
    /* Returns the WSDL definiton as specified in the metadata.  Note that this WSDL may not be
     * complete.
     */
    public Definition getWSDLDefinition() {
        Definition defn = null;
        if (getWSDLWrapper() != null) {
            defn = getWSDLWrapper().getDefinition();
        }
        return defn;
    }

    /**
     * Returns the WSDL definiton as created by calling the WSDL generator.  This will be null
     * unless the WSDL definition provided by the metadata is incomplete
     */
    public Definition getWSDLGeneratedDefinition() {
        Definition defn = null;
        if (getGeneratedWsdlWrapper() != null) {
            defn = getGeneratedWsdlWrapper().getDefinition();
        }
        return defn;
    }

    public Service getWSDLService() {
        Service returnWSDLService = null;
        Definition defn = getWSDLDefinition();
        if (defn != null) {
            returnWSDLService = defn.getService(getServiceQName());
        }
        return returnWSDLService;
    }

    public Map getWSDLPorts() {
        Service wsdlService = getWSDLService();
        if (wsdlService != null) {
            return wsdlService.getPorts();
        } else {
            return null;
        }
    }

    public List<QName> getPorts() {
        ArrayList<QName> portList = new ArrayList<QName>();
        // Note that we don't cache these results because the list of ports can be added
        // to via getPort(...) and addPort(...).

        // If the WSDL is specified, get the list of ports under this service
        Map wsdlPortsMap = getWSDLPorts();
        if (wsdlPortsMap != null) {
            Iterator wsdlPortsIterator = wsdlPortsMap.values().iterator();
            // Note that the WSDL Ports do not have a target namespace associated with them.
            // JAXWS says to use the TNS from the Service.
            String serviceTNS = getServiceQName().getNamespaceURI();
            for (Port wsdlPort = null; wsdlPortsIterator.hasNext();) {
                wsdlPort = (Port)wsdlPortsIterator.next();
                String wsdlPortLocalPart = wsdlPort.getName();
                portList.add(new QName(serviceTNS, wsdlPortLocalPart));
            }
        }

        // Go through the list of Endpoints that have been created and add any
        // not already in the list.  This will include ports added to the Service
        // via getPort(...) and addPort(...)
        Collection<EndpointDescription> endpointDescs = getEndpointDescriptions_AsCollection();
        for (EndpointDescription endpointDesc : endpointDescs) {
            QName endpointPortQName = endpointDesc.getPortQName();
            if (!portList.contains(endpointPortQName)) {
                portList.add(endpointPortQName);
            }
        }
        return portList;
    }

    public List<Port> getWSDLPortsUsingPortType(QName portTypeQN) {
        ArrayList<Port> portList = new ArrayList<Port>();
        if (!DescriptionUtils.isEmpty(portTypeQN)) {
            Map wsdlPortMap = getWSDLPorts();
            if (wsdlPortMap != null && !wsdlPortMap.isEmpty()) {
                for (Object mapElement : wsdlPortMap.values()) {
                    Port wsdlPort = (Port)mapElement;
                    PortType wsdlPortType = wsdlPort.getBinding().getPortType();
                    QName wsdlPortTypeQN = wsdlPortType.getQName();
                    if (portTypeQN.equals(wsdlPortTypeQN)) {
                        portList.add(wsdlPort);
                    }
                }
            }
        }
        return portList;
    }

    public List<Port> getWSDLPortsUsingSOAPAddress(List<Port> wsdlPorts) {
        ArrayList<Port> portsUsingAddress = new ArrayList<Port>();
        if (wsdlPorts != null && !wsdlPorts.isEmpty()) {
            for (Port checkPort : wsdlPorts) {
                List extensibilityElementList = checkPort.getExtensibilityElements();
                for (Object checkElement : extensibilityElementList) {
                    if (EndpointDescriptionImpl
                            .isSOAPAddressElement((ExtensibilityElement)checkElement)) {
                        portsUsingAddress.add(checkPort);
                    }
                }
            }
        }
        return portsUsingAddress;
    }

    public ServiceRuntimeDescription getServiceRuntimeDesc(String name) {
        // TODO Add toString support
        return runtimeDescMap.get(name);
    }

    public void setServiceRuntimeDesc(ServiceRuntimeDescription srd) {
        // TODO Add toString support
        runtimeDescMap.put(srd.getKey(), srd);
    }
    
    private void resetServiceRuntimeDescription() {
        runtimeDescMap.clear();
    }

    /** Return a string representing this Description object and all the objects it contains. */
    public String toString() {
        final String newline = "\n";
        final String sameline = "; ";
        // This produces a TREMENDOUS amount of output if we have the WSDL Definition objects 
        // do a toString on themselves.
        boolean dumpWSDLContents = false;
        StringBuffer string = new StringBuffer();
        try {
            // Basic information
            string.append(super.toString());
            string.append(newline);
            string.append("ServiceQName: " + getServiceQName());
            // WSDL information
            string.append(newline);
            string.append("isWSDLSpecified: " + isWSDLSpecified());
            string.append(sameline);
            string.append("WSDL Location: " + getWSDLLocation());
            string.append(newline);
            if (dumpWSDLContents) {
                string.append("WSDL Definition: " + getWSDLDefinition());
                string.append(newline);
                string.append("Generated WSDL Definition: " + getWSDLGeneratedDefinition());
            } else {
                string.append("WSDL Definition available: " + (getWSDLDefinition() != null));
                string.append(sameline);
                string.append("Generated WSDL Definition available: " +
                        (getWSDLGeneratedDefinition() != null));
            }
            // Ports
            string.append(newline);
            List<QName> ports = getPorts();
            string.append("Number of ports: " + ports.size());
            string.append(newline);
            string.append("Port QNames: ");
            for (QName port : ports) {
                string.append(port + sameline);
            }
            // Axis Config information
            // We don't print out the config context because it will force one to be created
            // if it doesn't already exist.
            // string.append(newline);
            // string.append("ConfigurationContext: " + getAxisConfigContext());
            // EndpointDescriptions
            string.append(newline);
            Collection<EndpointDescription> endpointDescs = getEndpointDescriptions_AsCollection();
            if (endpointDescs == null) {
                string.append("EndpointDescription array is null");
            }
            else {
                string.append("Number of EndpointDescrptions: " + endpointDescs.size());
                string.append(newline);
                for (EndpointDescription endpointDesc : endpointDescs) {
                    string.append(endpointDesc.toString());
                    string.append(newline);
                }
            }
            string.append("RuntimeDescriptions:" + this.runtimeDescMap.size());
            string.append(newline);
            for (ServiceRuntimeDescription runtimeDesc : runtimeDescMap.values()) {
                string.append(runtimeDesc.toString());
                string.append(newline);
            }
        }
        catch (Throwable t) {
            string.append(newline);
            string.append("Complete debug information not currently available for " +
                    "ServiceDescription");
            return string.toString();
        }
        return string.toString();

    }
}
