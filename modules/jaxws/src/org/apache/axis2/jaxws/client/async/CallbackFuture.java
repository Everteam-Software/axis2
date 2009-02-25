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
package org.apache.axis2.jaxws.client.async;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.async.AsyncResult;
import org.apache.axis2.client.async.Callback;
import org.apache.axis2.jaxws.core.InvocationContext;
import org.apache.axis2.jaxws.core.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.ws.AsyncHandler;
import javax.xml.ws.WebServiceException;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * The CallbackFuture implements the Axis2 <link>org.apache.axis2.client.async.Callback</link> API
 * and will get registered with the Axis2 engine to receive the asynchronous callback responses.
 * This object is also responsible for taking the <link>java.util.concurrent.Executor</link> given
 * to it by the JAX-WS client and using that as the thread on which to deliver the async response
 * the JAX-WS <link>javax.xml.ws.AsynchHandler</link>.
 */
public class CallbackFuture extends Callback {

    private static final Log log = LogFactory.getLog(CallbackFuture.class);
    private static final boolean debug = log.isDebugEnabled();

    private CallbackFutureTask cft;
    private Executor executor;
    private FutureTask task;
    
    private InvocationContext invocationCtx;
    
    /*
     * There are two Async Callback Future.cancel scenario that we address
     * 1) Client app creates request and call Async Operation. Now before the request is submitted
     * 	  by JAXWS to Executor for processing and any response is received client decides to cancel 
     * 	  the future task.
     * 2) Client app creates request and call Async Operation. Request is submitted by JAXWS 
     * 	  to Executor for processing and a response is received and client decides to cancel the future
     * 	  task.
     * 
     * We will address both these scenarios in the code. In scenario 1 we will do the following:
     * 1) Check the for the future.isCancelled before submitting the task to Executor 
     * 2) If cancelled then do not submit the task and do not call the Async Handler of client. 
     * 3)The client program in this case (Since it cancelled the future) will be responsible for cleaning any resources that it engages.
     * 
     * In Second Scenario we will call the AsyncHandler as Future.isCancelled will be false. As per java doc
     * the Future cannot be cancelled once the task has been submitted. Also the response has already arrived so 
     * we will make the AsyncHandler and let the client code decided how it wants to treat the response.
     */

    @SuppressWarnings("unchecked")
    public CallbackFuture(InvocationContext ic, AsyncHandler handler) {
        cft = new CallbackFutureTask(ic.getAsyncResponseListener(), handler);
        task = new FutureTask(cft);
        executor = ic.getExecutor();
        
        /*
         * TODO review.  We need to save the invocation context so we can set it on the
         * response (or fault) context so the FutureCallback has access to the handler list.
         */
        invocationCtx = ic;
    }

    public Future<?> getFutureTask() {
        return (Future<?>)task;
    }

    @Override
    public void onComplete(AsyncResult result) {
        if (debug) {
            log.debug("JAX-WS received the async response");
        }

        MessageContext response = null;
        try {
            response = AsyncUtils.createJAXWSMessageContext(result);
            response.setInvocationContext(invocationCtx);
            // make sure request and response contexts share a single parent
            response.setMEPContext(invocationCtx.getRequestMessageContext().getMEPContext());
        } catch (WebServiceException e) {
            cft.setError(e);
            if (debug) {
                log.debug(
                        "An error occured while processing the async response.  " + e.getMessage());
            }
        }

        if (response == null) {
            // TODO: throw an exception
        }

        cft.setMessageContext(response);
        execute();
    }

    @Override
    public void onError(Exception e) {
        // If a SOAPFault was returned by the AxisEngine, the AxisFault
        // that is returned should have a MessageContext with it.  Use
        // this to unmarshall the fault included there.
        if (e.getClass().isAssignableFrom(AxisFault.class)) {
            AxisFault fault = (AxisFault)e;
            MessageContext faultMessageContext = null;
            try {
                faultMessageContext  = AsyncUtils.createJAXWSMessageContext(fault.getFaultMessageContext());
                faultMessageContext.setInvocationContext(invocationCtx);
                // make sure request and response contexts share a single parent
                faultMessageContext.setMEPContext(invocationCtx.getRequestMessageContext().getMEPContext());
            } catch (WebServiceException wse) {
                cft.setError(wse);
            }

            cft.setError(e);
            cft.setMessageContext(faultMessageContext);
        } else {
            cft.setError(e);
        }

        execute();
    }

    private void execute() {
        if (log.isDebugEnabled()) {
            log.debug("Executor task starting to process async response");
        }

        if (executor != null) {
            if (task != null && !task.isCancelled()) {
                try {
                    executor.execute(task);
                }
                catch (Exception executorExc) {
                    if (log.isDebugEnabled()) {
                        log.debug("CallbackFuture.execute():  executor exception [" +
                                executorExc.getClass().getName() + "]");
                    }

                    // attempt to cancel the FutureTask
                    task.cancel(true);

                    //   note: if it is becomes required to return the actual exception
                    //         to the client, then we would need to doing something
                    //         similar to setting the CallbackFutureTask with the error
                    //         and invoking the CallbackFutureTask.call() interface
                    //         to process the information
                    //
                }

                if (log.isDebugEnabled()) {
                    log.debug("Task submitted to Executor");
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.info(
                            "Executor task was not sumbitted as Async Future task was cancelled by clients");
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Executor task completed");
        }
    }
}

class CallbackFutureTask implements Callable {

    private static final Log log = LogFactory.getLog(CallbackFutureTask.class);
    private static final boolean debug = log.isDebugEnabled();

    AsyncResponse response;
    MessageContext msgCtx;
    AsyncHandler handler;
    Exception error;

    CallbackFutureTask(AsyncResponse r, AsyncHandler h) {
        response = r;
        handler = h;
    }

    void setMessageContext(MessageContext mc) {
        msgCtx = mc;
    }

    void setError(Exception e) {
        error = e;
    }

    @SuppressWarnings("unchecked")
    public Object call() throws Exception {
        // Set the response or fault content on the AsyncResponse object
        // so that it can be collected inside the Executor thread and processed.
        if (error != null) {
            response.onError(error, msgCtx);
        } else {
            response.onComplete(msgCtx);
        }

        // Now that the content is available, call the JAX-WS AsyncHandler class
        // to deliver the response to the user.
        try {
            ClassLoader cl = handler.getClass().getClassLoader();
        	if (log.isDebugEnabled()) {
        		log.debug("Setting up the thread's ClassLoader");
        		log.debug(cl.toString());
        	}
        	Thread.currentThread().setContextClassLoader(cl);
            
            if (debug) {
                log.debug("Calling JAX-WS AsyncHandler with the Response object");
                log.debug("AyncHandler class: " + handler.getClass());
            }
            handler.handleResponse(response);
        }
        catch (Throwable t) {
            if (debug) {
                log.debug("An error occured while invoking the callback object.");
                log.debug("Error: " + t.getMessage());
            }
        }

        return null;
    }
}