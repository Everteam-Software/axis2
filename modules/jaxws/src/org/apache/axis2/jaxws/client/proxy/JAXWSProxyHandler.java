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

package org.apache.axis2.jaxws.client.proxy;

import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.jaxws.BindingProvider;
import org.apache.axis2.jaxws.ExceptionFactory;
import org.apache.axis2.jaxws.client.async.AsyncResponse;
import org.apache.axis2.jaxws.core.InvocationContext;
import org.apache.axis2.jaxws.core.InvocationContextFactory;
import org.apache.axis2.jaxws.core.MessageContext;
import org.apache.axis2.jaxws.core.controller.InvocationController;
import org.apache.axis2.jaxws.core.controller.InvocationControllerFactory;
import org.apache.axis2.jaxws.description.EndpointDescription;
import org.apache.axis2.jaxws.description.OperationDescription;
import org.apache.axis2.jaxws.description.ServiceDescription;
import org.apache.axis2.jaxws.i18n.Messages;
import org.apache.axis2.jaxws.marshaller.factory.MethodMarshallerFactory;
import org.apache.axis2.jaxws.message.Message;
import org.apache.axis2.jaxws.registry.FactoryRegistry;
import org.apache.axis2.jaxws.spi.Binding;
import org.apache.axis2.jaxws.spi.Constants;
import org.apache.axis2.jaxws.spi.ServiceDelegate;
import org.apache.axis2.jaxws.spi.migrator.ApplicationContextMigratorUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.soap.SOAPBinding;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * ProxyHandler is the java.lang.reflect.InvocationHandler implementation. When a JAX-WS client
 * calls the method on a proxy object, created by calling the ServiceDelegate.getPort(...) method,
 * the inovke method on the ProxyHandler is called.
 * <p/>
 * ProxyHandler uses EndpointInterfaceDescriptor and finds out if 1) The client call is Document
 * Literal or Rpc Literal 2) The WSDL is wrapped or unWrapped.
 * <p/>
 * ProxyHandler then reads OperationDescription using Method name called by Client From
 * OperationDescription it does the following 1) if the wsdl isWrapped() reads RequestWrapper Class
 * and responseWrapperClass 2) then reads the webParams for the Operation.
 * <p/>
 * isWrapped() = true  and DocLiteral then ProxyHandler then uses WrapperTool to create Request that
 * is a Wrapped JAXBObject. Creates JAXBBlock using JAXBBlockFactory Creates MessageContext->Message
 * and sets JAXBBlock to xmlPart as RequestMsgCtx in InvocationContext. Makes call to
 * InvocationController. Reads ResponseMsgCtx ->MessageCtx->Message->XMLPart. Converts that to
 * JAXBlock using JAXBBlockFactory and returns the BO from this JAXBBlock.
 * <p/>
 * isWrapped() != true and DocLiteral then ProxyHandler creates the JAXBBlock for the input request
 * creates a MessageContext that is then used by IbvocationController to invoke. Response is read
 * and return object is derived using @Webresult annotation. A JAXBBlock is created from the
 * Response and the BO from JAXBBlock is returned.
 */

public class JAXWSProxyHandler extends BindingProvider implements
        InvocationHandler {
    private static Log log = LogFactory.getLog(JAXWSProxyHandler.class);

    private Class seiClazz = null;
    private Method method = null;
    
    //Reference to ServiceDelegate instance that was used to create the Proxy
    protected ServiceDescription serviceDesc = null;

    protected InvocationController controller;
    
    public JAXWSProxyHandler(ServiceDelegate delegate,
                             Class seiClazz,
                             EndpointDescription epDesc,
                             WebServiceFeature... features) {
        this(delegate, seiClazz, epDesc, null, null, features);
    }

    public JAXWSProxyHandler(ServiceDelegate delegate,
            Class seiClazz,
            EndpointDescription epDesc,
            EndpointReference epr,
            String addressingNamespace,
            WebServiceFeature... features) {
        super(delegate, epDesc, epr, addressingNamespace, features);
        
        this.seiClazz = seiClazz;
        this.serviceDesc = delegate.getServiceDescription();
    }

    /* (non-Javadoc)
    * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
    *
    * Invokes the method that was called on the java.lang.reflect.Proxy instance.
    */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        boolean debug = log.isDebugEnabled();
        if (debug) {
            log.debug("Attemping to invoke Method: " + method.getName());
        }

        this.method = method;

        if (!isValidMethodCall(method)) {
            throw ExceptionFactory.makeWebServiceException(
                    Messages.getMessage("proxyErr1", method.getName(), seiClazz.getName()));
        }

        if (!isPublic(method)) {
            throw ExceptionFactory.makeWebServiceException(
                    Messages.getMessage("proxyPrivateMethod", method.getName()));
        }

        if (isBindingProviderInvoked(method)) {
            // Since the JAX-WS proxy instance must also implement the javax.xml.ws.BindingProvider
            // interface, this object must handle those invocations as well.  In that case, we'll
            // delegate those calls to the BindingProvider object.
            if (debug) {
                log.debug(
                        "Invoking a public method on the javax.xml.ws.BindingProvider interface.");
            }
            try {
                return method.invoke(this, args);
            }
            catch (Throwable e) {
                if (debug) {
                    log.debug("An error occured while invoking the method: " + e.getMessage());
                }
                throw ExceptionFactory.makeWebServiceException(e);
            }
        } else {
            OperationDescription operationDesc =
                    endpointDesc.getEndpointInterfaceDescription().getOperation(method);
            if (isMethodExcluded(operationDesc)) {
                throw ExceptionFactory.makeWebServiceException(
                        Messages.getMessage("proxyExcludedMethod", method.getName()));
            }
            return invokeSEIMethod(method, args);
        }
    }

    /*
    * Performs the invocation of the method defined on the Service Endpoint
    * Interface.
    */
    private Object invokeSEIMethod(Method method, Object[] args) throws Throwable {
        if (log.isDebugEnabled()) {
            log.debug("Attempting to invoke SEI Method " + method.getName());
        }

        OperationDescription operationDesc =
                endpointDesc.getEndpointInterfaceDescription().getOperation(method);

        // Create and configure the request MessageContext
        InvocationContext requestIC = InvocationContextFactory.createInvocationContext(null);
        MessageContext request = createRequest(method, args);
        request.setEndpointDescription(getEndpointDescription());
        request.setOperationDescription(operationDesc);

        // Enable MTOM on the Message if the property was set on the SOAPBinding.
        Binding bnd = (Binding) getBinding();
        if (bnd != null && bnd instanceof SOAPBinding) {
            if (((SOAPBinding)bnd).isMTOMEnabled()) {
                Message requestMsg = request.getMessage();
                requestMsg.setMTOMEnabled(true);
            }
        }
        
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
        requestIC.setHandlers(bnd.getHandlerChain());

        requestIC.setRequestMessageContext(request);
        requestIC.setServiceClient(serviceDelegate.getServiceClient(endpointDesc.getPortQName()));
        
        // Migrate the properties from the client request context bag to
        // the request MessageContext.
        ApplicationContextMigratorUtil.performMigrationToMessageContext(
                Constants.APPLICATION_CONTEXT_MIGRATOR_LIST_ID, 
                getRequestContext(), request);

        // Perform the WebServiceFeature configuration requested by the user.
        bnd.configure(request, this);

        // We'll need an InvocationController instance to send the request.
        InvocationControllerFactory icf = (InvocationControllerFactory) FactoryRegistry.getFactory(InvocationControllerFactory.class);
        controller = icf.getInvocationController();
        
        if (controller == null) {
            throw new WebServiceException(Messages.getMessage("missingInvocationController"));
        }
        
        // Check if the call is OneWay, Async or Sync
        if (operationDesc.isOneWay()) {
            if (log.isDebugEnabled()) {
                log.debug("OneWay Call");
            }
            controller.invokeOneWay(requestIC);

            // Check to see if we need to maintain session state
            checkMaintainSessionState(request, requestIC);
        }

        if (method.getReturnType() == Future.class) {
            if (log.isDebugEnabled()) {
                log.debug("Async Callback");
            }

            //Get AsyncHandler from Objects and sent that to InvokeAsync
            AsyncHandler asyncHandler = null;
            for (Object obj : args) {
                if (obj != null && AsyncHandler.class.isAssignableFrom(obj.getClass())) {
                    asyncHandler = (AsyncHandler)obj;
                    break;
                }
            }

            // Don't allow the invocation to continue if the invocation requires a callback
            // object, but none was supplied.
            if (asyncHandler == null) {
                throw ExceptionFactory
                        .makeWebServiceException(Messages.getMessage("proxyNullCallback"));
            }
            AsyncResponse listener = createProxyListener(args, operationDesc);
            requestIC.setAsyncResponseListener(listener);

            if ((serviceDelegate.getExecutor() != null) &&
                    (serviceDelegate.getExecutor() instanceof ExecutorService)) {
                ExecutorService es = (ExecutorService)serviceDelegate.getExecutor();
                if (es.isShutdown()) {
                    // the executor service is shutdown and won't accept new tasks
                    // so return an error back to the client
                    throw ExceptionFactory
                            .makeWebServiceException(Messages.getMessage("ExecutorShutdown"));
                }
            }

            requestIC.setExecutor(serviceDelegate.getExecutor());

            Future<?> future = controller.invokeAsync(requestIC, asyncHandler);

            //Check to see if we need to maintain session state
            checkMaintainSessionState(request, requestIC);

            return future;
        }

        if (method.getReturnType() == Response.class) {
            if (log.isDebugEnabled()) {
                log.debug("Async Polling");
            }
            AsyncResponse listener = createProxyListener(args, operationDesc);
            requestIC.setAsyncResponseListener(listener);
            requestIC.setExecutor(serviceDelegate.getExecutor());

            Response response = controller.invokeAsync(requestIC);

            //Check to see if we need to maintain session state
            checkMaintainSessionState(request, requestIC);

            return response;
        }

        if (!operationDesc.isOneWay()) {
            InvocationContext responseIC = controller.invoke(requestIC);

            //Check to see if we need to maintain session state
            checkMaintainSessionState(request, requestIC);

            MessageContext responseContext = responseIC.getResponseMessageContext();
            
            // Migrate the properties from the response MessageContext back
            // to the client response context bag.
            ApplicationContextMigratorUtil.performMigrationFromMessageContext(
                    Constants.APPLICATION_CONTEXT_MIGRATOR_LIST_ID, 
                    getResponseContext(), responseContext);
            
            Object responseObj = createResponse(method, args, responseContext, operationDesc);
            return responseObj;
        }

        return null;
    }
    
    private AsyncResponse createProxyListener(Object[] args, OperationDescription operationDesc) {
        ProxyAsyncListener listener = new ProxyAsyncListener(operationDesc);
        listener.setHandler(this);
        listener.setInputArgs(args);
        return listener;
    }

    protected boolean isAsync() {
        String methodName = method.getName();
        Class returnType = method.getReturnType();
        return methodName.endsWith("Async") && (returnType.isAssignableFrom(Response.class) ||
                returnType.isAssignableFrom(Future.class));
    }

    /**
     * Creates a request MessageContext for the method call. This request context will be used by
     * InvocationController to route the method call to axis engine.
     *
     * @param method - The method invoked on the proxy object.
     * @param args   - The parameter list
     * @return A MessageContext that can be used for the invocation
     */
    protected MessageContext createRequest(Method method, Object[] args) throws Throwable {
        if (log.isDebugEnabled()) {
            log.debug("Creating a new Message using the request parameters.");
        }

        OperationDescription operationDesc =
                endpointDesc.getEndpointInterfaceDescription().getOperation(method);

        Message message = MethodMarshallerFactory.getMarshaller(operationDesc, true, null)
                .marshalRequest(args, operationDesc);

        if (log.isDebugEnabled()) {
            log.debug("Request Message created successfully.");
        }

        MessageContext request = new MessageContext();
        request.setMessage(message);

        if (log.isDebugEnabled()) {
            log.debug("Request MessageContext created successfully.");
        }

        return request;
    }

    /**
     * Creates a response MessageContext for the method call. This response context will be used to
     * create response result to the client call.
     *
     * @param method          - The method invoked on the proxy object.
     * @param args            - The parameter list.
     * @param responseContext - The MessageContext to be used for the response.
     * @param operationDesc   - The OperationDescription that for the invoked method.
     * @return
     */
    protected Object createResponse(Method method, Object[] args, MessageContext responseContext,
                                    OperationDescription operationDesc) throws Throwable {
        Message responseMsg = responseContext.getMessage();
        try {

            if (log.isDebugEnabled()) {
                log.debug("Processing the response Message to create the return value(s).");
            }

            // Find out if there was a fault on the response and create the appropriate 
            // exception type.
            if (hasFaultResponse(responseContext)) {
                Throwable t = getFaultResponse(responseContext, operationDesc);
                throw t;
            }
            ClassLoader cl = (ClassLoader) responseContext.getProperty(Constants.CACHE_CLASSLOADER);
            Object object =
                    MethodMarshallerFactory.getMarshaller(operationDesc, true, cl)
                                       .demarshalResponse(responseMsg, args, operationDesc);
            if (log.isDebugEnabled()) {
                log.debug("The response was processed and the return value created successfully.");
            }
            return object;
        } finally {
            responseMsg.close();
        }
    }

    protected static Throwable getFaultResponse(MessageContext msgCtx,
                                                OperationDescription opDesc) {
        Message msg = msgCtx.getMessage();
        //Operation Description for Async method does not store the fault description as Asyc operation 
        //will never have throws clause in the method signature.
        //we will fetch the OperationDescription of the sync method and this should give us the
        //correct fault description so we can throw the right user defined exception.

        if (opDesc.isJAXWSAsyncClientMethod()) {
            opDesc = opDesc.getSyncOperation();
        }
        if (msg != null && msg.isFault()) {
            ClassLoader cl = (ClassLoader) msgCtx.getProperty(Constants.CACHE_CLASSLOADER);
            Object object = MethodMarshallerFactory.getMarshaller(opDesc, true, cl)
                    .demarshalFaultResponse(msg, opDesc);
            if (log.isDebugEnabled() && object != null) {
                log.debug("A fault was found and processed.");
                log.debug("Throwing a fault of type: " + object.getClass().getName() +
                        " back to the clent.");
            }

            return (Throwable)object;
        } else if (msgCtx.getLocalException() != null) {
            // use the factory, it'll throw the right thing:
            return ExceptionFactory.makeWebServiceException(msgCtx.getLocalException());
        }

        return null;
    }

    protected static boolean hasFaultResponse(MessageContext mc) {
        if (mc.getMessage() != null && mc.getMessage().isFault())
            return true;
        else if (mc.getLocalException() != null)
            return true;
        else
            return false;
    }

    private boolean isBindingProviderInvoked(Method method) {
        Class methodsClass = method.getDeclaringClass();
        return (seiClazz == methodsClass) ? false : true;
    }

    private boolean isValidMethodCall(Method method) {
        Class clazz = method.getDeclaringClass();
        if (clazz.isAssignableFrom(seiClazz) ||
                clazz.isAssignableFrom(org.apache.axis2.jaxws.spi.BindingProvider.class) ||
                clazz.isAssignableFrom(javax.xml.ws.BindingProvider.class)) {
            return true;
        }
        return false;
    }

    private boolean isPublic(Method method) {
        return Modifier.isPublic(method.getModifiers());
    }

    private boolean isMethodExcluded(OperationDescription operationDesc) {
        return operationDesc.isExcluded();
    }

    public Class getSeiClazz() {
        return seiClazz;
    }

    public void setSeiClazz(Class seiClazz) {
        this.seiClazz = seiClazz;
    }
}
