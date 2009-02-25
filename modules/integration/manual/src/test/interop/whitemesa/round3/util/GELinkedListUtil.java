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

package test.interop.whitemesa.round3.util;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import test.interop.whitemesa.SunClientUtil;


public class GELinkedListUtil implements SunClientUtil {

    public SOAPEnvelope getEchoSoapEnvelope() {

        SOAPFactory omfactory = OMAbstractFactory.getSOAP11Factory();
        SOAPEnvelope reqEnv = omfactory.getDefaultEnvelope();
        reqEnv.declareNamespace("http://schemas.xmlsoap.org/soap/envelope/", "soapenv");
        reqEnv.declareNamespace("http://www.w3.org/2001/XMLSchema", "xsd");
        reqEnv.declareNamespace("http://schemas.xmlsoap.org/soap/encoding/", "SOAP-ENC");
        reqEnv.declareNamespace("http://soapinterop.org/", "tns");
        reqEnv.declareNamespace("http://soapinterop.org/xsd", "s");
        reqEnv.declareNamespace("http://www.w3.org/2001/XMLSchema-instance", "xsi");


        OMElement operation = omfactory.createOMElement("echoLinkedList",
                                                        "http://soapinterop.org/WSDLInteropTestRpcEnc",
                                                        null);
        SOAPBody body = omfactory.createSOAPBody(reqEnv);
        body.addChild(operation);
        operation.addAttribute("soapenv:encodingStyle", "http://schemas.xmlsoap.org/soap/encoding/",
                               null);

        OMElement part = omfactory.createOMElement("param0", "", null);
        part.addAttribute("xsi:type", "s:List", null);


        OMElement value00 = omfactory.createOMElement("varInt", "", null);
        value00.addAttribute("xsi:type", "xsd:int", null);
        value00.addChild(omfactory.createOMText("255"));
        OMElement value01 = omfactory.createOMElement("varString", "", null);
        value01.addAttribute("xsi:type", "xsd:string", null);
        value01.addChild(omfactory.createOMText("Axis2"));
        OMElement value02 = omfactory.createOMElement("child", "", null);
        value02.addAttribute("href", "#ID1", null);


        OMElement part2 = omfactory.createOMElement("item0", null);
        part2.addAttribute("soapenv:encodingStyle", "http://schemas.xmlsoap.org/soap/encoding/",
                           null);

        part2.addAttribute("xsi:type", "s:List", null);
        part2.addAttribute("id", "ID1", null);


        OMElement value10 = omfactory.createOMElement("varInt", "", null);
        value10.addAttribute("xsi:type", "xsd:int", null);
        value10.addChild(omfactory.createOMText("21"));
        OMElement value11 = omfactory.createOMElement("varString", "", null);
        value11.addAttribute("xsi:type", "xsd:string", null);
        value11.addChild(omfactory.createOMText("LSF"));
        OMElement value12 = omfactory.createOMElement("child", "", null);
        value12.addAttribute("xsi:type", "xsd:anyType", null);
        value12.addAttribute(" xsi:nil", "1", null);
        part.addChild(value00);
        part.addChild(value01);
        part.addChild(value02);

        part2.addChild(value10);
        part2.addChild(value11);
        part2.addChild(value12);

        operation.addChild(part);
        body.addChild(part2);

        return reqEnv;

    }
}
