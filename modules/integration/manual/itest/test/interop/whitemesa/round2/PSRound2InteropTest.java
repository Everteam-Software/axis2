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

package test.interop.whitemesa.round2;

import org.apache.axis2.AxisFault;
import org.apache.axiom.soap.SOAPEnvelope;
import test.interop.whitemesa.SunClient;
import test.interop.whitemesa.SunClientUtil;
import test.interop.whitemesa.WhiteMesaIneterop;
import test.interop.whitemesa.round2.util.GroupbEcho2DStringArrayUtil;
import test.interop.whitemesa.round2.util.GroupbEchoNestedArrayUtil;
import test.interop.whitemesa.round2.util.GroupbEchoNestedStructUtil;
import test.interop.whitemesa.round2.util.GroupbEchoSimpleTypesAsStructUtil;
import test.interop.whitemesa.round2.util.GroupbEchoStructAsSimpleTypesUtil;
import test.interop.whitemesa.round2.util.GroupcBase64Util;
import test.interop.whitemesa.round2.util.GroupcBooleanUtil;
import test.interop.whitemesa.round2.util.GroupcEchoStringUtil;
import test.interop.whitemesa.round2.util.GroupcFloatArrayUtil;
import test.interop.whitemesa.round2.util.GroupcFloatUtil;
import test.interop.whitemesa.round2.util.GroupcHexBinaryUtil;
import test.interop.whitemesa.round2.util.GroupcIntegerArrayUtil;
import test.interop.whitemesa.round2.util.GroupcIntergerUtil;
import test.interop.whitemesa.round2.util.GroupcStringArrayUtil;
import test.interop.whitemesa.round2.util.GroupcStructArrayUtil;
import test.interop.whitemesa.round2.util.GroupcStructUtil;
import test.interop.whitemesa.round2.util.GroupcVoidUtil;
import test.interop.whitemesa.round2.util.Round2EchoBase64ClientUtil;
import test.interop.whitemesa.round2.util.Round2EchoBooleanClientUtil;
import test.interop.whitemesa.round2.util.Round2EchoDateClientUtil;
import test.interop.whitemesa.round2.util.Round2EchoDecimalClientUtil;
import test.interop.whitemesa.round2.util.Round2EchoFloatArrayClientUtil;
import test.interop.whitemesa.round2.util.Round2EchoFloatClientUtil;
import test.interop.whitemesa.round2.util.Round2EchoHexBinaryClientUtil;
import test.interop.whitemesa.round2.util.Round2EchoIntegerArrayclientUtil;
import test.interop.whitemesa.round2.util.Round2EchoIntegerClientUtil;
import test.interop.whitemesa.round2.util.Round2EchoStringArrayClientUtil;
import test.interop.whitemesa.round2.util.Round2EchoStringclientUtil;
import test.interop.whitemesa.round2.util.Round2EchoStructArrayClientUtil;
import test.interop.whitemesa.round2.util.Round2EchoStructClientUtil;
import test.interop.whitemesa.round2.util.Round2EchoVoidClientUtil;

import java.io.File;

/**
 * class PSRound2InteropTest
 * To test Interoperability Axis2 clients vs PEAR SOAP Server, Round2
 * WSDLs:-
 * "base"     http://caraveo.com/soap_interop/interop.wsdl
 * "Group B"  http://caraveo.com/soap_interop/interopB.wsdl
 * <p/>
 * Todo - All Tests fail
 * The returned envelope doesn't contain a valid xml content.
 */

/**
 * All test cases are failing since the server is not sending response
 * messages.
 */

public class PSRound2InteropTest extends WhiteMesaIneterop {

    SOAPEnvelope retEnv = null;
    File file = null;
    String url = "";
    String soapAction = "";
    String resFilePath = "interop/whitemesa/round2/";
    String tempPath = "";
    SunClientUtil util;
    SunClient client = new SunClient();

    /**
     * Round2
     * Group Base
     * operation echoString
     */
    public void testR2BaseEchoString() throws AxisFault {
        url = "http://www.caraveo.com/soap_interop/server_round2.php";
        soapAction = "http://soapinterop.org/";

        util = new Round2EchoStringclientUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "sunBaseStringRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group Base
     * operation echoStringArray
     */
    public void testR2BaseEchoStringArray() throws AxisFault {
        url = "http://www.caraveo.com/soap_interop/server_round2.php";
        soapAction = "http://soapinterop.org/";

        util = new Round2EchoStringArrayClientUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "SunBaseStringArrayRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group Base
     * operation echoInteger
     */
    public void testR2BaseEchoInteger() throws AxisFault {
        url = "http://www.caraveo.com/soap_interop/server_round2.php";
        soapAction = "http://soapinterop.org/";

        util = new Round2EchoIntegerClientUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "sunBaseIntegerRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group Base
     * operation echoIntegerArray
     */
    public void testR2BaseEchoIntegerArray() throws AxisFault {
        url = "http://www.caraveo.com/soap_interop/server_round2.php";
        soapAction = "http://soapinterop.org/";

        util = new Round2EchoIntegerArrayclientUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "sunBaseIntegerArrayRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group Base
     * operation echoFloat
     */
    public void testR2BaseEchoFloat() throws AxisFault {
        url = "http://www.caraveo.com/soap_interop/server_round2.php";
        soapAction = "http://soapinterop.org/";

        util = new Round2EchoFloatClientUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "sunBaseFloatRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group Base
     * operation echoFloatArray
     */
    public void testR2BaseEchoFloatArray() throws AxisFault {
        url = "http://www.caraveo.com/soap_interop/server_round2.php";
        soapAction = "http://soapinterop.org/";

        util = new Round2EchoFloatArrayClientUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "sunBaseFloatArrayRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group Base
     * operation echoStruct
     */
    public void testRBaseEchoStruct() throws AxisFault {
        url = "http://www.caraveo.com/soap_interop/server_round2.php";
        soapAction = "";

        util = new Round2EchoStructClientUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "sunBaseStructRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group Base
     * operation echoStructArray
     */
    public void testR2BaseEchoStructArray() throws AxisFault {
        url = "http://www.caraveo.com/soap_interop/server_round2.php";
        soapAction = "http://soapinterop.org/";

        util = new Round2EchoStructArrayClientUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "sunBaseStructArrayRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group Base
     * operation echoVoid
     */
    public void testR2BaseEchoVoid() throws AxisFault {
        url = "http://www.caraveo.com/soap_interop/server_round2.php";
        soapAction = "http://soapinterop.org/";

        util = new Round2EchoVoidClientUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "sunBaseVoidRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group Base
     * operation echoBase64
     */
    public void testR2BaseEchoBase64() throws AxisFault {
        url = "http://www.caraveo.com/soap_interop/server_round2.php";
        soapAction = "http://soapinterop.org/";

        util = new Round2EchoBase64ClientUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "sunBaseBase64Res.xml";
        assertR2DefaultEchoBase64Result(retEnv);
    }

    /**
     * Round2
     * Group Base
     * operation echoBase64
     */
    public void testR2BaseEchoDate() throws AxisFault {
        url = "http://www.caraveo.com/soap_interop/server_round2.php";
        soapAction = "http://soapinterop.org/";

        util = new Round2EchoDateClientUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "sunBaseDateRes.xml";
        compareXML(retEnv, tempPath);
    }


    /**
     * Round2
     * Group Base
     * operation echoHexBinary
     */
    public void testR2BaseEchoHexBinary() throws AxisFault {
        url = "http://www.caraveo.com/soap_interop/server_round2.php";
        soapAction = "http://soapinterop.org/";

        util = new Round2EchoHexBinaryClientUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "sunBaseHexBinaryRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group Base
     * operation echoDecimal
     */
    public void testR2BaseEchoDecimal() throws AxisFault {
        url = "http://www.caraveo.com/soap_interop/server_round2.php";
        soapAction = "http://soapinterop.org/";

        util = new Round2EchoDecimalClientUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "sunBaseDecimalRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group Base
     * operation echoBoolean
     */
    public void testR2BaseEchoBoolean() throws AxisFault {
        url = "http://www.caraveo.com/soap_interop/server_round2.php";
        soapAction = "http://soapinterop.org/";

        util = new Round2EchoBooleanClientUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "sunBaseBooleanRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group B
     * operation echoStructAsSimpleTypes
     */
    public void testR2GBEchoStructAsSimpleTypes() throws AxisFault {
        url = "http://www.caraveo.com/soap_interop/server_round2.php";
        soapAction = "http://soapinterop.org/";

        util = new GroupbEchoStructAsSimpleTypesUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "sunGroupbStructAsSimpleTypesRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group B
     * operation echoSimpleTypesAsStruct
     */
    public void testR2GBEchoSimpleTypesAsStruct() throws AxisFault {
        url = "http://www.caraveo.com/soap_interop/server_round2.php";
        soapAction = "http://soapinterop.org/";

        util = new GroupbEchoSimpleTypesAsStructUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "sunGroupbSimpletypesAsStructRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group B
     * operation echo2DStringArray
     */
    public void testR2GBEcho2DStringArray() throws AxisFault {
        url = "http://www.caraveo.com/soap_interop/server_round2.php";
        soapAction = "http://soapinterop.org/";

        util = new GroupbEcho2DStringArrayUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "sunGroupb2DStringArrayRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group B
     * operation echoNestedStruct
     */
    public void testR2GBEchoNestedStruct() throws AxisFault {
        url = "http://www.caraveo.com/soap_interop/server_round2.php";
        soapAction = "http://soapinterop.org/";

        util = new GroupbEchoNestedStructUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "sunGroupbNestedStructRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group B
     * operation echoNestedArray
     */
    public void testR2GBEchoNestedArray() throws AxisFault {
        url = "http://www.caraveo.com/soap_interop/server_round2.php";
        soapAction = "http://soapinterop.org/";

        util = new GroupbEchoNestedArrayUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "sunGroupbNestedArrayRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group C
     * operation echoString
     */
    public void testR2GCEchoString() throws AxisFault {
        url = "http://www.caraveo.com/soap_interop/server_round2.php";
        soapAction = "http://soapinterop.org/";

        util = new GroupcEchoStringUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "GroupcEchoStringRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group C
     * operation echoInterger
     */
    public void testR2GCEchoInterger() throws AxisFault {
        url = "http://www.caraveo.com/soap_interop/server_round2.php";
        soapAction = "http://soapinterop.org/";

        util = new GroupcIntergerUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "GroupcIntergerRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group C
     * operation echoStringArray
     */
    public void testR2GCEchoStringArray() throws AxisFault {
        url = "http://www.caraveo.com/soap_interop/server_round2.php";
        soapAction = "http://soapinterop.org/";

        util = new GroupcStringArrayUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "GroupcStringArrayRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group C
     * operation echoIntergerArray
     */
    public void testR2GCEchoIntergerArray() throws AxisFault {
        url = "http://www.caraveo.com/soap_interop/server_round2.php";
        soapAction = "http://soapinterop.org/";

        util = new GroupcIntegerArrayUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "GroupcIntegerArrayRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group C
     * operation echoFloat
     */
    public void testR2GCEchoFloat() throws AxisFault {
        url = "http://www.caraveo.com/soap_interop/server_round2.php";
        soapAction = "http://soapinterop.org/";

        util = new GroupcFloatUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "GroupcFloatRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group C
     * operation echoFloatArray
     */
    public void testR2GCEchoFloatArray() throws AxisFault {
        url = "http://www.caraveo.com/soap_interop/server_round2.php";
        soapAction = "http://soapinterop.org/";

        util = new GroupcFloatArrayUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "GroupcFloatArrayRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group C
     * operation echoStruct
     */
    public void testR2GCEchoStruct() throws AxisFault {
        url = "http://www.caraveo.com/soap_interop/server_round2.php";
        soapAction = "http://soapinterop.org/";

        util = new GroupcStructUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "GroupcStructRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group C
     * operation echoStructArray
     */
    public void testR2GCEchoStructArray() throws AxisFault {
        url = "http://www.caraveo.com/soap_interop/server_round2.php";
        soapAction = "http://soapinterop.org/";

        util = new GroupcStructArrayUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "GroupcStructArrayRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group C
     * operation echoVoid
     */
    public void testR2GCEchoVoid() throws AxisFault {
        url = "http://www.caraveo.com/soap_interop/server_round2.php";
        soapAction = "http://soapinterop.org/";

        util = new GroupcVoidUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "GroupcVoidRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group C
     * operation echoBase64
     */
    public void testR2GCEchoBase64() throws AxisFault {
        url = "http://www.caraveo.com/soap_interop/server_round2.php";
        soapAction = "http://soapinterop.org/";

        util = new GroupcBase64Util();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "GroupcBase64Res.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group C
     * operation echoHexBinary
     */
    public void testR2GCEchoHexBinary() throws AxisFault {
        url = "http://www.caraveo.com/soap_interop/server_round2.php";
        soapAction = "http://soapinterop.org/";

        util = new GroupcHexBinaryUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "GroupcHexBinaryRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group C
     * operation echoBoolean
     */
    public void testR2GCEchoBoolean() throws AxisFault {
        url = "http://www.caraveo.com/soap_interop/server_round2.php";
        soapAction = "http://soapinterop.org/";

        util = new GroupcBooleanUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "GroupcBooleanRes.xml";
        compareXML(retEnv, tempPath);
    }
}

