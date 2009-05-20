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

package org.apache.axis2.jaxws.context.utils;

import org.apache.axiom.soap.SOAPHeader;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.jaxws.addressing.util.ReferenceParameterList;
import org.apache.axis2.jaxws.core.MessageContext;
import org.apache.axis2.jaxws.description.EndpointDescription;
import org.apache.axis2.jaxws.description.EndpointInterfaceDescription;
import org.apache.axis2.jaxws.description.OperationDescription;
import org.apache.axis2.jaxws.description.ServiceDescription;
import org.apache.axis2.jaxws.description.ServiceDescriptionWSDL;
import org.apache.axis2.jaxws.i18n.Messages;
import org.apache.axis2.jaxws.server.endpoint.lifecycle.impl.EndpointLifecycleManagerImpl;
import org.apache.axis2.jaxws.utility.JavaUtils;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext.Scope;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;


public class ContextUtils {
    private static final Log log = LogFactory.getLog(ContextUtils.class);

    /**
     * Adds the appropriate properties to the MessageContext that the user will see
     *
     * @param soapMessageContext
     * @param jaxwsMessageContext
     */
    public static void addProperties(SOAPMessageContext soapMessageContext,
                                     MessageContext jaxwsMessageContext) {

        // Copy Axis2 MessageContext properties.  It's possible that some set of Axis2 handlers
        // have run and placed some properties in the context that need to be visible.
        soapMessageContext.putAll(jaxwsMessageContext.getProperties());

        EndpointDescription description = jaxwsMessageContext.getEndpointDescription();
        if (description !=null) {
            // Set the WSDL properties
            ServiceDescription sd =
                    description.getServiceDescription();
            if (sd != null) {
                String wsdlLocation = ((ServiceDescriptionWSDL)sd).getWSDLLocation();
                if (wsdlLocation != null && !"".equals(wsdlLocation)) {
                    URI wsdlLocationURI = JavaUtils.createURI(wsdlLocation);
                    if (wsdlLocationURI == null) {
                        log.warn(Messages.getMessage("addPropertiesErr",
                                 wsdlLocation.toString(),description.getServiceQName().toString()));
                    }
                    setProperty(soapMessageContext, javax.xml.ws.handler.MessageContext.WSDL_DESCRIPTION, wsdlLocationURI, true);
                }
                setProperty(soapMessageContext, javax.xml.ws.handler.MessageContext.WSDL_SERVICE, description.getServiceQName(), true);
            }
        }

        //Lazily provide a list of available reference parameters.
        org.apache.axis2.context.MessageContext msgContext =
            jaxwsMessageContext.getAxisMessageContext();
        SOAPHeader header = null;
        if (msgContext != null &&
            msgContext.getEnvelope() != null) {
            header = msgContext.getEnvelope().getHeader();
        }
        List<Element> list = new ReferenceParameterList(header);
        
        setProperty(soapMessageContext, javax.xml.ws.handler.MessageContext.REFERENCE_PARAMETERS, list);
        if (log.isDebugEnabled()) {
            log.debug("Added reference parameter list.");
        }
        
        // If we are running within a servlet container, then JAX-WS requires that the
        // servlet related properties be set on the MessageContext
        ServletContext servletContext = 
            (ServletContext)jaxwsMessageContext.getProperty(HTTPConstants.MC_HTTP_SERVLETCONTEXT);
        if (servletContext != null) {
            log.debug("Servlet Context Set");
            setProperty(soapMessageContext, javax.xml.ws.handler.MessageContext.SERVLET_CONTEXT, servletContext);
        } else {
            log.debug("Servlet Context not found");
        }

        HttpServletRequest req = (HttpServletRequest)jaxwsMessageContext
                .getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
        if (req == null) {
            if (log.isDebugEnabled()) {
                log.debug("HTTPServletRequest not found");
            }
        } else {
            setProperty(soapMessageContext, javax.xml.ws.handler.MessageContext.SERVLET_REQUEST, req);
            if (log.isDebugEnabled()) {
                log.debug("SERVLET_REQUEST Set");
            }

            String pathInfo = null;
            try {
                pathInfo = req.getPathInfo();
            } catch (Throwable t){
                log.debug("exception in getPathInfo", t);
            }
            setProperty(soapMessageContext, javax.xml.ws.handler.MessageContext.PATH_INFO, pathInfo);
            if (log.isDebugEnabled()) {
                if (pathInfo != null) {
                    log.debug("HTTP_REQUEST_PATHINFO Set");
                } else {
                    log.debug("HTTP_REQUEST_PATHINFO not found");
                }
            }
            String queryString = req.getQueryString();
            setProperty(soapMessageContext, javax.xml.ws.handler.MessageContext.QUERY_STRING, queryString);
            if (log.isDebugEnabled()) {
                if (queryString != null) {
                    log.debug("HTTP_REQUEST_QUERYSTRING Set");
                } else {
                    log.debug("HTTP_REQUEST_QUERYSTRING not found");
                }
            }
            String method = req.getMethod();
            setProperty(soapMessageContext, javax.xml.ws.handler.MessageContext.HTTP_REQUEST_METHOD, method);
            if (log.isDebugEnabled()) {
                if (method != null) {
                    log.debug("HTTP_REQUEST_METHOD Set");
                } else {
                    log.debug("HTTP_REQUEST_METHOD not found");
                }
            }

        }
        HttpServletResponse res = (HttpServletResponse)jaxwsMessageContext
                .getProperty(HTTPConstants.MC_HTTP_SERVLETRESPONSE);
        if (res == null) {
            if (log.isDebugEnabled()) {
                log.debug("Servlet Response not found");
            }
        } else {
            setProperty(soapMessageContext, javax.xml.ws.handler.MessageContext.SERVLET_RESPONSE, res);
            if (log.isDebugEnabled()) {
                log.debug("SERVLET_RESPONSE Set");
            }
        }
        
    }

    public static void addWSDLProperties(MessageContext jaxwsMessageContext) {
        addWSDLProperties(jaxwsMessageContext, getSOAPMessageContext(jaxwsMessageContext));                
    }
    
    public static void addWSDLProperties(MessageContext jaxwsMessageContext,
                                         SOAPMessageContext soapMessageContext) {
        OperationDescription op = jaxwsMessageContext.getOperationDescription();

        if (op != null && soapMessageContext != null) {
            setProperty(soapMessageContext, javax.xml.ws.handler.MessageContext.WSDL_OPERATION, op.getName(), true);

            EndpointInterfaceDescription eid = op.getEndpointInterfaceDescription();
            if (eid != null) {
                EndpointDescription ed = eid.getEndpointDescription();
                QName portType = eid.getPortType();
                if (portType == null || portType.getLocalPart().length() == 0) {
                    if (log.isDebugEnabled()) {
                        log.debug(
                                "Did not get port type from EndpointInterfaceDescription, attempting to get PortType from EndpointDescription");
                    }
                }
                if (ed != null) {
                    setProperty(soapMessageContext, javax.xml.ws.handler.MessageContext.WSDL_PORT, ed.getPortQName(), true);
                }
                setProperty(soapMessageContext, javax.xml.ws.handler.MessageContext.WSDL_INTERFACE, portType, true);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Unable to read WSDL operation, port and interface properties");
            }
        }
    }
    
    public static void addWSDLProperties_provider(MessageContext jaxwsMessageContext) {
        addWSDLProperties_provider(jaxwsMessageContext, getSOAPMessageContext(jaxwsMessageContext));
    }
    
    public static void addWSDLProperties_provider(MessageContext jaxwsMessageContext,
                                                  SOAPMessageContext soapMessageContext) {
        QName op = jaxwsMessageContext.getOperationName();    	
        
        if (op != null && soapMessageContext != null) {
            setProperty(soapMessageContext, javax.xml.ws.handler.MessageContext.WSDL_OPERATION, op, true);

            //EndpointInterfaceDescription eid = op.getEndpointInterfaceDescription();
            EndpointDescription ed = jaxwsMessageContext.getEndpointDescription();
            
            if (ed != null) {
                setProperty(soapMessageContext, javax.xml.ws.handler.MessageContext.WSDL_PORT, ed.getPortQName(), true);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Unable to read WSDL operation, port and interface properties");
            }
        }
    }
    
    private static SOAPMessageContext getSOAPMessageContext(MessageContext jaxwsMessageContext) {
        org.apache.axis2.context.MessageContext msgContext =
            jaxwsMessageContext.getAxisMessageContext();
        ServiceContext serviceContext = msgContext.getServiceContext();
        SOAPMessageContext soapMessageContext = null;
        if (serviceContext != null) {
            WebServiceContext wsc =
                (WebServiceContext)serviceContext.getProperty(EndpointLifecycleManagerImpl.WEBSERVICE_MESSAGE_CONTEXT);
            if (wsc != null) {
                soapMessageContext = (SOAPMessageContext)wsc.getMessageContext();
            }
        }
        return soapMessageContext;
    }
       
    private static void setProperty(SOAPMessageContext context, String name, Object value) {
        setProperty(context, name, value, false);
    }
    
    private static void setProperty(SOAPMessageContext context, String name, Object value, boolean logMessage) {
        context.put(name, value);
        context.setScope(name, Scope.APPLICATION);
        if (logMessage && log.isDebugEnabled()) {
            log.debug(name + " :" + value);
        }
    }
}
