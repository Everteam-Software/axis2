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

import org.apache.axis2.clustering.ClusteringConstants;
import org.apache.axis2.clustering.ClusteringFault;
import org.apache.axis2.clustering.configuration.ConfigurationClusteringCommand;
import org.apache.axis2.clustering.configuration.DefaultConfigurationManager;
import org.apache.axis2.clustering.context.ContextClusteringCommand;
import org.apache.axis2.clustering.context.DefaultContextManager;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.description.AxisServiceGroup;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.catalina.tribes.ByteMessage;
import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.RemoteProcessException;
import org.apache.catalina.tribes.group.RpcMessage;
import org.apache.catalina.tribes.io.XByteBuffer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ChannelListener implements org.apache.catalina.tribes.ChannelListener {
    private static final Log log = LogFactory.getLog(ChannelListener.class);

    private DefaultContextManager contextManager;
    private DefaultConfigurationManager configurationManager;
    private ControlCommandProcessor controlCommandProcessor;

    private ConfigurationContext configurationContext;

    public ChannelListener(ConfigurationContext configurationContext,
                           DefaultConfigurationManager configurationManager,
                           DefaultContextManager contextManager,
                           ControlCommandProcessor controlCommandProcessor) {
        this.configurationManager = configurationManager;
        this.contextManager = contextManager;
        this.controlCommandProcessor = controlCommandProcessor;
        this.configurationContext = configurationContext;
    }

    public void setContextManager(DefaultContextManager contextManager) {
        this.contextManager = contextManager;
    }

    public void setConfigurationManager(DefaultConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;
    }

    public void setConfigurationContext(ConfigurationContext configurationContext) {
        this.configurationContext = configurationContext;
    }

    /**
     * Invoked by the channel to determine if the listener will process this message or not.
     * @param msg Serializable
     * @param sender Member
     * @return boolean
     */
    public boolean accept(Serializable msg, Member sender) {
        return !(msg instanceof RpcMessage);  // RpcMessages  will not be handled by this listener
    }

    /**
     * Receive a message from the channel
     * @param msg Serializable
     * @param sender - the source of the message
     */
    public void messageReceived(Serializable msg, Member sender) {
        try {
            AxisConfiguration configuration = configurationContext.getAxisConfiguration();
            List classLoaders = new ArrayList();
            classLoaders.add(configuration.getSystemClassLoader());
            classLoaders.add(getClass().getClassLoader());
            for (Iterator iter = configuration.getServiceGroups(); iter.hasNext();) {
                AxisServiceGroup group = (AxisServiceGroup) iter.next();
                classLoaders.add(group.getServiceGroupClassLoader());
            }
            for (Iterator iter = configuration.getModules().values().iterator(); iter.hasNext();) {
                AxisModule module = (AxisModule) iter.next();
                classLoaders.add(module.getModuleClassLoader());
            }
            byte[] message = ((ByteMessage) msg).getMessage();
            msg = XByteBuffer.deserialize(message,
                                          0,
                                          message.length,
                                          (ClassLoader[]) classLoaders.toArray(new ClassLoader[classLoaders.size()]));
        } catch (Exception e) {
            String errMsg = "Cannot deserialize received message";
            log.error(errMsg, e);
            throw new RemoteProcessException(errMsg, e);
        }

        // If the system has not still been intialized, reject all incoming messages, except the
        // GetStateResponseCommand message
        if (configurationContext.
                getPropertyNonReplicable(ClusteringConstants.CLUSTER_INITIALIZED) == null) {
            log.warn("Received message " + msg +
                     " before cluster initialization has been completed from " +
                     TribesUtil.getHost(sender));
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("Received message " + msg + " from " + TribesUtil.getHost(sender));
        }

        try {
            processMessage(msg, sender);
        } catch (Exception e) {
            String errMsg = "Cannot process received message";
            log.error(errMsg, e);
            throw new RemoteProcessException(errMsg, e);
        }
    }

    private void processMessage(Serializable msg, Member sender) throws ClusteringFault {
        if (msg instanceof ContextClusteringCommand && contextManager != null) {
            ContextClusteringCommand ctxCmd = (ContextClusteringCommand) msg;
            ctxCmd.execute(configurationContext);
        } else if (msg instanceof ConfigurationClusteringCommand && configurationManager != null) {
            configurationManager.process((ConfigurationClusteringCommand) msg);
        } 
    }
}
