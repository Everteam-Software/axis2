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

package org.apache.axis2.jaxws.wrapper.impl;

import org.apache.axis2.jaxws.i18n.Messages;
import org.apache.axis2.jaxws.utility.PropertyDescriptorPlus;
import org.apache.axis2.jaxws.utility.XMLRootElementUtil;
import org.apache.axis2.jaxws.wrapper.JAXBWrapperTool;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The JAXBWrapper tool is used to create a JAXB Object from a series of child objects (wrap) or get
 * the child objects from a JAXB Object (unwrap)
 */
public class JAXBWrapperToolImpl implements JAXBWrapperTool {

    private static final Log log = LogFactory.getLog(JAXBWrapperTool.class);

    /**
     * unwrap Returns the list of child objects of the jaxb object
     *
     * @param jaxbObject that represents the type
     * @param childNames list of xml child names as String
     * @param pdMap      PropertyDescriptor map for this jaxbObject
     * @return list of Objects in the same order as the element names.
     */
    public Object[] unWrap(Object jaxbObject,
                           List<String> childNames,
                           Map<String, PropertyDescriptorPlus> pdMap) throws JAXBWrapperException {


        if (jaxbObject == null) {
            throw new JAXBWrapperException(Messages.getMessage("JAXBWrapperErr1"));
        }
        if (childNames == null) {
            throw new JAXBWrapperException(Messages.getMessage("JAXBWrapperErr2"));
        }

        // Get the object that will have the property descriptors (i.e. the object representing the complexType)
        Object jaxbComplexTypeObj = jaxbObject;

        if (log.isDebugEnabled()) {
            log.debug("Invoking unWrap() method with jaxb object:" +
                    jaxbComplexTypeObj.getClass().getName());
            log.debug("The input child xmlnames are: " + toString(childNames));
        }
        // Get the PropertyDescriptorPlus map.
        // The method makes sure that each child name has a matching jaxb property
        checkPropertyDescriptorMap(jaxbComplexTypeObj.getClass(), childNames, pdMap);

        // Get the corresponsing objects from the jaxb bean
        ArrayList<Object> objList = new ArrayList<Object>();
        for (String childName : childNames) {
            PropertyDescriptorPlus propInfo = pdMap.get(childName);
            Object object = null;
            try {
                object = propInfo.get(jaxbComplexTypeObj);
            } catch (Throwable e) {
                if (log.isDebugEnabled()) {
                    log.debug("An exception " + e.getClass() +
                            "occurred while trying to call get() on " + propInfo);
                    log.debug("The corresponding xml child name is: " + childName);
                }
                throw new JAXBWrapperException(e);
            }
            objList.add(object);
        }
        Object[] jaxbObjects = objList.toArray();
        objList = null;
        return jaxbObjects;

    }

    /**
     * wrap Creates a jaxb object that is initialized with the child objects.
     * <p/>
     * Note that the jaxbClass must be the class the represents the complexType. (It should never be
     * JAXBElement)
     *
     * @param jaxbClass
     * @param childNames    list of xml child names as String
     * @param childObjects, component type objects
     * @param pdMap         PropertyDescriptor map for this jaxbObject
     */
    public Object wrap(Class jaxbClass,
                       List<String> childNames,
                       Map<String, Object> childObjects,
                       Map<String, PropertyDescriptorPlus> pdMap) throws JAXBWrapperException {


        if (childNames == null || childObjects == null) {
            throw new JAXBWrapperException(Messages.getMessage("JAXBWrapperErr3"));
        }
        if (childNames.size() != childObjects.size()) {
            throw new JAXBWrapperException(Messages.getMessage("JAXBWrapperErr4"));
        }

        if (log.isDebugEnabled()) {
            log.debug("Invoking unwrap() method to create jaxb object:" + jaxbClass.getName());
            log.debug("The input child xmlnames are: " + toString(childNames));
        }

        // Just like unWrap, get the property info map
        checkPropertyDescriptorMap(jaxbClass, childNames, pdMap);

        // The jaxb object always has a default constructor.  Create the object
        Object jaxbObject = null;
        try {
            jaxbObject = jaxbClass.newInstance();
        } catch (Throwable t) {
            if (log.isDebugEnabled()) {
                log.debug("An exception " + t.getClass() +
                        "occurred while trying to create jaxbobject  " + jaxbClass.getName());
            }
            throw new JAXBWrapperException(t);
        }

        // Now set each object onto the jaxb object
        for (String childName : childNames) {
            PropertyDescriptorPlus propInfo = pdMap.get(childName);
            Object value = childObjects.get(childName);
            try {
                propInfo.set(jaxbObject, value);
            } catch (Throwable t) {

                if (log.isDebugEnabled()) {
                    log.debug("An exception " + t.getClass() +
                            "occurred while trying to call set() on  " + propInfo);
                    log.debug("The corresponding xml child name is: " + childName);
                    String name = (value == null) ? "<null>" : value.getClass().getName();
                    log.debug("The corresponding value object is: " + name);
                }
                throw new JAXBWrapperException(t);
            }
        }

        // Return the jaxb object 
        return jaxbObject;
    }

    /** Makes sure that each xmlChildName is present in the odMap */
    private void checkPropertyDescriptorMap(Class jaxbClass,
                                            List<String> xmlChildNames,
                                            Map<String, PropertyDescriptorPlus> pdMap)
            throws JAXBWrapperException {
        // The following code is slow, and doc/lit wrapped is in the main
        // performance flow.  So only do this check if debug is enabled.
        if (log.isDebugEnabled()) {
            for (int i = 0; i < xmlChildNames.size(); i++) {
                String xmlChildName = xmlChildNames.get(i);
                PropertyDescriptorPlus pd = pdMap.get(xmlChildName);
                if (pd == null) {
                    // Each xml child name must have a matching property.  
                    
                    log.debug(
                    "Error occurred trying to match an xml name to a child of a jaxb object");
                    log.debug("  The JAXBClass is:" + jaxbClass.getName());
                    log.debug("  The child name that we are looking for is:" + xmlChildName);
                    log.debug("  The JAXBClass has the following child xml names:" +
                            toString(pdMap.keySet()));
                    log.debug("  Complete list of child names that we are looking for:" +
                            toString(xmlChildNames));
                    
                    throw new JAXBWrapperException(
                            Messages.getMessage("JAXBWrapperErr6", jaxbClass.getName(), xmlChildName));
                }
            }
        }
    }


    /**
     * @param collection
     * @return list of the names in the collection
     */
    private String toString(Collection<String> collection) {
        String text = "[";
        if (collection == null) {
            return "[]";
        }
        boolean first = true;
        for (String name : collection) {
            if (first) {
                first = false;
                text += name;
            } else {
                text += "," + name;
            }
        }
        return text + "]";
    }

    public Object[] unWrap(Object jaxbObject, List<String> childNames) throws JAXBWrapperException {
        // Get the property descriptor map for this JAXBClass
        Class jaxbClass = jaxbObject.getClass();
        Map<String, PropertyDescriptorPlus>  pdMap = null;
        try {
            pdMap = XMLRootElementUtil.createPropertyDescriptorMap(jaxbClass);
        } catch (Throwable t) {
            log.debug("Error occurred to build the PropertyDescriptor map");
            log.debug("  The JAXBClass is:" + jaxbClass.getName());
            throw new JAXBWrapperException(t);
        }

        // Delegate
        return unWrap(jaxbObject, childNames, pdMap);
    }

    public Object wrap(Class jaxbClass, List<String> childNames, Map<String, Object> childObjects)
            throws JAXBWrapperException {
        // Get the property descriptor map
        Map<String, PropertyDescriptorPlus>  pdMap = null;
        try {
            pdMap = XMLRootElementUtil.createPropertyDescriptorMap(jaxbClass);
        } catch (Throwable t) {
            log.debug("Error occurred to build the PropertyDescriptor map");
            log.debug("  The JAXBClass is:" + jaxbClass.getName());
            throw new JAXBWrapperException(t);
        }

        // Delegate
        return wrap(jaxbClass, childNames, childObjects, pdMap);
    }


}
