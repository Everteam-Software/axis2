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
package org.apache.axis2.saaj;

import junit.framework.TestCase;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.soap.Text;
import javax.xml.transform.dom.DOMSource;
import java.io.File;
import java.util.Iterator;

/**
 * 
 */
public class SOAPPartTest extends TestCase {

    public void testAddSource() {
        DOMSource domSource;
        try {
            /*
            FileReader testFile = new FileReader(new File(System.getProperty("basedir",".")+"/test-resources" + File.separator + "soap-part.xml"));
            StAXOMBuilder stAXOMBuilder =
                    OMXMLBuilderFactory.createStAXOMBuilder(
                            OMAbstractFactory.getSOAP11Factory(),
                            XMLInputFactory.newInstance().createXMLStreamReader(
                                    testFile));
            */

            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(new File(System.getProperty("basedir", ".") +
                    "/test-resources" + File.separator + "soap-part.xml"));
            domSource = new DOMSource(document);

            SOAPMessage message = MessageFactory.newInstance().createMessage();

            // Get the SOAP part and set its content to domSource
            SOAPPart soapPart = message.getSOAPPart();
            soapPart.setContent(domSource);
            message.saveChanges();

            SOAPHeader header = message.getSOAPHeader();
            if (header != null) {
                Iterator iter1 = header.getChildElements();
                getContents(iter1, "");
            }

            SOAPBody body = message.getSOAPBody();
            Iterator iter2 = body.getChildElements();
            getContents(iter2, "");

        } catch (Exception e) {
            fail("Unexpected Exception : " + e);
        }
    }

    public void getContents(Iterator iterator, String indent) {
        while (iterator.hasNext()) {
            Node node = (Node)iterator.next();
            SOAPElement element = null;
            Text text = null;

            if (node instanceof SOAPElement) {
                element = (SOAPElement)node;

                Name name = element.getElementName();

                Iterator attrs = element.getAllAttributes();

                while (attrs.hasNext()) {
                    Name attrName = (Name)attrs.next();
                    assertNotNull(attrName);
                }

                Iterator iter2 = element.getChildElements();
                getContents(iter2, indent + " ");
            } else {
                text = (Text)node;
                String content = text.getValue();
                assertNotNull(content);
            }
        }
    }


    public void testAddSource2() throws Exception {
        javax.xml.soap.SOAPMessage soapMessage =
                javax.xml.soap.MessageFactory.newInstance().createMessage();
        javax.xml.soap.SOAPEnvelope soapEnv =
                soapMessage.getSOAPPart().getEnvelope();
        javax.xml.soap.SOAPHeader header = soapEnv.getHeader();
        javax.xml.soap.SOAPBody body = soapEnv.getBody();

        assertTrue(header.addChildElement("ebxmlms1", "ch2",
                                          "http://test.apache.org") instanceof SOAPHeaderElement);
        assertTrue(header.addHeaderElement(
                soapEnv.createName("ebxmlms2", "ch3", "http://test2.apache.org")) != null);
        assertTrue(header.addHeaderElement(
                new PrefixedQName("http://test3.apache.org", "ebxmlms3", "ch5")) != null);

        body.addChildElement("bodyEle1", "ele1", "http://ws.apache.org");
        soapMessage.saveChanges();

        javax.xml.soap.SOAPMessage soapMessage2 =
                javax.xml.soap.MessageFactory.newInstance().createMessage();
        SOAPPart soapPart = soapMessage2.getSOAPPart();
        soapPart.setContent(soapMessage.getSOAPPart().getContent());
        soapMessage2.saveChanges();
        assertNotNull(soapMessage2);
    }

    public void testAddSource3() throws Exception {
        javax.xml.soap.SOAPMessage soapMessage =
                javax.xml.soap.MessageFactory.newInstance().createMessage();
        javax.xml.soap.SOAPEnvelope soapEnv =
                soapMessage.getSOAPPart().getEnvelope();
        javax.xml.soap.SOAPHeader header = soapEnv.getHeader();
        javax.xml.soap.SOAPBody body = soapEnv.getBody();

        assertTrue(header.addChildElement("ebxmlms1", "ch2",
                                          "http://test.apache.org") instanceof SOAPHeaderElement);
        assertTrue(header.addHeaderElement(
                soapEnv.createName("ebxmlms2", "ch3", "http://test2.apache.org")) != null);
        assertTrue(header.addHeaderElement(
                new PrefixedQName("http://test3.apache.org", "ebxmlms3", "ch5")) != null);

        body.addChildElement("bodyEle1", "ele1", "http://ws.apache.org");
        soapMessage.saveChanges();

        javax.xml.soap.SOAPMessage soapMessage2 =
                javax.xml.soap.MessageFactory.newInstance().createMessage();
        SOAPPart soapPart = soapMessage2.getSOAPPart();
        soapPart.setContent(soapMessage.getSOAPPart().getContent());
        soapMessage2.saveChanges();
        assertNotNull(soapMessage2);
    }


    public void _testInputEncoding() {
        try {
            DOMSource domSource;
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(new File(System.getProperty("basedir", ".") +
                    "/test-resources" + File.separator + "soap-part.xml"));
            domSource = new DOMSource(document);

            SOAPMessage message = MessageFactory.newInstance().createMessage();

            // Get the SOAP part and set its content to domSource
            SOAPPart soapPart = message.getSOAPPart();
            soapPart.setContent(domSource);
            message.saveChanges();

            SOAPPart sp = message.getSOAPPart();

//            String inputEncoding = sp.getInputEncoding();
//            assertNotNull(inputEncoding);
        } catch (Exception e) {
            fail("Unexpected Exception " + e);
        }
    }
}
