<!--
  ~ Licensed to the Apache Software Foundation (ASF) under one
  ~ or more contributor license agreements. See the NOTICE file
  ~ distributed with this work for additional information
  ~ regarding copyright ownership. The ASF licenses this file
  ~ to you under the Apache License, Version 2.0 (the
  ~ "License"); you may not use this file except in compliance
  ~ with the License. You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing,
  ~ software distributed under the License is distributed on an
  ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  ~ KIND, either express or implied. See the License for the
  ~ specific language governing permissions and limitations
  ~ under the License.
  -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="text"/>

    
    <xsl:template match="/class">
      <xsl:variable name="interfaceName"><xsl:value-of select="@interfaceName"/></xsl:variable>
      <xsl:variable name="package"><xsl:value-of select="@package"/></xsl:variable>
      <xsl:variable name="isSync"><xsl:value-of select="@isSync"/></xsl:variable>
      <xsl:variable name="isAsync"><xsl:value-of select="@isAsync"/></xsl:variable>
      <xsl:variable name="soapVersion"><xsl:value-of select="@soap-version"/></xsl:variable>
      <xsl:variable name="callbackname"><xsl:value-of select="@callbackname"/></xsl:variable>
      <xsl:variable name="method-prefix"><xsl:value-of select="@prefix"/></xsl:variable> <!-- This is no longer using -->
      <xsl:variable name="qname"><xsl:value-of select="@qname"/></xsl:variable>
      <xsl:variable name="servicename"><xsl:value-of select="@servicename"/></xsl:variable>

      /**
       * <xsl:value-of select="@name"/>.c
       *
       * This file was auto-generated from WSDL for "<xsl:value-of select="$qname"/>" service
       * by the Apache Axis2/Java version: #axisVersion# #today#
       */

      #include "<xsl:value-of select="@name"/>.h"
      #include &lt;axis2_msg.h&gt;

      /**
       * <xsl:value-of select="@name"/> C implementation
       */

      axis2_stub_t*
      axis2_stub_create_<xsl:value-of select="$servicename"/>(const axutil_env_t *env,
                                      axis2_char_t *client_home,
                                      axis2_char_t *endpoint_uri)
      {
         axis2_stub_t *stub = NULL;
         axis2_endpoint_ref_t *endpoint_ref = NULL;
         AXIS2_FUNC_PARAM_CHECK (client_home, env, NULL)

         if (NULL == endpoint_uri)
         {
            endpoint_uri = axis2_stub_get_endpoint_uri_of_<xsl:value-of select="$servicename"/>(env);
         }

         endpoint_ref = axis2_endpoint_ref_create(env, endpoint_uri);

         stub = axis2_stub_create_with_endpoint_ref_and_client_home (env, endpoint_ref, client_home);

         if (NULL == stub)
         {
            if(NULL != endpoint_ref)
            {
                axis2_endpoint_ref_free(endpoint_ref, env);
            }
            return NULL;
         }


         axis2_stub_populate_services_for_<xsl:value-of select="$servicename"/>(stub, env);
         return stub;
      }


      void
      axis2_stub_populate_services_for_<xsl:value-of select="$servicename"/>(axis2_stub_t *stub, const axutil_env_t *env)
      {
         axis2_svc_client_t *svc_client = NULL;
         axutil_qname_t *svc_qname =  NULL;
         axutil_qname_t *op_qname =  NULL;
         axis2_svc_t *svc = NULL;
         axis2_op_t *op = NULL;
         axis2_op_t *annon_op = NULL;
         axis2_msg_t *msg_out = NULL;
         axis2_msg_t *msg_in = NULL;
         axis2_msg_t *msg_out_fault = NULL;
         axis2_msg_t *msg_in_fault = NULL;


         /* Modifying the Service */
         svc_client = axis2_stub_get_svc_client (stub, env );
         svc = (axis2_svc_t*)axis2_svc_client_get_svc( svc_client, env );

         annon_op = axis2_svc_get_op_with_name(svc, env, AXIS2_ANON_OUT_IN_OP);
         msg_out = axis2_op_get_msg(annon_op, env, AXIS2_MSG_OUT);
         msg_in = axis2_op_get_msg(annon_op, env, AXIS2_MSG_IN);
         msg_out_fault = axis2_op_get_msg(annon_op, env, AXIS2_MSG_OUT_FAULT);
         msg_in_fault = axis2_op_get_msg(annon_op, env, AXIS2_MSG_IN_FAULT);

         svc_qname = axutil_qname_create(env,"<xsl:value-of select="@servicename"/>" ,NULL, NULL);
         axis2_svc_set_qname (svc, env, svc_qname);

         /* creating the operations*/

         <xsl:for-each select="method">
           op_qname = axutil_qname_create(env,
                                         "<xsl:value-of select="@localpart"/>" ,
                                         "<xsl:value-of select="@namespace"/>",
                                         NULL);
           op = axis2_op_create_with_qname(env, op_qname);
           <xsl:choose>
             <xsl:when test="@mep='10'">
               axis2_op_set_msg_exchange_pattern(op, env, AXIS2_MEP_URI_IN_ONLY);
             </xsl:when>
             <xsl:otherwise>
               axis2_op_set_msg_exchange_pattern(op, env, AXIS2_MEP_URI_OUT_IN);
             </xsl:otherwise>
           </xsl:choose>
           axis2_msg_increment_ref(msg_out, env);
           axis2_msg_increment_ref(msg_in, env);
           axis2_msg_increment_ref(msg_out_fault, env);
           axis2_msg_increment_ref(msg_in_fault, env);
           axis2_op_add_msg(op, env, AXIS2_MSG_OUT, msg_out);
           axis2_op_add_msg(op, env, AXIS2_MSG_IN, msg_in);
           axis2_op_add_msg(op, env, AXIS2_MSG_OUT_FAULT, msg_out_fault);
           axis2_op_add_msg(op, env, AXIS2_MSG_IN_FAULT, msg_in_fault);
           
           axis2_svc_add_op(svc, env, op);

         </xsl:for-each>
      }

      /**
       *return end point picked from wsdl
       */
      axis2_char_t*
      axis2_stub_get_endpoint_uri_of_<xsl:value-of select="$servicename"/>( const axutil_env_t *env )
      {
        axis2_char_t *endpoint_uri = NULL;
        /* set the address from here */
        <xsl:for-each select="endpoint">
          <xsl:choose>
            <xsl:when test="position()=1">
              endpoint_uri = "<xsl:value-of select="."/>";
            </xsl:when>
           </xsl:choose>
        </xsl:for-each>
        return endpoint_uri;
      }


  <xsl:for-each select="method">
    <xsl:variable name="outputours"><xsl:value-of select="output/param/@ours"></xsl:value-of></xsl:variable>
    <xsl:variable name="outputtype">
      <xsl:choose>
        <xsl:when test="output/param/@ours">
            <xsl:choose>
                    <xsl:when test="not(@type='char' or @type='bool' or @type='date_time' or @type='duration')">adb_<xsl:value-of select="output/param/@type"/>_t*</xsl:when>
                    <xsl:when test="@type='duration' or @type='date_time' or @type='uri' or @type='qname' or @type='base64_binary'">axutil_<xsl:value-of select="@type"/>_t*</xsl:when>
                    <xsl:otherwise>axis2_<xsl:value-of select="output/param/@type"/>_t*</xsl:otherwise>
            </xsl:choose>
        </xsl:when>
        <xsl:otherwise><xsl:value-of select="output/param/@type"></xsl:value-of></xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="caps-outputtype"><xsl:value-of select="output/param/@caps-type"></xsl:value-of></xsl:variable>
    <xsl:variable name="style"><xsl:value-of select="@style"></xsl:value-of></xsl:variable>
    <xsl:variable name="soapAction"><xsl:value-of select="@soapaction"></xsl:value-of></xsl:variable>
    <xsl:variable name="mep"><xsl:value-of select="@mep"/></xsl:variable>

    <xsl:variable name="method-name"><xsl:value-of select="@name"/></xsl:variable>
    <xsl:variable name="method-ns"><xsl:value-of select="@namespace"/> </xsl:variable>

    <!-- Code generation for the in-out mep -->
    <xsl:if test="$mep='12'">
      <xsl:if test="$isSync='1'">
         /**
          * auto generated method signature
          * for "<xsl:value-of select="@qname"/>" operation.
          *<xsl:for-each select="input/param[@type!='']"><xsl:text>
          </xsl:text>* @param _<xsl:value-of select="@name"/></xsl:for-each>
          *<xsl:for-each select="output/param[@location='soap_header']"><xsl:text>
          </xsl:text>* @param dp_<xsl:value-of select="@name"/> - output header</xsl:for-each>
          * @return <xsl:value-of select="$outputtype"/>
          */
         <xsl:choose>
         <xsl:when test="$outputtype=''">void</xsl:when> <!--this case is unexpected-->
         <xsl:otherwise><xsl:value-of select="$outputtype"/></xsl:otherwise>
         </xsl:choose>
         <xsl:text> </xsl:text>
         axis2_stub_op_<xsl:value-of select="$servicename"/>_<xsl:value-of select="@name"/>( axis2_stub_t *stub, const axutil_env_t *env<xsl:for-each select="input/param[@type!='']">,
                                              <xsl:variable name="inputtype"><xsl:if test="@ours">adb_</xsl:if><xsl:value-of select="@type"/><xsl:if test="@ours">_t*</xsl:if></xsl:variable>
                                              <xsl:value-of select="$inputtype"/><xsl:text> _</xsl:text><xsl:value-of select="@name"/>
                                              </xsl:for-each><xsl:for-each select="output/param[@location='soap_header']">,
                                              <xsl:variable name="outputtype"><xsl:if test="@ours">adb_</xsl:if><xsl:value-of select="@type"/><xsl:if test="@ours">_t**</xsl:if></xsl:variable>
                                              <xsl:value-of select="$outputtype"/><xsl:text> dp_</xsl:text><xsl:value-of select="@name"/><xsl:text> /* output header double ptr*/</xsl:text>
                                              </xsl:for-each>)
         {
            axis2_svc_client_t *svc_client = NULL;
            axis2_options_t *options = NULL;
            axiom_node_t *ret_node = NULL;

            const axis2_char_t *soap_action = NULL;
            axutil_qname_t *op_qname =  NULL;
            axiom_node_t *payload = NULL;
            axis2_bool_t is_soap_act_set = AXIS2_TRUE;
            <xsl:if test="$style='doc'">
            axutil_string_t *soap_act = NULL;
            </xsl:if>    
            <xsl:if test="output/param/@ours">
           	    <!-- this means data binding is enable -->
                <xsl:value-of select="$outputtype"/> ret_val = NULL;
            </xsl:if>
            <xsl:if test="input/param[@location='soap_header']">
                axiom_node_t *input_header = NULL;
            </xsl:if>
            <xsl:if test="output/param[@location='soap_header']">
                axis2_op_client_t *op_client = NULL;
                const axis2_msg_ctx_t *msg_ctx = NULL;
                axiom_soap_envelope_t *res_soap_env = NULL;
                axiom_soap_header_t *res_soap_header = NULL;
                axiom_node_t *header_base_node = NULL;
                axiom_node_t *output_header = NULL;
            </xsl:if>
            <xsl:for-each select="output/param[@location='soap_header']">
                <xsl:variable name="outputtype"><xsl:if test="@ours">adb_</xsl:if><xsl:value-of select="@type"/><xsl:if test="@ours">_t*</xsl:if></xsl:variable>
                <xsl:value-of select="$outputtype"/><xsl:text> _</xsl:text><xsl:value-of select="@name"/> = NULL;
            </xsl:for-each>

            <!-- for service client the 1st input param is the payload -->
            <xsl:variable name="firstParam" select="input/param[1]"/>
            <xsl:if test="$firstParam/@type!=''">
               <xsl:choose>
                   <xsl:when test="$firstParam/@ours">
                       payload = adb_<xsl:value-of select="$firstParam/@type"/>_serialize(_<xsl:value-of select="$firstParam/@name"/>, env, NULL, NULL, AXIS2_TRUE, NULL, NULL);
                   </xsl:when>
                   <xsl:otherwise>
                       payload = _<xsl:value-of select="$firstParam/@name"/>;
                   </xsl:otherwise>
               </xsl:choose>
            </xsl:if>
            svc_client = axis2_stub_get_svc_client(stub, env );
            <!-- handling header params -->
           
            <!-- adding input headers -->
            <xsl:for-each select="input/param[@location='soap_header']">
                <xsl:choose>
                <xsl:when test="@ours">
                input_header = adb_<xsl:value-of select="@type"/>_serialize(_<xsl:value-of select="@name"/>, env, NULL, NULL, AXIS2_TRUE, NULL, NULL);
                </xsl:when>
                <xsl:otherwise>
                input_header = _<xsl:value-of select="@name"/>;
                </xsl:otherwise>
                </xsl:choose>
                axis2_svc_client_add_header(svc_client, env, input_header);
            </xsl:for-each>

            options = axis2_stub_get_options( stub, env);
            if (NULL == options)
            {
                AXIS2_ERROR_SET(env->error, AXIS2_ERROR_INVALID_NULL_PARAM, AXIS2_FAILURE);
                AXIS2_LOG_ERROR(env->log, AXIS2_LOG_SI, "options is null in stub");
                return NULL;
            }
            soap_action = axis2_options_get_action( options, env );
            if (NULL == soap_action)
            {
              is_soap_act_set = AXIS2_FALSE;
              soap_action = "<xsl:value-of select="$soapAction"/>";
              <xsl:if test="$style='doc'">
              soap_act = axutil_string_create(env, "<xsl:value-of select="$soapAction"/>");
              axis2_options_set_soap_action(options, env, soap_act);    
              </xsl:if>
              axis2_options_set_action( options, env, soap_action );
            }
            <xsl:if test="$soapVersion='1.2'">
            axis2_options_set_soap_version(options, env, AXIOM_SOAP12 );
            </xsl:if>
            <xsl:if test="$soapVersion!='1.1'">
            axis2_options_set_soap_version(options, env, AXIOM_SOAP11 );
            </xsl:if>
            ret_node =  axis2_svc_client_send_receive_with_op_qname( svc_client, env, op_qname, payload);
 
            if (!is_soap_act_set)
            {
              <xsl:if test="$style='doc'">
              axis2_options_set_soap_action(options, env, NULL);    
              </xsl:if>
              axis2_options_set_action( options, env, NULL);
            }

            <!-- extract out the headers at this point -->
            <xsl:if test="output/param[@location='soap_header']">
                op_client = axis2_svc_client_get_op_client(svc_client, env);
                if(!op_client)
                {
                    AXIS2_LOG_ERROR( env->log, AXIS2_LOG_SI, "op client is NULL");
                    return NULL;
                }
                msg_ctx = axis2_op_client_get_msg_ctx(op_client, env, AXIS2_WSDL_MESSAGE_LABEL_OUT);
                if(!msg_ctx)
                {
                    AXIS2_LOG_ERROR( env->log, AXIS2_LOG_SI, "response msg ctx is NULL");
                    return NULL;
                }
                res_soap_env = axis2_msg_ctx_get_response_soap_envelope(msg_ctx, env);
                if(!res_soap_env)
                {
                    AXIS2_LOG_ERROR( env->log, AXIS2_LOG_SI, "response evelope is NULL");
                    return NULL;
                }
                res_soap_header = axiom_soap_envelope_get_header(res_soap_env, env);

                if(res_soap_header)
                {
                    header_base_node = axiom_soap_header_get_base_node(res_soap_header, env);
                }

                if(!header_base_node)
                {
                    AXIS2_LOG_ERROR( env->log, AXIS2_LOG_SI, "Required response header is NULL");
                }
                
                <xsl:for-each select="output/param[@location='soap_header']">
                <xsl:choose>
                <xsl:when test="position()=1">
                    output_header = axiom_node_get_first_child(header_base_node, env);

                    while(output_header &amp;&amp; axiom_node_get_node_type(output_header, env) != AXIOM_ELEMENT)
                    {
                        output_header = axiom_node_get_next_sibling(output_header, env);
                    }
                </xsl:when>
                <xsl:otherwise>
                    output_header = axiom_node_get_next_sibling(output_header, env);

                    while(output_header &amp;&amp; axiom_node_get_node_type(output_header, env) != AXIOM_ELEMENT)
                    {
                        output_header = axiom_node_get_next_sibling(output_header, env);
                    }
                </xsl:otherwise>
                </xsl:choose>

                <xsl:variable name="header_var"><xsl:text>_</xsl:text><xsl:value-of select="@name"/></xsl:variable>

                if( NULL == output_header)
                {
                    AXIS2_LOG_ERROR( env->log, AXIS2_LOG_SI, "Response header <xsl:value-of select="@name"/> is NULL");
                    /* you can't have a response header NULL, just free things and exit */
                    axis2_stub_op_<xsl:value-of select="$servicename"/>_<xsl:value-of select="$method-name"/>_free_output_headers(env, <xsl:for-each select="../../output/param[@location='soap_header']"><xsl:if test="position()!=1">,</xsl:if>
                                                 <xsl:text> _</xsl:text><xsl:value-of select="@name"/>
                                                 </xsl:for-each>);
                    <xsl:choose>
                    <xsl:when test="$outputtype=''">
                      return;
                    </xsl:when>
                    <xsl:otherwise>
                      return NULL;
                    </xsl:otherwise>
                    </xsl:choose>
                }
                /* you can have these parameters NULL, to avoid deserializing them */
                if(dp_<xsl:value-of select="@name"/>)
                {
                <xsl:choose>
                <xsl:when test="@ours">
                    <xsl:value-of select="$header_var"/> = adb_<xsl:value-of select="@type"/>_create(env);

                    if(adb_<xsl:value-of select="@type"/>_deserialize(<xsl:value-of select="$header_var"/>, env, &amp;output_header, NULL, AXIS2_FALSE ) == AXIS2_FAILURE)
                    {
                        <!-- this too will be freed from the _free_output_headers 
                        if(<xsl:value-of select="$header_var"/> != NULL)
                        {
                            adb_<xsl:value-of select="@type"/>_free(<xsl:value-of select="$header_var"/>, env);
                        }-->
                        AXIS2_LOG_ERROR( env->log, AXIS2_LOG_SI, "NULL returnted from the <xsl:value-of select="@type"/>_deserialize: "
                                                                "This should be due to an invalid output header");
                        axis2_stub_op_<xsl:value-of select="$servicename"/>_<xsl:value-of select="$method-name"/>_free_output_headers(env, <xsl:for-each select="../../output/param[@location='soap_header']"><xsl:if test="position()!=1">,</xsl:if>
                                                     <xsl:text> _</xsl:text><xsl:value-of select="@name"/>
                                                     </xsl:for-each>);
                        <xsl:choose>
                        <xsl:when test="$outputtype=''">
                          return;
                        </xsl:when>
                        <xsl:otherwise>
                          return NULL;
                        </xsl:otherwise>
                        </xsl:choose>
                    }
                </xsl:when>
                <xsl:otherwise>
                    _<xsl:value-of select="@name"/> = input_header;
                </xsl:otherwise>
                </xsl:choose>
                    *dp_<xsl:value-of select="@name"/> = _<xsl:value-of select="@name"/>;
                }

                <!-- just handle next type-->
                </xsl:for-each>
            </xsl:if>

            <xsl:choose>
                <xsl:when test="$outputtype=''">
                    return;
                </xsl:when>
                <xsl:when test="output/param/@ours">
                    if ( NULL == ret_node )
                    {
                        return NULL;
                    }
                    ret_val = adb_<xsl:value-of select="output/param/@type"/>_create(env);

                    if(adb_<xsl:value-of select="output/param/@type"/>_deserialize(ret_val, env, &amp;ret_node, NULL, AXIS2_FALSE ) == AXIS2_FAILURE)
                    {
                        if(ret_val != NULL)
                        {
                            adb_<xsl:value-of select="output/param/@type"/>_free(ret_val, env);
                        }

                        AXIS2_LOG_ERROR( env->log, AXIS2_LOG_SI, "NULL returnted from the <xsl:value-of select="output/param/@type"/>_deserialize: "
                                                                "This should be due to an invalid XML");
                        return NULL;
                    }
                    return ret_val;
                </xsl:when>
                <xsl:otherwise>
                    return ret_node;
                </xsl:otherwise>
            </xsl:choose>
        }
        </xsl:if>  <!--close for  test="$isSync='1'-->
       </xsl:if> <!-- close for  test="$mep='http://www.w3.org/2004/08/wsdl/in-out' -->

  </xsl:for-each>

  <xsl:for-each select="method">
    <xsl:variable name="outputours"><xsl:value-of select="output/param/@ours"></xsl:value-of></xsl:variable>
    <xsl:variable name="outputtype">
      <xsl:choose>
        <xsl:when test="output/param/@ours">
            <xsl:choose>
                    <xsl:when test="not(@type='char' or @type='bool' or @type='date_time' or @type='duration')">adb_<xsl:value-of select="output/param/@type"/>_t*</xsl:when>
                    <xsl:when test="@type='duration' or @type='date_time' or @type='uri' or @type='qname' or @type='base64_binary'">axutil_<xsl:value-of select="@type"/>_t*</xsl:when>
                    <xsl:otherwise>axis2_<xsl:value-of select="output/param/@type"/>_t*</xsl:otherwise>
                </xsl:choose>
            
                </xsl:when>
        <xsl:otherwise><xsl:value-of select="output/param/@type"></xsl:value-of></xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="caps-outputtype"><xsl:value-of select="output/param/@caps-type"></xsl:value-of></xsl:variable>
    <xsl:variable name="style"><xsl:value-of select="@style"></xsl:value-of></xsl:variable>
    <xsl:variable name="soapAction"><xsl:value-of select="@soapaction"></xsl:value-of></xsl:variable>
    <xsl:variable name="mep"><xsl:value-of select="@mep"/></xsl:variable>

    <xsl:variable name="method-name"><xsl:value-of select="@name"/></xsl:variable>
    <xsl:variable name="method-ns"><xsl:value-of select="@namespace"/> </xsl:variable>


     <xsl:if test="$mep='12'">
        <!-- Async method generation -->
        <xsl:if test="$isAsync='1'">

        struct axis2_stub_<xsl:value-of select="$servicename"/>_<xsl:value-of select="@name"/>_callback_data
        {   
            void *data;
            axis2_status_t ( AXIS2_CALL *on_complete ) (const axutil_env_t *, <xsl:value-of select="$outputtype"/><xsl:text> _</xsl:text><xsl:value-of select="output/param/@name"/><xsl:for-each select="output/param[@location='soap_header']">,
                                                      <xsl:variable name="header_outputtype"><xsl:if test="@ours">adb_</xsl:if><xsl:value-of select="@type"/><xsl:if test="@ours">_t*</xsl:if></xsl:variable>
                                                      <xsl:value-of select="$header_outputtype"/><xsl:text> </xsl:text><xsl:value-of select="@name"/>
                                                      </xsl:for-each>, void *data);
            axis2_status_t ( AXIS2_CALL *on_error ) (const axutil_env_t *, int exception, void *data);
        };

        static axis2_status_t AXIS2_CALL axis2_stub_on_error_<xsl:value-of select="$servicename"/>_<xsl:value-of select="@name"/>(axis2_callback_t *callback, const axutil_env_t *env, int exception)
        {
            axis2_status_t ( AXIS2_CALL *on_error ) (const axutil_env_t *, int, void *data);
            struct axis2_stub_<xsl:value-of select="$servicename"/>_<xsl:value-of select="@name"/>_callback_data* callback_data = NULL;
            void *user_data = NULL;

            axis2_status_t status;
        
            callback_data = (struct axis2_stub_<xsl:value-of select="$servicename"/>_<xsl:value-of select="@name"/>_callback_data*)axis2_callback_get_data(callback);
        
            user_data = callback_data->data;
            on_error = callback_data->on_error;
        
            status = on_error(env, exception, user_data);

            if(callback_data)
            {
                AXIS2_FREE(env->allocator, callback_data);
            }
            return status;
        } 

        axis2_status_t AXIS2_CALL axis2_stub_on_complete_<xsl:value-of select="$servicename"/>_<xsl:value-of select="@name"/>(axis2_callback_t *callback, const axutil_env_t *env)
        {
            axis2_status_t ( AXIS2_CALL *on_complete ) (const axutil_env_t *, <xsl:value-of select="$outputtype"/><xsl:text> _</xsl:text><xsl:value-of select="output/param/@name"/><xsl:for-each select="output/param[@location='soap_header']">,
                                                      <xsl:variable name="header_outputtype"><xsl:if test="@ours">adb_</xsl:if><xsl:value-of select="@type"/><xsl:if test="@ours">_t*</xsl:if></xsl:variable>
                                                      <xsl:value-of select="$header_outputtype"/><xsl:text> </xsl:text><xsl:value-of select="@name"/>
                                                      </xsl:for-each>, void *data);
            struct axis2_stub_<xsl:value-of select="$servicename"/>_<xsl:value-of select="@name"/>_callback_data* callback_data = NULL;
            void *user_data = NULL;
            axis2_status_t status = AXIS2_SUCCESS;
 
            <xsl:if test="output/param/@ours">
           	    <!-- this means data binding is enable -->
                <xsl:value-of select="$outputtype"/> ret_val = NULL;
            </xsl:if>

            axiom_node_t *ret_node = NULL;
            axiom_soap_envelope_t *soap_envelope = NULL;

            <xsl:if test="output/param[@location='soap_header']">
                axiom_soap_header_t *res_soap_header = NULL;
                axiom_node_t *header_base_node = NULL;
                axiom_node_t *output_header = NULL;
            </xsl:if>
            <xsl:for-each select="output/param[@location='soap_header']">
                <xsl:variable name="outputtype"><xsl:if test="@ours">adb_</xsl:if><xsl:value-of select="@type"/><xsl:if test="@ours">_t*</xsl:if></xsl:variable>
                <xsl:value-of select="$outputtype"/><xsl:text> _</xsl:text><xsl:value-of select="@name"/> = NULL;
            </xsl:for-each>

            callback_data = (struct axis2_stub_<xsl:value-of select="$servicename"/>_<xsl:value-of select="@name"/>_callback_data*)axis2_callback_get_data(callback);

            soap_envelope = axis2_callback_get_envelope(callback, env);
            if(soap_envelope)
            {
                axiom_soap_body_t *soap_body;
                soap_body = axiom_soap_envelope_get_body(soap_envelope, env);
                if(soap_body)
                {
                    axiom_node_t *body_node = axiom_soap_body_get_base_node(soap_body, env);
                    if(body_node)
                    {
                        ret_node = axiom_node_get_first_child(body_node, env);
                    }
                }
                <!-- extract out the headers at this point -->
                <xsl:if test="output/param[@location='soap_header']">
                    res_soap_header = axiom_soap_envelope_get_header(soap_envelope, env);

                    if(res_soap_header)
                    {
                        header_base_node = axiom_soap_header_get_base_node(res_soap_header, env);
                    }

                    if(!header_base_node)
                    {
                        AXIS2_LOG_ERROR( env->log, AXIS2_LOG_SI, "Required response header is NULL");
                    }
                    if(header_base_node)
                    {
                    <xsl:for-each select="output/param[@location='soap_header']">
                    if(status == AXIS2_SUCCESS)
                    {
                    <xsl:choose>
                    <xsl:when test="position()=1">
                        output_header = axiom_node_get_first_child(header_base_node, env);

                        while(output_header &amp;&amp; axiom_node_get_node_type(output_header, env) != AXIOM_ELEMENT)
                        {
                            output_header = axiom_node_get_next_sibling(output_header, env);
                        }
                    </xsl:when>
                    <xsl:otherwise>
                        output_header = axiom_node_get_next_sibling(output_header, env);

                        while(output_header &amp;&amp; axiom_node_get_node_type(output_header, env) != AXIOM_ELEMENT)
                        {
                            output_header = axiom_node_get_next_sibling(output_header, env);
                        }
                    </xsl:otherwise>
                    </xsl:choose>

                    <xsl:variable name="header_var"><xsl:text>_</xsl:text><xsl:value-of select="@name"/></xsl:variable>

                    if( NULL == output_header)
                    {
                        AXIS2_LOG_ERROR( env->log, AXIS2_LOG_SI, "Response header <xsl:value-of select="@name"/> is NULL");
                        /* you can't have a response header NULL, just free things and exit */
                        axis2_stub_op_<xsl:value-of select="$servicename"/>_<xsl:value-of select="$method-name"/>_free_output_headers(env, <xsl:for-each select="../../output/param[@location='soap_header']"><xsl:if test="position()!=1">,</xsl:if>
                                                     <xsl:text> _</xsl:text><xsl:value-of select="@name"/>
                                                     </xsl:for-each>);
                        status = AXIS2_FAILURE;
                    }
                    else
                    {
                    <xsl:choose>
                    <xsl:when test="@ours">
                        <xsl:value-of select="$header_var"/> = adb_<xsl:value-of select="@type"/>_create(env);

                        if(adb_<xsl:value-of select="@type"/>_deserialize(<xsl:value-of select="$header_var"/>, env, &amp;output_header, NULL, AXIS2_FALSE ) == AXIS2_FAILURE)
                        {
                            <!-- this too will be freed from the _free_output_headers function
                            if(<xsl:value-of select="$header_var"/> != NULL)
                            {
                                adb_<xsl:value-of select="@type"/>_free(<xsl:value-of select="$header_var"/>, env);
                            }-->
                            AXIS2_LOG_ERROR( env->log, AXIS2_LOG_SI, "NULL returnted from the <xsl:value-of select="@type"/>_deserialize: "
                                                                    "This should be due to an invalid output header");
                            axis2_stub_op_<xsl:value-of select="$servicename"/>_<xsl:value-of select="$method-name"/>_free_output_headers(env, <xsl:for-each select="../../output/param[@location='soap_header']"><xsl:if test="position()!=1">,</xsl:if>
                                                         <xsl:text> _</xsl:text><xsl:value-of select="@name"/>
                                                         </xsl:for-each>);
                            status = AXIS2_FAILURE;        
                        }
                    </xsl:when>
                    <xsl:otherwise>
                        _<xsl:value-of select="@name"/> = output_header;
                    </xsl:otherwise>
                    </xsl:choose>
                    }
                    }
                    </xsl:for-each> <!-- for each output header param -->

                    <!-- free them if anything goes wrong -->
                    <xsl:for-each select="output/param[@location='soap_header']">
                        if(status == AXIS2_FAILURE)
                        {
                            <xsl:text>_</xsl:text><xsl:value-of select="@name"/> = NULL;
                        }
                    </xsl:for-each>
                    }
                </xsl:if>
            }

            user_data = callback_data->data;
            on_complete = callback_data->on_complete;

            <xsl:choose>
                <xsl:when test="output/param/@ours">
                    if(ret_node != NULL)
                    {
                        ret_val = adb_<xsl:value-of select="output/param/@type"/>_create(env);
     
                        if(adb_<xsl:value-of select="output/param/@type"/>_deserialize(ret_val, env, &amp;ret_node, NULL, AXIS2_FALSE ) == AXIS2_FAILURE)
                        {
                            AXIS2_LOG_ERROR( env->log, AXIS2_LOG_SI, "NULL returnted from the LendResponse_deserialize: "
                                                                    "This should be due to an invalid XML");
                            adb_<xsl:value-of select="output/param/@type"/>_free(ret_val, env);
                            ret_val = NULL;
                        }
                     }
                     else
                     {
                         ret_val = NULL; 
                     }
                     status = on_complete(env, ret_val<xsl:for-each select="output/param[@location='soap_header']">,
                                              <xsl:text>_</xsl:text><xsl:value-of select="@name"/>
                                              </xsl:for-each>, user_data);
                </xsl:when>
                <xsl:otherwise>
                     status = on_complete(env, ret_node<xsl:for-each select="output/param[@location='soap_header']">,
                                              <xsl:text>_</xsl:text><xsl:value-of select="@name"/>
                                              </xsl:for-each>, user_data);
                </xsl:otherwise>
            </xsl:choose>
 
            if(callback_data)
            {
                AXIS2_FREE(env->allocator, callback_data);
            }
            return status;
        }

        /**
          * auto generated method signature for asynchronous invocations
          * for "<xsl:value-of select="@qname"/>" operation.
          <!--  select only the body parameters  -->
          *<xsl:for-each select="input/param[@type!='']"><xsl:text>
          </xsl:text>* @param _<xsl:value-of select="@name"/></xsl:for-each>
          * @param on_complete callback to handle on complete
          * @param on_error callback to handle on error
          */
         void axis2_stub_start_op_<xsl:value-of select="$servicename"/>_<xsl:value-of select="@name"/>( axis2_stub_t *stub, const axutil_env_t *env<xsl:for-each select="input/param[@type!='']">,
                                                    <xsl:variable name="inputtype"><xsl:if test="@ours">adb_</xsl:if><xsl:value-of select="@type"/><xsl:if test="@ours">_t*</xsl:if></xsl:variable>
                                                    <xsl:value-of select="$inputtype"/><xsl:text> _</xsl:text><xsl:value-of select="@name"/>
                                                  </xsl:for-each>,
                                                  void *user_data,
                                                  axis2_status_t ( AXIS2_CALL *on_complete ) (const axutil_env_t *, <xsl:value-of select="$outputtype"/><xsl:text> _</xsl:text><xsl:value-of select="output/param/@name"/><xsl:for-each select="output/param[@location='soap_header']">,
                                                      <xsl:variable name="header_outputtype"><xsl:if test="@ours">adb_</xsl:if><xsl:value-of select="@type"/><xsl:if test="@ours">_t*</xsl:if></xsl:variable>
                                                      <xsl:value-of select="$header_outputtype"/><xsl:text> </xsl:text><xsl:value-of select="@name"/>
                                                      </xsl:for-each>, void *data) ,
                                                  axis2_status_t ( AXIS2_CALL *on_error ) (const axutil_env_t *, int exception, void *data) )
         {

            axis2_callback_t *callback = NULL;

            axis2_svc_client_t *svc_client = NULL;
            axis2_options_t *options = NULL;

            const axis2_char_t *soap_action = NULL;
            axiom_node_t *payload = NULL;

            axis2_bool_t is_soap_act_set = AXIS2_TRUE;
            <xsl:if test="$style='doc'">
            axutil_string_t *soap_act = NULL;
            </xsl:if>
            
            struct axis2_stub_<xsl:value-of select="$servicename"/>_<xsl:value-of select="@name"/>_callback_data *callback_data;

            callback_data = (struct axis2_stub_<xsl:value-of select="$servicename"/>_<xsl:value-of select="@name"/>_callback_data*) AXIS2_MALLOC(env->allocator, 
                                    sizeof(struct axis2_stub_<xsl:value-of select="$servicename"/>_<xsl:value-of select="@name"/>_callback_data));
            if(NULL == callback_data)
            {
                AXIS2_LOG_ERROR( env->log, AXIS2_LOG_SI, "Can not allocate memeory for the callback data structures");
                return;
            }
            <!-- for service client currently suppported only 1 input param -->
            <xsl:variable name="firstParam" select="input/param[1]"/>
            <xsl:if test="$firstParam/@type!=''">
               <xsl:choose>
                   <xsl:when test="$firstParam/@ours">
                       payload = adb_<xsl:value-of select="$firstParam/@type"/>_serialize(_<xsl:value-of select="$firstParam/@name"/>, env, NULL, NULL, AXIS2_TRUE, NULL, NULL);
                   </xsl:when>
                   <xsl:otherwise>
                       payload = _<xsl:value-of select="$firstParam/@name"/>;
                   </xsl:otherwise>
               </xsl:choose>
            </xsl:if>



            options = axis2_stub_get_options( stub, env);
            if (NULL == options)
            {
              AXIS2_ERROR_SET(env->error, AXIS2_ERROR_INVALID_NULL_PARAM, AXIS2_FAILURE);
              AXIS2_LOG_ERROR( env->log, AXIS2_LOG_SI, "options is null in stub");
              return;
            }
            svc_client = axis2_stub_get_svc_client (stub, env);
            soap_action =axis2_options_get_action (options, env);
            if (NULL == soap_action)
            {
              is_soap_act_set = AXIS2_FALSE;
              soap_action = "<xsl:value-of select="$soapAction"/>";
              <xsl:if test="$style='doc'">
              soap_act = axutil_string_create(env, "<xsl:value-of select="$soapAction"/>");
              axis2_options_set_soap_action(options, env, soap_act);
              </xsl:if>
              axis2_options_set_action( options, env, soap_action);
            }
            <xsl:choose>
             <xsl:when test="$soapVersion='1.2'">
            axis2_options_set_soap_version(options, env, AXIOM_SOAP12);
             </xsl:when>
             <xsl:otherwise>
            axis2_options_set_soap_version(options, env, AXIOM_SOAP11);
             </xsl:otherwise>
            </xsl:choose>

            callback = axis2_callback_create(env);
            /* Set our on_complete fucntion pointer to the callback object */
            axis2_callback_set_on_complete(callback, axis2_stub_on_complete_<xsl:value-of select="$servicename"/>_<xsl:value-of select="@name"/>);
            /* Set our on_error function pointer to the callback object */
            axis2_callback_set_on_error(callback, axis2_stub_on_error_<xsl:value-of select="$servicename"/>_<xsl:value-of select="@name"/>);

            callback_data-> data = user_data;
            callback_data-> on_complete = on_complete;
            callback_data-> on_error = on_error;

            axis2_callback_set_data(callback, (void*)callback_data);

            /* Send request */
            axis2_svc_client_send_receive_non_blocking(svc_client, env, payload, callback);
            
            if (!is_soap_act_set)
            {
              <xsl:if test="$style='doc'">
              axis2_options_set_soap_action(options, env, NULL);
              </xsl:if>
              axis2_options_set_action(options, env, NULL);
            }
         }

         </xsl:if>  <!--close for  test="$isASync='1'-->
       <!-- End of in-out mep -->
       </xsl:if> <!-- close for  test="$mep='http://www.w3.org/2004/08/wsdl/in-out' -->

       <xsl:if test="$mep='10'">
         /**
          * auto generated method signature for in only mep invocations
          * for "<xsl:value-of select="@qname"/>" operation.
          <!--  select only the body parameters  -->
          <xsl:for-each select="input/param[@type!='']">* @param _<xsl:value-of select="@name"></xsl:value-of></xsl:for-each>
          * @param on_complete callback to handle on complete
          * @param on_error callback to handle on error
          */
         axis2_status_t
         axis2_stub_op_<xsl:value-of select="$servicename"/>_<xsl:value-of select="@name"/>( axis2_stub_t *stub, const axutil_env_t *env <xsl:for-each select="input/param[@type!='']"> ,
                                                 <xsl:variable name="inputtype"><xsl:if test="@ours">adb_</xsl:if><xsl:value-of select="@type"/><xsl:if test="@ours">_t*</xsl:if></xsl:variable>
                                                 <xsl:value-of select="$inputtype"/><xsl:text> _</xsl:text><xsl:value-of select="@name"/>
                                              </xsl:for-each>)
         {
            axis2_status_t status;

            axis2_svc_client_t *svc_client = NULL;
            axis2_options_t *options = NULL;

            const axis2_char_t *soap_action = NULL;
            axutil_qname_t *op_qname =  NULL;
            axiom_node_t *payload = NULL;
            <xsl:if test="input/param[@location='soap_header']">
                axiom_node_t *input_header = NULL;
            </xsl:if>

            <!-- for service client currently suppported only 1 input param -->
            <xsl:variable name="firstParam" select="input/param[1]"/>
            <xsl:if test="$firstParam/@type!=''">
               <xsl:choose>
                   <xsl:when test="$firstParam/@ours">
                       payload = adb_<xsl:value-of select="$firstParam/@type"/>_serialize(_<xsl:value-of select="$firstParam/@name"/>, env, NULL, NULL, AXIS2_TRUE, NULL, NULL);
                   </xsl:when>
                   <xsl:otherwise>
                       payload = _<xsl:value-of select="$firstParam/@name"/>;
                   </xsl:otherwise>
               </xsl:choose>
            </xsl:if>

            <!-- adding input headers -->
            <xsl:for-each select="input/param[@location='soap_header']">
                <xsl:choose>
                <xsl:when test="@ours">
                input_header = adb_<xsl:value-of select="@type"/>_serialize(_<xsl:value-of select="@name"/>, env, NULL, NULL, AXIS2_TRUE, NULL, NULL);
                </xsl:when>
                <xsl:otherwise>
                input_header = _<xsl:value-of select="@name"/>;
                </xsl:otherwise>
                </xsl:choose>
                axis2_svc_client_add_header(svc_client, env, input_header);
            </xsl:for-each>


            options = axis2_stub_get_options( stub, env);
            if ( NULL == options )
            { 
              AXIS2_ERROR_SET(env->error, AXIS2_ERROR_INVALID_NULL_PARAM, AXIS2_FAILURE);
              AXIS2_LOG_ERROR(env->log, AXIS2_LOG_SI, "options is null in stub");
              return AXIS2_FAILURE;
            }
            svc_client = axis2_stub_get_svc_client (stub, env );
            soap_action = axis2_options_get_action ( options, env );
            if ( NULL == soap_action )
            {
              soap_action = "<xsl:value-of select="$soapAction"/>";
              axis2_options_set_action( options, env, soap_action );
            }
            <xsl:choose>
             <xsl:when test="$soapVersion='1.2'">
            axis2_options_set_soap_version(options, env, AXIOM_SOAP12 );
             </xsl:when>
             <xsl:otherwise>
            axis2_options_set_soap_version(options, env, AXIOM_SOAP11 );
             </xsl:otherwise>
            </xsl:choose>
            op_qname = axutil_qname_create(env,
                                        "<xsl:value-of select="@localpart"/>" ,
                                        "<xsl:value-of select="@namespace"/>",
                                        NULL);
            status =  axis2_svc_client_send_robust_with_op_qname( svc_client, env, op_qname, payload);
            return status;

        }
       </xsl:if> <!-- close for  test="$mep='http://www.w3.org/2004/08/wsdl/in-only' -->
     </xsl:for-each>   <!-- close of for-each select = "method" -->


     /**
      * function to free any soap input headers 
      */
     <xsl:for-each select="method">
        <xsl:if test="input/param[@location='soap_header']">
         void
         axis2_stub_op_<xsl:value-of select="$servicename"/>_<xsl:value-of select="@name"/>_free_input_headers(const axutil_env_t *env, <xsl:for-each select="input/param[@location='soap_header']"><xsl:if test="position()!=1">,</xsl:if>
                                                 <xsl:variable name="inputtype"><xsl:if test="@ours">adb_</xsl:if><xsl:value-of select="@type"/><xsl:if test="@ours">_t*</xsl:if></xsl:variable>
                                                 <xsl:value-of select="$inputtype"/><xsl:text> _</xsl:text><xsl:value-of select="@name"/>
                                                 </xsl:for-each>)
         {
            <xsl:for-each select="input/param[@location='soap_header']">
               <xsl:variable name="header_var"><xsl:text>_</xsl:text><xsl:value-of select="@name"/></xsl:variable>
               <xsl:choose>
                <xsl:when test="@ours">
                    if(<xsl:value-of select="$header_var"/>)
                    {
                        adb_<xsl:value-of select="@type"/>_free(<xsl:value-of select="$header_var"/>, env);
                    }
                </xsl:when>
                <xsl:otherwise>
                    /* we don't have anything to free on <xsl:value-of select="$header_var"/> */
                </xsl:otherwise>
               </xsl:choose>
            </xsl:for-each>
         }
        </xsl:if>
     </xsl:for-each>


     /**
      * function to free any soap output headers 
      */
     <xsl:for-each select="method">
        <xsl:if test="output/param[@location='soap_header']">
         void
         axis2_stub_op_<xsl:value-of select="$servicename"/>_<xsl:value-of select="@name"/>_free_output_headers(const axutil_env_t *env, <xsl:for-each select="output/param[@location='soap_header']"><xsl:if test="position()!=1">,</xsl:if>
                                                 <xsl:variable name="outputtype"><xsl:if test="@ours">adb_</xsl:if><xsl:value-of select="@type"/><xsl:if test="@ours">_t*</xsl:if></xsl:variable>
                                                 <xsl:value-of select="$outputtype"/><xsl:text> _</xsl:text><xsl:value-of select="@name"/>
                                                 </xsl:for-each>)
         {
            <xsl:for-each select="output/param[@location='soap_header']">
               <xsl:variable name="header_var"><xsl:text>_</xsl:text><xsl:value-of select="@name"/></xsl:variable>
               <xsl:choose>
                <xsl:when test="@ours">
                    if(<xsl:value-of select="$header_var"/>)
                    {
                        adb_<xsl:value-of select="@type"/>_free(<xsl:value-of select="$header_var"/>, env);
                    }
                </xsl:when>
                <xsl:otherwise>
                    /* we don't have anything to free on <xsl:value-of select="$header_var"/> */
                </xsl:otherwise>
               </xsl:choose>
            </xsl:for-each>
         }
        </xsl:if>
     </xsl:for-each>

   </xsl:template>
</xsl:stylesheet>
