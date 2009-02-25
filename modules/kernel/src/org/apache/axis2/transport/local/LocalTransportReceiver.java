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


package org.apache.axis2.transport.local;

import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.util.Utils;
import org.apache.axis2.util.MessageContextBuilder;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.builder.BuilderUtil;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.AxisEngine;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class LocalTransportReceiver {
    public static ConfigurationContext CONFIG_CONTEXT;
    private ConfigurationContext confContext;
    private LocalTransportSender sender;

    public LocalTransportReceiver(ConfigurationContext configContext) {
        confContext = configContext;
    }

    public LocalTransportReceiver(LocalTransportSender sender) {
        this(CONFIG_CONTEXT);
        this.sender = sender;
    }

    public void processMessage(InputStream in, EndpointReference to, String action)
            throws AxisFault {
        MessageContext msgCtx = confContext.createMessageContext();
        TransportInDescription tIn = confContext.getAxisConfiguration().getTransportIn(
                Constants.TRANSPORT_LOCAL);
        TransportOutDescription tOut = confContext.getAxisConfiguration().getTransportOut(
                Constants.TRANSPORT_LOCAL);
        try {

            tOut.setSender(new LocalResponder(sender));

            msgCtx.setTransportIn(tIn);
            msgCtx.setTransportOut(tOut);
            msgCtx.setProperty(MessageContext.TRANSPORT_OUT, sender.getResponse());

            msgCtx.setTo(to);
            msgCtx.setWSAAction(action);
            msgCtx.setServerSide(true);

            InputStreamReader streamReader = new InputStreamReader(in);
            OMXMLParserWrapper builder;
            try {
                builder = BuilderUtil.getBuilder(streamReader);
            } catch (XMLStreamException e) {
                throw AxisFault.makeFault(e);
            }
            SOAPEnvelope envelope = (SOAPEnvelope) builder.getDocumentElement();

            msgCtx.setEnvelope(envelope);

            AxisEngine.receive(msgCtx);
        } catch (AxisFault e) {
            // write the fault back.
            try {
                MessageContext faultContext =
                        MessageContextBuilder.createFaultMessageContext(msgCtx, e);
                
                faultContext.setTransportOut(tOut);
                faultContext.setProperty(MessageContext.TRANSPORT_OUT, sender.getResponse());

                AxisEngine.sendFault(faultContext);
            } catch (AxisFault axisFault) {
                // can't handle this, so just throw it
                throw axisFault;
            }
        }
    }
}
