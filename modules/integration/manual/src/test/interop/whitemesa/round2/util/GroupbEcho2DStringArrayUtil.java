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

package test.interop.whitemesa.round2.util;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import test.interop.whitemesa.SunClientUtil;

public class GroupbEcho2DStringArrayUtil implements SunClientUtil {

    public SOAPEnvelope getEchoSoapEnvelope() {

        SOAPFactory omfactory = OMAbstractFactory.getSOAP11Factory();
        SOAPEnvelope reqEnv = omfactory.getDefaultEnvelope();

        reqEnv.declareNamespace("http://www.w3.org/2001/XMLSchema", "xsd");
        reqEnv.declareNamespace("http://soapinterop.org/", "tns");
        OMNamespace encNs =
                reqEnv.declareNamespace("http://schemas.xmlsoap.org/soap/encoding/", "SOAP-ENC");
        OMNamespace envNs =
                reqEnv.declareNamespace("http://schemas.xmlsoap.org/soap/envelope/", "soapenv");
        OMNamespace typeNs =
                reqEnv.declareNamespace("http://www.w3.org/2001/XMLSchema-instance", "xsi");

        OMElement operation =
                omfactory.createOMElement("echo2DStringArray", "http://soapinterop.org/", null);
        reqEnv.getBody().addChild(operation);
        operation.declareNamespace(envNs);
        operation.addAttribute("encodingStyle", "http://schemas.xmlsoap.org/soap/encoding/", envNs);

        OMElement part = omfactory.createOMElement("input2DStringArray", null);
        part.declareNamespace(typeNs);
        part.declareNamespace(encNs);
        part.addAttribute("type", "SOAP-ENC:Array", typeNs);
        part.addAttribute("arrayType", "xsd:string[2,2]", encNs);

        OMElement value0 = omfactory.createOMElement("varString", null);
        value0.declareNamespace(typeNs);
        value0.addAttribute("type", "xsd:string", typeNs);
        value0.addChild(omfactory.createOMText("strss fdfing1"));
        OMElement value1 = omfactory.createOMElement("varString", null);
        value1.declareNamespace(typeNs);
        value1.addAttribute("type", "xsd:string", typeNs);
        value1.addChild(omfactory.createOMText("sdfsdf25"));
        OMElement value2 = omfactory.createOMElement("varString", null);
        value2.declareNamespace(typeNs);
        value2.addAttribute("type", "xsd:string", typeNs);
        value2.addChild(omfactory.createOMText("25dsasd dfasdas23"));
        OMElement value3 = omfactory.createOMElement("varString", null);
        value3.declareNamespace(typeNs);
        value3.addAttribute("type", "xsd:string", typeNs);
        value3.addChild(omfactory.createOMText("25dsasd dfasdas23"));

        part.addChild(value0);
        part.addChild(value1);
        part.addChild(value2);
        part.addChild(value3);

        operation.addChild(part);

        return reqEnv;
    }

}
