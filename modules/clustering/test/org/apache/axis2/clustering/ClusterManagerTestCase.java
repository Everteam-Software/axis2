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

package org.apache.axis2.clustering;

import junit.framework.TestCase;
import org.apache.axis2.clustering.configuration.ConfigurationManagerListener;
import org.apache.axis2.clustering.configuration.DefaultConfigurationManagerListener;
import org.apache.axis2.clustering.configuration.TestConfigurationManagerListener;
import org.apache.axis2.clustering.context.DefaultContextManagerListener;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisServiceGroup;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.transport.http.server.HttpUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class ClusterManagerTestCase extends TestCase {

    protected ClusterManager clusterManager1 = null;
    protected ClusterManager clusterManager2 = null;
    protected AxisConfiguration axisConfiguration1 = null;
    protected AxisConfiguration axisConfiguration2 = null;
    protected ConfigurationContext configurationContext1 = null;
    protected ConfigurationContext configurationContext2 = null;
    protected ConfigurationManagerListener configManagerListener1;
    protected ConfigurationManagerListener configManagerListener2;
    protected AxisServiceGroup serviceGroup1 = null;
    protected AxisServiceGroup serviceGroup2 = null;
    protected AxisService service1 = null;
    protected AxisService service2 = null;
    protected String serviceName = "testService";

    protected abstract ClusterManager getClusterManager(ConfigurationContext configCtx);

    protected boolean skipChannelTests = false;
    protected TestConfigurationManagerListener configurationManagerListener;
    protected DefaultContextManagerListener contextManagerListener;

    private static final Log log = LogFactory.getLog(ClusterManagerTestCase.class);

    protected void setUp() throws Exception {

        Thread.sleep(3000);

        configurationContext1 = ConfigurationContextFactory.createDefaultConfigurationContext();
        configurationContext2 = ConfigurationContextFactory.createDefaultConfigurationContext();

        clusterManager1 = getClusterManager(configurationContext1);
        clusterManager2 = getClusterManager(configurationContext2);

        clusterManager1.getContextManager().setConfigurationContext(configurationContext1);
        clusterManager2.getContextManager().setConfigurationContext(configurationContext2);

        clusterManager1.getConfigurationManager().setConfigurationContext(configurationContext1);
        clusterManager2.getConfigurationManager().setConfigurationContext(configurationContext2);

        configManagerListener1 = new DefaultConfigurationManagerListener();
        clusterManager1.getConfigurationManager().setConfigurationManagerListener(configManagerListener1);
        configManagerListener2 = new DefaultConfigurationManagerListener();
        clusterManager2.getConfigurationManager().setConfigurationManagerListener(configManagerListener2);

        //giving both Nodes the same deployment configuration
        axisConfiguration1 = configurationContext1.getAxisConfiguration();
        serviceGroup1 = new AxisServiceGroup(axisConfiguration1);
        service1 = new AxisService(serviceName);
        serviceGroup1.addService(service1);
        axisConfiguration1.addServiceGroup(serviceGroup1);

        axisConfiguration2 = configurationContext2.getAxisConfiguration();
        serviceGroup2 = new AxisServiceGroup(axisConfiguration2);
        service2 = new AxisService(serviceName);
        serviceGroup2.addService(service2);
        axisConfiguration2.addServiceGroup(serviceGroup2);

        //Initiating ClusterManagers
        System.setProperty(ClusteringConstants.LOCAL_IP_ADDRESS, HttpUtils.getIpAddress());
        try {
            clusterManager1.init();
            System.out.println("ClusterManager-1 successfully initialized");
            System.out.println("*** PLEASE IGNORE THE java.net.ConnectException STACKTRACES. THIS IS EXPECTED ***");
            clusterManager2.init();
            System.out.println("ClusterManager-2 successfully initialized");
        } catch (ClusteringFault e) {
            String message =
                    "Could not initialize ClusterManagers. Please check the network connection";
            log.error(message, e);
            skipChannelTests = true;
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        clusterManager1.shutdown();
        clusterManager2.shutdown();
    }

}
