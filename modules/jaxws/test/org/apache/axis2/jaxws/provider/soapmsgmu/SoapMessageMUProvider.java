/*
 * Copyright 2004,2005 The Apache Software Foundation.
 * Copyright 2007 International Business Machines Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.axis2.jaxws.provider.soapmsgmu;

import javax.xml.ws.Provider;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceProvider;
import javax.xml.ws.BindingType;
import javax.xml.ws.soap.SOAPBinding;
import javax.xml.soap.SOAPMessage;

import org.apache.axis2.jaxws.provider.AttachmentUtil;

/**
 * This class provides the server side implementation for JAX-WS Provider<MESSAGE>
 * for SOAP11 Binding with Mode = MESSAGE.
 *
 * The receiving message and the sending back message
 * must have the headers defined in wsdl.
 */

@WebServiceProvider()
@ServiceMode(value = Service.Mode.MESSAGE)
@BindingType(SOAPBinding.SOAP11HTTP_BINDING)
public class SoapMessageMUProvider implements Provider<SOAPMessage> {
    /**
     * This service receives soap message and return it back to client as is
     * or add a soap mustUnderstand attribute header and then return it back to client
     *
     * @param SOAPMessage object sent by the client
     * @return the SOAPMessage
     */
    public SOAPMessage invoke(SOAPMessage request) {
        System.out.println("----------------------------------------------");
        System.out.println("SoapMessageMUProvider:Invoke: Request received");
        SOAPMessage response = null;

        try {
            String string = AttachmentUtil.toString(request);
            if (string != null) {
                System.out.println("invoke: ---Received message= " + string);
                if (string.contains(new StringBuffer(AttachmentUtil.UNDERSTOOD_MU_TEXT))) {
                    String responseStr =
                            AttachmentUtil.msgEnvMU_understood.replaceAll(AttachmentUtil.MUHEADER_CLIENT_UNDERSTOOD,
                                                                          AttachmentUtil.MUHEADER_SERVER_UNDERSTOOD);
                    response = AttachmentUtil.toSOAPMessage(responseStr);
                    System.out.println("invoke: ---Response message= "
                            + AttachmentUtil.toString(response));
                } else if (string.contains(new StringBuffer(AttachmentUtil.MU_TEXT))) {
                    String responseStr =
                            AttachmentUtil.msgEnvMU.replaceAll(AttachmentUtil.MUHEADER_CLIENT,
                                                               AttachmentUtil.MUHEADER_SERVER);
                    response = AttachmentUtil.toSOAPMessage(responseStr);
                    System.out.println("invoke: ---Response message= "
                            + AttachmentUtil.toString(response));
                } else {
                    response = request;
                }
            } else {
                String badResult = "***ERROR at Service Endpoint: Received message is NULL.";
                throw new NullPointerException(badResult);
            }
        } catch (Exception e) {
            System.out.println("SoapMessageMUProviderService: Failed with exception.");
            e.printStackTrace();
        }
        return response;
    }
}
