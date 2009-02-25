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
package org.apache.axis2.metadata.registry;

import java.util.Hashtable;
import java.util.Map;

import org.apache.axis2.jaxws.ClientConfigurationFactory;
import org.apache.axis2.metadata.factory.ResourceFinderFactory;

public class MetadataFactoryRegistry {
    private final static Map<Class,Object> table;
        static {
                table = new Hashtable<Class,Object>();
                table.put(ResourceFinderFactory.class, new ResourceFinderFactory());
                table.put(ClientConfigurationFactory.class, new ClientConfigurationFactory());
        }
        
        /**
         * FactoryRegistry is currently a static singleton
         */
        private MetadataFactoryRegistry() {
        }
        
        /**
         * getFactory
         * @param intface of the Factory
         * @return Object that is the factory implementation for the intface
         */
        public static Object getFactory(Class intface) {
                return table.get(intface);
        }
        
        /**
         * setFactory
         * @param intface
         * @param factoryObject
         */
        public static void setFactory(Class intface, Object factoryObject){
                table.put(intface, factoryObject);
        }

}
