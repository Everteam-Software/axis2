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

package org.apache.axis2.clustering.tribes;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.clustering.ClusterManager;
import org.apache.axis2.clustering.ClusteringConstants;
import org.apache.axis2.clustering.ClusteringFault;
import org.apache.axis2.clustering.LoadBalanceEventHandler;
import org.apache.axis2.clustering.MembershipListener;
import org.apache.axis2.clustering.MembershipScheme;
import org.apache.axis2.clustering.RequestBlockingHandler;
import org.apache.axis2.clustering.configuration.ConfigurationManager;
import org.apache.axis2.clustering.configuration.DefaultConfigurationManager;
import org.apache.axis2.clustering.context.ClusteringContextListener;
import org.apache.axis2.clustering.context.ContextManager;
import org.apache.axis2.clustering.context.DefaultContextManager;
import org.apache.axis2.clustering.control.ControlCommand;
import org.apache.axis2.clustering.control.GetConfigurationCommand;
import org.apache.axis2.clustering.control.GetStateCommand;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.PhaseRule;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.DispatchPhase;
import org.apache.axis2.engine.Phase;
import org.apache.catalina.tribes.Channel;
import org.apache.catalina.tribes.ChannelException;
import org.apache.catalina.tribes.ManagedChannel;
import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.group.GroupChannel;
import org.apache.catalina.tribes.group.Response;
import org.apache.catalina.tribes.group.RpcChannel;
import org.apache.catalina.tribes.transport.MultiPointSender;
import org.apache.catalina.tribes.transport.ReplicationTransmitter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * The main ClusterManager class for the Tribes based clustering implementation
 */
public class TribesClusterManager implements ClusterManager {

    private static final Log log = LogFactory.getLog(TribesClusterManager.class);

    private DefaultConfigurationManager configurationManager;
    private DefaultContextManager contextManager;

    private HashMap<String, Parameter> parameters;
    private ManagedChannel channel;
    private RpcChannel rpcInitChannel;
    private ConfigurationContext configurationContext;
    private ChannelListener channelListener;
    private ChannelSender channelSender;
    private MembershipManager primaryMembershipManager;
    private RpcInitializationRequestHandler rpcInitRequestHandler;
    private MembershipScheme membershipScheme;

    /**
     * The mode in which this member operates such as "loadBalance" or "application"
     */
    private OperationMode mode;

    /**
     * Static members
     */
    private List<org.apache.axis2.clustering.Member> members;

    private final Map<String, LoadBalanceEventHandler> lbEventHandlers =
            new HashMap<String, LoadBalanceEventHandler>();
    private boolean loadBalanceMode;

    public TribesClusterManager() {
        parameters = new HashMap<String, Parameter>();
    }

    public void setMembers(List<org.apache.axis2.clustering.Member> members) {
        this.members = members;
    }

    public List<org.apache.axis2.clustering.Member> getMembers() {
        return members;
    }

    public void addLoadBalanceEventHandler(LoadBalanceEventHandler eventHandler,
                                           String applicationDomain) {
        log.info("Load balancing for application domain " + applicationDomain +
                 " using event handler " + eventHandler.getClass());
        lbEventHandlers.put(applicationDomain, eventHandler);
        loadBalanceMode = true;
    }

    public LoadBalanceEventHandler getLoadBalanceEventHandler(String applicationDomain) {
        return lbEventHandlers.get(applicationDomain);
    }

    public Set<String> getDomains() {
        return lbEventHandlers.keySet();
    }

    public ContextManager getContextManager() {
        return contextManager;
    }

    public ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }

    /**
     * Initialize the cluster.
     *
     * @throws ClusteringFault If initialization fails
     */
    public void init() throws ClusteringFault {
        log.info("Initializing cluster...");
        addRequestBlockingHandlerToInFlows();
        primaryMembershipManager = new MembershipManager(configurationContext);

        channel = new GroupChannel();
        channelSender = new ChannelSender(channel, primaryMembershipManager, synchronizeAllMembers());
        channelListener =
                new ChannelListener(configurationContext, configurationManager, contextManager);
        channel.addChannelListener(channelListener);

        byte[] domain = getClusterDomain();
        log.info("Cluster domain: " + new String(domain));
        primaryMembershipManager.setDomain(domain);

        // RpcChannel is a ChannelListener. When the reply to a particular request comes back, it
        // picks it up. Each RPC is given a UUID, hence can correlate the request-response pair
        rpcInitRequestHandler = new RpcInitializationRequestHandler(configurationContext);
        rpcInitChannel =
                new RpcChannel(TribesUtil.getRpcInitChannelId(domain),
                               channel, rpcInitRequestHandler);
        if (log.isDebugEnabled()) {
            log.debug("Created RPC Channel for domain " + new String(domain));
        }

        setMaximumRetries();
        configureMode(domain);
        configureMembershipScheme(domain, mode.getMembershipManagers());
        setMemberInfo();

        TribesMembershipListener membershipListener = new TribesMembershipListener(primaryMembershipManager);
        channel.addMembershipListener(membershipListener);
        try {
            channel.start(Channel.DEFAULT); // At this point, this member joins the group
            String localHost = TribesUtil.getLocalHost(channel);
            if (localHost.startsWith("127.0.")) {
                channel.stop(Channel.DEFAULT);
                throw new ClusteringFault("Cannot join cluster using IP " + localHost +
                                          ". Please set an IP address other than " +
                                          localHost + " in the axis2.xml file");
            }
        } catch (ChannelException e) {
            String msg = "Error starting Tribes channel";
            log.error(msg, e);
            throw new ClusteringFault(msg, e);
        }

        log.info("Local Member " + TribesUtil.getLocalHost(channel));
        TribesUtil.printMembers(primaryMembershipManager);

        membershipScheme.joinGroup();

        // If configuration management is enabled, get the latest config from a neighbour
        if (configurationManager != null) {
            configurationManager.setSender(channelSender);
            initializeSystem(new GetConfigurationCommand());
        }

        // If context replication is enabled, get the latest state from a neighbour
        if (contextManager != null) {
            contextManager.setSender(channelSender);
            channelListener.setContextManager(contextManager);
            initializeSystem(new GetStateCommand());
            ClusteringContextListener contextListener = new ClusteringContextListener(channelSender);
            configurationContext.addContextListener(contextListener);
        }

        configurationContext.
                setNonReplicableProperty(ClusteringConstants.CLUSTER_INITIALIZED, "true");
        log.info("Cluster initialization completed.");
    }

    private void setMemberInfo() throws ClusteringFault {
        Properties memberInfo = new Properties();
        AxisConfiguration axisConfig = configurationContext.getAxisConfiguration();
        TransportInDescription httpTransport = axisConfig.getTransportIn("http");
        if (httpTransport != null) {
            Parameter port = httpTransport.getParameter("port");
            if (port != null) {
                memberInfo.put("httpPort", port.getValue());
            }
        }
        TransportInDescription httpsTransport = axisConfig.getTransportIn("https");
        if (httpsTransport != null) {
            Parameter port = httpsTransport.getParameter("port");
            if (port != null) {
                memberInfo.put("httpsPort", port.getValue());
            }
        }
        Parameter isActiveParam = getParameter(ClusteringConstants.Parameters.IS_ACTIVE);
        if (isActiveParam != null) {
            memberInfo.setProperty(ClusteringConstants.Parameters.IS_ACTIVE,
                                   (String) isActiveParam.getValue());
        }

        memberInfo.setProperty("hostName",
                               TribesUtil.getLocalHost(getParameter(TribesConstants.LOCAL_MEMBER_HOST)));

        Parameter propsParam = getParameter("properties");
        if(propsParam != null){
            OMElement paramEle = propsParam.getParameterElement();
            for(Iterator iter = paramEle.getChildrenWithLocalName("property"); iter.hasNext();){
                OMElement propEle = (OMElement) iter.next();
                OMAttribute nameAttrib = propEle.getAttribute(new QName("name"));
                if(nameAttrib != null){
                    OMAttribute valueAttrib = propEle.getAttribute(new QName("value"));
                    if  (valueAttrib != null) {
                        String attribVal = valueAttrib.getAttributeValue();
                        attribVal = replaceProperty(attribVal, memberInfo);
                        memberInfo.setProperty(nameAttrib.getAttributeValue(), attribVal);
                    }
                }
            }
        }

        memberInfo.remove("hostName"); // this was needed only to populate other properties. No need to send it.

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
            memberInfo.store(bout, "");
        } catch (IOException e) {
            String msg = "Cannot store member transport properties in the ByteArrayOutputStream";
            log.error(msg, e);
            throw new ClusteringFault(msg, e);
        }
        channel.getMembershipService().setPayload(bout.toByteArray());
    }

    private static String replaceProperty(String text, Properties props) {
        int indexOfStartingChars = -1;
        int indexOfClosingBrace;

        // The following condition deals with properties.
        // Properties are specified as ${system.property},
        // and are assumed to be System properties
        while (indexOfStartingChars < text.indexOf("${") &&
               (indexOfStartingChars = text.indexOf("${")) != -1 &&
            (indexOfClosingBrace = text.indexOf("}")) != -1) { // Is a property used?
            String sysProp = text.substring(indexOfStartingChars + 2,
                                            indexOfClosingBrace);
            String propValue = props.getProperty(sysProp);
            if (propValue != null) {
                text = text.substring(0, indexOfStartingChars) + propValue +
                       text.substring(indexOfClosingBrace + 1);
            }
        }
        return text;
    }

    /**
     * Get the membership scheme applicable to this cluster
     *
     * @return The membership scheme. Only "wka" & "multicast" are valid return values.
     * @throws ClusteringFault If the membershipScheme specified in the axis2.xml file is invalid
     */
    private String getMembershipScheme() throws ClusteringFault {
        Parameter membershipSchemeParam =
                getParameter(ClusteringConstants.Parameters.MEMBERSHIP_SCHEME);
        String mbrScheme = ClusteringConstants.MembershipScheme.MULTICAST_BASED;
        if (membershipSchemeParam != null) {
            mbrScheme = ((String) membershipSchemeParam.getValue()).trim();
        }
        if (!mbrScheme.equals(ClusteringConstants.MembershipScheme.MULTICAST_BASED) &&
            !mbrScheme.equals(ClusteringConstants.MembershipScheme.WKA_BASED)) {
            String msg = "Invalid membership scheme '" + mbrScheme + "'. Supported schemes are " +
                         ClusteringConstants.MembershipScheme.MULTICAST_BASED + " & " +
                         ClusteringConstants.MembershipScheme.WKA_BASED;
            log.error(msg);
            throw new ClusteringFault(msg);
        }
        return mbrScheme;
    }

    /**
     * Get the clustering domain to which this node belongs to
     *
     * @return The clustering domain to which this node belongs to
     */
    private byte[] getClusterDomain() {
        Parameter domainParam = getParameter(ClusteringConstants.Parameters.DOMAIN);
        byte[] domain;
        if (domainParam != null) {
            domain = ((String) domainParam.getValue()).getBytes();
        } else {
            domain = ClusteringConstants.DEFAULT_DOMAIN.getBytes();
        }
        return domain;
    }

    /**
     * Set the maximum number of retries, if message sending to a particular node fails
     */
    private void setMaximumRetries() {
        Parameter maxRetriesParam = getParameter(TribesConstants.MAX_RETRIES);
        int maxRetries = 10;
        if (maxRetriesParam != null) {
            maxRetries = Integer.parseInt((String) maxRetriesParam.getValue());
        }
        ReplicationTransmitter replicationTransmitter =
                (ReplicationTransmitter) channel.getChannelSender();
        MultiPointSender multiPointSender = replicationTransmitter.getTransport();
        multiPointSender.setMaxRetryAttempts(maxRetries);
    }

    /**
     * A RequestBlockingHandler, which is an implementation of
     * {@link org.apache.axis2.engine.Handler} is added to the InFlow & InFaultFlow. This handler
     * is used for rejecting Web service requests until this node has been initialized. This handler
     * can also be used for rejecting requests when this node is reinitializing or is in an
     * inconsistent state (which can happen when a configuration change is taking place).
     */
    private void addRequestBlockingHandlerToInFlows() {
        AxisConfiguration axisConfig = configurationContext.getAxisConfiguration();
        for (Object o : axisConfig.getInFlowPhases()) {
            Phase phase = (Phase) o;
            if (phase instanceof DispatchPhase) {
                RequestBlockingHandler requestBlockingHandler = new RequestBlockingHandler();
                if (!phase.getHandlers().contains(requestBlockingHandler)) {
                    PhaseRule rule = new PhaseRule("Dispatch");
                    rule.setAfter("SOAPMessageBodyBasedDispatcher");
                    rule.setBefore("InstanceDispatcher");
                    HandlerDescription handlerDesc = requestBlockingHandler.getHandlerDesc();
                    handlerDesc.setHandler(requestBlockingHandler);
                    handlerDesc.setName(ClusteringConstants.REQUEST_BLOCKING_HANDLER);
                    handlerDesc.setRules(rule);
                    phase.addHandler(requestBlockingHandler);

                    log.debug("Added " + ClusteringConstants.REQUEST_BLOCKING_HANDLER +
                              " between SOAPMessageBodyBasedDispatcher & InstanceDispatcher to InFlow");
                    break;
                }
            }
        }
        for (Object o : axisConfig.getInFaultFlowPhases()) {
            Phase phase = (Phase) o;
            if (phase instanceof DispatchPhase) {
                RequestBlockingHandler requestBlockingHandler = new RequestBlockingHandler();
                if (!phase.getHandlers().contains(requestBlockingHandler)) {
                    PhaseRule rule = new PhaseRule("Dispatch");
                    rule.setAfter("SOAPMessageBodyBasedDispatcher");
                    rule.setBefore("InstanceDispatcher");
                    HandlerDescription handlerDesc = requestBlockingHandler.getHandlerDesc();
                    handlerDesc.setHandler(requestBlockingHandler);
                    handlerDesc.setName(ClusteringConstants.REQUEST_BLOCKING_HANDLER);
                    handlerDesc.setRules(rule);
                    phase.addHandler(requestBlockingHandler);

                    log.debug("Added " + ClusteringConstants.REQUEST_BLOCKING_HANDLER +
                              " between SOAPMessageBodyBasedDispatcher & InstanceDispatcher to InFaultFlow");
                    break;
                }
            }
        }
    }

    private void configureMode(byte[] domain) {
        if (loadBalanceMode) {
            mode = new LoadBalancerMode(domain, lbEventHandlers, primaryMembershipManager);
        } else {
            mode = new ApplicationMode(domain, primaryMembershipManager);
        }
        mode.init(channel);
    }

    /**
     * Handle specific configurations related to different membership management schemes.
     *
     * @param localDomain        The clustering loadBalancerDomain to which this member belongs to
     * @param membershipManagers MembershipManagers for different domains
     * @throws ClusteringFault If the membership scheme is invalid, or if an error occurs
     *                         while configuring membership scheme
     */
    private void configureMembershipScheme(byte[] localDomain,
                                           List<MembershipManager> membershipManagers)
            throws ClusteringFault {
        MembershipListener membershipListener;
        Parameter parameter = getParameter(ClusteringConstants.Parameters.MEMBERSHIP_LISTENER);
        if (parameter != null) {
            OMElement paramEle = parameter.getParameterElement();
            String clazz =
                    paramEle.getFirstChildWithName(new QName("class")).getText().trim();
            try {
                membershipListener = (MembershipListener) Class.forName(clazz).newInstance();
            } catch (Exception e) {
                String msg = "Cannot instantiate MembershipListener " + clazz;
                log.error(msg, e);
                throw new ClusteringFault(msg, e);
            }
            OMElement propsEle = paramEle.getFirstChildWithName(new QName("properties"));
            if (propsEle != null) {
                for (Iterator iter = propsEle.getChildElements(); iter.hasNext();) {
                    OMElement propEle = (OMElement) iter.next();
                    OMAttribute nameAttrib = propEle.getAttribute(new QName("name"));
                    if (nameAttrib != null) {
                        String name = nameAttrib.getAttributeValue();
                        setInstanceProperty(name, propEle.getText().trim(), membershipListener);
                    }
                }
            }
        }

        String scheme = getMembershipScheme();
        log.info("Using " + scheme + " based membership management scheme");
        if (scheme.equals(ClusteringConstants.MembershipScheme.WKA_BASED)) {
            membershipScheme =
                    new WkaBasedMembershipScheme(channel, mode,
                                                 membershipManagers,
                                                 primaryMembershipManager,
                                                 parameters, localDomain, members,
                                                 getBooleanParam(ClusteringConstants.Parameters.ATMOST_ONCE_MSG_SEMANTICS),
                                                 getBooleanParam(ClusteringConstants.Parameters.PRESERVE_MSG_ORDER));
        } else if (scheme.equals(ClusteringConstants.MembershipScheme.MULTICAST_BASED)) {
            membershipScheme =
                    new MulticastBasedMembershipScheme(channel, mode, parameters,
                                                       localDomain,
                                                       getBooleanParam(ClusteringConstants.Parameters.ATMOST_ONCE_MSG_SEMANTICS),
                                                       getBooleanParam(ClusteringConstants.Parameters.PRESERVE_MSG_ORDER));
        } else {
            String msg = "Invalid membership scheme '" + scheme +
                         "'. Supported schemes are multicast & wka";
            log.error(msg);
            throw new ClusteringFault(msg);
        }
        membershipScheme.init();
    }

    private boolean getBooleanParam(String name) {
        boolean result = false;
        Parameter parameter = getParameter(name);
        if (parameter != null) {
            Object value = parameter.getValue();
            if (value != null) {
                result = Boolean.valueOf(((String) value).trim());
            }
        }
        return result;
    }

    /**
     * Find and invoke the setter method with the name of form setXXX passing in the value given
     * on the POJO object
     *
     * @param name name of the setter field
     * @param val  value to be set
     * @param obj  POJO instance
     * @throws ClusteringFault If an error occurs while setting the property
     */
    private void setInstanceProperty(String name, Object val, Object obj) throws ClusteringFault {

        String mName = "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        Method method;
        try {
            Method[] methods = obj.getClass().getMethods();
            boolean invoked = false;
            for (Method method1 : methods) {
                if (mName.equals(method1.getName())) {
                    Class[] params = method1.getParameterTypes();
                    if (params.length != 1) {
                        handleException("Did not find a setter method named : " + mName +
                                        "() that takes a single String, int, long, float, double " +
                                        "or boolean parameter");
                    } else if (val instanceof String) {
                        String value = (String) val;
                        if (params[0].equals(String.class)) {
                            method = obj.getClass().getMethod(mName, String.class);
                            method.invoke(obj, new String[]{value});
                        } else if (params[0].equals(int.class)) {
                            method = obj.getClass().getMethod(mName, int.class);
                            method.invoke(obj, new Integer[]{new Integer(value)});
                        } else if (params[0].equals(long.class)) {
                            method = obj.getClass().getMethod(mName, long.class);
                            method.invoke(obj, new Long[]{new Long(value)});
                        } else if (params[0].equals(float.class)) {
                            method = obj.getClass().getMethod(mName, float.class);
                            method.invoke(obj, new Float[]{new Float(value)});
                        } else if (params[0].equals(double.class)) {
                            method = obj.getClass().getMethod(mName, double.class);
                            method.invoke(obj, new Double[]{new Double(value)});
                        } else if (params[0].equals(boolean.class)) {
                            method = obj.getClass().getMethod(mName, boolean.class);
                            method.invoke(obj, new Boolean[]{Boolean.valueOf(value)});
                        } else {
                            handleException("Did not find a setter method named : " + mName +
                                            "() that takes a single String, int, long, float, double " +
                                            "or boolean parameter");
                        }
                    } else {
                        if (params[0].equals(OMElement.class)) {
                            method = obj.getClass().getMethod(mName, OMElement.class);
                            method.invoke(obj, new OMElement[]{(OMElement) val});
                        }
                    }
                    invoked = true;
                }
            }

            if (!invoked) {
                handleException("Did not find a setter method named : " + mName +
                                "() that takes a single String, int, long, float, double " +
                                "or boolean parameter");
            }

        } catch (Exception e) {
            handleException("Error invoking setter method named : " + mName +
                            "() that takes a single String, int, long, float, double " +
                            "or boolean parameter", e);
        }
    }

    private void handleException(String msg, Exception e) throws ClusteringFault {
        log.error(msg, e);
        throw new ClusteringFault(msg, e);
    }

    private void handleException(String msg) throws ClusteringFault {
        log.error(msg);
        throw new ClusteringFault(msg);
    }

    /**
     * Get some information from a neighbour. This information will be used by this node to
     * initialize itself
     * <p/>
     * rpcChannel is The utility for sending RPC style messages to the channel
     *
     * @param command The control command to send
     * @throws ClusteringFault If initialization code failed on this node
     */
    private void initializeSystem(ControlCommand command) throws ClusteringFault {
        // If there is at least one member in the cluster,
        //  get the current initialization info from a member
        int numberOfTries = 0; // Don't keep on trying indefinitely

        // Keep track of members to whom we already sent an initialization command
        // Do not send another request to these members
        List<String> sentMembersList = new ArrayList<String>();
        sentMembersList.add(TribesUtil.getLocalHost(channel));
        Member[] members = primaryMembershipManager.getMembers();
        if (members.length == 0) {
            return;
        }

        while (members.length > 0 && numberOfTries < 5) {
            Member member = (numberOfTries == 0) ?
                            primaryMembershipManager.getLongestLivingMember() : // First try to get from the longest member alive
                            primaryMembershipManager.getRandomMember(); // Else get from a random member
            String memberHost = TribesUtil.getName(member);
            log.info("Trying to send intialization request to " + memberHost);
            try {
                if (!sentMembersList.contains(memberHost)) {
                    Response[] responses;
//                    do {
                    responses = rpcInitChannel.send(new Member[]{member},
                                                    command,
                                                    RpcChannel.FIRST_REPLY,
                                                    Channel.SEND_OPTIONS_ASYNCHRONOUS,
                                                    10000);
                    if (responses.length == 0) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ignored) {
                        }
                    }
                    // TODO: If we do not get a response within some time, try to recover from this fault
//                    }
//                    while (responses.length == 0 || responses[0] == null || responses[0].getMessage() == null);    // TODO: #### We will need to check this
                    if (responses.length != 0 && responses[0] != null && responses[0].getMessage() != null) {
                        ((ControlCommand) responses[0].getMessage()).execute(configurationContext); // Do the initialization
                        break;
                    }
                }
            } catch (Exception e) {
                log.error("Cannot get initialization information from " +
                          memberHost + ". Will retry in 2 secs.", e);
                sentMembersList.add(memberHost);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                    log.debug("Interrupted", ignored);
                }
            }
            numberOfTries++;
            members = primaryMembershipManager.getMembers();
            if (numberOfTries >= members.length) {
                break;
            }
        }
    }

    public void setConfigurationManager(ConfigurationManager configurationManager) {
        this.configurationManager = (DefaultConfigurationManager) configurationManager;
        this.configurationManager.setSender(channelSender);
    }

    public void setContextManager(ContextManager contextManager) {
        this.contextManager = (DefaultContextManager) contextManager;
        this.contextManager.setSender(channelSender);
    }

    public void addParameter(Parameter param) throws AxisFault {
        parameters.put(param.getName(), param);
    }

    public void deserializeParameters(OMElement parameterElement) throws AxisFault {
        throw new UnsupportedOperationException();
    }

    public Parameter getParameter(String name) {
        return parameters.get(name);
    }

    public ArrayList getParameters() {
        ArrayList<Parameter> list = new ArrayList<Parameter>();
        for (String msg : parameters.keySet()) {
            list.add(parameters.get(msg));
        }
        return list;
    }

    public boolean isParameterLocked(String parameterName) {
        Parameter parameter = parameters.get(parameterName);
        return parameter != null && parameter.isLocked();
    }

    public void removeParameter(Parameter param) throws AxisFault {
        parameters.remove(param.getName());
    }

    /**
     * Shutdown the cluster. This member will leave the cluster when this method is called.
     *
     * @throws ClusteringFault If an error occurs while shutting down
     */
    public void shutdown() throws ClusteringFault {
        log.debug("Enter: TribesClusterManager::shutdown");
        if (channel != null) {
            try {
                channel.removeChannelListener(rpcInitChannel);
                channel.removeChannelListener(channelListener);
                channel.stop(Channel.DEFAULT);
            } catch (ChannelException e) {

                if (log.isDebugEnabled()) {
                    log.debug("Exit: TribesClusterManager::shutdown");
                }

                throw new ClusteringFault(e);
            }
        }
        log.debug("Exit: TribesClusterManager::shutdown");
    }

    public void setConfigurationContext(ConfigurationContext configurationContext) {
        this.configurationContext = configurationContext;
        if (rpcInitRequestHandler != null) {
            rpcInitRequestHandler.setConfigurationContext(configurationContext);
        }
        if (channelListener != null) {
            channelListener.setConfigurationContext(configurationContext);
        }
        if (configurationManager != null) {
            configurationManager.setConfigurationContext(configurationContext);
        }
        if (contextManager != null) {
            contextManager.setConfigurationContext(configurationContext);
        }
    }

    /**
     * Method to check whether all members in the cluster have to be kept in sync at all times.
     * Typically, this will require each member in the cluster to ACKnowledge receipt of a
     * particular message, which may have a significant performance hit.
     *
     * @return true - if all members in the cluster should be kept in sync at all times, false
     *         otherwise
     */
    public boolean synchronizeAllMembers() {
        Parameter syncAllParam = getParameter(ClusteringConstants.Parameters.SYNCHRONIZE_ALL_MEMBERS);
        return syncAllParam == null || Boolean.parseBoolean((String) syncAllParam.getValue());
    }
}
