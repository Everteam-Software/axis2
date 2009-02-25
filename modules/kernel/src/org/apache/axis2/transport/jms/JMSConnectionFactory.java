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
package org.apache.axis2.transport.jms;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axis2.addressing.EndpointReference;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

/**
 * Encapsulate a JMS Connection factory definition within an Axis2.xml
 * <p/>
 * More than one JMS connection factory could be defined within an Axis2 XML
 * specifying the JMSListener as the transportReceiver.
 * <p/>
 * These connection factories are created at the initialization of the
 * transportReceiver, and any service interested in using any of these could
 * specify the name of the factory and the destination through Parameters named
 * JMSConstants.CONFAC_PARAM and JMSConstants.DEST_PARAM as shown below.
 * <p/>
 * <parameter name="transport.jms.ConnectionFactory" locked="true">myQueueConnectionFactory</parameter>
 * <parameter name="transport.jms.Destination" locked="true">TestQueue</parameter>
 * <p/>
 * If a connection factory is defined by a parameter named
 * JMSConstants.DEFAULT_CONFAC_NAME in the Axis2 XML, services which does not
 * explicitly specify a connection factory will be defaulted to it - if it is
 * defined in the Axis2 configuration.
 * <p/>
 * e.g.
 * <transportReceiver name="jms" class="org.apache.axis2.transport.jms.JMSListener">
 * <parameter name="myTopicConnectionFactory" locked="false">
 * <parameter name="java.naming.factory.initial" locked="false">org.apache.activemq.jndi.ActiveMQInitialContextFactory</parameter>
 * <parameter name="java.naming.provider.url" locked="false">tcp://localhost:61616</parameter>
 * <parameter name="transport.jms.ConnectionFactoryJNDIName" locked="false">TopicConnectionFactory</parameter>
 * <parameter name="transport.jms.Destination" locked="false">myTopicOne, myTopicTwo</parameter>
 * </parameter>
 * <parameter name="myQueueConnectionFactory" locked="false">
 * <parameter name="java.naming.factory.initial" locked="false">org.apache.activemq.jndi.ActiveMQInitialContextFactory</parameter>
 * <parameter name="java.naming.provider.url" locked="false">tcp://localhost:61616</parameter>
 * <parameter name="transport.jms.ConnectionFactoryJNDIName" locked="false">QueueConnectionFactory</parameter>
 * <parameter name="transport.jms.Destination" locked="false">myQueueOne, myQueueTwo</parameter>
 * </parameter>
 * <parameter name="default" locked="false">
 * <parameter name="java.naming.factory.initial" locked="false">org.apache.activemq.jndi.ActiveMQInitialContextFactory</parameter>
 * <parameter name="java.naming.provider.url" locked="false">tcp://localhost:61616</parameter>
 * <parameter name="transport.jms.ConnectionFactoryJNDIName" locked="false">ConnectionFactory</parameter>
 * <parameter name="transport.jms.Destination" locked="false">myDestinationOne, myDestinationTwo</parameter>
 * </parameter>
 * </transportReceiver>
 */
public class JMSConnectionFactory {

    private static final Log log = LogFactory.getLog(JMSConnectionFactory.class);

    /**
     * The name used for the connection factory definition within Axis2
     */
    private String name = null;
    /**
     * The JNDI name of the actual connection factory
     */
    private String jndiName = null;
    /**
     * The JNDI name of the actual connection factory username
     */
    private String jndiUser = null;
    /**
     * The JNDI name of the actual connection factory password
     */
    private String jndiPass = null;
    /**
     * Map of destination JNDI names to service names
     */
    private Map serviceJNDINameMapping = null;
    /**
     * Map of destinations to service names
     */
    private Map serviceDestinationMapping = null;
    /**
     * The JMS Sessions listening for messages
     */
    private Map jmsSessions = null;
    /**
     * Properties of the connection factory
     */
    private Hashtable properties = null;
    /**
     * The JNDI Context used
     */
    private Context context = null;
    /**
     * The actual ConnectionFactory instance held within
     */
    private ConnectionFactory conFactory = null;
    /**
     * The JMS Connection is opened.
     */
    private Connection connection = null;
    /**
     * The JMS Message receiver for this connection factory
     */
    private JMSMessageReceiver msgRcvr = null;
    /**
     * The actual password for the connection factory after retrieval from JNDI.
     * If this is not supplied, the OS username will be used by default
     */
    private String user = null;
    /**
     * The actual password for the connection factory after retrieval from JNDI.
     * If this is not supplied, the OS credentials will be used by default
     */
    private String pass = null;
    
    /**
     * Create a JMSConnectionFactory for the given Axis2 name and JNDI name
     *
     * @param name     the local Axis2 name of the connection factory
     * @param jndiName the JNDI name of the actual connection factory used
     */
    JMSConnectionFactory(String name, String jndiName) {
        this.name = name;
        this.jndiName = jndiName;
        serviceJNDINameMapping = new HashMap();
        serviceDestinationMapping = new HashMap();
        properties = new Hashtable();
        jmsSessions = new HashMap();
    }

    /**
     * Create a JMSConnectionFactory for the given Axis2 name
     *
     * @param name the local Axis2 name of the connection factory
     */
    JMSConnectionFactory(String name) {
        this(name, null);
    }

    /**
     * Connect to the actual JMS connection factory specified by the JNDI name
     *
     * @throws NamingException if the connection factory cannot be found
     */
    public void connect() throws NamingException {
        if (context == null) {
            createInitialContext();
        }
        conFactory = (ConnectionFactory) context.lookup(jndiName);

        if (jndiUser != null)
        	user = (String ) context.lookup(jndiUser);
        
        if (jndiPass != null)
        	pass = (String ) context.lookup(jndiPass);
        
        log.debug("Connected to the actual connection factory : " + jndiName);
    }

    /**
     * Creates the initial context using the set properties
     *
     * @throws NamingException
     */
    private void createInitialContext() throws NamingException {
        context = new InitialContext(properties);
    }

    /**
     * Set the JNDI connection factory name
     *
     * @param jndiName
     */
    public void setJndiName(String jndiName) {
        this.jndiName = jndiName;
    }

    /**
     * Get the JNDI name of the actual factory username
     *
     * @return the jndi name of the actual connection factory username
     */
	public void setJndiUser(String jndiUser) {
		this.jndiUser = jndiUser;
	}   
    
    /**
     * Get the JNDI name of the actual factory password
     *
     * @return the jndi name of the actual connection factory password
     */
	public void setJndiPass(String jndiPass) {
		this.jndiPass = jndiPass;
	}

    /**
     * Add a listen destination on this connection factory on behalf of the given service
     *
     * @param destinationJndi destination JNDI name
     * @param serviceName     the service to which it belongs
     */
    public void addDestination(String destinationJndi, String serviceName) {

        serviceJNDINameMapping.put(destinationJndi, serviceName);
        String destinationName = getDestinationName(destinationJndi);

        if (destinationName == null) {
            log.warn("JMS Destination with JNDI name : " + destinationJndi + " does not exist");

            Connection con = null;
            try {
            	if ((jndiUser == null) || (jndiPass == null)){
            		// Use the OS username and credentials
                    con = conFactory.createConnection();            		
            	} else{
            		// use an explicit username and password
                    con = conFactory.createConnection(user, pass);            		            		
            	}
                Session session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Queue queue = session.createQueue(destinationJndi);
                destinationName = queue.getQueueName();
                log.warn("JMS Destination with JNDI name : " + destinationJndi + " created");

            } catch (JMSException e) {
                log.error("Unable to create a Destination with JNDI name : " + destinationJndi, e);
                // mark service as faulty
                JMSUtils.markServiceAsFaulty(
                    (String) serviceJNDINameMapping.get(destinationJndi),
                    "Error looking up JMS destination : " + destinationJndi,
                    msgRcvr.getAxisConf().getAxisConfiguration());

            } finally {
                if (con != null) {
                    try {
                        con.close();
                    } catch (JMSException ignore) {}
                }
            }
        }
        
        serviceDestinationMapping.put(destinationName, serviceName);
        log.info("Mapping JNDI name : " + destinationJndi + " and JMS Destination name : " +
            destinationName + " against service : " + serviceName);
    }

    /**
     * Remove listen destination on this connection factory
     *
     * @param destinationJndi the JMS destination to be removed
     * @throws if an error occurs trying to stop listening for messages before removal
     */
    public void removeDestination(String destinationJndi) throws JMSException {
        // find and save provider specific Destination name before we delete
        String providerSpecificDestination = getDestinationName(destinationJndi);
        stoplistenOnDestination(destinationJndi);
        serviceJNDINameMapping.remove(destinationJndi);
        if (providerSpecificDestination != null) {
            serviceDestinationMapping.remove(providerSpecificDestination);
        }
    }

    /**
     * Add a property to the connection factory
     *
     * @param key
     * @param value
     */
    public void addProperty(String key, String value) {
        properties.put(key, value);
    }

    /**
     * Return the name of the connection factory
     *
     * @return the Axis2 name of this factory
     */
    public String getName() {
        return name;
    }

    /**
     * Get the JNDI name of the actual factory
     *
     * @return the jndi name of the actual connection factory
     */
    public String getJndiName() {
        return jndiName;
    }
    
    /**
     * Get the JNDI name of the actual factory username
     *
     * @return the jndi name of the actual connection factory username
     */
	public String getJndiUser() {
		return jndiUser;
	}
    
    /**
     * Get the JNDI name of the actual factory password
     *
     * @return the jndi name of the actual connection factory password
     */
    public String getJndiPass() {
		return jndiPass;
	}
  
    
    /**
     * This is the real password for the connection factory after the JNDI lookup
     *
     * @return the real password for the connection factory after the JNDI lookup
     */
	public String getPass() {
		return pass;
	}

    /**
     * This is the real username for the connection factory after the JNDI lookup
     *
     * @return the eal username for the connection factory after the JNDI lookup
     */
	public String getUser() {
		return user;
	}

	/**
     * Get the actual underlying connection factory
     *
     * @return actual connection factory
     */
    public ConnectionFactory getConFactory() {
        return conFactory;
    }

    /**
     * Get the list of destinations (JNDI) associated with this connection factory
     *
     * @return destinations to service maping
     */
    public Map getDestinations() {
        return serviceJNDINameMapping;
    }

    /**
     * Get the connection factory properties
     *
     * @return properties
     */
    public Hashtable getProperties() {
        return properties;
    }

    /**
     * Begin listening for messages on the list of destinations associated
     * with this connection factory. (Called during Axis2 initialization of
     * the Transport receivers)
     *
     * @param msgRcvr the message receiver which will process received messages
     * @throws JMSException on exceptions
     */
    public void listen(JMSMessageReceiver msgRcvr) throws JMSException {

        // save a reference to the message receiver
        this.msgRcvr = msgRcvr;

        log.debug("Connection factory : " + name + " initializing...");

        if (conFactory == null || context == null) {
            handleException(
                    "Connection factory must be 'connected' before listening");
        } else {
            try {
            	if ((jndiUser == null) || (jndiPass == null)){
            		// User the OS username and credentials
                    connection = conFactory.createConnection();            		
            	} else{
            		// use an explicit username and password
            		connection = conFactory.createConnection(user, pass);            		            		
            	}
            } catch (JMSException e) {
                handleException("Error creating a JMS connection using the " +
                        "factory : " + jndiName, e);
            }
        }

        Iterator iter = serviceJNDINameMapping.keySet().iterator();
        while (iter.hasNext()) {
            String destinationJndi = (String) iter.next();
            listenOnDestination(destinationJndi);
        }

        // start the connection
        connection.start();
        log.info("Connection factory : " + name + " initialized...");
    }

    /**
     * Listen on the given destination from this connection factory. Used to
     * start listening on a destination associated with a newly deployed service
     *
     * @param destinationJndi the JMS destination to listen on
     * @throws JMSException on exception
     */
    public void listenOnDestination(String destinationJndi) throws JMSException {
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination = null;
        try {
            Object o = context.lookup(destinationJndi);
            destination = (Destination) o;

        } catch (NameNotFoundException e) {
            log.warn("Cannot find destination : " + destinationJndi +
                    " Creating a Queue with this name");
            destination = session.createQueue(destinationJndi);

        } catch (NamingException e) {
            log.warn("Error looking up destination : " + destinationJndi, e);
            // mark service as faulty
            JMSUtils.markServiceAsFaulty(
                    (String) serviceJNDINameMapping.get(destinationJndi),
                    "Error looking up JMS destination : " + destinationJndi,
                    this.msgRcvr.getAxisConf().getAxisConfiguration());
        }

        MessageConsumer consumer = session.createConsumer(destination);
        consumer.setMessageListener(this.msgRcvr);
        jmsSessions.put(destinationJndi, session);
    }

    /**
     * Stop listening on the given destination - for undeployment of services
     *
     * @param destinationJndi the JNDI name of the JMS destination
     * @throws JMSException on exception
     */
    private void stoplistenOnDestination(String destinationJndi) throws JMSException {
        ((Session) jmsSessions.get(destinationJndi)).close();
    }

    /**
     * Return the service name using this destination
     *
     * @param destination the destination name
     * @return the service which uses the given destination, or null
     */
    public String getServiceNameForDestination(String destination) {

        return (String) serviceJNDINameMapping.get(destination);
    }

    /**
     * Close all connections, sessions etc.. and stop this connection factory
     */
    public void stop() {
        try {
            connection.close();
        } catch (JMSException e) {
            log.warn("Error shutting down connection factory : " + name, e);
        }
    }

    /**
     * Return the provider specific Destination name if any for the destination with the given
     * JNDI name
     * @param destinationJndi the JNDI name of the destination
     * @return the provider specific Destination name or null if cannot be found
     */
    public String getDestinationName(String destinationJndi) {
        try {
            Destination destination = (Destination) context.lookup(destinationJndi);
            if (destination != null && destination instanceof Queue) {
                return ((Queue) destination).getQueueName();
            } else if (destination != null && destination instanceof Topic) {
                return ((Topic) destination).getTopicName();
            }
        } catch (JMSException e) {
            log.warn("Error reading provider specific JMS destination name for destination " +
                "with JNDI name : " + destinationJndi, e);
        } catch (NamingException e) {
            log.warn("Error looking up destination with JNDI name : " + destinationJndi +
                " to map its corresponding provider specific Destination name");
        }
        return null;
    }

    /**
     * Return the EPR for the JMS Destination with the given JNDI name and using
     * this connection factory
     * @param destination the JNDI name of the JMS Destionation
     * @return the EPR
     */
    public EndpointReference getEPRForDestination(String destination) {

        StringBuffer sb = new StringBuffer();
        sb.append(JMSConstants.JMS_PREFIX).append(destination);
        sb.append("?").append(JMSConstants.CONFAC_JNDI_NAME_PARAM).
                append("=").append(getJndiName());
        Iterator props = getProperties().keySet().iterator();
        while (props.hasNext()) {
            String key = (String) props.next();
            String value = (String) getProperties().get(key);
            sb.append("&").append(key).append("=").append(value);
        }

        return new EndpointReference(sb.toString());
    }

    public String getServiceByDestination(String destinationName) {
        return (String) serviceDestinationMapping.get(destinationName);
    }

    private void handleException(String msg) throws AxisJMSException {
        log.error(msg);
        throw new AxisJMSException(msg);
    }

    private void handleException(String msg, Exception e) throws AxisJMSException {
        log.error(msg, e);
        throw new AxisJMSException(msg, e);
    }
}
