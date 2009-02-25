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
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.Name;
import javax.xml.soap.Node;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.soap.Text;
import java.io.File;
import java.util.Iterator;

public class SOAPBodyTest extends TestCase {

    /**
     * Method suite
     *                                                         
     * @return
     */
    /*  public static Test suite() {
          return new TestSuite(test.message.TestSOAPBody.class);
      }
    */

    /**
     * Constructor TestSOAPBody
     *
     * @param name
     */
    public SOAPBodyTest(String name) {
        super(name);
    }

    /**
     * Method testSoapBodyBUG
     *
     * @throws Exception
     */
    public void testSoapBody() throws Exception {

        MessageFactory fact = MessageFactory.newInstance();
        SOAPMessage message = fact.createMessage();
        SOAPPart soapPart = message.getSOAPPart();
        SOAPEnvelopeImpl env = (SOAPEnvelopeImpl)soapPart.getEnvelope();
        SOAPHeader header = env.getHeader();
        Name hns = env.createName("Hello",
                                  "shw",
                                  "http://www.jcommerce.net/soap/ns/SOAPHelloWorld");
        SOAPElement headElmnt = header.addHeaderElement(hns);
        Name hns1 = env.createName("Myname",
                                   "shw",
                                   "http://www.jcommerce.net/soap/ns/SOAPHelloWorld");
        SOAPElement myName = headElmnt.addChildElement(hns1);
        myName.addTextNode("Tony");
        Name ns = env.createName("Address",
                                 "shw",
                                 "http://www.jcommerce.net/soap/ns/SOAPHelloWorld");
        SOAPBody body = env.getBody();
        SOAPElement bodyElmnt = body.addBodyElement(ns);
        Name ns1 = env.createName("City",
                                  "shw",
                                  "http://www.jcommerce.net/soap/ns/SOAPHelloWorld");
        SOAPElement city = bodyElmnt.addChildElement(ns1);
        city.addTextNode("GENT");

        SOAPElement city2 = body.addChildElement(ns1);
        assertTrue(city2 instanceof SOAPBodyElement);
        city2.addTextNode("CIT2");

        Iterator it = body.getChildElements();
        int count = 0;

        while (it.hasNext()) {
            Object o = it.next();
            assertTrue(o instanceof SOAPBodyElement);
            SOAPBodyElement bodyElement = (SOAPBodyElement)o;
            assertEquals("http://www.jcommerce.net/soap/ns/SOAPHelloWorld",
                         bodyElement.getNamespaceURI());
            assertEquals("shw", bodyElement.getPrefix());
            assertTrue(bodyElement.getLocalName().equals("City") ||
                    bodyElement.getLocalName().equals("Address"));
            count++;
        }
        assertEquals(2, count);
    }

    public void _testAddDocument() {
        try {
            Document document = null;
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(new File(System.getProperty("basedir", ".") + "/" +
                    "test-resources" + File.separator + "soap-body.xml"));
            MessageFactory fact = MessageFactory.newInstance();
            SOAPMessage message = fact.createMessage();

            message.getSOAPHeader().detachNode();
            // assertNull(message.getSOAPHeader());    
            // TODO:this fails. Header is always being created if it doesnt exist in DOOM

            SOAPBody soapBody = message.getSOAPBody();
            soapBody.addDocument(document);
            message.saveChanges();

            // Get contents using SAAJ APIs
            Iterator iter1 = soapBody.getChildElements();
            getContents(iter1, "");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected Exception : " + e);
        }
    }

    private void getContents(Iterator iterator, String indent) {
        while (iterator.hasNext()) {
            Node node = (Node)iterator.next();
            SOAPElement element = null;
            Text text = null;

            if (node instanceof SOAPElement) {
                element = (SOAPElement)node;
                Name name = element.getElementName();
                Iterator attrs = element.getAllAttributes();

                /*
                while (attrs.hasNext()) {
                    Name attrName = (Name) attrs.next();
                    System.out.println(indent + " Attribute name is " +
                                       attrName.getQualifiedName());
                    System.out.println(indent + " Attribute value is " +
                                       element.getAttributeValue(attrName));
                }
                */

                Iterator iter2 = element.getChildElements();
                getContents(iter2, indent + " ");
            } else {
                text = (Text)node;
                assertNotNull(text);
            }
        }
    }

    //TODO : fix
    public void testExtractContentAsDocument() {
        try {
            MessageFactory fact = MessageFactory.newInstance();
            SOAPMessage message = fact.createMessage();
            SOAPBody soapBody = message.getSOAPBody();

            QName qname1 = new QName("http://wombat.ztrade.com",
                                     "GetLastTradePrice", "ztrade");
            SOAPElement child1 = soapBody.addChildElement(qname1);
            Document document = soapBody.extractContentAsDocument();

            assertNotNull(document);
            assertTrue(document instanceof Document);
            Element element = document.getDocumentElement();
            String elementName = element.getTagName();

            //Retreive the children of the SOAPBody (should be none)
            Iterator childElements = soapBody.getChildElements();
            int childCount = 0;
            while (childElements.hasNext()) {
                Object object = childElements.next();
                childCount++;
            }
            assertEquals(childCount, 0);
        }
        catch (Exception e) {
            fail("Unexpected Exception : " + e);
        }
    }

    /*
     * For SOAP 1.1 message 
     */
    public void testAddAttribute() {
        try {
            MessageFactory fact = MessageFactory.newInstance();
            SOAPMessage message = fact.createMessage();
            SOAPBody soapBody = message.getSOAPBody();
            QName qname = new QName("http://test.apache.org/", "Child1", "ch");
            String value = "MyValue1";
            soapBody.addAttribute(qname, value);
            message.saveChanges();

        } catch (Exception e) {
            fail("Unexpected Exception : " + e);
        }
    }

    /*
     * For SOAP 1.2 message 
     */
    public void testAddAttribute2() {
        try {
            MessageFactory fact = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
            SOAPMessage message = fact.createMessage();
            SOAPBody soapBody = message.getSOAPBody();
            QName qname = new QName("http://test.apache.org/", "Child1", "ch");
            String value = "MyValue1";
            soapBody.addAttribute(qname, value);
            message.saveChanges();
        } catch (Exception e) {
            fail("Unexpected Exception : " + e);
        }
    }

    /*
    * For SOAP 1.2 message
    */
    public void testAddFault() {
        try {
            MessageFactory fact = MessageFactory.newInstance();
            SOAPMessage message = fact.createMessage();
            SOAPPart soapPart = message.getSOAPPart();
            SOAPEnvelope soapEnvelope = soapPart.getEnvelope();
            SOAPBody soapBody = soapEnvelope.getBody();

            QName qname = new QName("http://test.apache.org/", "Child1", "ch");
            String value = "MyFault";
            SOAPFault soapFault = soapBody.addFault(qname, value);
            message.saveChanges();
            assertNotNull(soapFault);
            assertTrue(soapFault instanceof SOAPFault);
        } catch (Exception e) {
            fail("Unexpected Exception : " + e);
        }
    }


}
