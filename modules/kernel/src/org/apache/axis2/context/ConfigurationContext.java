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


package org.apache.axis2.context;

import org.apache.axiom.om.util.UUIDGenerator;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.clustering.ClusterManager;
import org.apache.axis2.clustering.ClusteringConstants;
import org.apache.axis2.clustering.configuration.ConfigurationManager;
import org.apache.axis2.clustering.context.ContextManager;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisServiceGroup;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.DependencyManager;
import org.apache.axis2.engine.ListenerManager;
import org.apache.axis2.i18n.Messages;
import org.apache.axis2.util.JavaUtils;
import org.apache.axis2.util.threadpool.ThreadFactory;
import org.apache.axis2.util.threadpool.ThreadPool;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <p>Axis2 states are held in two information models, called description hierarchy
 * and context hierarchy. Description hierarchy hold deployment configuration
 * and it's values does not change unless deployment configuration change occurs
 * where Context hierarchy hold run time information. Both hierarchies consists
 * four levels, Global, Service Group, Operation and Message. Please look at
 * "Information Model" section  of "Axis2 Architecture Guide" for more information.</p>
 * <p/>
 * <p>Configuration Context hold Global level run-time information. This allows
 * same configurations to be used by two Axis2 instances and most Axis2 wide
 * configurations can changed by setting name value pairs of the configurationContext.
 * This hold all OperationContexts, ServiceGroups, Sessions, and ListenerManager.
 */
public class ConfigurationContext extends AbstractContext {

    /**
     * Map containing <code>MessageID</code> to
     * <code>OperationContext</code> mapping.
     */
    private final Map operationContextMap = new HashMap();
    private Hashtable serviceGroupContextMap = new Hashtable();
    private Hashtable applicationSessionServiceGroupContexts = new Hashtable();
    private AxisConfiguration axisConfiguration;
    private ThreadFactory threadPool;
    //To keep TransportManager instance
    private ListenerManager listenerManager;

    // current time out interval is 30 secs. Need to make this configurable
    private long serviceGroupContextTimoutInterval = 30 * 1000;

    //To specify url mapping for services
    private String contextRoot;
    private String servicePath;

    private String cachedServicePath = null;
    protected List contextListeners;

    public ConfigurationContext(AxisConfiguration axisConfiguration) {
        super(null);
        this.axisConfiguration = axisConfiguration;
        initConfigContextTimeout(axisConfiguration);
    }

    private void initConfigContextTimeout(AxisConfiguration axisConfiguration) {
        Parameter parameter = axisConfiguration
                .getParameter(Constants.Configuration.CONFIG_CONTEXT_TIMOUT_INTERVAL);
        if (parameter != null) {
            Object value = parameter.getValue();
            if (value != null && value instanceof String) {
                serviceGroupContextTimoutInterval = Integer.parseInt((String) value);
            }
        }
    }

    public void initCluster() throws AxisFault {
        ClusterManager clusterManager = axisConfiguration.getClusterManager();
        if (clusterManager != null) {
            ContextManager contextManager = clusterManager.getContextManager();
            if (contextManager != null) {
                contextManager.setConfigurationContext(this);
            }
            ConfigurationManager configManager = clusterManager.getConfigurationManager();
            if (configManager != null) {
                configManager.setConfigurationContext(this);
            }
            if (shouldClusterBeInitiated(clusterManager)) {
                clusterManager.setConfigurationContext(this);
                clusterManager.init();
            }
        }
    }

    private static boolean shouldClusterBeInitiated(ClusterManager clusterManager) {
        Parameter param = clusterManager.getParameter(ClusteringConstants.AVOID_INITIATION_KEY);
        return !(param != null && JavaUtils.isTrueExplicitly(param.getValue()));
    }

    /**
     * Inform any listeners of a new context being created
     *
     * @param context the just-created subcontext
     */
    void contextCreated(AbstractContext context) {
        if (contextListeners == null) {
            return;
        }
        for (Iterator iter = contextListeners.iterator(); iter.hasNext();) {
            ContextListener listener = (ContextListener) iter.next();
            listener.contextCreated(context);
        }
    }

    /**
     * Inform any listeners of a context being removed
     *
     * @param context the just-created subcontext
     */
    void contextRemoved(AbstractContext context) {
        if (contextListeners == null) {
            return;
        }
        for (Iterator iter = contextListeners.iterator(); iter.hasNext();) {
            ContextListener listener = (ContextListener) iter.next();
            listener.contextRemoved(context);
        }
    }

    /**
     * Register a {@link ContextListener} to be notified of all sub-context events.
     *
     * @param contextListener A ContextListener
     * @see #removeContextListener
     */
    public void addContextListener(ContextListener contextListener) {
        if (contextListeners == null) {
            contextListeners = new ArrayList();
        }
        contextListeners.add(contextListener);
    }

    /**
     * Remove an already registered {@link ContextListener}
     *
     * @param contextListener A ContextListener
     * @see #addContextListener
     */
    public void removeContextListener(ContextListener contextListener) {
        if (contextListeners != null) {
            contextListeners.remove(contextListener);
        }
    }

    protected void finalize() throws Throwable {
        super.finalize();
    }

    /**
     * Searches for a ServiceGroupContext in the map with given id as the key.
     * <pre>
     * If(key != null && found)
     * check for a service context for the intended service.
     * if (!found)
     * create one and hook up to ServiceGroupContext
     * else
     * create new ServiceGroupContext with the given key or if key is null with a new key
     * create a new service context for the service
     * </pre>
     *
     * @param messageContext : MessageContext
     * @throws AxisFault : If something goes wrong
     */
    public void fillServiceContextAndServiceGroupContext(MessageContext messageContext)
            throws AxisFault {
        // by this time service group context id must have a value. Either from transport or from addressing
        ServiceGroupContext serviceGroupContext;
        ServiceContext serviceContext = messageContext.getServiceContext();

        AxisService axisService = messageContext.getAxisService();

        if (serviceContext == null) {
            String scope = axisService.getScope();
            if (Constants.SCOPE_APPLICATION.equals(scope)) {
                String serviceGroupName = axisService.getAxisServiceGroup().getServiceGroupName();
                serviceGroupContext =
                        (ServiceGroupContext) applicationSessionServiceGroupContexts.get(
                                serviceGroupName);
                if (serviceGroupContext == null) {
                    AxisServiceGroup axisServiceGroup = messageContext.getAxisServiceGroup();
                    if (axisServiceGroup == null) {
                        axisServiceGroup = axisService.getAxisServiceGroup();
                        messageContext.setAxisServiceGroup(axisServiceGroup);
                    }
                    ConfigurationContext cfgCtx = messageContext.getConfigurationContext();
                    serviceGroupContext = cfgCtx.createServiceGroupContext(axisServiceGroup);
                    applicationSessionServiceGroupContexts
                            .put(serviceGroupName, serviceGroupContext);
                }
                messageContext.setServiceGroupContext(serviceGroupContext);
                messageContext.setServiceContext(serviceGroupContext.getServiceContext(axisService));
            } else if (Constants.SCOPE_SOAP_SESSION.equals(scope)) {
                //cleaning the session
                cleanupServiceGroupContexts();
                String serviceGroupContextId = messageContext.getServiceGroupContextId();
                if (serviceGroupContextId != null) {
                    serviceGroupContext =
                            getServiceGroupContextFromSoapSessionTable(serviceGroupContextId,
                                                                       messageContext);
                    if (serviceGroupContext == null) {

                        // TODO: Adding this code so that requests to services deployed in soapsession scope will work
                        // TODO: However, soapsession functionality is still broken
                        serviceGroupContext =
                                new ServiceGroupContext(this,
                                                        axisService.getAxisServiceGroup());
                        serviceGroupContext.setId(serviceGroupContextId);
                        addServiceGroupContextIntoSoapSessionTable(serviceGroupContext);
//                        throw new AxisFault("Unable to find corresponding context" +
//                                            " for the serviceGroupId: " + serviceGroupContextId);
                    }
                } else {
                    AxisServiceGroup axisServiceGroup = axisService.getAxisServiceGroup();
                    serviceGroupContext = createServiceGroupContext(axisServiceGroup);
                    serviceContext = serviceGroupContext.getServiceContext(axisService);
                    // set the serviceGroupContextID
                    serviceGroupContextId = UUIDGenerator.getUUID();
                    serviceGroupContext.setId(serviceGroupContextId);
                    messageContext.setServiceGroupContextId(serviceGroupContextId);
                    addServiceGroupContextIntoSoapSessionTable(serviceGroupContext);
                }
                messageContext.setServiceGroupContext(serviceGroupContext);
                messageContext.setServiceContext(serviceGroupContext.getServiceContext(axisService));
            } else if (Constants.SCOPE_REQUEST.equals(scope)) {
                AxisServiceGroup axisServiceGroup = axisService.getAxisServiceGroup();
                serviceGroupContext = createServiceGroupContext(axisServiceGroup);
                messageContext.setServiceGroupContext(serviceGroupContext);
                serviceContext = serviceGroupContext.getServiceContext(axisService);
                messageContext.setServiceContext(serviceContext);
            }
        }
        if (messageContext.getOperationContext() != null) {
            messageContext.getOperationContext().setParent(serviceContext);
        }
    }

    /**
     * Registers a OperationContext with a given message ID.
     * If the given message id already has a registered operation context,
     * no change is made and the methid resturns false.
     *
     * @param messageID  the message-id to register
     * @param mepContext the OperationContext for the specified message-id
     * @return true if we added a new context, false if the messageID was already there and we did
     *         nothing
     */
    public boolean registerOperationContext(String messageID,
                                            OperationContext mepContext) {
        mepContext.setKey(messageID);  // TODO: Doing this here seems dangerous....
        synchronized (operationContextMap) {
            if (!operationContextMap.containsKey(messageID)) {
                this.operationContextMap.put(messageID, mepContext);
                return true;
            }
        }
        return false;
    }

    /**
     * Unregisters the operation context associated with the given messageID
     *
     * @param key
     */
    public void unregisterOperationContext(String key) {
        synchronized (operationContextMap) {
            OperationContext opCtx = (OperationContext) operationContextMap.get(key);
            operationContextMap.remove(key);
            contextRemoved(opCtx);
        }
    }

    public void addServiceGroupContextIntoSoapSessionTable(
            ServiceGroupContext serviceGroupContext) {
        String id = serviceGroupContext.getId();
        serviceGroupContextMap.put(id, serviceGroupContext);
        serviceGroupContext.touch();
        serviceGroupContext.setParent(this);
        // this is the best time to clean up the SGCtxts since are not being used anymore
        cleanupServiceGroupContexts();
    }

    public void addServiceGroupContextIntoApplicationScopeTable
            (ServiceGroupContext serviceGroupContext) {
        if (applicationSessionServiceGroupContexts == null) {
            applicationSessionServiceGroupContexts = new Hashtable();
        }
        applicationSessionServiceGroupContexts.put(
                serviceGroupContext.getDescription().getServiceGroupName(), serviceGroupContext);
    }

    /**
     * Deploy a service to the embedded AxisConfiguration, and initialize it.
     *
     * @param service service to deploy
     * @throws AxisFault if there's a problem
     */
    public void deployService(AxisService service) throws AxisFault {
        axisConfiguration.addService(service);
        if (Constants.SCOPE_APPLICATION.equals(service.getScope())) {
            ServiceGroupContext sgc = createServiceGroupContext(service.getAxisServiceGroup());
            DependencyManager.initService(sgc);
        }
    }

    public AxisConfiguration getAxisConfiguration() {
        return axisConfiguration;
    }

    /**
     * Gets a OperationContext given a Message ID.
     *
     * @return Returns OperationContext <code>OperationContext<code>
     * @param id
     */
    public OperationContext getOperationContext(String id) {
        OperationContext opCtx;
        synchronized (operationContextMap) {
            if (operationContextMap == null) {
                return null;
            }
            opCtx = (OperationContext) this.operationContextMap.get(id);
        }

        return opCtx;
    }

    public OperationContext findOperationContext(String operationName, String serviceName,
                                                 String serviceGroupName) {
        if (operationName == null) {
            return null;
        }

        if (serviceName == null) {
            return null;
        }

        // group name is not necessarily a prereq
        // but if the group name is non-null, then it has to match

        synchronized (operationContextMap) {
            Iterator it = operationContextMap.keySet().iterator();

            while (it.hasNext()) {
                Object key = it.next();
                OperationContext value = (OperationContext) operationContextMap.get(key);

                String valueOperationName;
                String valueServiceName;
                String valueServiceGroupName;

                if (value != null) {
                    valueOperationName = value.getOperationName();
                    valueServiceName = value.getServiceName();
                    valueServiceGroupName = value.getServiceGroupName();

                    if ((valueOperationName != null) && (valueOperationName.equals(operationName))) {
                        if ((valueServiceName != null) && (valueServiceName.equals(serviceName))) {
                            if ((valueServiceGroupName != null) && (serviceGroupName != null)
                                && (valueServiceGroupName.equals(serviceGroupName))) {
                                // match
                                return value;
                            }

                            // or, both need to be null
                            if ((valueServiceGroupName == null) && (serviceGroupName == null)) {
                                // match
                                return value;
                            }
                        }
                    }
                }
            }
        }

        // if we got here, we did not find an operation context
        // that fits the criteria
        return null;
    }

    /**
     * Create a MessageContext, and notify any registered ContextListener.
     *
     * @return a new MessageContext
     */
    public MessageContext createMessageContext() {
        MessageContext msgCtx = new MessageContext(this);
        contextCreated(msgCtx);
        return msgCtx;
    }

    /**
     * Create a ServiceGroupContext for the specified service group, and notify any
     * registered ContextListener.
     *
     * @param serviceGroup an AxisServiceGroup
     * @return a new ServiceGroupContext
     */
    public ServiceGroupContext createServiceGroupContext(AxisServiceGroup serviceGroup) {
        ServiceGroupContext sgCtx = new ServiceGroupContext(this, serviceGroup);
        contextCreated(sgCtx);
        return sgCtx;
    }

    /**
     * Allows users to resolve the path relative to the root diretory.
     *
     * @param path
     * @return
     */
    public File getRealPath(String path) {
        URL repository = axisConfiguration.getRepository();
        if (repository != null) {
            File repo = new File(repository.getFile());
            return new File(repo, path);
        }
        return null;
    }

    public ServiceGroupContext getServiceGroupContextFromSoapSessionTable(
            String serviceGroupContextId,
            MessageContext msgContext) throws AxisFault {
        ServiceGroupContext serviceGroupContext =
                (ServiceGroupContext) serviceGroupContextMap.get(serviceGroupContextId);

        if (serviceGroupContext != null) {
            serviceGroupContext.touch();
            return serviceGroupContext;
        } else {
            throw new AxisFault("Unable to find corresponding context" +
                    " for the serviceGroupId: " + serviceGroupContextId);
        }
    }


    /**
     * Returns a ServiceGroupContext object associated
     * with the specified ID from the internal table.
     *
     * @param serviceGroupCtxId The ID string associated with the ServiceGroupContext object
     * @return The ServiceGroupContext object, or null if not found
     */
    public ServiceGroupContext getServiceGroupContext(String serviceGroupCtxId) {

        if (serviceGroupCtxId == null) {
            // Hashtables require non-null key-value pairs
            return null;
        }

        ServiceGroupContext serviceGroupContext = null;

        if (serviceGroupContextMap != null) {
            serviceGroupContext = (ServiceGroupContext) serviceGroupContextMap.get(serviceGroupCtxId);
            if (serviceGroupContext != null) {
                serviceGroupContext.touch();
            } else {
                serviceGroupContext =
                        (ServiceGroupContext) applicationSessionServiceGroupContexts.get(serviceGroupCtxId);
                if (serviceGroupContext != null) {
                    serviceGroupContext.touch();
                }
            }
        }


        return serviceGroupContext;
    }

    /**
     * Gets all service groups in the system.
     *
     * @return Returns hashmap of ServiceGroupContexts.
     */
    public String[] getServiceGroupContextIDs() {
        String[] ids = new String[serviceGroupContextMap.size() +
                                  applicationSessionServiceGroupContexts.size()];
        int index = 0;
        for (Iterator iter = serviceGroupContextMap.keySet().iterator(); iter.hasNext();) {
            ids[index] = (String) iter.next();
            index++;
        }
        for (Iterator iter = applicationSessionServiceGroupContexts.keySet().iterator();
             iter.hasNext();) {
            ids[index] = (String) iter.next();
            index++;
        }
        return ids;
    }

    /**
     * @return The ServiceGroupContexts
     * @deprecated Use {@link #getServiceGroupContextIDs} & {@link #getServiceGroupContext(String)}
     */
    public Hashtable getServiceGroupContexts() {
        return serviceGroupContextMap;
    }

    /**
     * Returns the thread factory.
     *
     * @return Returns configuration specific thread pool
     */
    public ThreadFactory getThreadPool() {
        if (threadPool == null) {
            threadPool = new ThreadPool();
        }

        return threadPool;
    }

    /**
     * @param configuration
     */
    public void setAxisConfiguration(AxisConfiguration configuration) {
        axisConfiguration = configuration;
    }

    /**
     * Sets the thread factory.
     *
     * @param pool The thread pool
     * @throws AxisFault If a thread pool has already been set
     */
    public void setThreadPool(ThreadFactory pool) throws AxisFault {
        if (threadPool == null) {
            threadPool = pool;
        } else {
            throw new AxisFault(Messages.getMessage("threadpoolset"));
        }
    }

    /**
     * Remove a ServiceGroupContext
     *
     * @param serviceGroupContextId The ID of the ServiceGroupContext
     */
    public void removeServiceGroupContext(String serviceGroupContextId) {
        if (serviceGroupContextMap == null) {
            return;
        }
        ServiceGroupContext serviceGroupContext =
                (ServiceGroupContext) serviceGroupContextMap.get(serviceGroupContextId);
        serviceGroupContextMap.remove(serviceGroupContextId);
        cleanupServiceContexts(serviceGroupContext);
    }

    private void cleanupServiceGroupContexts() {
        if (serviceGroupContextMap == null) {
            return;
        }
        long currentTime = new Date().getTime();
        for (Iterator sgCtxtMapKeyIter = serviceGroupContextMap.keySet().iterator();
             sgCtxtMapKeyIter.hasNext();) {
            String sgCtxtId = (String) sgCtxtMapKeyIter.next();
            ServiceGroupContext serviceGroupContext =
                    (ServiceGroupContext) serviceGroupContextMap.get(sgCtxtId);
            if ((currentTime - serviceGroupContext.getLastTouchedTime()) >
                getServiceGroupContextTimoutInterval()) {
                sgCtxtMapKeyIter.remove();
                cleanupServiceContexts(serviceGroupContext);
                contextRemoved(serviceGroupContext);
            }
        }
    }

    public ListenerManager getListenerManager() {
        return listenerManager;
    }

    public void setTransportManager(ListenerManager listenerManager) {
        this.listenerManager = listenerManager;
    }

    private void cleanupServiceContexts(ServiceGroupContext serviceGroupContext) {
        if (serviceGroupContext == null) {
            return;
        }
        Iterator serviceContextIter = serviceGroupContext.getServiceContexts();
        if (serviceContextIter == null) {
            return;
        }
        while (serviceContextIter.hasNext()) {
            ServiceContext serviceContext = (ServiceContext) serviceContextIter.next();
            DependencyManager.destroyServiceObject(serviceContext);
        }
    }

    public void cleanupContexts() {
        if ((applicationSessionServiceGroupContexts != null) &&
            (applicationSessionServiceGroupContexts.size() > 0)) {
            for (Iterator applicationScopeSgs =
                    applicationSessionServiceGroupContexts.values().iterator();
                 applicationScopeSgs.hasNext();) {
                ServiceGroupContext serviceGroupContext =
                        (ServiceGroupContext) applicationScopeSgs.next();
                cleanupServiceContexts(serviceGroupContext);
            }
            applicationSessionServiceGroupContexts.clear();
        }
        if ((serviceGroupContextMap != null) && (serviceGroupContextMap.size() > 0)) {
            for (Iterator soapSessionSgs = serviceGroupContextMap.values().iterator();
                 soapSessionSgs.hasNext();) {
                ServiceGroupContext serviceGroupContext =
                        (ServiceGroupContext) soapSessionSgs.next();
                cleanupServiceContexts(serviceGroupContext);
            }
            serviceGroupContextMap.clear();
        }
    }

    public void terminate() throws AxisFault {
        if (listenerManager != null) {
            listenerManager.stop();
        }
        axisConfiguration.cleanup();
        cleanupTemp();
    }

    /**
     * This include all the major changes we have done from 1.2
     * release to 1.3 release. This will include API changes , class
     * deprecating etc etc.
     */
    private void cleanupTemp() {
        File tempFile = (File) axisConfiguration.getParameterValue(
                Constants.Configuration.ARTIFACTS_TEMP_DIR);
        if (tempFile == null) {
            tempFile = new File(System.getProperty("java.io.tmpdir"), "_axis2");
        }
        deleteTempFiles(tempFile);
    }

    private void deleteTempFiles(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                deleteTempFiles(new File(dir, children[i]));
            }
        }
        dir.delete();
    }

    public String getServiceContextPath() {
        if (cachedServicePath == null) {
            cachedServicePath = internalGetServiceContextPath();
        }
        return cachedServicePath;
    }

    private String internalGetServiceContextPath() {
        String ctxRoot = getContextRoot();
        String path = "/";
        if (ctxRoot != null) {
            if (!ctxRoot.equals("/")) {
                path = ctxRoot + "/";
            }
            if (servicePath == null || servicePath.trim().length() == 0) {
                throw new IllegalArgumentException("service path cannot be null or empty");
            } else {
                path += servicePath.trim();
            }
        }
        return path;
    }

    public String getServicePath() {
        if (servicePath == null || servicePath.trim().length() == 0) {
            throw new IllegalArgumentException("service path cannot be null or empty");
        }
        return servicePath.trim();
    }

    public void setServicePath(String servicePath) {
        this.servicePath = servicePath;
    }

    public String getContextRoot() {
        return contextRoot;
    }

    public void setContextRoot(String contextRoot) {
        if (contextRoot != null) {
            this.contextRoot = contextRoot.trim();  // Trim before storing away for good hygiene
            cachedServicePath = internalGetServiceContextPath();
        }
    }

    /**
     * This will be used to fetch the serviceGroupContextTimoutInterval from any place available.
     *
     * @return long
     */
    public long getServiceGroupContextTimoutInterval() {
        Integer serviceGroupContextTimoutIntervalParam =
                (Integer) getProperty(Constants.Configuration.CONFIG_CONTEXT_TIMOUT_INTERVAL);
        if (serviceGroupContextTimoutIntervalParam != null) {
            serviceGroupContextTimoutInterval = serviceGroupContextTimoutIntervalParam.intValue();
        }
        return serviceGroupContextTimoutInterval;
    }

    public void removeServiceGroupContext(AxisServiceGroup serviceGroup) {
        if (serviceGroup != null) {
            Object obj = applicationSessionServiceGroupContexts.get(
                    serviceGroup.getServiceGroupName());
            if (obj == null) {
                ArrayList toBeRemovedList = new ArrayList();
                Iterator serviceGroupContexts = serviceGroupContextMap.values().iterator();
                while (serviceGroupContexts.hasNext()) {
                    ServiceGroupContext serviceGroupContext =
                            (ServiceGroupContext) serviceGroupContexts.next();
                    if (serviceGroupContext.getDescription().equals(serviceGroup)) {
                        toBeRemovedList.add(serviceGroupContext.getId());
                    }
                }
                for (int i = 0; i < toBeRemovedList.size(); i++) {
                    String s = (String) toBeRemovedList.get(i);
                    serviceGroupContextMap.remove(s);
                }
            }
        }
    }

    public ConfigurationContext getRootContext() {
        return this;
    }
}
