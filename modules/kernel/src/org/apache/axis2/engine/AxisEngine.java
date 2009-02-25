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


package org.apache.axis2.engine;

import org.apache.axiom.soap.RolePlayer;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.client.async.AxisCallback;
import org.apache.axis2.client.async.Callback;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.engine.Handler.InvocationResponse;
import org.apache.axis2.i18n.Messages;
import org.apache.axis2.transport.TransportSender;
import org.apache.axis2.util.CallbackReceiver;
import org.apache.axis2.util.LoggingControl;
import org.apache.axis2.util.MessageContextBuilder;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * There is one engine for the Server and the Client. the send() and receive()
 * Methods are the basic operations the Sync, Async messageing are build on top.
 */
public class AxisEngine {

    /**
     * Field log
     */
    private static final Log log = LogFactory.getLog(AxisEngine.class);

    private static boolean RESUMING_EXECUTION = true;
    private static boolean NOT_RESUMING_EXECUTION = false;

    /**
     * Constructor AxisEngine
     */
    public AxisEngine(ConfigurationContext engineContext) {
    }

    private static void checkMustUnderstand(MessageContext msgContext) throws AxisFault {
        List unprocessed = null;
        SOAPEnvelope envelope = msgContext.getEnvelope();
        if (envelope.getHeader() == null) {
            return;
        }
        // Get all the headers targeted to us
        Iterator headerBlocks = envelope.getHeader().getHeadersToProcess((RolePlayer)msgContext.getConfigurationContext().getAxisConfiguration().getParameterValue("rolePlayer"));
        while (headerBlocks.hasNext()) {
            SOAPHeaderBlock headerBlock = (SOAPHeaderBlock) headerBlocks.next();
            QName headerName = headerBlock.getQName();
            // if this header block has been processed or mustUnderstand isn't
            // turned on then its cool
            if (headerBlock.isProcessed() || !headerBlock.getMustUnderstand()) {
                continue;
            }

            if(LoggingControl.debugLoggingAllowed && log.isDebugEnabled()){
                log.debug("MustUnderstand header not processed or registered as understood"+headerName);
            }
            if(isReceiverMustUnderstandProcessor(msgContext)){
                if(unprocessed == null){
                    unprocessed = new ArrayList();
                }
                if(!unprocessed.contains(headerName)){
                    unprocessed.add(headerName);
                }
                continue;
            }
            // Oops, throw an appropriate MustUnderstand fault!!
            QName faultQName = headerBlock.getVersion().getMustUnderstandFaultCode();
            throw new AxisFault(Messages.getMessage("mustunderstandfailed",
                headerBlock.getNamespace().getNamespaceURI(),
                headerBlock.getLocalName()), faultQName);
        }
        if(unprocessed !=null && unprocessed.size()>0){
            //Adding HeaderQNames that failed MU check as AxisService Parameter
            //They will be examined later by MessageReceivers.
            if(log.isDebugEnabled()){
                log.debug("Adding Unprocessed headers to MessageContext.");
            }
            msgContext.setProperty(Constants.UNPROCESSED_HEADER_QNAMES, unprocessed);           
        }       
    }

    private static boolean isReceiverMustUnderstandProcessor(MessageContext msgContext){
        MessageReceiver receiver = null;
        if(msgContext.isServerSide()){
            receiver = msgContext.getAxisOperation().getMessageReceiver();
        }
        return (receiver!=null && receiver.getClass().getName().endsWith("JAXWSMessageReceiver"));
    }
    /**
     * This method is called to handle any error that occurs at inflow or outflow. But if the
     * method is called twice, it implies that sending the error handling has failed, in which case
     * the method logs the error and exists.
     *
     * @deprecated (post 1.1 branch)
     */
    public static MessageContext createFaultMessageContext(MessageContext processingContext, Throwable e)
            throws AxisFault {
        return MessageContextBuilder.createFaultMessageContext(processingContext, e);
    }

    /**
     * This methods represents the inflow of the Axis, this could be either at the server side or the client side.
     * Here the <code>ExecutionChain</code> is created using the Phases. The Handlers at the each Phases is ordered in
     * deployment time by the deployment module
     *
     * @throws AxisFault
     * @see MessageContext
     * @see Phase
     * @see Handler
     */
    public static InvocationResponse receive(MessageContext msgContext) throws AxisFault {
        if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
            log.trace(msgContext.getLogIDString() + " receive:" + msgContext.getMessageID());
        }
        ConfigurationContext confContext = msgContext.getConfigurationContext();
        ArrayList preCalculatedPhases;
        if (msgContext.isFault() || msgContext.isProcessingFault()) {
            preCalculatedPhases = confContext.getAxisConfiguration().getInFaultFlowPhases();
            msgContext.setFLOW(MessageContext.IN_FAULT_FLOW);
        } else {
            preCalculatedPhases = confContext.getAxisConfiguration().getInFlowPhases();
            msgContext.setFLOW(MessageContext.IN_FLOW);
        }
        // Set the initial execution chain in the MessageContext to a *copy* of what
        // we got above.  This allows individual message processing to change the chain without
        // affecting later messages.
        msgContext.setExecutionChain((ArrayList) preCalculatedPhases.clone());
        try {
            InvocationResponse pi = invoke(msgContext, NOT_RESUMING_EXECUTION);

            if (pi.equals(InvocationResponse.CONTINUE)) {
                checkMustUnderstand(msgContext);
                if (msgContext.isServerSide()) {
                    // invoke the Message Receivers

                    MessageReceiver receiver = msgContext.getAxisOperation().getMessageReceiver();
                    if (receiver == null) {
                        throw new AxisFault(Messages.getMessage(
                                "nomessagereciever",
                                msgContext.getAxisOperation().getName().toString()));
                    }
                    receiver.receive(msgContext);
                }
                flowComplete(msgContext);
            } else if (pi.equals(InvocationResponse.SUSPEND)) {
                return pi;
            } else if (pi.equals(InvocationResponse.ABORT)) {
                flowComplete(msgContext);
                // Undo any partial work.
                // Remove the incoming message context
                if (log.isDebugEnabled()) {
                    log.debug("InvocationResponse is aborted.  " +
                                "The incoming MessageContext is removed, " +
                                "and the OperationContext is marked as incomplete");
                }
				AxisOperation axisOp = msgContext.getAxisOperation();
                if(axisOp!=null){
					String mepURI  = axisOp.getMessageExchangePattern();
					if (WSDL2Constants.MEP_URI_OUT_IN.equals(mepURI)) {
						OperationContext opCtx = msgContext.getOperationContext();
						if (opCtx != null) {
							opCtx.removeMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
						}
					}
				}
				else{
					log.debug("Could not clean up op ctx for " + msgContext);
				}
                return pi;
            } else {
                String errorMsg =
                        "Unrecognized InvocationResponse encountered in AxisEngine.receive()";
                log.error(msgContext.getLogIDString() + " " + errorMsg);
                throw new AxisFault(errorMsg);
            }
        }
        catch (AxisFault e) {
            log.error(e.getMessage(), e);
            msgContext.setFailureReason(e);
            flowComplete(msgContext);
            throw e;
        }

        return InvocationResponse.CONTINUE;
    }

    private static void processFault(MessageContext msgContext, AxisFault e) {
        try {
            MessageContext faultMC = MessageContextBuilder.createFaultMessageContext(msgContext, e);

            // Figure out where this goes
            sendFault(faultMC);
        } catch (AxisFault axisFault) {
            log.error(axisFault.getMessage(), axisFault);
        }
    }

    /**
     * Take the execution chain from the msgContext , and then take the current Index
     * and invoke all the phases in the arraylist
     * if the msgContext is pauesd then the execution will be breaked
     *
     * @param msgContext
     * @return An InvocationResponse that indicates what
     *         the next step in the message processing should be.
     * @throws AxisFault
     */
    private static InvocationResponse invoke(MessageContext msgContext, boolean resuming)
            throws AxisFault {

        if (msgContext.getCurrentHandlerIndex() == -1) {
            msgContext.setCurrentHandlerIndex(0);
        }

        InvocationResponse pi = InvocationResponse.CONTINUE;

        while (msgContext.getCurrentHandlerIndex() < msgContext.getExecutionChain().size()) {
            Handler currentHandler = (Handler) msgContext.getExecutionChain().
                    get(msgContext.getCurrentHandlerIndex());

            try {
                if (!resuming) {
                    msgContext.addExecutedPhase(currentHandler);
                } else {
                    /* If we are resuming the flow, we don't want to add the phase
                    * again, as it has already been added.
                    */
                    resuming = false;
                }
                pi = currentHandler.invoke(msgContext);
            }
            catch (AxisFault e) {
                if (msgContext.getCurrentPhaseIndex() == 0) {
                    /* If we got a fault, we still want to add the phase to the
                    list to be executed for flowComplete(...) unless this was
                    the first handler, as then the currentPhaseIndex will be
                    set to 0 and this will look like we've executed all of the
                    handlers.  If, at some point, a phase really needs to get
                    notification of flowComplete, then we'll need to introduce
                    some more complex logic to keep track of what has been
                    executed.*/
                    msgContext.removeFirstExecutedPhase();
                }
                throw e;
            }

            if (pi.equals(InvocationResponse.SUSPEND) ||
                    pi.equals(InvocationResponse.ABORT)) {
                break;
            }

            msgContext.setCurrentHandlerIndex(msgContext.getCurrentHandlerIndex() + 1);
        }

        return pi;
    }

    private static void flowComplete(MessageContext msgContext) {
        Iterator invokedPhaseIterator = msgContext.getExecutedPhases();

        while (invokedPhaseIterator.hasNext()) {
            Handler currentHandler = ((Handler) invokedPhaseIterator.next());
            currentHandler.flowComplete(msgContext);
        }

        /*This is needed because the OutInAxisOperation currently invokes
        * receive() even when a fault occurs, and we will have already executed
        * the flowComplete on those before receiveFault() is called.
        */
        msgContext.resetExecutedPhases();
    }

    /**
     * If the msgConetext is puased and try to invoke then
     * first invoke the phase list and after the message receiver
     *
     * @param msgContext
     * @return An InvocationResponse allowing the invoker to perhaps determine
     *         whether or not the message processing will ever succeed.
     * @throws AxisFault
     */
    public static InvocationResponse resumeReceive(MessageContext msgContext) throws AxisFault {
        if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
            log.trace(msgContext.getLogIDString() + " resumeReceive:" + msgContext.getMessageID());
        }

        //REVIEW: This name is a little misleading, as it seems to indicate that there should be a resumeReceiveFault as well, when, in fact, this does both
        //REVIEW: Unlike with receive, there is no wrapping try/catch clause which would
        //fire off the flowComplete on an error, as we have to assume that the
        //message will be resumed again, but perhaps we need to unwind back to
        //the point at which the message was resumed and provide another API
        //to allow the full unwind if the message is going to be discarded.
        //invoke the phases
        InvocationResponse pi = invoke(msgContext, RESUMING_EXECUTION);
        //invoking the MR

        if (pi.equals(InvocationResponse.CONTINUE)) {
            checkMustUnderstand(msgContext);
            if (msgContext.isServerSide()) {
                // invoke the Message Receivers
                MessageReceiver receiver = msgContext.getAxisOperation().getMessageReceiver();
                if (receiver == null) {
                    throw new AxisFault(Messages.getMessage(
                            "nomessagereciever",
                            msgContext.getAxisOperation().getName().toString()));
                }
                receiver.receive(msgContext);
            }
            flowComplete(msgContext);
        }

        return pi;
    }

    /**
     * To resume the invocation at the send path , this is neened since it is require to call
     * TransportSender at the end
     *
     * @param msgContext
     * @return An InvocationResponse allowing the invoker to perhaps determine
     *         whether or not the message processing will ever succeed.
     * @throws AxisFault
     */
    public static InvocationResponse resumeSend(MessageContext msgContext) throws AxisFault {
        if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
            log.trace(msgContext.getLogIDString() + " resumeSend:" + msgContext.getMessageID());
        }

        //REVIEW: This name is a little misleading, as it seems to indicate that there should be a resumeSendFault as well, when, in fact, this does both
        //REVIEW: Unlike with send, there is no wrapping try/catch clause which would
        //fire off the flowComplete on an error, as we have to assume that the
        //message will be resumed again, but perhaps we need to unwind back to
        //the point at which the message was resumed and provide another API
        //to allow the full unwind if the message is going to be discarded.
        //invoke the phases
        InvocationResponse pi = invoke(msgContext, RESUMING_EXECUTION);
        //Invoking Transport Sender
        if (pi.equals(InvocationResponse.CONTINUE)) {
            // write the Message to the Wire
            TransportOutDescription transportOut = msgContext.getTransportOut();
            TransportSender sender = transportOut.getSender();
            sender.invoke(msgContext);
            flowComplete(msgContext);
        }

        return pi;
    }

    /**
     * Resume processing of a message.
     *
     * @param msgctx
     * @return An InvocationResponse allowing the invoker to perhaps determine
     *         whether or not the message processing will ever succeed.
     * @throws AxisFault
     */
    public static InvocationResponse resume(MessageContext msgctx) throws AxisFault {
        if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
            log.trace(msgctx.getLogIDString() + " resume:" + msgctx.getMessageID());
        }

        msgctx.setPaused(false);
        if (msgctx.getFLOW() == MessageContext.IN_FLOW) {
            return resumeReceive(msgctx);
        } else {
            return resumeSend(msgctx);
        }
    }

    /**
     * This methods represents the outflow of the Axis, this could be either at the server side or the client side.
     * Here the <code>ExecutionChain</code> is created using the Phases. The Handlers at the each Phases is ordered in
     * deployment time by the deployment module
     *
     * @param msgContext
     * @throws AxisFault
     * @see MessageContext
     * @see Phase
     * @see Handler
     */
    public static void send(MessageContext msgContext) throws AxisFault {
        if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
            log.trace(msgContext.getLogIDString() + " send:" + msgContext.getMessageID());
        }
        // find and invoke the Phases
        OperationContext operationContext = msgContext.getOperationContext();
        ArrayList executionChain = operationContext.getAxisOperation().getPhasesOutFlow();
        //rather than having two steps added both oparation and global chain together
        ArrayList outPhases = new ArrayList();
        outPhases.addAll(executionChain);
        outPhases.addAll(msgContext.getConfigurationContext().getAxisConfiguration().getOutFlowPhases());
        msgContext.setExecutionChain(outPhases);
        msgContext.setFLOW(MessageContext.OUT_FLOW);
        try {
            InvocationResponse pi = invoke(msgContext, NOT_RESUMING_EXECUTION);

            if (pi.equals(InvocationResponse.CONTINUE)) {
                // write the Message to the Wire
                TransportOutDescription transportOut = msgContext.getTransportOut();
                if (transportOut == null) {
                    throw new AxisFault("Transport out has not been set");
                }
                TransportSender sender = transportOut.getSender();
                // This boolean property only used in client side fireAndForget invocation
                //It will set a property into message context and if some one has set the
                //property then transport sender will invoke in a diffrent thread
                Object isTransportNonBlocking = msgContext.getProperty(
                        MessageContext.TRANSPORT_NON_BLOCKING);
                if (isTransportNonBlocking != null &&
                        ((Boolean) isTransportNonBlocking).booleanValue()) {
                    msgContext.getConfigurationContext().getThreadPool().execute(
                            new TransportNonBlockingInvocationWorker(msgContext, sender));
                } else {
                    sender.invoke(msgContext);
                }
                //REVIEW: In the case of the TransportNonBlockingInvocationWorker, does this need to wait until that finishes?
                flowComplete(msgContext);
            } else if (pi.equals(InvocationResponse.SUSPEND)) {
            } else if (pi.equals(InvocationResponse.ABORT)) {
                flowComplete(msgContext);
            } else {
                String errorMsg =
                        "Unrecognized InvocationResponse encountered in AxisEngine.send()";
                log.error(msgContext.getLogIDString() + " " + errorMsg);
                throw new AxisFault(errorMsg);
            }
        } catch (AxisFault e) {
            msgContext.setFailureReason(e);
            flowComplete(msgContext);
            throw e;
        }
    }

    /**
     * Sends the SOAP Fault to another SOAP node.
     *
     * @param msgContext
     * @throws AxisFault
     */
    public static void sendFault(MessageContext msgContext) throws AxisFault {
        if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
            log.trace(msgContext.getLogIDString() + " sendFault:" + msgContext.getMessageID());
        }
        OperationContext opContext = msgContext.getOperationContext();

        //FIXME: If this gets paused in the operation-specific phases, the resume is not going to function correctly as the phases will not have all been set

        // find and execute the Fault Out Flow Handlers
        if (opContext != null) {
            AxisOperation axisOperation = opContext.getAxisOperation();
            ArrayList faultExecutionChain = axisOperation.getPhasesOutFaultFlow();

            //adding both operation specific and global out fault flows.

            ArrayList outFaultPhases = new ArrayList();
            outFaultPhases.addAll((ArrayList) faultExecutionChain.clone());
            msgContext.setExecutionChain((ArrayList) outFaultPhases.clone());
            msgContext.setFLOW(MessageContext.OUT_FAULT_FLOW);
            try {
                InvocationResponse pi = invoke(msgContext, NOT_RESUMING_EXECUTION);

                if (pi.equals(InvocationResponse.SUSPEND)) {
                    log.warn(msgContext.getLogIDString() +
                            " The resumption of this flow may function incorrectly, as the OutFaultFlow will not be used");
                    return;
                } else if (pi.equals(InvocationResponse.ABORT)) {
                    flowComplete(msgContext);
                    return;
                } else if (!pi.equals(InvocationResponse.CONTINUE)) {
                    String errorMsg =
                            "Unrecognized InvocationResponse encountered in AxisEngine.sendFault()";
                    log.error(msgContext.getLogIDString() + " " + errorMsg);
                    throw new AxisFault(errorMsg);
                }
            }
            catch (AxisFault e) {
                msgContext.setFailureReason(e);
                flowComplete(msgContext);
                throw e;
            }
        }

        msgContext.setExecutionChain((ArrayList) msgContext.getConfigurationContext()
                .getAxisConfiguration().getOutFaultFlowPhases().clone());
        msgContext.setFLOW(MessageContext.OUT_FAULT_FLOW);
        InvocationResponse pi = invoke(msgContext, NOT_RESUMING_EXECUTION);

        if (pi.equals(InvocationResponse.CONTINUE)) {
            // Actually send the SOAP Fault
            TransportOutDescription transportOut = msgContext.getTransportOut();
            if (transportOut == null) {
                throw new AxisFault("Transport out has not been set");
            }
            TransportSender sender = transportOut.getSender();

            sender.invoke(msgContext);
            flowComplete(msgContext);
        } else if (pi.equals(InvocationResponse.SUSPEND)) {
        } else if (pi.equals(InvocationResponse.ABORT)) {
            flowComplete(msgContext);
        } else {
            String errorMsg =
                    "Unrecognized InvocationResponse encountered in AxisEngine.sendFault()";
            log.error(msgContext.getLogIDString() + " " + errorMsg);
            throw new AxisFault(errorMsg);
        }
    }

    /**
     * here we assume that it is resume from an operation level handler
     * @param msgContext
     * @throws AxisFault
     */
    public static void resumeSendFault(MessageContext msgContext) throws AxisFault{
        if (LoggingControl.debugLoggingAllowed && log.isTraceEnabled()) {
            log.trace(msgContext.getLogIDString() + " resumeSendFault:" + msgContext.getMessageID());
        }
        OperationContext opContext = msgContext.getOperationContext();

        if (opContext != null) {

            try {
                InvocationResponse pi = invoke(msgContext, RESUMING_EXECUTION);

                if (pi.equals(InvocationResponse.SUSPEND)) {
                    log.warn(msgContext.getLogIDString() +
                            " The resumption of this flow may function incorrectly, as the OutFaultFlow will not be used");
                    return;
                } else if (pi.equals(InvocationResponse.ABORT)) {
                    flowComplete(msgContext);
                    return;
                } else if (!pi.equals(InvocationResponse.CONTINUE)) {
                    String errorMsg =
                            "Unrecognized InvocationResponse encountered in AxisEngine.sendFault()";
                    log.error(msgContext.getLogIDString() + " " + errorMsg);
                    throw new AxisFault(errorMsg);
                }
            } catch (AxisFault e) {
                msgContext.setFailureReason(e);
                flowComplete(msgContext);
                throw e;
            }
        }

        msgContext.setExecutionChain((ArrayList) msgContext.getConfigurationContext()
                .getAxisConfiguration().getOutFaultFlowPhases().clone());
        msgContext.setFLOW(MessageContext.OUT_FAULT_FLOW);
        InvocationResponse pi = invoke(msgContext, NOT_RESUMING_EXECUTION);

        if (pi.equals(InvocationResponse.CONTINUE)) {
            // Actually send the SOAP Fault
            TransportOutDescription transportOut = msgContext.getTransportOut();
            if (transportOut == null) {
                throw new AxisFault("Transport out has not been set");
            }
            TransportSender sender = transportOut.getSender();

            sender.invoke(msgContext);
            flowComplete(msgContext);
        } else if (pi.equals(InvocationResponse.SUSPEND)) {
        } else if (pi.equals(InvocationResponse.ABORT)) {
            flowComplete(msgContext);
        } else {
            String errorMsg =
                    "Unrecognized InvocationResponse encountered in AxisEngine.sendFault()";
            log.error(msgContext.getLogIDString() + " " + errorMsg);
            throw new AxisFault(errorMsg);
        }
    }


    /**
     * This class is used when someone invoke a service invocation with two transports
     * If we dont create a new thread then the main thread will block untill it gets the
     * response . In the case of HTTP transportsender will block untill it gets HTTP 200
     * So , main thread also block till transport sender rereases the tread. So there is no
     * actual non-blocking. That is why when sending we creat a new thead and send the
     * requset via that.
     * <p/>
     * So whole porpose of this class to send the requset via a new thread
     * <p/>
     * way transport.
     */
    private static class TransportNonBlockingInvocationWorker implements Runnable {
        private MessageContext msgctx;
        private TransportSender sender;

        public TransportNonBlockingInvocationWorker(MessageContext msgctx,
            TransportSender sender) {
            this.msgctx = msgctx;
            this.sender = sender;
        }

        public void run() {
            try {
                sender.invoke(msgctx);
            } catch (Exception e) {
                log.info(msgctx.getLogIDString() + " " + e.getMessage());
                if (msgctx.getProperty(MessageContext.DISABLE_ASYNC_CALLBACK_ON_TRANSPORT_ERROR) ==
                        null) {
                    AxisOperation axisOperation = msgctx.getAxisOperation();
                    if (axisOperation != null) {
                        MessageReceiver msgReceiver = axisOperation.getMessageReceiver();
                        if ((msgReceiver != null) && (msgReceiver instanceof CallbackReceiver)) {
                            Object callback = ((CallbackReceiver) msgReceiver)
                                    .lookupCallback(msgctx.getMessageID());
                            if (callback == null) return; // TODO: should we log this??

                            if (callback instanceof Callback) {
                                ((Callback)callback).onError(e);
                            } else {
                                ((AxisCallback)callback).onError(e);
                            }
                        }
                    }
                }
            }
        }
    }
}
