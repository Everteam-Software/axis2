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


package org.apache.axis2.transport.http;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.deployment.DeploymentConstants;
import org.apache.axis2.description.AxisDescription;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.PolicyInclude;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.util.ExternalPolicySerializer;
import org.apache.axis2.util.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyRegistry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class ListingAgent extends AbstractAgent {

    private static final Log log = LogFactory.getLog(ListingAgent.class);

    private static final String LIST_MULTIPLE_SERVICE_JSP_NAME =
            "listServices.jsp";
    private static final String LIST_SINGLE_SERVICE_JSP_NAME =
            "listSingleService.jsp";
    private static final String LIST_FAULTY_SERVICES_JSP_NAME = "listFaultyService.jsp";

    public static final String RUNNING_PORT = "RUNNING_PORT";

    public ListingAgent(ConfigurationContext aConfigContext) {
        super(aConfigContext);
    }

    private void addTransportListener(String schema, int port) {
        try {
            TransportInDescription trsIn =
                    configContext.getAxisConfiguration().getTransportIn(schema);
            if (trsIn == null) {
                trsIn = new TransportInDescription(schema);
                CustomListener httspReceiver = new CustomListener(port, schema);
                httspReceiver.init(configContext, trsIn);
                trsIn.setReceiver(httspReceiver);
                configContext.getListenerManager().addListener(trsIn, true);
            }
        } catch (AxisFault axisFault) {
            //
        }
    }

    public void handle(HttpServletRequest httpServletRequest,
                       HttpServletResponse httpServletResponse)
            throws IOException, ServletException {

        initTransportListener(httpServletRequest);

        String query = httpServletRequest.getQueryString();
        if (query != null) {
            if (query.indexOf("wsdl2") > 0 || query.indexOf("wsdl") > 0 ||
                query.indexOf("xsd") > 0 || query.indexOf("policy") > 0) {
                processListService(httpServletRequest, httpServletResponse);
            } else {
                super.handle(httpServletRequest, httpServletResponse);
            }
        } else {
            super.handle(httpServletRequest, httpServletResponse);
        }
    }

    protected void initTransportListener(HttpServletRequest httpServletRequest) {
        // httpServletRequest.getLocalPort() , giving me a build error so I had to use the followin
        String filePart;
        try {
            filePart = httpServletRequest.getRequestURL().toString();
        } catch (Throwable t){
            log.info("Old Servlet API (fallback to HttpServletRequest.getRequestURI) :" + t);    
            filePart = httpServletRequest.getRequestURI();
        }
        int ipindex = filePart.indexOf("//");
        String ip;
        if (ipindex >= 0) {
            ip = filePart.substring(ipindex + 2, filePart.length());
            int seperatorIndex = ip.indexOf(":");
            int slashIndex = ip.indexOf("/");
            String portstr;
            if (seperatorIndex >= 0) {
                portstr = ip.substring(seperatorIndex + 1, slashIndex);
            } else {
                portstr = "80";
            }
            try {
                addTransportListener(httpServletRequest.getScheme(), Integer.parseInt(portstr));
            } catch (NumberFormatException e) {
                log.debug(e.toString(), e);
            }
        }
    }

    protected void processListFaultyServices(HttpServletRequest req, HttpServletResponse res)
            throws IOException, ServletException {
        String serviceName = req.getParameter("serviceName");
        if (serviceName != null) {
            AxisService service = configContext.getAxisConfiguration().getService(serviceName);
            try {
                req.getSession().setAttribute(Constants.SINGLE_SERVICE, service);
            } catch (Throwable t) {
                log.info("Old Servlet API :" + t);
            }
        }
        renderView(LIST_FAULTY_SERVICES_JSP_NAME, req, res);
    }


    protected void processIndex(HttpServletRequest httpServletRequest,
                                HttpServletResponse httpServletResponse)
            throws IOException, ServletException {
        processListServices(httpServletRequest, httpServletResponse);
    }

    private String extractHostAndPort(String filePart, boolean isHttp) {
        int ipindex = filePart.indexOf("//");
        String ip = null;
        if (ipindex >= 0) {
            ip = filePart.substring(ipindex + 2, filePart.length());
            int seperatorIndex = ip.indexOf(":");
            int slashIndex = ip.indexOf("/");
            String port;
            if (seperatorIndex >= 0) {
                port = ip.substring(seperatorIndex + 1, slashIndex);
                ip = ip.substring(0, seperatorIndex);
            } else {
                ip = ip.substring(0, slashIndex);
                port = "80";
            }
            if (isHttp) {
                configContext.setProperty(RUNNING_PORT, port);
            }
        }
        return ip;
    }

    public void processExplicitSchemaAndWSDL(HttpServletRequest req,
                                             HttpServletResponse res)
            throws IOException, ServletException {
        HashMap services = configContext.getAxisConfiguration().getServices();
        String filePart = req.getRequestURL().toString();
        String schema = filePart.substring(filePart.lastIndexOf("/") + 1,
                                           filePart.length());
        if ((services != null) && !services.isEmpty()) {
            Iterator i = services.values().iterator();
            while (i.hasNext()) {
                AxisService service = (AxisService) i.next();
                InputStream stream = service.getClassLoader().getResourceAsStream("META-INF/" + schema);
                if (stream != null) {
                    OutputStream out = res.getOutputStream();
                    res.setContentType("text/xml");
                    IOUtils.copy(stream, out, true);
                    return;
                }
            }
        }
    }

    /**
     * Copies the input stream to the output stream
     *
     * @param stream  the <code>InputStream</code>
     * @param ostream the <code>OutputStream</code>
     */
    public static void copy(InputStream stream, OutputStream ostream) throws IOException {
        IOUtils.copy(stream, ostream, false);
    }

    public String extractServiceName(String urlString) {
        int n = urlString.indexOf(configContext.getServiceContextPath());
        if (n != -1) {
            String serviceName = urlString.substring(n + configContext.getServiceContextPath().length(),
                    urlString.length());
            if (serviceName.length() > 0) {
                if(serviceName.charAt(0)=='/'){
                    serviceName = serviceName.substring(1);
                }
                return serviceName;
            }
        }
        return urlString.substring(urlString.lastIndexOf("/") + 1,
                urlString.length());
    }

    public void processListService(HttpServletRequest req,
                                   HttpServletResponse res)
            throws IOException, ServletException {

        String url;
        try {
        url = req.getRequestURL().toString();
        } catch (Throwable t) {
            log.info("Old Servlet API (Fallback to HttpServletRequest.getRequestURI) :" + t);    
            url = req.getRequestURI();
        }
        String serviceName = extractServiceName(url);
        HashMap services = configContext.getAxisConfiguration().getServices();
        String query = req.getQueryString();
        int wsdl2 = query.indexOf("wsdl2");
        int wsdl = query.indexOf("wsdl");
        int xsd = query.indexOf("xsd");
        int policy = query.indexOf("policy");

        if ((services != null) && !services.isEmpty()) {
            Object serviceObj = services.get(serviceName);
            if (serviceObj != null) {
                AxisService axisService = (AxisService) serviceObj;
                boolean isHttp = "http".equals(req.getScheme());
                if (wsdl2 >= 0) {
                    res.setContentType("text/xml");
                    String ip = extractHostAndPort(url, isHttp);
                    String wsdlName = req.getParameter("wsdl2");
                    
                    int ret = axisService.printWSDL2(res.getOutputStream(), ip, wsdlName);
                    if (ret == 0) {
                        res.sendRedirect("");
                    } else if (ret == -1) {
                        res.sendError(HttpServletResponse.SC_NOT_FOUND);
                    }
                    return;
                } else if (wsdl >= 0) {
                    OutputStream out = res.getOutputStream();
                    res.setContentType("text/xml");
                    String ip = extractHostAndPort(url, isHttp);
                    String wsdlName = req.getParameter("wsdl");

                    if (wsdlName != null && wsdlName.length()>0) {
                        axisService.printUserWSDL(out, wsdlName);
                        out.flush();
                        out.close();
                    } else {
                        axisService.printWSDL(out, ip);
                        out.flush();
                        out.close();
                    }
                    return;
                } else if (xsd >= 0) {
                    res.setContentType("text/xml");
                    int ret = axisService.printXSD(res.getOutputStream(), req.getParameter("xsd"));
                    if (ret == 0) {
                        //multiple schemas are present and the user specified
                        //no name - in this case we cannot possibly pump a schema
                        //so redirect to the service root
                        res.sendRedirect("");
                    } else if (ret == -1) {
                        res.sendError(HttpServletResponse.SC_NOT_FOUND);
                    }
                    return;
                } else if (policy >= 0) {

                    ExternalPolicySerializer serializer = new ExternalPolicySerializer();
                    serializer.setAssertionsToFilter(configContext
                            .getAxisConfiguration().getLocalPolicyAssertions());

                    // check whether Id is set
                    String idParam = req.getParameter("id");

                    if (idParam != null) {
                        // Id is set

                        Policy targetPolicy = findPolicy(idParam, axisService);

                        if (targetPolicy != null) {
                            XMLStreamWriter writer;

                            try {
                                OutputStream out = res.getOutputStream();
                                writer = XMLOutputFactory.newInstance()
                                        .createXMLStreamWriter(out);

                                res.setContentType("application/wspolicy+xml");
                                targetPolicy.serialize(writer);
                                writer.flush();

                            } catch (XMLStreamException e) {
                                throw new ServletException(
                                        "Error occured when serializing the Policy",
                                        e);

                            } catch (FactoryConfigurationError e) {
                                throw new ServletException(
                                        "Error occured when serializing the Policy",
                                        e);
                            }

                        } else {

                            OutputStream out = res.getOutputStream();
                            res.setContentType("text/html");
                            String outStr = "<b>No policy found for id="
                                            + idParam + "</b>";
                            out.write(outStr.getBytes());
                        }

                    } else {

                        PolicyInclude policyInclude = axisService.getPolicyInclude();
                        Policy effecPolicy = policyInclude.getEffectivePolicy();

                        if (effecPolicy != null) {
                            XMLStreamWriter writer;

                            try {
                                OutputStream out = res.getOutputStream();
                                writer = XMLOutputFactory.newInstance()
                                        .createXMLStreamWriter(out);

                                res.setContentType("application/wspolicy+xml");
                                effecPolicy.serialize(writer);
                                writer.flush();

                            } catch (XMLStreamException e) {
                                throw new ServletException(
                                        "Error occured when serializing the Policy",
                                        e);

                            } catch (FactoryConfigurationError e) {
                                throw new ServletException(
                                        "Error occured when serializing the Policy",
                                        e);
                            }
                        } else {

                            OutputStream out = res.getOutputStream();
                            res.setContentType("text/html");
                            String outStr = "<b>No effective policy for "
                                            + serviceName + " service</b>";
                            out.write(outStr.getBytes());
                        }
                    }

                    return;
                } else {
                    try {
                        req.getSession().setAttribute(Constants.SINGLE_SERVICE,
                                serviceObj);
                    } catch (Throwable t) {
                        log.info("Old Servlet API :" + t);
                    }
                }
            } else {
                try {
                    req.getSession().setAttribute(Constants.SINGLE_SERVICE, null);
                } catch (Throwable t){
                    log.info("Old Servlet API :" + t);    
                }
                    
                res.sendError(HttpServletResponse.SC_NOT_FOUND, url);
            }
        }

        renderView(LIST_SINGLE_SERVICE_JSP_NAME, req, res);
    }

    protected void processListServices(HttpServletRequest req,
                                       HttpServletResponse res)
            throws IOException, ServletException {

        populateSessionInformation(req);
        try {
            req.getSession().setAttribute(Constants.ERROR_SERVICE_MAP,
                                          configContext.getAxisConfiguration().getFaultyServices());
        } catch (Throwable t){
            log.info("Old Servlet API :" + t);    
        }
        renderView(LIST_MULTIPLE_SERVICE_JSP_NAME, req, res);
    }

    private Policy findPolicy(String id, AxisDescription des) {

        List policyElements = des.getPolicyInclude().getPolicyElements();
        PolicyRegistry registry = des.getPolicyInclude().getPolicyRegistry();

        Object policyComponent;

        Policy policy = registry.lookup(id);

        if (policy != null) {
            return policy;
        }

        for (Iterator iterator = policyElements.iterator(); iterator.hasNext();) {
            policyComponent = iterator.next();

            if (policyComponent instanceof Policy) {
                // policy found for the id

                if (id.equals(((Policy) policyComponent).getId())) {
                    return (Policy) policyComponent;
                }
            }
        }

        AxisDescription child;

        for (Iterator iterator = des.getChildren(); iterator.hasNext();) {
            child = (AxisDescription) iterator.next();
            policy = findPolicy(id, child);

            if (policy != null) {
                return policy;
            }
        }

        return null;
    }

}
