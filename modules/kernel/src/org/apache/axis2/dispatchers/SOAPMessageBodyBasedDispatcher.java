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


package org.apache.axis2.dispatchers;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axis2.AxisFault;
import org.apache.axis2.engine.AbstractDispatcher;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.util.LoggingControl;
import org.apache.axis2.util.Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;

/**
 * Dispatches based on the namespace URI of the first child of
 * the body.
 */
public class SOAPMessageBodyBasedDispatcher extends AbstractDispatcher {

    /**
     * Field NAME
     */
    public static final String NAME = "SOAPMessageBodyBasedDispatcher";
    private static final Log log = LogFactory.getLog(SOAPMessageBodyBasedDispatcher.class);

    public AxisOperation findOperation(AxisService service, MessageContext messageContext)
            throws AxisFault {

        OMElement bodyFirstChild = messageContext.getEnvelope().getBody().getFirstElement();

        AxisOperation axisOperation = null;
        if (bodyFirstChild != null){
           axisOperation = service.getOperationByMessageElementQName(bodyFirstChild.getQName());

           // this is required for services uses the RPC message receiver
           if (axisOperation == null){
               QName operationName = new QName(bodyFirstChild.getLocalName());
               axisOperation = service.getOperation(operationName);
           }

        }
        return axisOperation;
    }

    /*
     *  (non-Javadoc)
     * @see org.apache.axis2.engine.AbstractDispatcher#findService(org.apache.axis2.context.MessageContext)
     */
    public AxisService findService(MessageContext messageContext) throws AxisFault {
        String serviceName;

        OMElement bodyFirstChild = messageContext.getEnvelope().getBody().getFirstElement();

        if (bodyFirstChild != null) {
            OMNamespace ns = bodyFirstChild.getNamespace();

            if (ns != null) {
                String filePart = ns.getNamespaceURI();

                if (LoggingControl.debugLoggingAllowed && log.isDebugEnabled()) {
                    log.debug(messageContext.getLogIDString() +
                            " Checking for Service using SOAP message body's first child's namespace : "
                            + filePart);
                }
                ConfigurationContext configurationContext =
                        messageContext.getConfigurationContext();
                String[] values = Utils.parseRequestURLForServiceAndOperation(filePart,
                                                                              configurationContext.getServiceContextPath());

                if (values[0] != null) {
                    serviceName = values[0];

                    AxisConfiguration registry =
                            configurationContext.getAxisConfiguration();

                    return registry.getService(serviceName);
                }
            }
        }

        return null;
    }

    public void initDispatcher() {
        init(new HandlerDescription(NAME));
    }
}
