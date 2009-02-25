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

package org.apache.axis2.jaxws.client.dispatch;

import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.jaxws.BindingProvider;
import org.apache.axis2.jaxws.ExceptionFactory;
import org.apache.axis2.jaxws.client.async.AsyncResponse;
import org.apache.axis2.jaxws.core.InvocationContext;
import org.apache.axis2.jaxws.core.InvocationContextFactory;
import org.apache.axis2.jaxws.core.MessageContext;
import org.apache.axis2.jaxws.core.controller.InvocationController;
import org.apache.axis2.jaxws.core.controller.InvocationControllerFactory;
import org.apache.axis2.jaxws.description.EndpointDescription;
import org.apache.axis2.jaxws.i18n.Messages;
import org.apache.axis2.jaxws.marshaller.impl.alt.MethodMarshallerUtils;
import org.apache.axis2.jaxws.message.Message;
import org.apache.axis2.jaxws.registry.FactoryRegistry;
import org.apache.axis2.jaxws.spi.Binding;
import org.apache.axis2.jaxws.spi.Constants;
import org.apache.axis2.jaxws.spi.ServiceDelegate;
import org.apache.axis2.jaxws.spi.migrator.ApplicationContextMigratorUtil;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.ProtocolException;
import javax.xml.ws.Response;
import javax.xml.ws.Service.Mode;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.http.HTTPBinding;
import javax.xml.ws.soap.SOAPBinding;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

public abstract class BaseDispatch<T> extends BindingProvider
        implements javax.xml.ws.Dispatch {

    private static Log log = LogFactory.getLog(BaseDispatch.class);

    protected InvocationController ic;

    protected ServiceClient serviceClient;

    protected Mode mode;

    protected BaseDispatch(ServiceDelegate svcDelgate,
                           EndpointDescription epDesc,
                           EndpointReference epr,
                           String addressingNamespace,
                           WebServiceFeature... features) {
        super(svcDelgate, epDesc, epr, addressingNamespace, features);

        InvocationControllerFactory icf = (InvocationControllerFactory) FactoryRegistry.getFactory(InvocationControllerFactory.class);
        ic = icf.getInvocationController();
        
        if (ic == null) {
            throw new WebServiceException(Messages.getMessage("missingInvocationController"));
        }
    }

    /**
     * Take the input object and turn it into an OMElement so that it can be sent.
     *
     * @param value
     * @return
     */
    protected abstract Message createMessageFromValue(Object value);

    /**
     * Given a message, return the business object based on the requestor's required format (PAYLOAD
     * vs. MESSAGE) and datatype.
     *
     * @param message
     * @return
     */
    protected abstract Object getValueFromMessage(Message message);

    /**
     * Creates an instance of the AsyncListener that is to be used for waiting for async responses.
     *
     * @return a configured AsyncListener instance
     */
    protected abstract AsyncResponse createAsyncResponseListener();

    public Object invoke(Object obj) throws WebServiceException {

        // Catch all exceptions and rethrow an appropriate WebService Exception
        try {
            if (log.isDebugEnabled()) {
                log.debug("Entered synchronous invocation: BaseDispatch.invoke()");
            }

            // Create the InvocationContext instance for this request/response flow.
            InvocationContext invocationContext =
                    InvocationContextFactory.createInvocationContext(null);
            invocationContext.setServiceClient(serviceClient);

            // Create the MessageContext to hold the actual request message and its
            // associated properties
            MessageContext requestMsgCtx = new MessageContext();
            requestMsgCtx.setEndpointDescription(getEndpointDescription());
            invocationContext.setRequestMessageContext(requestMsgCtx);
            
            /*
             * TODO: review: make sure the handlers are set on the InvocationContext
             * This implementation of the JAXWS runtime does not use Endpoint, which
             * would normally be the place to initialize and store the handler list.
             * In lieu of that, we will have to intialize and store them on the 
             * InvocationContext.  also see the InvocationContextFactory.  On the client
             * side, the binding is not yet set when we call into that factory, so the
             * handler list doesn't get set on the InvocationContext object there.  Thus
             * we gotta do it here.
             */

            // be sure to use whatever handlerresolver is registered on the Service
            Binding binding = (Binding) getBinding();
            invocationContext.setHandlers(binding.getHandlerChain());

            initMessageContext(obj, requestMsgCtx);

            /*
             * if SESSION_MAINTAIN_PROPERTY is true, and the client app has explicitly set a HEADER_COOKIE on the request context, assume the client
             * app is expecting the HEADER_COOKIE to be the session id.  If we were establishing a new session, no cookie would be sent, and the 
             * server would reply with a "Set-Cookie" header, which is copied as a "Cookie"-keyed property to the service context during response.
             * In this case, if we succeed in using an existing server session, no "Set-Cookie" header will be returned, and therefore no
             * "Cookie"-keyed property would be set on the service context.  So, let's copy our request context HEADER_COOKIE key to the service
             * context now to prevent the "no cookie" exception in BindingProvider.setupSessionContext.  It is possible the server does not support
             * sessions, in which case no error occurs, but the client app would assume it is participating in a session.
             */
            if ((requestContext.containsKey(BindingProvider.SESSION_MAINTAIN_PROPERTY)) && ((Boolean)requestContext.get(BindingProvider.SESSION_MAINTAIN_PROPERTY))) {
                if ((requestContext.containsKey(HTTPConstants.HEADER_COOKIE)) && (requestContext.get(HTTPConstants.HEADER_COOKIE) != null)) {
                    if (invocationContext.getServiceClient().getServiceContext().getProperty(HTTPConstants.HEADER_COOKIE) == null) {
                        invocationContext.getServiceClient().getServiceContext().setProperty(HTTPConstants.HEADER_COOKIE, requestContext.get(HTTPConstants.HEADER_COOKIE));
                        if (log.isDebugEnabled()) {
                            log.debug("Client-app defined Cookie property (assume to be session cookie) on request context copied to service context." +
                                    "  Caution:  server may or may not support sessions, but client app will not be informed when not supported.");
                        }
                    }
                }
            }
                        
            // Migrate the properties from the client request context bag to
            // the request MessageContext.
            ApplicationContextMigratorUtil.performMigrationToMessageContext(
                    Constants.APPLICATION_CONTEXT_MIGRATOR_LIST_ID,
                    getRequestContext(), requestMsgCtx);

            // Perform the WebServiceFeature configuration requested by the user.
            binding.configure(requestMsgCtx, this);

            // Send the request using the InvocationController
            ic.invoke(invocationContext);

            MessageContext responseMsgCtx = invocationContext.getResponseMessageContext();
            responseMsgCtx.setEndpointDescription(requestMsgCtx.getEndpointDescription());

            // Migrate the properties from the response MessageContext back
            // to the client response context bag.
            ApplicationContextMigratorUtil.performMigrationFromMessageContext(
                    Constants.APPLICATION_CONTEXT_MIGRATOR_LIST_ID,
                    getResponseContext(), responseMsgCtx);

            if (hasFaultResponse(responseMsgCtx)) {
                WebServiceException wse = BaseDispatch.getFaultResponse(responseMsgCtx);
                throw wse;
            }

            // Get the return object
            Object returnObj = null;
            try {
                Message responseMsg = responseMsgCtx.getMessage();
                returnObj = getValueFromMessage(responseMsg);
            }
            finally {
                // Free the incoming input stream
                try {
                    responseMsgCtx.freeInputStream();
                }
                catch (Throwable t) {
                    throw ExceptionFactory.makeWebServiceException(t);
                }
            }
           
            //Check to see if we need to maintain session state
            checkMaintainSessionState(requestMsgCtx, invocationContext);

            if (log.isDebugEnabled()) {
                log.debug("Synchronous invocation completed: BaseDispatch.invoke()");
            }

            return returnObj;
        } catch (WebServiceException e) {
            throw e;
        } catch (Exception e) {
            // All exceptions are caught and rethrown as a WebServiceException
            throw ExceptionFactory.makeWebServiceException(e);
        }
    }

    protected void initMessageContext(Object obj, MessageContext requestMsgCtx) {
        Message requestMsg = createRequestMessage(obj);
        setupMessageProperties(requestMsg);
        requestMsgCtx.setMessage(requestMsg);
        // handle HTTP_REQUEST_METHOD property
        String method = (String)requestContext.get(javax.xml.ws.handler.MessageContext.HTTP_REQUEST_METHOD);
        if (method != null) {
            requestMsgCtx.setProperty(org.apache.axis2.Constants.Configuration.HTTP_METHOD, method);
        }
    }

    public void invokeOneWay(Object obj) throws WebServiceException {

        // All exceptions are caught and rethrown as a WebServiceException
        try {
            if (log.isDebugEnabled()) {
                log.debug("Entered one-way invocation: BaseDispatch.invokeOneWay()");
            }

            // Create the InvocationContext instance for this request/response flow.
            InvocationContext invocationContext =
                    InvocationContextFactory.createInvocationContext(null);
            invocationContext.setServiceClient(serviceClient);

            // Create the MessageContext to hold the actual request message and its
            // associated properties
            MessageContext requestMsgCtx = new MessageContext();
            requestMsgCtx.setEndpointDescription(getEndpointDescription());
            invocationContext.setRequestMessageContext(requestMsgCtx);
            
            /*
             * TODO: review: make sure the handlers are set on the InvocationContext
             * This implementation of the JAXWS runtime does not use Endpoint, which
             * would normally be the place to initialize and store the handler list.
             * In lieu of that, we will have to intialize and store them on the 
             * InvocationContext.  also see the InvocationContextFactory.  On the client
             * side, the binding is not yet set when we call into that factory, so the
             * handler list doesn't get set on the InvocationContext object there.  Thus
             * we gotta do it here.
             */

            // be sure to use whatever handlerresolver is registered on the Service
            Binding binding = (Binding) getBinding();
            invocationContext.setHandlers(binding.getHandlerChain());

            initMessageContext(obj, requestMsgCtx);

            // Migrate the properties from the client request context bag to
            // the request MessageContext.
            ApplicationContextMigratorUtil.performMigrationToMessageContext(
                    Constants.APPLICATION_CONTEXT_MIGRATOR_LIST_ID,
                    getRequestContext(), requestMsgCtx);

            // Perform the WebServiceFeature configuration requested by the user.
            binding.configure(requestMsgCtx, this);

            // Send the request using the InvocationController
            ic.invokeOneWay(invocationContext);

            //Check to see if we need to maintain session state
            checkMaintainSessionState(requestMsgCtx, invocationContext);

            if (log.isDebugEnabled()) {
                log.debug("One-way invocation completed: BaseDispatch.invokeOneWay()");
            }

            return;
        } catch (WebServiceException e) {
            throw e;
        } catch (Exception e) {
            // All exceptions are caught and rethrown as a WebServiceException
            throw ExceptionFactory.makeWebServiceException(e);
        }
    }

    public Future<?> invokeAsync(Object obj, AsyncHandler asynchandler) throws WebServiceException {

        // All exceptions are caught and rethrown as a WebServiceException
        try {
            if (log.isDebugEnabled()) {
                log.debug("Entered asynchronous (callback) invocation: BaseDispatch.invokeAsync()");
            }

            // Create the InvocationContext instance for this request/response flow.
            InvocationContext invocationContext =
                    InvocationContextFactory.createInvocationContext(null);
            invocationContext.setServiceClient(serviceClient);

            // Create the MessageContext to hold the actual request message and its
            // associated properties
            MessageContext requestMsgCtx = new MessageContext();
            requestMsgCtx.setEndpointDescription(getEndpointDescription());
            invocationContext.setRequestMessageContext(requestMsgCtx);
            
            /*
             * TODO: review: make sure the handlers are set on the InvocationContext
             * This implementation of the JAXWS runtime does not use Endpoint, which
             * would normally be the place to initialize and store the handler list.
             * In lieu of that, we will have to intialize and store them on the 
             * InvocationContext.  also see the InvocationContextFactory.  On the client
             * side, the binding is not yet set when we call into that factory, so the
             * handler list doesn't get set on the InvocationContext object there.  Thus
             * we gotta do it here.
             */

            // be sure to use whatever handlerresolver is registered on the Service
            Binding binding = (Binding) getBinding();
            invocationContext.setHandlers(binding.getHandlerChain());

            initMessageContext(obj, requestMsgCtx);

            // Migrate the properties from the client request context bag to
            // the request MessageContext.
            ApplicationContextMigratorUtil.performMigrationToMessageContext(
                    Constants.APPLICATION_CONTEXT_MIGRATOR_LIST_ID,
                    getRequestContext(), requestMsgCtx);

            // Perform the WebServiceFeature configuration requested by the user.
            binding.configure(requestMsgCtx, this);

            // Setup the Executor that will be used to drive async responses back to 
            // the client.
            // FIXME: We shouldn't be getting this from the ServiceDelegate, rather each 
            // Dispatch object should have it's own.
            Executor e = serviceDelegate.getExecutor();
            invocationContext.setExecutor(e);

            // Create the AsyncListener that is to be used by the InvocationController.
            AsyncResponse listener = createAsyncResponseListener();
            invocationContext.setAsyncResponseListener(listener);

            // Send the request using the InvocationController
            Future<?> asyncResponse = ic.invokeAsync(invocationContext, asynchandler);

            //Check to see if we need to maintain session state
            checkMaintainSessionState(requestMsgCtx, invocationContext);

            if (log.isDebugEnabled()) {
                log.debug("Asynchronous (callback) invocation sent: BaseDispatch.invokeAsync()");
            }

            return asyncResponse;
        } catch (WebServiceException e) {
            throw e;
        } catch (Exception e) {
            // All exceptions are caught and rethrown as a WebServiceException
            throw ExceptionFactory.makeWebServiceException(e);
        }
    }

    public Response invokeAsync(Object obj) throws WebServiceException {

        // All exceptions are caught and rethrown as a WebServiceException
        try {
            if (log.isDebugEnabled()) {
                log.debug("Entered asynchronous (polling) invocation: BaseDispatch.invokeAsync()");
            }

            // Create the InvocationContext instance for this request/response flow.
            InvocationContext invocationContext =
                    InvocationContextFactory.createInvocationContext(null);
            invocationContext.setServiceClient(serviceClient);

            // Create the MessageContext to hold the actual request message and its
            // associated properties
            MessageContext requestMsgCtx = new MessageContext();
            requestMsgCtx.setEndpointDescription(getEndpointDescription());
            invocationContext.setRequestMessageContext(requestMsgCtx);
            
            /*
             * TODO: review: make sure the handlers are set on the InvocationContext
             * This implementation of the JAXWS runtime does not use Endpoint, which
             * would normally be the place to initialize and store the handler list.
             * In lieu of that, we will have to intialize and store them on the 
             * InvocationContext.  also see the InvocationContextFactory.  On the client
             * side, the binding is not yet set when we call into that factory, so the
             * handler list doesn't get set on the InvocationContext object there.  Thus
             * we gotta do it here.
             */

            // be sure to use whatever handlerresolver is registered on the Service
            Binding binding = (Binding) getBinding();
            invocationContext.setHandlers(binding.getHandlerChain());

            initMessageContext(obj, requestMsgCtx);

            // Migrate the properties from the client request context bag to
            // the request MessageContext.
            ApplicationContextMigratorUtil.performMigrationToMessageContext(
                    Constants.APPLICATION_CONTEXT_MIGRATOR_LIST_ID,
                    getRequestContext(), requestMsgCtx);

            // Perform the WebServiceFeature configuration requested by the user.
            binding.configure(requestMsgCtx, this);

            // Setup the Executor that will be used to drive async responses back to 
            // the client.
            // FIXME: We shouldn't be getting this from the ServiceDelegate, rather each 
            // Dispatch object should have it's own.
            Executor e = serviceDelegate.getExecutor();
            invocationContext.setExecutor(e);

            // Create the AsyncListener that is to be used by the InvocationController.
            AsyncResponse listener = createAsyncResponseListener();
            invocationContext.setAsyncResponseListener(listener);

            // Send the request using the InvocationController
            Response asyncResponse = ic.invokeAsync(invocationContext);

            //Check to see if we need to maintain session state
            checkMaintainSessionState(requestMsgCtx, invocationContext);

            if (log.isDebugEnabled()) {
                log.debug("Asynchronous (polling) invocation sent: BaseDispatch.invokeAsync()");
            }

            return asyncResponse;
        } catch (WebServiceException e) {
            throw e;
        } catch (Exception e) {
            // All exceptions are caught and rethrown as a WebServiceException
            throw ExceptionFactory.makeWebServiceException(e);
        }
    }

    public void setServiceClient(ServiceClient sc) {
        serviceClient = sc;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode m) {
        mode = m;
    }

    /**
     * Returns the fault that is contained within the MessageContext for an invocation. If no fault
     * exists, null will be returned.
     *
     * @param msgCtx
     * @return
     */
    public static WebServiceException getFaultResponse(MessageContext msgCtx) {
        try {
            Message msg = msgCtx.getMessage();
            if (msg != null && msg.isFault()) {
                //XMLFault fault = msg.getXMLFault();
                // 4.3.2 conformance bullet 1 requires a ProtocolException here
                ProtocolException pe =
                    MethodMarshallerUtils.createSystemException(msg.getXMLFault(), msg);
                return pe;
            } else if (msgCtx.getLocalException() != null) {
                // use the factory, it'll throw the right thing:
                return ExceptionFactory.makeWebServiceException(msgCtx.getLocalException());
            }
        } finally {
            // Free the incoming input stream
            try {
                msgCtx.freeInputStream();
            } catch (IOException ioe) {
                return ExceptionFactory.makeWebServiceException(ioe);
            }
        }

        return null;
    }

    /**
     * Returns a boolean indicating whether or not the MessageContext contained a fault.
     *
     * @param msgCtx
     * @return
     */
    public boolean hasFaultResponse(MessageContext msgCtx) {
        if (msgCtx.getMessage() != null && msgCtx.getMessage().isFault())
            return true;
        else if (msgCtx.getLocalException() != null)
            return true;
        else
            return false;
    }

    /*
     * Configure any properties that will be needed on the Message
     */
    private void setupMessageProperties(Message msg) {
        // If the user has enabled MTOM on the SOAPBinding, we need
        // to make sure that gets pushed to the Message object.
        Binding binding = (Binding) getBinding();
        if (binding != null && binding instanceof SOAPBinding) {
            SOAPBinding soapBinding = (SOAPBinding)binding;
            if (soapBinding.isMTOMEnabled())
                msg.setMTOMEnabled(true);
        }
    }

    /*
    * Checks to see if the parameter for the invocation is valid
    * given the scenario that the client is operating in.  There are
    * some cases when nulls are allowed and others where it is
    * an error.
    */
    private boolean isValidInvocationParam(Object object) {
        String bindingId = endpointDesc.getClientBindingID();

        // If no bindingId was found, use the default.
        if (bindingId == null) {
            bindingId = SOAPBinding.SOAP11HTTP_BINDING;
        }

        // If it's not an HTTP_BINDING, then we can allow for null params,  
        // but only in PAYLOAD mode per JAX-WS Section 4.3.2.
        if (!bindingId.equals(HTTPBinding.HTTP_BINDING)) {
            if (mode.equals(Mode.MESSAGE) && object == null) {
                throw ExceptionFactory.makeWebServiceException(Messages.getMessage("dispatchNullParamMessageMode"));
            }
        } else {
            // In all cases (PAYLOAD and MESSAGE) we must throw a WebServiceException
            // if the parameter is null and request method is POST or PUT.
            if (object == null && isPOSTorPUTRequest()) {
                throw ExceptionFactory.makeWebServiceException(Messages.getMessage("dispatchNullParamHttpBinding"));
            }
        }

        if (object instanceof DOMSource) {
            DOMSource ds = (DOMSource)object;
            if (ds.getNode() == null && ds.getSystemId() == null) {
                throw ExceptionFactory.makeWebServiceException(Messages.getMessage("dispatchBadDOMSource"));
            }
        }

        // If we've gotten this far, then all is good.
        return true;
    }
    
    private boolean isPOSTorPUTRequest() {
        String method = (String)this.requestContext.get(javax.xml.ws.handler.MessageContext.HTTP_REQUEST_METHOD);
        // if HTTP_REQUEST_METHOD is not specified, assume it is a POST method
        return (method == null || 
                HTTPConstants.HEADER_POST.equalsIgnoreCase(method) || 
                HTTPConstants.HEADER_PUT.equalsIgnoreCase(method));
    }
    
    private Message createRequestMessage(Object obj) throws WebServiceException {
        
        // Check to see if the object is a valid invocation parameter.  
        // Then create the message from the object.
        // If an exception occurs, it is local to the client and therefore is a
        // WebServiceException (and not ProtocolExceptions).
        // This code complies with JAX-WS 2.0 sections 4.3.2, 4.3.3 and 4.3.4.
        if (!isValidInvocationParam(obj)) {
            throw ExceptionFactory.makeWebServiceException(Messages.getMessage("dispatchInvalidParam"));
        } 
        Message requestMsg = null;
        try {
             requestMsg = createMessageFromValue(obj);
        } catch (Throwable t) {
            // The webservice exception wraps the thrown exception.
            throw ExceptionFactory.makeWebServiceException(t);
        }
        return requestMsg;
    }
}
