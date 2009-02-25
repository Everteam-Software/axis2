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

package test.interop.whitemesa.round2.Soap12;

import org.apache.axis2.AxisFault;
import org.apache.axiom.soap.SOAPEnvelope;
import test.interop.whitemesa.SunClient;
import test.interop.whitemesa.SunClientUtil;
import test.interop.whitemesa.WhiteMesaIneterop;
import test.interop.whitemesa.round2.util.soap12.GroupbSoap12Echo2DStringArrayUtil;
import test.interop.whitemesa.round2.util.soap12.GroupbSoap12EchoNestedArrayUtil;
import test.interop.whitemesa.round2.util.soap12.GroupbSoap12EchoNestedStructUtil;
import test.interop.whitemesa.round2.util.soap12.GroupbSoap12EchoSimpleTypesAsStructUtil;
import test.interop.whitemesa.round2.util.soap12.GroupbSoap12EchoStructAsSimpleTypesUtil;
import test.interop.whitemesa.round2.util.soap12.Round2SOAP12EchoIntegerArrayclientUtil;
import test.interop.whitemesa.round2.util.soap12.Round2Soap12EchoBase64ClientUtil;
import test.interop.whitemesa.round2.util.soap12.Round2Soap12EchoBooleanClientUtil;
import test.interop.whitemesa.round2.util.soap12.Round2Soap12EchoDateClientUtil;
import test.interop.whitemesa.round2.util.soap12.Round2Soap12EchoDecimalClientUtil;
import test.interop.whitemesa.round2.util.soap12.Round2Soap12EchoFloatArrayClientUtil;
import test.interop.whitemesa.round2.util.soap12.Round2Soap12EchoFloatClientUtil;
import test.interop.whitemesa.round2.util.soap12.Round2Soap12EchoHexBinaryUtil;
import test.interop.whitemesa.round2.util.soap12.Round2Soap12EchoStructArrayClientUtil;
import test.interop.whitemesa.round2.util.soap12.Round2Soap12EchoStructClientUtil;
import test.interop.whitemesa.round2.util.soap12.Round2Soap12EchoVoidClientUtil;
import test.interop.whitemesa.round2.util.soap12.Round2Soap12IntegerUtil;
import test.interop.whitemesa.round2.util.soap12.Round2Soap12StringArrayUtil;
import test.interop.whitemesa.round2.util.soap12.Round2Soap12StringUtil;
import test.interop.whitemesa.round2.util.soap12.WMRound2Saop12GroupcFloatUtil;
import test.interop.whitemesa.round2.util.soap12.WMRound2Soap12GroupcBase64Util;
import test.interop.whitemesa.round2.util.soap12.WMRound2Soap12GroupcBooleanUtil;
import test.interop.whitemesa.round2.util.soap12.WMRound2Soap12GroupcEchoStringUtil;
import test.interop.whitemesa.round2.util.soap12.WMRound2Soap12GroupcFloatArrayUtil;
import test.interop.whitemesa.round2.util.soap12.WMRound2Soap12GroupcHexBinaryUtil;
import test.interop.whitemesa.round2.util.soap12.WMRound2Soap12GroupcIntegerArrayUtil;
import test.interop.whitemesa.round2.util.soap12.WMRound2Soap12GroupcIntergerUtil;
import test.interop.whitemesa.round2.util.soap12.WMRound2Soap12GroupcStringArrayUtil;
import test.interop.whitemesa.round2.util.soap12.WMRound2Soap12GroupcStructArrayUtil;
import test.interop.whitemesa.round2.util.soap12.WMRound2Soap12GroupcStructUtil;
import test.interop.whitemesa.round2.util.soap12.WMRound2Soap12GroupcVoidUtil;

import java.io.File;

/**
 * class
 * To test Interoperability Axis2 clients vs sun Server, Round2
 * WSDLs:-
 * "base"     http://soapinterop.java.sun.com/round2/base?WSDL
 * "Group B"  http://soapinterop.java.sun.com/round2/groupb?WSDL
 * "Group C"  http://soapinterop.java.sun.com/round2/groupc?WSDL
 */

public class WMRound2Soap12InteropTest extends WhiteMesaIneterop {

    SOAPEnvelope retEnv = null;
    File file = null;
    String url = "";
    String soapAction = "";
    String resFilePath = "interop/whitemesa/round2/SOAP12/";
    String tempPath = "";
    SunClientUtil util;
    SunClient client = new SunClient();

    /**
     * Round2
     * Group Base
     * operation echoString
     */
    public void testR2BaseEchoString() throws AxisFault {
        url = "http://www.whitemesa.net/interop/r2/base";
        soapAction = "http://soapinterop.org/";

        util = new Round2Soap12StringUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "WMR2_S12_StringRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group Base
     * operation echoStringArray
     */
    public void testR2BaseEchoStringArray() throws AxisFault {
        url = "http://www.whitemesa.net/interop/r2/base";
        soapAction = "http://soapinterop.org/";

        util = new Round2Soap12StringArrayUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "WMR2_S12_StringArrayRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group Base
     * operation echoInteger
     */
    public void testR2BaseEchoInteger() throws AxisFault {
        url = "http://www.whitemesa.net/interop/r2/base";
        soapAction = "http://soapinterop.org/";

        util = new Round2Soap12IntegerUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "WMR2_S12_IntegerRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group Base
     * operation echoIntegerArray
     */
    public void testR2BaseEchoIntegerArray() throws AxisFault {
        url = "http://www.whitemesa.net/interop/r2/base";
        soapAction = "http://soapinterop.org/";

        util = new Round2SOAP12EchoIntegerArrayclientUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "WMR2_S12_IntegerArrayRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group Base
     * operation echoFloat
     */
    public void testR2BaseEchoFloat() throws AxisFault {
        url = "http://www.whitemesa.net/interop/r2/base";
        soapAction = "http://soapinterop.org/";

        util = new Round2Soap12EchoFloatClientUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "WMR2_S12_FloatRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group Base
     * operation echoFloatArray
     */
    public void testR2BaseEchoFloatArray() throws AxisFault {
        url = "http://www.whitemesa.net/interop/r2/base";
        soapAction = "http://soapinterop.org/";

        util = new Round2Soap12EchoFloatArrayClientUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "WMR2_S12_FloatArrayRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group Base
     * operation echoStruct
     */
    public void testRBaseEchoStruct() throws AxisFault {
        url = "http://www.whitemesa.net/interop/r2/base";
        soapAction = "http://soapinterop.org/";

        util = new Round2Soap12EchoStructClientUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "WMR2_S12_StructRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group Base
     * operation echoStructArray
     */
    public void testR2BaseEchoStructArray() throws AxisFault {
        url = "http://www.whitemesa.net/interop/r2/base";
        soapAction = "http://soapinterop.org/";

        util = new Round2Soap12EchoStructArrayClientUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "WMR2_S12_StructArrayRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group Base
     * operation echoVoid
     */
    public void testR2BaseEchoVoid() throws AxisFault {
        url = "http://www.whitemesa.net/interop/r2/base";
        soapAction = "http://soapinterop.org/";

        util = new Round2Soap12EchoVoidClientUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "WMR2_S12_VoidRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group Base
     * operation echoBase64
     */
    public void testR2BaseEchoBase64() throws AxisFault {
        url = "http://www.whitemesa.net/interop/r2/base";
        soapAction = "http://soapinterop.org/";

        util = new Round2Soap12EchoBase64ClientUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "WMR2_S12_Base64Res.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group Base
     * operation echoBase64
     */
    public void testR2BaseEchoDate() throws AxisFault {
        url = "http://www.whitemesa.net/interop/r2/base";
        soapAction = "http://soapinterop.org/";

        util = new Round2Soap12EchoDateClientUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "WMR2_S12_DateRes.xml";
        compareXML(retEnv, tempPath);
    }


    /**
     * Round2
     * Group Base
     * operation echoHexBinary
     */
    public void testR2BaseEchoHexBinary() throws AxisFault {
        url = "http://www.whitemesa.net/interop/r2/base";
        soapAction = "http://soapinterop.org/";

        util = new Round2Soap12EchoHexBinaryUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "WMR2_S12_HexBinaryRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group Base
     * operation echoDecimal
     */
    public void testR2BaseEchoDecimal() throws AxisFault {
        url = "http://www.whitemesa.net/interop/r2/base";
        soapAction = "http://soapinterop.org/";

        util = new Round2Soap12EchoDecimalClientUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "WMR2_S12_DecimalRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group Base
     * operation echoBoolean
     */
    public void testR2BaseEchoBoolean() throws AxisFault {
        url = "http://www.whitemesa.net/interop/r2/base";
        soapAction = "http://soapinterop.org/";

        util = new Round2Soap12EchoBooleanClientUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "WMR2_S12_BooleanRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group B
     * operation echoStructAsSimpleTypes
     */
    public void testR2GBEchoStructAsSimpleTypes() throws AxisFault {
        url = "http://www.whitemesa.net/interop/r2/groupB";
        soapAction = "http://soapinterop.org/";

        util = new GroupbSoap12EchoStructAsSimpleTypesUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "WMR2Gb_S12_StructAsSimpleTypesRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group B
     * operation echoSimpleTypesAsStruct
     */
    public void testR2GBEchoSimpleTypesAsStruct() throws AxisFault {
        url = "http://www.whitemesa.net/interop/r2/groupB";
        soapAction = "http://soapinterop.org/";

        util = new GroupbSoap12EchoSimpleTypesAsStructUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "WMR2Gb_S12_SimpleTypesAsStructRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group B
     * operation echo2DStringArray
     */
    public void testR2GBEcho2DStringArray() throws AxisFault {
        url = "http://www.whitemesa.net/interop/r2/groupB";
        soapAction = "http://soapinterop.org/";

        util = new GroupbSoap12Echo2DStringArrayUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "WMR2Gb_S12_2DStringArrayRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group B
     * operation echoNestedStruct
     */
    public void testR2GBEchoNestedStruct() throws AxisFault {
        url = "http://www.whitemesa.net/interop/r2/groupB";
        soapAction = "http://soapinterop.org/";

        util = new GroupbSoap12EchoNestedStructUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "WMR2Gb_S12_NestedStructRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group B
     * operation echoNestedArray
     */
    public void testR2GBEchoNestedArray() throws AxisFault {
        url = "http://www.whitemesa.net/interop/r2/groupB";
        soapAction = "http://soapinterop.org/";

        util = new GroupbSoap12EchoNestedArrayUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "WMR2Gb_S12_NestedArrayRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group C
     * operation echoString
     */
    public void testR2GCEchoString() throws AxisFault {
        url = "http://www.whitemesa.net/interop/r2/groupC";
        soapAction = "http://soapinterop.org/";

        util = new WMRound2Soap12GroupcEchoStringUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "WMRound2Soap12GroupcEchoStringRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group C
     * operation echoInterger
     */
    public void testR2GCEchoInterger() throws AxisFault {
        url = "http://www.whitemesa.net/interop/r2/groupC";
        soapAction = "http://soapinterop.org/";

        util = new WMRound2Soap12GroupcIntergerUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "WMRound2Soap12GroupcIntergerRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group C
     * operation echoStringArray
     */
    public void testR2GCEchoStringArray() throws AxisFault {
        url = "http://www.whitemesa.net/interop/r2/groupC";
        soapAction = "http://soapinterop.org/";

        util = new WMRound2Soap12GroupcStringArrayUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "WMRound2Soap12GroupcStringArrayRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group C
     * operation echoIntergerArray
     */
    public void testR2GCEchoIntergerArray() throws AxisFault {
        url = "http://www.whitemesa.net/interop/r2/groupC";
        soapAction = "http://soapinterop.org/";

        util = new WMRound2Soap12GroupcIntegerArrayUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "WMRound2Soap12GroupcIntegerArrayRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group C
     * operation echoFloat
     */
    public void testR2GCEchoFloat() throws AxisFault {
        url = "http://www.whitemesa.net/interop/r2/groupC";
        soapAction = "http://soapinterop.org/";

        util = new WMRound2Saop12GroupcFloatUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "WMRound2Soap12GroupcFloatRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group C
     * operation echoFloatArray
     */
    public void testR2GCEchoFloatArray() throws AxisFault {
        url = "http://www.whitemesa.net/interop/r2/groupC";
        soapAction = "http://soapinterop.org/";

        util = new WMRound2Soap12GroupcFloatArrayUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "WMRound2Soap12GroupcFloatArrayRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group C
     * operation echoStruct
     */
    public void testR2GCEchoStruct() throws AxisFault {
        url = "http://www.whitemesa.net/interop/r2/groupC";
        soapAction = "http://soapinterop.org/";

        util = new WMRound2Soap12GroupcStructUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "WMRound2Soap12GroupcStructRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group C
     * operation echoStructArray
     */
    public void testR2GCEchoStructArray() throws AxisFault {
        url = "http://www.whitemesa.net/interop/r2/groupC";
        soapAction = "http://soapinterop.org/";

        util = new WMRound2Soap12GroupcStructArrayUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "WMRound2Soap12GroupcStructArrayRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group C
     * operation echoVoid
     */
    public void testR2GCEchoVoid() throws AxisFault {
        url = "http://www.whitemesa.net/interop/r2/groupC";
        soapAction = "http://soapinterop.org/";

        util = new WMRound2Soap12GroupcVoidUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "WMRound2Soap12GroupcVoidRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group C
     * operation echoBase64
     */
    public void testR2GCEchoBase64() throws AxisFault {
        url = "http://www.whitemesa.net/interop/r2/groupC";
        soapAction = "http://soapinterop.org/";

        util = new WMRound2Soap12GroupcBase64Util();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "WMRound2Soap12GroupcBase64Res.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group C
     * operation echoHexBinary
     */
    public void testR2GCEchoHexBinary() throws AxisFault {
        url = "http://www.whitemesa.net/interop/r2/groupC";
        soapAction = "http://soapinterop.org/";

        util = new WMRound2Soap12GroupcHexBinaryUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "WMRound2Soap12GroupcHexBinaryRes.xml";
        compareXML(retEnv, tempPath);
    }

    /**
     * Round2
     * Group C
     * operation echoBoolean
     */
    public void testR2GCEchoBoolean() throws AxisFault {
        url = "http://www.whitemesa.net/interop/r2/groupC";
        soapAction = "http://soapinterop.org/";

        util = new WMRound2Soap12GroupcBooleanUtil();
        retEnv = client.sendMsg(util, url, soapAction);
        tempPath = resFilePath + "WMRound2Soap12GroupcBooleanRes.xml";
        compareXML(retEnv, tempPath);
    }
}

