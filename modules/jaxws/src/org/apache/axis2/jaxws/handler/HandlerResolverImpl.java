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
package org.apache.axis2.jaxws.handler;

import org.apache.axis2.client.OperationClient;
import org.apache.axis2.java.security.AccessController;
import org.apache.axis2.jaxws.ExceptionFactory;
import org.apache.axis2.jaxws.core.MessageContext;
import org.apache.axis2.jaxws.description.EndpointDescription;
import org.apache.axis2.jaxws.description.ServiceDescription;
import org.apache.axis2.jaxws.description.xml.handler.HandlerChainType;
import org.apache.axis2.jaxws.description.xml.handler.HandlerChainsType;
import org.apache.axis2.jaxws.description.xml.handler.HandlerType;
import org.apache.axis2.jaxws.handler.lifecycle.factory.HandlerLifecycleManager;
import org.apache.axis2.jaxws.handler.lifecycle.factory.HandlerLifecycleManagerFactory;
import org.apache.axis2.jaxws.i18n.Messages;
import org.apache.axis2.jaxws.registry.FactoryRegistry;
import org.apache.axis2.jaxws.runtime.description.injection.ResourceInjectionServiceRuntimeDescription;
import org.apache.axis2.jaxws.runtime.description.injection.impl.ResourceInjectionServiceRuntimeDescriptionBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.HandlerResolver;
import javax.xml.ws.handler.LogicalHandler;
import javax.xml.ws.handler.PortInfo;
import javax.xml.ws.handler.soap.SOAPHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
/* 
 * This class should be created by the ServiceDelegate.
 * HandlerResolverImpl.getHandlerChain(PortInfo) will be called by the
 * InvocationContext, and the return value will be set on the Binding
 * under the BindingProvider.
 * 
 * HandlerResolverImpl.getHandlerChain(PortInfo) will be responsible for
 * starting each Handler's lifecycle according to JAX-WS spec 9.3.1
 */

public class HandlerResolverImpl implements HandlerResolver {

    // TODO should probably use constants defined elsewhere
    static final Map<String, String> protocolBindingsMap = new HashMap<String, String>(5);
    static {
        protocolBindingsMap.put("##SOAP11_HTTP",        "http://schemas.xmlsoap.org/wsdl/soap/http");
        protocolBindingsMap.put("##SOAP11_HTTP_MTOM",   "http://schemas.xmlsoap.org/wsdl/soap/http?mtom=true");
        protocolBindingsMap.put("##SOAP12_HTTP",        "http://www.w3.org/2003/05/soap/bindings/HTTP/");
        protocolBindingsMap.put("##SOAP12_HTTP_MTOM",   "http://www.w3.org/2003/05/soap/bindings/HTTP/?mtom=true");
        protocolBindingsMap.put("##XML_HTTP",           "http://www.w3.org/2004/08/wsdl/http");
    }
    private static Log log = LogFactory.getLog(HandlerResolverImpl.class);
    /*
      * TODO:  is there any value/reason in caching the list we collect from the
      * ports?  It is a "live" list in the sense that we could possibly return
      * a List or ArrayList object to a service or client application, where
      * they could manipulate it.
      */

    // we'll need to refer to this object to get the port, and thus handlers
    //private EndpointDescription endpointDesc;
    private ServiceDescription serviceDesc;

    public HandlerResolverImpl(ServiceDescription sd) { //EndpointDescription ed) {
        //this.endpointDesc = ed;
        this.serviceDesc = sd;
    }

    public ArrayList<Handler> getHandlerChain(PortInfo portinfo) {
        // TODO:  would check and/or build cache here if implemented later
        return resolveHandlers(portinfo);
    }

    /*
      * The list of handlers (rather, list of class names) is already
      * available per port.  Ports are stored under the ServiceDelegate
      * as PortData objects.
      *
	 * The resolveHandlers method is responsible for instantiating each Handler,
	 * running the annotated PostConstruct method, resolving the list,
	 * and returning it.  We do not sort here.
      */
    private ArrayList<Handler> resolveHandlers(PortInfo portinfo) throws WebServiceException {
        /*

            A sample XML file for the handler-chains:
            
            <jws:handler-chains xmlns:jws="http://java.sun.com/xml/ns/javaee">
                <jws:handler-chain>
                    <jws:protocol-bindings>##XML_HTTP</jws:protocol-bindings>
                    <jws:handler>
                        <jws:handler-name>MyHandler</jws:handler-name>
                        <jws:handler-class>org.apache.axis2.jaxws.MyHandler</jws:handler-class>
                    </jws:handler>
                </jws:handler-chain>
                <jws:handler-chain>
                    <jws:port-name-pattern>jws:Foo*</jws:port-name-pattern>
                    <jws:handler>
                        <jws:handler-name>MyHandler</jws:handler-name>
                        <jws:handler-class>org.apache.axis2.jaxws.MyHandler</jws:handler-class>
                    </jws:handler>
                </jws:handler-chain>
                <jws:handler-chain>
                    <jws:service-name-pattern>jws:Bar</jws:service-name-pattern>
                    <jws:handler>
                        <jws:handler-name>MyHandler</jws:handler-name>
                        <jws:handler-class>org.apache.axis2.jaxws.MyHandler</jws:handler-class>
                    </jws:handler>
                </jws:handler-chain>
            </jws:handler-chains>
            
            Couple of things I'm not sure about...
            1)  if the protocol-binding, port-name-pattern, and service-name-pattern all
                match the PortInfo object, does MyHandler get added three times?  Probably would get added 3 times.
            2)  I assume the asterisk "*" is a wildcard.  Can the asterisk only occur on the local part of the qname?
            3)  Can there be more than one service-name-pattern or port-name-pattern, just like for protocol-bindings?
            4)  How many protocol-bindings are there?  ##XML_HTTP ##SOAP11_HTTP ##SOAP12_HTTP ##SOAP11_HTTP_MTOM ##SOAP12_HTTP_MTOM
                They are separated by spaces
         */

        // our implementation already has a reference to the EndpointDescription,
        // which is where one might get the portinfo object.  We still have the 
        // passed-in variable, however, due to the spec

        ArrayList<Handler> handlers = new ArrayList<Handler>();

        /*
         * TODO: do a better job checking that the return value matches up
         * with the PortInfo object before we add it to the chain.
         */
        
        HandlerChainsType handlerCT = serviceDesc.getHandlerChain();  
        // if there's a handlerChain on the serviceDesc, it means the WSDL defined an import for a HandlerChain.
        // the spec indicates that if a handlerchain also appears on the SEI on the client.
        EndpointDescription ed = null;
        if(portinfo !=null){
             ed = serviceDesc.getEndpointDescription(portinfo.getPortName());
        }
        
        if (ed != null) {
            HandlerChainsType handlerCT_fromEndpointDesc = ed.getHandlerChain();
            if (handlerCT == null) {
                handlerCT = handlerCT_fromEndpointDesc;
            } 
        }

        Iterator it = handlerCT == null ? null : handlerCT.getHandlerChain().iterator();

        while ((it != null) && (it.hasNext())) {
            HandlerChainType handlerChainType = ((HandlerChainType)it.next());
            
            // if !match, continue (to next chain)
            if (!(chainResolvesToPort(handlerChainType, portinfo)))
                continue;
            
            List<HandlerType> handlerTypeList = handlerChainType.getHandler();
            Iterator ht = handlerTypeList.iterator();
            while (ht.hasNext()) {
                
                HandlerType handlerType = (HandlerType)ht.next();
                
                // TODO must do better job comparing the handlerType with the PortInfo param
                // to see if the current iterator handler is intended for this service.

                // TODO review: need to check for null getHandlerClass() return?
                // or will schema not allow it?
                String portHandler = handlerType.getHandlerClass().getValue();
                Handler handler;
                // Create temporary MessageContext to pass information to HandlerLifecycleManager
                MessageContext ctx = new MessageContext();
                ctx.setEndpointDescription(ed);
                
                HandlerLifecycleManager hlm = createHandlerlifecycleManager();
                    
                //  instantiate portHandler class 
                try {
                    handler = hlm.createHandlerInstance(ctx, loadClass(portHandler));
                } catch (Exception e) {
                    // TODO: should we just ignore this problem?
                    // TODO: NLS log and throw
                    throw ExceptionFactory.makeWebServiceException(e);
                }
                // 9.2.1.2 sort them by Logical, then SOAP
                if (LogicalHandler.class.isAssignableFrom(handler.getClass()))
                    handlers.add((LogicalHandler) handler);
                else if (SOAPHandler.class.isAssignableFrom(handler.getClass()))
                    // instanceof ProtocolHandler
                    handlers.add((SOAPHandler) handler);
                else if (Handler.class.isAssignableFrom(handler.getClass())) {
                    // TODO: NLS better error message
                    throw ExceptionFactory.makeWebServiceException(Messages
                            .getMessage("handlerChainErr1", handler
                                    .getClass().getName()));
                } else {
                    // TODO: NLS better error message
                    throw ExceptionFactory.makeWebServiceException(Messages
                            .getMessage("handlerChainErr2", handler
                                    .getClass().getName()));
                }
            }
        }

        return handlers;
    }

    private HandlerLifecycleManager createHandlerlifecycleManager() {
        HandlerLifecycleManagerFactory elmf = (HandlerLifecycleManagerFactory)FactoryRegistry
                .getFactory(HandlerLifecycleManagerFactory.class);
        return elmf.createHandlerLifecycleManager();
    }
    
    private static Class loadClass(String clazz) throws ClassNotFoundException {
        try {
            return forName(clazz, true, getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw e;
        }
    }

    /**
     * Return the class for this name
     *
     * @return Class
     */
    private static Class forName(final String className, final boolean initialize,
                                 final ClassLoader classLoader) throws ClassNotFoundException {
        // NOTE: This method must remain protected because it uses AccessController
        Class cl = null;
        try {
            cl = (Class)AccessController.doPrivileged(
                    new PrivilegedExceptionAction() {
                        public Object run() throws ClassNotFoundException {
                        	try{
                        		if (log.isDebugEnabled()) {
        	                        log.debug("HandlerResolverImpl attempting to load Class: "+className);
        	                    }
                        		return Class.forName(className, initialize, classLoader);
                        	} catch (Throwable e) {
        	                    // TODO Should the exception be swallowed ?
        	                    if (log.isDebugEnabled()) {
        	                        log.debug("HandlerResolverImpl cannot load the following class Throwable Exception Occured: " + className);
        	                    }
        	                    throw new ClassNotFoundException("HandlerResolverImpl cannot load the following class Throwable Exception Occured:" + className);
        	                }
                        }
                    }
            );
        } catch (PrivilegedActionException e) {
            if (log.isDebugEnabled()) {
                log.debug("Exception thrown from AccessController: " + e);
            }
            throw (ClassNotFoundException)e.getException();
        }

        return cl;
    }


    /** @return ClassLoader */
    private static ClassLoader getContextClassLoader() {
        // NOTE: This method must remain private because it uses AccessController
        ClassLoader cl = null;
        try {
            cl = (ClassLoader)AccessController.doPrivileged(
                    new PrivilegedExceptionAction() {
                        public Object run() throws ClassNotFoundException {
                            return Thread.currentThread().getContextClassLoader();
                        }
                    }
            );
        } catch (PrivilegedActionException e) {
            if (log.isDebugEnabled()) {
                log.debug("Exception thrown from AccessController: " + e);
            }
            throw ExceptionFactory.makeWebServiceException(e.getException());
        }

        return cl;
    }

   
    private static boolean chainResolvesToPort(HandlerChainType handlerChainType, PortInfo portinfo) {
        
        List<String> protocolBindings = handlerChainType.getProtocolBindings();
        if (protocolBindings != null) {
            boolean match = true;
            for (Iterator<String> it = protocolBindings.iterator() ; it.hasNext();) {
                match = false;  // default to false in the protocol bindings until we find a match
                String protocolBinding = it.next();
                protocolBinding = protocolBinding.startsWith("##") ? protocolBindingsMap.get(protocolBinding) : protocolBinding;
                // if the protocolBindingsMap returns null, it would mean someone has some nonsense ##binding
                if ((protocolBinding != null) && (protocolBinding.equals(portinfo.getBindingID()))) {
                    match = true;
                    break;
                }
            }
            if (match == false) {
                // we've checked all the protocolBindings, but didn't find a match, no need to continue
                return match;
            }
        }

        /*
         * need to figure out how to get the namespace declaration out of the port-name-pattern and service-name-pattern
         */
        
        if (!doesPatternMatch(portinfo.getPortName(), handlerChainType.getPortNamePattern())) {
                // we've checked the port-name-pattern, and didn't find a match, no need to continue
                return false;
        }
        
        if (!doesPatternMatch(portinfo.getServiceName(), handlerChainType.getServiceNamePattern())) {
                // we've checked the service-name-pattern, and didn't find a match, no need to continue
                return false;
        }

        return true;
    }
    
    /*
     * A comparison routing to check service-name-pattern and port-name-pattern.  These patterns may be of
     * the form:
     * 
     * 1)  namespace:localpart
     * 2)  namespace:localpart*
     * 3)  namespace:*    (not sure about this one)
     * 4)  *   (which is equivalent to not specifying a pattern, therefore always matching)
     * 
     * I've not seen any examples where the wildcard may be placed mid-string or on the namespace, such as:
     * 
     * namespace:local*part
     * *:localpart
     * 
     */
    private static boolean doesPatternMatch(QName portInfoQName, QName pattern) {
        if (pattern == null)
            return true;
        String portInfoString = portInfoQName.toString();
        String patternString = pattern.toString();
        if (patternString.equals("*"))
            return true;
        if (patternString.contains("*")) {
            patternString = patternString.substring(0, patternString.length() - 1);
            return portInfoString.startsWith(patternString);
        }
        return portInfoString.equals(patternString);
        
    }
    
}
