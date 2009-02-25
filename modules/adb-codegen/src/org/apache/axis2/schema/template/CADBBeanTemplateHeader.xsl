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

     <!-- cater for the multiple classes - wrappped mode -->
    <xsl:template match="/beans">
        <xsl:variable name="name"><xsl:value-of select="@name"/></xsl:variable>
        <xsl:variable name="axis2_name">adb_<xsl:value-of select="@name"/></xsl:variable>
        <xsl:variable name="caps_axis2_name">ADB_<xsl:value-of select="@caps-name"/></xsl:variable>
        #ifndef <xsl:value-of select="$caps_axis2_name"/>_H
        #define <xsl:value-of select="$caps_axis2_name"/>_H

        /**
        * <xsl:value-of select="$axis2_name"/>.h
        *
        * This file was auto-generated from WSDL
        * by the Apache Axis2/Java version: #axisVersion# #today#
        */

        #include &lt;stdio.h&gt;
        #include &lt;axiom.h&gt;
        #include &lt;axis2_util.h&gt;
        #include &lt;axiom_soap.h&gt;
        #include &lt;axis2_client.h&gt;

        #ifdef __cplusplus
        extern "C"
        {
        #endif

        #define ADB_DEFAULT_DIGIT_LIMIT 64
        #define ADB_DEFAULT_NAMESPACE_PREFIX_LIMIT 64
        <xsl:if test="itemtype">
        #define ADB_DEFAULT_LIST_SEPERATOR " "
        </xsl:if>

        /**
        *  <xsl:value-of select="$axis2_name"/> wrapped class classes ( structure for C )
        */

        <xsl:apply-templates/>


        #ifdef __cplusplus
        }
        #endif

        #endif /* <xsl:value-of select="$caps_axis2_name"/>_H */
    </xsl:template>

    <!--cater for the multiple classes - unwrappped mode -->
    <xsl:template match="/">
        <xsl:apply-templates/>
    </xsl:template>


    <xsl:template match="class">
        <xsl:variable name="name"><xsl:value-of select="@name"/></xsl:variable>
        <xsl:variable name="axis2_name">adb_<xsl:value-of select="@name"/></xsl:variable>
        <xsl:variable name="caps_axis2_name">ADB_<xsl:value-of select="@caps-name"/></xsl:variable>

        #ifndef <xsl:value-of select="$caps_axis2_name"/>_H
        #define <xsl:value-of select="$caps_axis2_name"/>_H

       /**
        * <xsl:value-of select="$axis2_name"/>.h
        *
        * This file was auto-generated from WSDL
        * by the Apache Axis2/Java version: #axisVersion# #today#
        */

       /**
        *  <xsl:value-of select="$axis2_name"/> class
        */
        typedef struct <xsl:value-of select="$axis2_name"/><xsl:text> </xsl:text><xsl:value-of select="$axis2_name"/>_t;

        <xsl:for-each select="property">
          <xsl:if test="@ours">
          <xsl:variable name="propertyType"><xsl:if test="@ours">adb_</xsl:if><xsl:value-of select="@type"></xsl:value-of></xsl:variable>
          #include "<xsl:value-of select="$propertyType"/>.h"
          </xsl:if>
        </xsl:for-each>
        <!--include special headers-->
        <xsl:for-each select="property[@type='axutil_date_time_t*']">
          <xsl:if test="position()=1">
            #include &lt;axutil_date_time.h&gt;
          </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="property[@type='axutil_base64_binary_t*']">
          <xsl:if test="position()=1">
            #include &lt;axutil_base64_binary.h&gt;
          </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="property[@type='axutil_duration_t*']">
          <xsl:if test="position()=1">
            #include &lt;axutil_duration.h&gt;
          </xsl:if>
        </xsl:for-each>

        #include &lt;stdio.h&gt;
        #include &lt;axiom.h&gt;
        #include &lt;axis2_util.h&gt;
        #include &lt;axiom_soap.h&gt;
        #include &lt;axis2_client.h&gt;

        #ifdef __cplusplus
        extern "C"
        {
        #endif

        #define ADB_DEFAULT_DIGIT_LIMIT 64
        #define ADB_DEFAULT_NAMESPACE_PREFIX_LIMIT 64
        <xsl:if test="itemtype">
        #define ADB_DEFAULT_LIST_SEPERATOR " "
        </xsl:if>

        /******************************* Create and Free functions *********************************/

        /**
         * Constructor for creating <xsl:value-of select="$axis2_name"/>_t
         * @param env pointer to environment struct
         * @return newly created <xsl:value-of select="$axis2_name"/>_t object
         */
        <xsl:value-of select="$axis2_name"/>_t* AXIS2_CALL
        <xsl:value-of select="$axis2_name"/>_create(
            const axutil_env_t *env );

        /**
         * Free <xsl:value-of select="$axis2_name"/>_t object
         * @param <xsl:text> _</xsl:text><xsl:value-of select="$name"/> <xsl:text> </xsl:text><xsl:value-of select="$axis2_name"/>_t object to free
         * @param env pointer to environment struct
         * @return AXIS2_SUCCESS on success, else AXIS2_FAILURE
         */
        axis2_status_t AXIS2_CALL
        <xsl:value-of select="$axis2_name"/>_free (
            <xsl:value-of select="$axis2_name"/>_t*<xsl:text> _</xsl:text><xsl:value-of select="$name"/>,
            const axutil_env_t *env);



        /********************************** Getters and Setters **************************************/
        <xsl:if test="count(property[@array])!=0">/******** Deprecated for array types, Use 'Getters and Setters for Arrays' instead ***********/</xsl:if>
        <xsl:if test="@choice">/******** In a case of a choose among elements, the last one to set will be chooosen *********/</xsl:if>
        <xsl:if test="@list">/******* This is a list, please use Getters and 'Setters for Array' Instead of following *****/</xsl:if>

        <xsl:for-each select="property">
            <xsl:variable name="propertyType">
            <xsl:choose>
                <xsl:when test="@isarray">axutil_array_list_t*</xsl:when>
                <xsl:when test="not(@type)">axiom_node_t*</xsl:when> <!-- these are anonymous -->
                <xsl:when test="@ours">adb_<xsl:value-of select="@type"/>_t*</xsl:when>
                <xsl:otherwise><xsl:value-of select="@type"/></xsl:otherwise>
            </xsl:choose>
            </xsl:variable>
            <xsl:variable name="propertyName"><xsl:value-of select="@name"></xsl:value-of></xsl:variable>
            <xsl:variable name="CName"><xsl:value-of select="@cname"></xsl:value-of></xsl:variable>

            <xsl:variable name="nativePropertyType"> <!--these are used in arrays to take the native type-->
               <xsl:choose>
                 <xsl:when test="not(@type)">axiom_node_t*</xsl:when> <!-- these are anonymous -->
                 <xsl:when test="@ours">adb_<xsl:value-of select="@type"/>_t*</xsl:when>
                 <xsl:otherwise><xsl:value-of select="@type"/></xsl:otherwise>
               </xsl:choose>
            </xsl:variable>
              <xsl:variable name="PropertyTypeArrayParam"> <!--these are used in arrays to take the type stored in the arraylist-->
                 <xsl:choose>
                   <xsl:when test="not(@type)">axiom_node_t*</xsl:when> <!-- these are anonymous -->
                   <xsl:when test="@ours">adb_<xsl:value-of select="@type"/>_t*</xsl:when>
                   <xsl:when test="@type='unsigned short' or @type='uint64_t' or @type='unsigned int' or @type='unsigned char' or @type='short' or @type='char' or @type='int' or @type='float' or @type='double' or @type='int64_t'"><xsl:value-of select="@type"/><xsl:text>*</xsl:text></xsl:when>
                   <xsl:otherwise><xsl:value-of select="@type"/></xsl:otherwise>
                 </xsl:choose>
              </xsl:variable>
            <xsl:variable name="paramComment">
                <xsl:choose>
                    <xsl:when test="@isarray"><xsl:text>Array of </xsl:text><xsl:value-of select="$PropertyTypeArrayParam"/><xsl:text>s.</xsl:text></xsl:when>
                    <xsl:otherwise><xsl:value-of select="$nativePropertyType"/></xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:variable name="constValue">
                <xsl:choose>
                   <xsl:when test="@isarray"></xsl:when>
                   <xsl:when test="@type='axis2_char_t*' or @type='unsigned short' or @type='uint64_t' or @type='unsigned int' or @type='unsigned char' or @type='short' or @type='char' or @type='int' or @type='float' or @type='double' or @type='int64_t'">const </xsl:when>
                </xsl:choose>
            </xsl:variable>
        

        /**
         * Getter for <xsl:value-of select="$propertyName"/>. <xsl:if test="@isarray">Deprecated for array types, Use <xsl:value-of select="$axis2_name"/>_get_<xsl:value-of select="$CName"/>_at instead</xsl:if>
         * @param <xsl:text> _</xsl:text><xsl:value-of select="$name"/> <xsl:text> </xsl:text><xsl:value-of select="$axis2_name"/>_t object
         * @param env pointer to environment struct
         * @return <xsl:value-of select="$paramComment"/>
         */
        <xsl:value-of select="$propertyType"/> AXIS2_CALL
        <xsl:value-of select="$axis2_name"/>_get_<xsl:value-of select="$CName"/>(
            <xsl:value-of select="$axis2_name"/>_t*<xsl:text> _</xsl:text><xsl:value-of select="$name"/>,
            const axutil_env_t *env);

        /**
         * Setter for <xsl:value-of select="$propertyName"/>.<xsl:if test="@isarray">Deprecated for array types, Use <xsl:value-of select="$axis2_name"/>_set_<xsl:value-of select="$CName"/>_at
         * or <xsl:value-of select="$axis2_name"/>_add_<xsl:value-of select="$CName"/> instead.</xsl:if>
         * @param <xsl:text> _</xsl:text><xsl:value-of select="$name"/> <xsl:text> </xsl:text><xsl:value-of select="$axis2_name"/>_t object
         * @param env pointer to environment struct
         * @param arg_<xsl:value-of select="$CName"/><xsl:text> </xsl:text> <xsl:value-of select="$paramComment"/>
         * @return AXIS2_SUCCESS on success, else AXIS2_FAILURE
         */
        axis2_status_t AXIS2_CALL
        <xsl:value-of select="$axis2_name"/>_set_<xsl:value-of select="$CName"/>(
            <xsl:value-of select="$axis2_name"/>_t*<xsl:text> _</xsl:text><xsl:value-of select="$name"/>,
            const axutil_env_t *env,
            <xsl:value-of select="$constValue"/><xsl:value-of select="$propertyType"/><xsl:text> </xsl:text> arg_<xsl:value-of select="$CName"/>);

        /**
         * Resetter for <xsl:value-of select="$propertyName"/>
         * @param <xsl:text> _</xsl:text><xsl:value-of select="$name"/> <xsl:text> </xsl:text><xsl:value-of select="$axis2_name"/>_t object
         * @param env pointer to environment struct
         * @return AXIS2_SUCCESS on success, else AXIS2_FAILURE
         */
        axis2_status_t AXIS2_CALL
        <xsl:value-of select="$axis2_name"/>_reset_<xsl:value-of select="$CName"/>(
            <xsl:value-of select="$axis2_name"/>_t*<xsl:text> _</xsl:text><xsl:value-of select="$name"/>,
            const axutil_env_t *env);

        </xsl:for-each>

        <!-- The following take care of list items -->

        <xsl:for-each select="itemtype">
            <xsl:variable name="propertyType">axutil_array_list_t*</xsl:variable>
            <xsl:variable name="propertyName"><xsl:value-of select="$name"></xsl:value-of></xsl:variable>
            <xsl:variable name="CName"><xsl:value-of select="$name"></xsl:value-of></xsl:variable>

            <xsl:variable name="nativePropertyType"> <!--these are used in arrays to take the native type-->
               <xsl:choose>
                 <xsl:when test="not(@type)">axiom_node_t*</xsl:when> <!-- these are anonymous -->
                 <xsl:when test="@ours">adb_<xsl:value-of select="@type"/>_t*</xsl:when>
                 <xsl:otherwise><xsl:value-of select="@type"/></xsl:otherwise>
               </xsl:choose>
            </xsl:variable>
              <xsl:variable name="PropertyTypeArrayParam"> <!--these are used in arrays to take the type stored in the arraylist-->
                 <xsl:choose>
                   <xsl:when test="not(@type)">axiom_node_t*</xsl:when> <!-- these are anonymous -->
                   <xsl:when test="@ours">adb_<xsl:value-of select="@type"/>_t*</xsl:when>
                   <xsl:when test="@type='unsigned short' or @type='uint64_t' or @type='unsigned int' or @type='unsigned char' or @type='short' or @type='char' or @type='int' or @type='float' or @type='double' or @type='int64_t'"><xsl:value-of select="@type"/><xsl:text>*</xsl:text></xsl:when>
                   <xsl:otherwise><xsl:value-of select="@type"/></xsl:otherwise>
                 </xsl:choose>
              </xsl:variable>
            <xsl:variable name="paramComment"><xsl:text>Array of </xsl:text><xsl:value-of select="$PropertyTypeArrayParam"/><xsl:text>s.</xsl:text></xsl:variable>
            <xsl:variable name="constValue">
                <xsl:choose>
                   <xsl:when test="@type='axis2_char_t*' or @type='unsigned short' or @type='uint64_t' or @type='unsigned int' or @type='unsigned char' or @type='short' or @type='char' or @type='int' or @type='float' or @type='double' or @type='int64_t'">const </xsl:when>
                </xsl:choose>
            </xsl:variable>

        /**
         * Getter for <xsl:value-of select="$propertyName"/>. Deprecated for array types, Use <xsl:value-of select="$axis2_name"/>_get_<xsl:value-of select="$CName"/>_at instead
         * @param <xsl:text> _</xsl:text><xsl:value-of select="$name"/> <xsl:text> </xsl:text><xsl:value-of select="$axis2_name"/>_t object
         * @param env pointer to environment struct
         * @return <xsl:value-of select="$paramComment"/>
         */
        <xsl:value-of select="$propertyType"/> AXIS2_CALL
        <xsl:value-of select="$axis2_name"/>_get_<xsl:value-of select="$CName"/>(
            <xsl:value-of select="$axis2_name"/>_t*<xsl:text> _</xsl:text><xsl:value-of select="$name"/>,
            const axutil_env_t *env);

        /**
         * Setter for <xsl:value-of select="$propertyName"/>. Deprecated for array types, Use <xsl:value-of select="$axis2_name"/>_set_<xsl:value-of select="$CName"/>_at
         * or <xsl:value-of select="$axis2_name"/>_add_<xsl:value-of select="$CName"/> instead.
         * @param <xsl:text> _</xsl:text><xsl:value-of select="$name"/> <xsl:text> </xsl:text><xsl:value-of select="$axis2_name"/>_t object
         * @param env pointer to environment struct
         * @param arg_<xsl:value-of select="$CName"/><xsl:text> </xsl:text> <xsl:value-of select="$paramComment"/>
         * @return AXIS2_SUCCESS on success, else AXIS2_FAILURE
         */
        axis2_status_t AXIS2_CALL
        <xsl:value-of select="$axis2_name"/>_set_<xsl:value-of select="$CName"/>(
            <xsl:value-of select="$axis2_name"/>_t*<xsl:text> _</xsl:text><xsl:value-of select="$name"/>,
            const axutil_env_t *env,
            <xsl:value-of select="$propertyType"/><xsl:text> </xsl:text> arg_<xsl:value-of select="$CName"/>);

        /**
         * Resetter for <xsl:value-of select="$propertyName"/>
         * @param <xsl:text> _</xsl:text><xsl:value-of select="$name"/> <xsl:text> </xsl:text><xsl:value-of select="$axis2_name"/>_t object
         * @param env pointer to environment struct
         * @return AXIS2_SUCCESS on success, else AXIS2_FAILURE
         */
        axis2_status_t AXIS2_CALL
        <xsl:value-of select="$axis2_name"/>_reset_<xsl:value-of select="$CName"/>(
            <xsl:value-of select="$axis2_name"/>_t*<xsl:text> _</xsl:text><xsl:value-of select="$name"/>,
            const axutil_env_t *env);

        </xsl:for-each>


        <xsl:if test="count(property[@array])!=0 or count(itemtype)!=0">
        /****************************** Getters and Setters For Arrays **********************************/
        /************ Array Specific Operations: get_at, set_at, add, remove_at, sizeof *****************/

        /**
         * E.g. use of get_at, set_at, add and sizeof
         *
         * for(i = 0; i &lt; adb_element_sizeof_property(adb_object, env); i ++ )
         * {
         *     // Getting ith value to property_object variable
         *     property_object = adb_element_get_property_at(adb_object, env, i);
         *
         *     // Setting ith value from property_object variable
         *     adb_element_set_property_at(adb_object, env, i, property_object);
         *
         *     // Appending the value to the end of the array from property_object variable
         *     adb_element_add_property(adb_object, env, property_object);
         *
         *     // Removing the ith value from an array
         *     adb_element_remove_property_at(adb_object, env, i);
         *     
         * }
         *
         */

        </xsl:if>

        <xsl:for-each select="property">
            <xsl:variable name="propertyType">
            <xsl:choose>
                <xsl:when test="@isarray">axutil_array_list_t*</xsl:when>
                <xsl:when test="not(@type)">axiom_node_t*</xsl:when> <!-- these are anonymous -->
                <xsl:when test="@ours">adb_<xsl:value-of select="@type"/>_t*</xsl:when>
                <xsl:otherwise><xsl:value-of select="@type"/></xsl:otherwise>
            </xsl:choose>
            </xsl:variable>
            <xsl:variable name="propertyName"><xsl:value-of select="@name"></xsl:value-of></xsl:variable>
            <xsl:variable name="CName"><xsl:value-of select="@cname"></xsl:value-of></xsl:variable>

            <xsl:variable name="nativePropertyType"> <!--these are used in arrays to take the native type-->
               <xsl:choose>
                 <xsl:when test="not(@type)">axiom_node_t*</xsl:when> <!-- these are anonymous -->
                 <xsl:when test="@ours">adb_<xsl:value-of select="@type"/>_t*</xsl:when>
                 <xsl:otherwise><xsl:value-of select="@type"/></xsl:otherwise>
               </xsl:choose>
            </xsl:variable>
              <xsl:variable name="PropertyTypeArrayParam"> <!--these are used in arrays to take the type stored in the arraylist-->
                 <xsl:choose>
                   <xsl:when test="not(@type)">axiom_node_t*</xsl:when> <!-- these are anonymous -->
                   <xsl:when test="@ours">adb_<xsl:value-of select="@type"/>_t*</xsl:when>
                   <xsl:when test="@type='unsigned short' or @type='uint64_t' or @type='unsigned int' or @type='unsigned char' or @type='short' or @type='char' or @type='int' or @type='float' or @type='double' or @type='int64_t'"><xsl:value-of select="@type"/><xsl:text>*</xsl:text></xsl:when>
                   <xsl:otherwise><xsl:value-of select="@type"/></xsl:otherwise>
                 </xsl:choose>
              </xsl:variable>
            <xsl:variable name="paramComment">
                <xsl:choose>
                    <xsl:when test="@isarray"><xsl:text>Array of </xsl:text><xsl:value-of select="$PropertyTypeArrayParam"/><xsl:text>s.</xsl:text></xsl:when>
                    <xsl:otherwise><xsl:value-of select="$nativePropertyType"/></xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:variable name="constValue">
                <xsl:choose>
                   <xsl:when test="@type='axis2_char_t*' or @type='unsigned short' or @type='uint64_t' or @type='unsigned int' or @type='unsigned char' or @type='short' or @type='char' or @type='int' or @type='float' or @type='double' or @type='int64_t'">const </xsl:when>
                </xsl:choose>
            </xsl:variable>


        <xsl:if test="@isarray">
        
        /**
         * Get the ith element of <xsl:value-of select="$propertyName"/>.
         * @param <xsl:text> _</xsl:text><xsl:value-of select="$name"/> <xsl:text> </xsl:text><xsl:value-of select="$axis2_name"/>_t object
         * @param env pointer to environment struct
         * @param i index of the item to return
         * @return ith <xsl:value-of select="$nativePropertyType"/> of the array
         */
        <xsl:value-of select="$nativePropertyType"/> AXIS2_CALL
        <xsl:value-of select="$axis2_name"/>_get_<xsl:value-of select="$CName"/>_at(
                <xsl:value-of select="$axis2_name"/>_t*<xsl:text> _</xsl:text><xsl:value-of select="$name"/>,
                const axutil_env_t *env, int i);

        /**
         * Set the ith element of <xsl:value-of select="$propertyName"/>. (If the ith already exist, it will be replaced)
         * @param <xsl:text> _</xsl:text><xsl:value-of select="$name"/> <xsl:text> </xsl:text><xsl:value-of select="$axis2_name"/>_t object
         * @param env pointer to environment struct
         * @param i index of the item to return
         * @param <xsl:text>arg_</xsl:text> <xsl:value-of select="$CName"/> element to set <xsl:value-of select="$nativePropertyType"/> to the array
         * @return ith <xsl:value-of select="$nativePropertyType"/> of the array
         */
        axis2_status_t AXIS2_CALL
        <xsl:value-of select="$axis2_name"/>_set_<xsl:value-of select="$CName"/>_at(
                <xsl:value-of select="$axis2_name"/>_t*<xsl:text> _</xsl:text><xsl:value-of select="$name"/>,
                const axutil_env_t *env, int i,
                <xsl:value-of select="$constValue"/><xsl:value-of select="$nativePropertyType"/><xsl:text> arg_</xsl:text> <xsl:value-of select="$CName"/>);


        /**
         * Add to <xsl:value-of select="$propertyName"/>.
         * @param <xsl:text> _</xsl:text><xsl:value-of select="$name"/> <xsl:text> </xsl:text><xsl:value-of select="$axis2_name"/>_t object
         * @param env pointer to environment struct
         * @param <xsl:text>arg_</xsl:text> <xsl:value-of select="$CName"/> element to add <xsl:value-of select="$nativePropertyType"/> to the array
         * @return AXIS2_SUCCESS on success, else AXIS2_FAILURE
         */
        axis2_status_t AXIS2_CALL
        <xsl:value-of select="$axis2_name"/>_add_<xsl:value-of select="$CName"/>(
                <xsl:value-of select="$axis2_name"/>_t*<xsl:text> _</xsl:text><xsl:value-of select="$name"/>,
                const axutil_env_t *env,
                <xsl:value-of select="$constValue"/><xsl:value-of select="$nativePropertyType"/><xsl:text> arg_</xsl:text> <xsl:value-of select="$CName"/>);

        /**
         * Get the size of the <xsl:value-of select="$propertyName"/> array.
         * @param <xsl:text> _</xsl:text><xsl:value-of select="$name"/> <xsl:text> </xsl:text><xsl:value-of select="$axis2_name"/>_t object
         * @param env pointer to environment struct.
         * @return the size of the <xsl:value-of select="$propertyName"/> array.
         */
        int AXIS2_CALL
        <xsl:value-of select="$axis2_name"/>_sizeof_<xsl:value-of select="$CName"/>(
                    <xsl:value-of select="$axis2_name"/>_t*<xsl:text> _</xsl:text><xsl:value-of select="$name"/>,
                    const axutil_env_t *env);

        /**
         * Remove the ith element of <xsl:value-of select="$propertyName"/>.
         * @param <xsl:text> _</xsl:text><xsl:value-of select="$name"/> <xsl:text> </xsl:text><xsl:value-of select="$axis2_name"/>_t object
         * @param env pointer to environment struct
         * @param i index of the item to remove
         * @return AXIS2_SUCCESS on success, else AXIS2_FAILURE
         */
        axis2_status_t AXIS2_CALL
        <xsl:value-of select="$axis2_name"/>_remove_<xsl:value-of select="$CName"/>_at(
                <xsl:value-of select="$axis2_name"/>_t*<xsl:text> _</xsl:text><xsl:value-of select="$name"/>,
                const axutil_env_t *env, int i);

        </xsl:if> <!-- xsl:if test="@isarray" -->
        </xsl:for-each> <!-- xsl:for-each select="property" -->

        <!-- The section covers the list types -->
        <xsl:for-each select="itemtype">
            <xsl:variable name="propertyType">axutil_array_list_t*</xsl:variable>
            <xsl:variable name="propertyName"><xsl:value-of select="$name"></xsl:value-of></xsl:variable>
            <xsl:variable name="CName"><xsl:value-of select="$name"></xsl:value-of></xsl:variable>

            <xsl:variable name="nativePropertyType"> <!--these are used in arrays to take the native type-->
               <xsl:choose>
                 <xsl:when test="not(@type)">axiom_node_t*</xsl:when> <!-- these are anonymous -->
                 <xsl:when test="@ours">adb_<xsl:value-of select="@type"/>_t*</xsl:when>
                 <xsl:otherwise><xsl:value-of select="@type"/></xsl:otherwise>
               </xsl:choose>
            </xsl:variable>
              <xsl:variable name="PropertyTypeArrayParam"> <!--these are used in arrays to take the type stored in the arraylist-->
                 <xsl:choose>
                   <xsl:when test="not(@type)">axiom_node_t*</xsl:when> <!-- these are anonymous -->
                   <xsl:when test="@ours">adb_<xsl:value-of select="@type"/>_t*</xsl:when>
                   <xsl:when test="@type='unsigned short' or @type='uint64_t' or @type='unsigned int' or @type='unsigned char' or @type='short' or @type='char' or @type='int' or @type='float' or @type='double' or @type='int64_t'"><xsl:value-of select="@type"/><xsl:text>*</xsl:text></xsl:when>
                   <xsl:otherwise><xsl:value-of select="@type"/></xsl:otherwise>
                 </xsl:choose>
              </xsl:variable>
            <xsl:variable name="paramComment"><xsl:text>Array of </xsl:text><xsl:value-of select="$PropertyTypeArrayParam"/><xsl:text>s.</xsl:text></xsl:variable>
            <xsl:variable name="constValue">
                <xsl:choose>
                   <xsl:when test="@type='axis2_char_t*' or @type='unsigned short' or @type='uint64_t' or @type='unsigned int' or @type='unsigned char' or @type='short' or @type='char' or @type='int' or @type='float' or @type='double' or @type='int64_t'">const </xsl:when>
                </xsl:choose>
            </xsl:variable>
         
        /**
         * Get the ith element of <xsl:value-of select="$propertyName"/>.
         * @param <xsl:text> _</xsl:text><xsl:value-of select="$name"/> <xsl:text> </xsl:text><xsl:value-of select="$axis2_name"/>_t object
         * @param env pointer to environment struct
         * @param i index of the item to return
         * @return ith <xsl:value-of select="$nativePropertyType"/> of the array
         */
        <xsl:value-of select="$nativePropertyType"/> AXIS2_CALL
        <xsl:value-of select="$axis2_name"/>_get_<xsl:value-of select="$CName"/>_at(
                <xsl:value-of select="$axis2_name"/>_t*<xsl:text> _</xsl:text><xsl:value-of select="$name"/>,
                const axutil_env_t *env, int i);

        /**
         * Set the ith element of <xsl:value-of select="$propertyName"/>. (If the ith already exist, it will be replaced)
         * @param <xsl:text> _</xsl:text><xsl:value-of select="$name"/> <xsl:text> </xsl:text><xsl:value-of select="$axis2_name"/>_t object
         * @param env pointer to environment struct
         * @param i index of the item to return
         * @param <xsl:text>arg_</xsl:text> <xsl:value-of select="$CName"/> element to set <xsl:value-of select="$nativePropertyType"/> to the array
         * @return ith <xsl:value-of select="$nativePropertyType"/> of the array
         */
        axis2_status_t AXIS2_CALL
        <xsl:value-of select="$axis2_name"/>_set_<xsl:value-of select="$CName"/>_at(
                <xsl:value-of select="$axis2_name"/>_t*<xsl:text> _</xsl:text><xsl:value-of select="$name"/>,
                const axutil_env_t *env, int i,
                <xsl:value-of select="$constValue"/><xsl:value-of select="$nativePropertyType"/><xsl:text> arg_</xsl:text> <xsl:value-of select="$CName"/>);


        /**
         * Add to <xsl:value-of select="$propertyName"/>.
         * @param <xsl:text> _</xsl:text><xsl:value-of select="$name"/> <xsl:text> </xsl:text><xsl:value-of select="$axis2_name"/>_t object
         * @param env pointer to environment struct
         * @param <xsl:text>arg_</xsl:text> <xsl:value-of select="$CName"/> element to add <xsl:value-of select="$nativePropertyType"/> to the array
         * @return AXIS2_SUCCESS on success, else AXIS2_FAILURE
         */
        axis2_status_t AXIS2_CALL
        <xsl:value-of select="$axis2_name"/>_add_<xsl:value-of select="$CName"/>(
                <xsl:value-of select="$axis2_name"/>_t*<xsl:text> _</xsl:text><xsl:value-of select="$name"/>,
                const axutil_env_t *env,
                <xsl:value-of select="$constValue"/><xsl:value-of select="$nativePropertyType"/><xsl:text> arg_</xsl:text> <xsl:value-of select="$CName"/>);

        /**
         * Get the size of the <xsl:value-of select="$propertyName"/> array.
         * @param <xsl:text> _</xsl:text><xsl:value-of select="$name"/> <xsl:text> </xsl:text><xsl:value-of select="$axis2_name"/>_t object
         * @param env pointer to environment struct.
         * @return the size of the <xsl:value-of select="$propertyName"/> array.
         */
        int AXIS2_CALL
        <xsl:value-of select="$axis2_name"/>_sizeof_<xsl:value-of select="$CName"/>(
                    <xsl:value-of select="$axis2_name"/>_t*<xsl:text> _</xsl:text><xsl:value-of select="$name"/>,
                    const axutil_env_t *env);

        /**
         * Remove the ith element of <xsl:value-of select="$propertyName"/>.
         * @param <xsl:text> _</xsl:text><xsl:value-of select="$name"/> <xsl:text> </xsl:text><xsl:value-of select="$axis2_name"/>_t object
         * @param env pointer to environment struct
         * @param i index of the item to remove
         * @return AXIS2_SUCCESS on success, else AXIS2_FAILURE
         */
        axis2_status_t AXIS2_CALL
        <xsl:value-of select="$axis2_name"/>_remove_<xsl:value-of select="$CName"/>_at(
                <xsl:value-of select="$axis2_name"/>_t*<xsl:text> _</xsl:text><xsl:value-of select="$name"/>,
                const axutil_env_t *env, int i);

   
        </xsl:for-each>


        /******************************* Checking and Setting NIL values *********************************/
        <xsl:if test="count(property[@array])!=0">/* Use 'Checking and Setting NIL values for Arrays' to check and set nil for individual elements */</xsl:if>

        /**
         * NOTE: set_nil is only available for nillable properties
         */

        <xsl:for-each select="property">
            <xsl:variable name="propertyType">
            <xsl:choose>
                <xsl:when test="@isarray">axutil_array_list_t*</xsl:when>
                <xsl:when test="not(@type)">axiom_node_t*</xsl:when> <!-- these are anonymous -->
                <xsl:when test="@ours">adb_<xsl:value-of select="@type"/>_t*</xsl:when>
                <xsl:otherwise><xsl:value-of select="@type"/></xsl:otherwise>
            </xsl:choose>
            </xsl:variable>
            <xsl:variable name="propertyName"><xsl:value-of select="@name"></xsl:value-of></xsl:variable>
            <xsl:variable name="CName"><xsl:value-of select="@cname"></xsl:value-of></xsl:variable>

            <xsl:variable name="nativePropertyType"> <!--these are used in arrays to take the native type-->
               <xsl:choose>
                 <xsl:when test="not(@type)">axiom_node_t*</xsl:when> <!-- these are anonymous -->
                 <xsl:when test="@ours">adb_<xsl:value-of select="@type"/>_t*</xsl:when>
                 <xsl:otherwise><xsl:value-of select="@type"/></xsl:otherwise>
               </xsl:choose>
            </xsl:variable>
              <xsl:variable name="PropertyTypeArrayParam"> <!--these are used in arrays to take the type stored in the arraylist-->
                 <xsl:choose>
                   <xsl:when test="not(@type)">axiom_node_t*</xsl:when> <!-- these are anonymous -->
                   <xsl:when test="@ours">adb_<xsl:value-of select="@type"/>_t*</xsl:when>
                   <xsl:when test="@type='unsigned short' or @type='uint64_t' or @type='unsigned int' or @type='unsigned char' or @type='short' or @type='char' or @type='int' or @type='float' or @type='double' or @type='int64_t'"><xsl:value-of select="@type"/><xsl:text>*</xsl:text></xsl:when>
                   <xsl:otherwise><xsl:value-of select="@type"/></xsl:otherwise>
                 </xsl:choose>
              </xsl:variable>
            <xsl:variable name="paramComment">
                <xsl:choose>
                    <xsl:when test="@isarray"><xsl:text>Array of </xsl:text><xsl:value-of select="$PropertyTypeArrayParam"/><xsl:text>s.</xsl:text></xsl:when>
                    <xsl:otherwise><xsl:value-of select="$nativePropertyType"/></xsl:otherwise>
                </xsl:choose>
            </xsl:variable>

        /**
         * Check whether <xsl:value-of select="$propertyName"/> is nill
         * @param <xsl:text> _</xsl:text><xsl:value-of select="$name"/> <xsl:text> </xsl:text><xsl:value-of select="$axis2_name"/>_t object
         * @param env pointer to environment struct
         * @return AXIS2_TRUE if the element is nil or AXIS2_FALSE otherwise
         */
        axis2_bool_t AXIS2_CALL
        <xsl:value-of select="$axis2_name"/>_is_<xsl:value-of select="$CName"/>_nil(
                <xsl:value-of select="$axis2_name"/>_t*<xsl:text> _</xsl:text><xsl:value-of select="$name"/>,
                const axutil_env_t *env);


        <xsl:if test="@nillable or @optional">
        /**
         * Set <xsl:value-of select="$propertyName"/> to nill (currently the same as reset)
         * @param <xsl:text> _</xsl:text><xsl:value-of select="$name"/> <xsl:text> </xsl:text><xsl:value-of select="$axis2_name"/>_t object
         * @param env pointer to environment struct
         * @return AXIS2_SUCCESS on success, else AXIS2_FAILURE
         */
        axis2_status_t AXIS2_CALL
        <xsl:value-of select="$axis2_name"/>_set_<xsl:value-of select="$CName"/>_nil(
                <xsl:value-of select="$axis2_name"/>_t*<xsl:text> _</xsl:text><xsl:value-of select="$name"/>,
                const axutil_env_t *env);
        </xsl:if>

        </xsl:for-each> <!-- for-each select="proprety" -->

        <xsl:if test="count(property[@array])!=0">
        /*************************** Checking and Setting 'NIL' values in Arrays *****************************/

        /**
         * NOTE: You may set this to remove specific elements in the array
         *       But you can not remove elements, if the specific property is declared to be non-nillable or sizeof(array) &lt; minOccurs
         */
        </xsl:if>

        <xsl:for-each select="property">
            <xsl:variable name="propertyType">
            <xsl:choose>
                <xsl:when test="@isarray">axutil_array_list_t*</xsl:when>
                <xsl:when test="not(@type)">axiom_node_t*</xsl:when> <!-- these are anonymous -->
                <xsl:when test="@ours">adb_<xsl:value-of select="@type"/>_t*</xsl:when>
                <xsl:otherwise><xsl:value-of select="@type"/></xsl:otherwise>
            </xsl:choose>
            </xsl:variable>
            <xsl:variable name="propertyName"><xsl:value-of select="@name"></xsl:value-of></xsl:variable>
            <xsl:variable name="CName"><xsl:value-of select="@cname"></xsl:value-of></xsl:variable>

            <xsl:variable name="nativePropertyType"> <!--these are used in arrays to take the native type-->
               <xsl:choose>
                 <xsl:when test="not(@type)">axiom_node_t*</xsl:when> <!-- these are anonymous -->
                 <xsl:when test="@ours">adb_<xsl:value-of select="@type"/>_t*</xsl:when>
                 <xsl:otherwise><xsl:value-of select="@type"/></xsl:otherwise>
               </xsl:choose>
            </xsl:variable>
              <xsl:variable name="PropertyTypeArrayParam"> <!--these are used in arrays to take the type stored in the arraylist-->
                 <xsl:choose>
                   <xsl:when test="not(@type)">axiom_node_t*</xsl:when> <!-- these are anonymous -->
                   <xsl:when test="@ours">adb_<xsl:value-of select="@type"/>_t*</xsl:when>
                   <xsl:when test="@type='unsigned short' or @type='uint64_t' or @type='unsigned int' or @type='unsigned char' or @type='short' or @type='char' or @type='int' or @type='float' or @type='double' or @type='int64_t'"><xsl:value-of select="@type"/><xsl:text>*</xsl:text></xsl:when>
                   <xsl:otherwise><xsl:value-of select="@type"/></xsl:otherwise>
                 </xsl:choose>
              </xsl:variable>
            <xsl:variable name="paramComment">
                <xsl:choose>
                    <xsl:when test="@isarray"><xsl:text>Array of </xsl:text><xsl:value-of select="$PropertyTypeArrayParam"/><xsl:text>s.</xsl:text></xsl:when>
                    <xsl:otherwise><xsl:value-of select="$nativePropertyType"/></xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
        <xsl:if test="@isarray">
        /**
         * Check whether <xsl:value-of select="$propertyName"/> is nill at i
         * @param <xsl:text> _</xsl:text><xsl:value-of select="$name"/> <xsl:text> </xsl:text><xsl:value-of select="$axis2_name"/>_t object
         * @param env pointer to environment struct.
         * @param i index of the item to return.
         * @return AXIS2_TRUE if the element is nil or AXIS2_FALSE otherwise
         */
        axis2_bool_t AXIS2_CALL
        <xsl:value-of select="$axis2_name"/>_is_<xsl:value-of select="$CName"/>_nil_at(
                <xsl:value-of select="$axis2_name"/>_t*<xsl:text> _</xsl:text><xsl:value-of select="$name"/>,
                const axutil_env_t *env, int i);
 
       
        /**
         * Set <xsl:value-of select="$propertyName"/> to nill at i
         * @param <xsl:text> _</xsl:text><xsl:value-of select="$name"/> _<xsl:text> </xsl:text><xsl:value-of select="$axis2_name"/>_t object
         * @param env pointer to environment struct.
         * @param i index of the item to set.
         * @return AXIS2_SUCCESS on success, or AXIS2_FAILURE otherwise.
         */
        axis2_status_t AXIS2_CALL
        <xsl:value-of select="$axis2_name"/>_set_<xsl:value-of select="$CName"/>_nil_at(
                <xsl:value-of select="$axis2_name"/>_t*<xsl:text> _</xsl:text><xsl:value-of select="$name"/>, 
                const axutil_env_t *env, int i);

        </xsl:if> <!-- closes isarray -->
        </xsl:for-each>

        /**************************** Serialize and Deserialize functions ***************************/
        /*********** These functions are for use only inside the generated code *********************/

        <xsl:if test="@simple">
            /**
             * Deserialize the content from a string to adb objects
             * @param <xsl:text> _</xsl:text><xsl:value-of select="$name"/><xsl:text> </xsl:text> <xsl:value-of select="$axis2_name"/>_t object
             * @param env pointer to environment struct
             * @param node_value to deserialize
             * @param parent_element The parent element if it is an element, NULL otherwise
             * @return AXIS2_SUCCESS on success, else AXIS2_FAILURE
             */
            axis2_status_t AXIS2_CALL
            <xsl:value-of select="$axis2_name"/>_deserialize_from_string(
                            <xsl:value-of select="$axis2_name"/>_t*<xsl:text> _</xsl:text><xsl:value-of select="$name"/>,
                                            const axutil_env_t *env,
                                            axis2_char_t *node_value,
                                            axiom_node_t *parent);
        </xsl:if>
        /**
         * Deserialize an XML to adb objects
         * @param <xsl:text> _</xsl:text><xsl:value-of select="$name"/> <xsl:text> </xsl:text><xsl:value-of select="$axis2_name"/>_t object
         * @param env pointer to environment struct
         * @param dp_parent double pointer to the parent node to deserialize
         * @param dp_is_early_node_valid double pointer to a flag (is_early_node_valid?)
         * @param dont_care_minoccurs Dont set errors on validating minoccurs, 
         *              (Parent will order this in a case of choice)
         * @return AXIS2_SUCCESS on success, else AXIS2_FAILURE
         */
        axis2_status_t AXIS2_CALL
        <xsl:value-of select="$axis2_name"/>_deserialize(
            <xsl:value-of select="$axis2_name"/>_t*<xsl:text> _</xsl:text><xsl:value-of select="$name"/>,
            const axutil_env_t *env,
            axiom_node_t** dp_parent,
            axis2_bool_t *dp_is_early_node_valid,
            axis2_bool_t dont_care_minoccurs);
                            
            <!-- Here the double pointer is used to change the parent pointer - This can be happned when deserialize is called in a particle class -->

       /**
         * Declare namespace in the most parent node 
         * @param <xsl:text> _</xsl:text><xsl:value-of select="$name"/><xsl:text> </xsl:text> <xsl:value-of select="$axis2_name"/>_t object
         * @param env pointer to environment struct
         * @param parent_element parent element
         * @param namespaces hash of namespace uri to prefix
         * @param next_ns_index pointer to an int which contain the next namespace index
         */
       void AXIS2_CALL
       <xsl:value-of select="$axis2_name"/>_declare_parent_namespaces(
                    <xsl:value-of select="$axis2_name"/>_t*<xsl:text> _</xsl:text><xsl:value-of select="$name"/>,
                    const axutil_env_t *env, axiom_element_t *parent_element,
                    axutil_hash_t *namespaces, int *next_ns_index);

        <xsl:if test="@simple">
        /**
         * Serialize to a String from the adb objects
         * @param <xsl:text> _</xsl:text><xsl:value-of select="$name"/> <xsl:text> </xsl:text><xsl:value-of select="$axis2_name"/>_t object
         * @param env pointer to environment struct
         * @param namespaces hash of namespace uri to prefix
         * @return serialized string
         */
            axis2_char_t* AXIS2_CALL
            <xsl:value-of select="$axis2_name"/>_serialize_to_string(
                    <xsl:value-of select="$axis2_name"/>_t*<xsl:text> _</xsl:text><xsl:value-of select="$name"/>,
                    const axutil_env_t *env, axutil_hash_t *namespaces);
        </xsl:if>

        /**
         * Serialize to an XML from the adb objects
         * @param <xsl:text> _</xsl:text><xsl:value-of select="$name"/> <xsl:text> </xsl:text><xsl:value-of select="$axis2_name"/>_t object
         * @param env pointer to environment struct
         * @param <xsl:value-of select="$name"/>_om_node node to serialize from
         * @param <xsl:value-of select="$name"/>_om_element parent element to serialize from
         * @param tag_closed whether the parent tag is closed or not
         * @param namespaces hash of namespace uri to prefix
         * @param next_ns_index an int which contain the next namespace index
         * @return AXIS2_SUCCESS on success, else AXIS2_FAILURE
         */
        axiom_node_t* AXIS2_CALL
        <xsl:value-of select="$axis2_name"/>_serialize(
            <xsl:value-of select="$axis2_name"/>_t*<xsl:text> _</xsl:text><xsl:value-of select="$name"/>,
            const axutil_env_t *env,
            axiom_node_t* <xsl:value-of select="$name"/>_om_node, axiom_element_t *<xsl:value-of select="$name"/>_om_element, int tag_closed, axutil_hash_t *namespaces, int *next_ns_index);

        /**
         * Check whether the <xsl:value-of select="$axis2_name"/> is a particle class (E.g. group, inner sequence)
         * @return whether this is a particle class.
         */
        axis2_bool_t AXIS2_CALL
        <xsl:value-of select="$axis2_name"/>_is_particle();


     #ifdef __cplusplus
     }
     #endif

     #endif /* <xsl:value-of select="$caps_axis2_name"/>_H */
    </xsl:template>
</xsl:stylesheet>
