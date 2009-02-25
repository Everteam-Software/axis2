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

package org.apache.axis2.jaxws.message.databinding;

import org.apache.axis2.java.security.AccessController;
import org.apache.axis2.jaxws.ExceptionFactory;
import org.apache.axis2.jaxws.i18n.Messages;
import org.apache.axis2.jaxws.message.factory.ClassFinderFactory;
import org.apache.axis2.jaxws.registry.FactoryRegistry;
import org.apache.axis2.jaxws.utility.ClassUtils;
import org.apache.axis2.jaxws.utility.JavaUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.ws.Holder;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;


/**
 * JAXB Utilites to pool JAXBContext and related objects. 
 */
public class JAXBUtils {

    private static final Log log = LogFactory.getLog(JAXBUtils.class);

    // Create a concurrent map to get the JAXBObject: keys are ClassLoader and String (package names).
    private static Map<ClassLoader, Map<String, JAXBContextValue>> jaxbMap =
            new ConcurrentHashMap<ClassLoader, Map<String, JAXBContextValue>>();

    private static Pool<JAXBContext, Marshaller>       mpool = new Pool<JAXBContext, Marshaller>();
    private static Pool<JAXBContext, Unmarshaller>     upool = new Pool<JAXBContext, Unmarshaller>();
    private static Pool<JAXBContext, JAXBIntrospector> ipool = new Pool<JAXBContext, JAXBIntrospector>();
    
    // From Lizet Ernand:
    // If you really care about the performance, 
    // and/or your application is going to read a lot of small documents, 
    // then creating Unmarshaller could be relatively an expensive operation. 
    // In that case, consider pooling Unmarshaller objects.
    // Different threads may reuse one Unmarshaller instance, 
    // as long as you don't use one instance from two threads at the same time. 
    // ENABLE_ADV_POOLING is false...which means they are obtained from the JAXBContext instead of
    // from the pool.
    private static boolean ENABLE_MARSHALL_POOLING = false;
    private static boolean ENABLE_UNMARSHALL_POOLING = true;
    private static boolean ENABLE_INTROSPECTION_POOLING = false;
    
    private static int MAX_LOAD_FACTOR = 32;  // Maximum number of JAXBContext to store

    // Construction Type
    public enum CONSTRUCTION_TYPE {
        BY_CLASS_ARRAY, BY_CONTEXT_PATH, UNKNOWN}

    ;
    
    /**
     * Get a JAXBContext for the class
     *
     * @param contextPackage Set<Package>
     * @return JAXBContext
     * @throws JAXBException
     * @deprecated
     */
    public static JAXBContext getJAXBContext(TreeSet<String> contextPackages) throws JAXBException {
        return getJAXBContext(contextPackages, new Holder<CONSTRUCTION_TYPE>(), 
                              contextPackages.toString(), null);
    }

    /**
     * Get a JAXBContext for the class
     * 
     * Note: The contextPackage object is used by multiple threads.  It should be considered immutable
     * and not altered by this method.
     *
     * @param contextPackage Set<Package>
     * @param cacheKey ClassLoader
     * @return JAXBContext
     * @throws JAXBException
     * @deprecated
     */
    public static JAXBContext getJAXBContext(TreeSet<String> contextPackages, ClassLoader 
                                             cacheKey) throws JAXBException {
        return getJAXBContext(contextPackages, new Holder<CONSTRUCTION_TYPE>(),
                              contextPackages.toString(), cacheKey);
    }
    
    public static JAXBContext getJAXBContext(TreeSet<String> contextPackages, 
                                             Holder<CONSTRUCTION_TYPE> constructionType,
                                             String key)
        throws JAXBException {
        return getJAXBContext(contextPackages, constructionType, key, null);
    }

    /**
     * Get a JAXBContext for the class
     *
     * Note: The contextPackage object is used by multiple threads.  It should be considered immutable
     * and not altered by this method.
     * 
     * @param contextPackage  Set<Package> 
     * @param contructionType (output value that indicates how the context was constructed)
     * @param cacheKey ClassLoader
     * @return JAXBContext
     * @throws JAXBException
     */
    public static JAXBContext getJAXBContext(TreeSet<String> contextPackages,
                                             Holder<CONSTRUCTION_TYPE> constructionType, 
                                             String key,
                                             ClassLoader cacheKey)
            throws JAXBException {
        // JAXBContexts for the same class can be reused and are supposed to be thread-safe
        if (log.isDebugEnabled()) {
            log.debug("Following packages are in this batch of getJAXBContext() :");
            for (String pkg : contextPackages) {
                log.debug(pkg);
            }
        }
        JAXBUtilsMonitor.addPackageKey(key);
        
         // The JAXBContexts are keyed by ClassLoader and the set of Strings
        ClassLoader cl = getContextClassLoader();

        // Get the innerMap 
        Map<String, JAXBContextValue> innerMap = null;
        innerMap = getInnerMap(cacheKey, cl);
        if (innerMap == null) {
            synchronized(jaxbMap) {
                innerMap = getInnerMap(cacheKey, cl);
                if(innerMap==null) {
                    adjustPoolSize(jaxbMap);
                    innerMap = new ConcurrentHashMap<String, JAXBContextValue>();
                    if (cacheKey != null) {
                        jaxbMap.put(cacheKey, innerMap);
                    }
                }
            }
        }

        if (contextPackages == null) {
            contextPackages = new TreeSet<String>();
        }

        JAXBContextValue contextValue = innerMap.get(key);
        if (contextValue == null) {
            synchronized (innerMap) {
                contextValue = innerMap.get(key);
                if(contextValue==null) {
                    adjustPoolSize(innerMap);

                    // Create a copy of the contextPackages.  This new TreeSet will
                    // contain only the valid contextPackages.
                    // Note: The original contextPackage set is accessed by multiple 
                    // threads and should not be altered.

                    TreeSet<String> validContextPackages = new TreeSet<String>(contextPackages);  
                    contextValue = createJAXBContextValue(validContextPackages, cl);

                    // If we don't get all the classes, try the cached classloader 
                    if (cacheKey != null && validContextPackages.size() != contextPackages.size()) {
                        validContextPackages = new TreeSet<String>(contextPackages);
                        contextValue = createJAXBContextValue(validContextPackages, cacheKey);
                    }

                    // Put the new context in the map keyed by both the original and valid list of packages
                    String validPackagesKey = validContextPackages.toString();
                    innerMap.put(key, contextValue);
                    innerMap.put(validPackagesKey, contextValue);
                    if (log.isDebugEnabled()) {
                        log.debug("JAXBContext [created] for " + key);
                        log.debug("JAXBContext also stored by the list of valid packages:" + validPackagesKey);
                    }
                }
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("JAXBContext [from pool] for " + key);
            }
        }
        constructionType.value = contextValue.constructionType;
        return contextValue.jaxbContext;
    }

    private static Map<String, JAXBContextValue> getInnerMap(ClassLoader cacheKey, ClassLoader cl) {
        Map<String, JAXBContextValue> innerMap;
        if(cacheKey != null) {
            if(log.isDebugEnabled()) {
                log.debug("Using supplied classloader to retrieve JAXBContext: " + 
                          cacheKey);
            }
            innerMap = jaxbMap.get(cacheKey);
        }else {
            if(log.isDebugEnabled()) {
                log.debug("Using classloader from Thread to retrieve JAXBContext: " + 
                          cl);
            }
            innerMap = jaxbMap.get(cl);
        }
        return innerMap;
    }

    /**
     * Create a JAXBContext using the contextPackages
     *
     * @param contextPackages Set<String>
     * @param cl              ClassLoader
     * @return JAXBContextValue (JAXBContext + constructionType)
     * @throws JAXBException
     */
    private static JAXBContextValue createJAXBContextValue(TreeSet<String> contextPackages,
                                                           ClassLoader cl) throws JAXBException {

        JAXBContextValue contextValue = null;
        if (log.isDebugEnabled()) {
            
            log.debug("Following packages are in this batch of getJAXBContext() :");
            
            for (String pkg : contextPackages) {
                log.debug(pkg);
            }
            log.debug("This classloader will be used to construct the JAXBContext" + cl);
        }
        // The contextPackages is a set of package names that are constructed using PackageSetBuilder.
        // PackageSetBuilder gets the packages names from various sources.
        //   a) It walks the various annotations on the WebService collecting package names.
        //   b) It walks the wsdl/schemas and builds package names for each target namespace.
        //
        // The combination of these two sources should produce all of the package names.
        // -------------
        // Note that (b) is necessary for the following case:
        // An operation has a parameter named BASE.
        // Object DERIVED is an extension of BASE and is defined in a different package/schema.
        // In this case, there will not be any annotations on the WebService that reference DERIVED.
        // The only way to find the package for DERIVED is to walk the schemas.
        // -------------

        Iterator<String> it = contextPackages.iterator();
        while (it.hasNext()) {
            String p = it.next();
            // Don't consider java and javax packages
            // REVIEW: We might have to refine this
            if (p.startsWith("javax.xml.ws.wsaddressing")) {
                continue;
            }
            if (p.startsWith("java.") ||
                    p.startsWith("javax.")) {
                it.remove();
            }
        }

        // There are two ways to construct the context.
        // 1) USE A CONTEXTPATH, which is a string containing
        //    all of the packages separated by colons.
        // 2) USE A CLASS[], which is an array of all of the classes
        //    involved in the marshal/unmarshal.
        //   
        // There are pros/cons with both approaches.
        // USE A CONTEXTPATH: 
        //    Pros: preferred way of doing this.  
        //          performant
        //          most dynamic
        //    Cons: Each package in context path must have an ObjectFactory
        //        
        //
        // USE CLASS[]:
        //    Pros: Doesn't require ObjectFactory in each package
        //    Cons: Hard to set up, must account for JAX-WS classes, etc.
        //          Does not work if arrays of classes are needed
        //          slower
        //
        //  The following code attempts to build a context path.  It then
        //  choose one of the two constructions above (prefer USE A CONTEXT_PATH)
        //

        // The packages are examined to see if they have ObjectFactory/package-info classes.
        // Invalid packages are removed from the list
        it = contextPackages.iterator();
        boolean contextConstruction = true;
        boolean isJAXBFound = false;
        while (it.hasNext()) {
            String p = it.next();
            // See if this package has an ObjectFactory or package-info
            if (checkPackage(p, cl)) {
                // Flow to here indicates package can be used for CONTEXT construction
                isJAXBFound = true;
                if (log.isDebugEnabled()) {
                    log.debug("Package " + p + " contains an ObjectFactory or package-info class.");
                }
            } else {
                // Flow to here indicates that the package is not valid for context construction.
                // Perhaps the package is invalid.
                if (log.isDebugEnabled()) {
                    log.debug("Package " + p +
                            " does not contain an ObjectFactory or package-info class.  Searching for JAXB classes");
                }
                List<Class> classes = null;
                classes = getAllClassesFromPackage(p, cl);
                if (classes == null || classes.size() == 0) {
                    if (log.isDebugEnabled()) {
                        log.debug("Package " + p +
                                " does not have any JAXB classes.  It is removed from the JAXB context path.");
                    }
                    it.remove();
                } else {
                    // Classes are found in the package.  We cannot use the CONTEXT construction
                    contextConstruction = false;
                    if (log.isDebugEnabled()) {
                        log.debug("Package " + p +
                                " does not contain ObjectFactory, but it does contain other JAXB classes.");
                    }
                }
            }
        }

        if (!isJAXBFound) {
            if (log.isDebugEnabled()) {
                log.debug("Both ObjectFactory & package-info not found in package hierachy");
            }
        }

        // The code above may have removed some packages from the list. 
        // Retry our lookup with the updated list
        Map<String, JAXBContextValue> innerMap = jaxbMap.get(cl);
        if (innerMap != null) {
            contextValue = innerMap.get(contextPackages.toString());
            if (contextValue != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Successfully found JAXBContext with updated context list:" +
                            contextValue.jaxbContext.toString());
                }
                return contextValue;
            }
        }

        // CONTEXT construction
        if (contextConstruction) {
            JAXBContext context = createJAXBContextUsingContextPath(contextPackages, cl);
            if (context != null) {
                contextValue = new JAXBContextValue(context, CONSTRUCTION_TYPE.BY_CONTEXT_PATH);
            }
        }

        // CLASS construction
        if (contextValue == null) {
            it = contextPackages.iterator();
            List<Class> fullList = new ArrayList<Class>();
            while (it.hasNext()) {
                String pkg = it.next();
                fullList.addAll(getAllClassesFromPackage(pkg, cl));
            }
            //Lets add all common array classes
            addCommonArrayClasses(fullList);
            Class[] classArray = fullList.toArray(new Class[0]);
            JAXBContext context = JAXBContext_newInstance(classArray, cl);
            if (context != null) {
                contextValue = new JAXBContextValue(context, CONSTRUCTION_TYPE.BY_CLASS_ARRAY);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Successfully created JAXBContext " + contextValue.jaxbContext.toString());
        }
        return contextValue;
    }

    /**
     * Get the unmarshaller.  You must call releaseUnmarshaller to put it back into the pool
     *
     * @param context JAXBContext
     * @return Unmarshaller
     * @throws JAXBException
     */
    public static Unmarshaller getJAXBUnmarshaller(JAXBContext context) throws JAXBException {
        if (!ENABLE_UNMARSHALL_POOLING) {
            if (log.isDebugEnabled()) {
                log.debug("Unmarshaller created [no pooling]");
            }
            return context.createUnmarshaller();
        }
        Unmarshaller unm = upool.get(context);
        if (unm == null) {
            if (log.isDebugEnabled()) {
                log.debug("Unmarshaller created [not in pool]");
            }
            unm = context.createUnmarshaller();
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Unmarshaller obtained [from  pool]");
            }
        }
        return unm;
    }

    /**
     * Release Unmarshaller Do not call this method if an exception occurred while using the
     * Unmarshaller. We object my be in an invalid state.
     *
     * @param context      JAXBContext
     * @param unmarshaller Unmarshaller
     */
    public static void releaseJAXBUnmarshaller(JAXBContext context, Unmarshaller unmarshaller) {
        if (log.isDebugEnabled()) {
            log.debug("Unmarshaller placed back into pool");
        }
        if (ENABLE_UNMARSHALL_POOLING) {
        	unmarshaller.setAttachmentUnmarshaller(null);
            upool.put(context, unmarshaller);
        }
    }

    /**
     * Get JAXBMarshaller
     *
     * @param context JAXBContext
     * @return Marshaller
     * @throws JAXBException
     */
    public static Marshaller getJAXBMarshaller(JAXBContext context) throws JAXBException {
        Marshaller m = null;
        if (!ENABLE_MARSHALL_POOLING) {
            if (log.isDebugEnabled()) {
                log.debug("Marshaller created [no pooling]");
            }
            m = context.createMarshaller();
        } else {
            m = mpool.get(context);
            if (m == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Marshaller created [not in pool]");
                }
                m = context.createMarshaller();
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Marshaller obtained [from  pool]");
                }
            }
        }
        m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE); // No PIs
        return m;
    }

    /**
     * releaseJAXBMarshalller Do not call this method if an exception occurred while using the
     * Marshaller. We object my be in an invalid state.
     *
     * @param context    JAXBContext
     * @param marshaller Marshaller
     */
    public static void releaseJAXBMarshaller(JAXBContext context, Marshaller marshaller) {
        if (log.isDebugEnabled()) {
            log.debug("Marshaller placed back into pool");
        }
        if (ENABLE_MARSHALL_POOLING) {
            marshaller.setAttachmentMarshaller(null);
            mpool.put(context, marshaller);
        }
    }

    /**
     * get JAXB Introspector
     *
     * @param context JAXBContext
     * @return JAXBIntrospector
     * @throws JAXBException
     */
    public static JAXBIntrospector getJAXBIntrospector(JAXBContext context) throws JAXBException {
        JAXBIntrospector i = null;
        if (!ENABLE_INTROSPECTION_POOLING) {
            if (log.isDebugEnabled()) {
                log.debug("JAXBIntrospector created [no pooling]");
            }
            i = context.createJAXBIntrospector();
        } else {
            i = ipool.get(context);
            if (i == null) {
                if (log.isDebugEnabled()) {
                    log.debug("JAXBIntrospector created [not in pool]");
                }
                i = context.createJAXBIntrospector();
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("JAXBIntrospector obtained [from  pool]");
                }
            }
        }
        return i;
    }

    /**
     * Release JAXBIntrospector Do not call this method if an exception occurred while using the
     * JAXBIntrospector. We object my be in an invalid state.
     *
     * @param context      JAXBContext
     * @param introspector JAXBIntrospector
     */
    public static void releaseJAXBIntrospector(JAXBContext context, JAXBIntrospector introspector) {
        if (log.isDebugEnabled()) {
            log.debug("JAXBIntrospector placed back into pool");
        }
        if (ENABLE_INTROSPECTION_POOLING) {
            ipool.put(context, introspector);
        }
    }

    /**
     * @param p  Package
     * @param cl
     * @return true if each package has a ObjectFactory class or package-info
     */
    private static boolean checkPackage(String p, ClassLoader cl) {

        // Each package must have an ObjectFactory
        if (log.isDebugEnabled()) {
            log.debug("checking package :" + p);

        }
        try {
            Class cls = forName(p + ".ObjectFactory", false, cl);
            if (cls != null) {
                return true;
            }
            //Catch Throwable as ClassLoader can throw an NoClassDefFoundError that
            //does not extend Exception. So we will absorb any Throwable exception here.
        } catch (Throwable e) {
            if (log.isDebugEnabled()) {
                log.debug("ObjectFactory Class Not Found " + e);
                log.debug("...caused by " + e.getCause() + " " + JavaUtils.stackToString(e));
            }
        }

        try {
            Class cls = forName(p + ".package-info", false, cl);
            if (cls != null) {
                return true;
            }
            //Catch Throwable as ClassLoader can throw an NoClassDefFoundError that
            //does not extend Exception. So we will absorb any Throwable exception here.
        } catch (Throwable e) {
            if (log.isDebugEnabled()) {
                log.debug("package-info Class Not Found " + e);
                log.debug("...caused by " + e.getCause() + " " + JavaUtils.stackToString(e));
            }
        }

        return false;
    }

    /**
     * Create a JAXBContext using the contextpath approach
     *
     * @param packages
     * @param cl       ClassLoader
     * @return JAXBContext or null if unsuccessful
     */
    private static JAXBContext createJAXBContextUsingContextPath(TreeSet<String> packages,
                                                                 ClassLoader cl) {
        JAXBContext context = null;
        String contextpath = "";

        // Iterate through the classes and build the contextpath
        Iterator<String> it = packages.iterator();
        while (it.hasNext()) {
            String p = it.next();
            if (contextpath.length() != 0) {
                contextpath += ":";
            }
            contextpath += p;

        }
        try {
            if (log.isDebugEnabled()) {
                log.debug("Attempting to create JAXBContext with contextPath=" + contextpath);
            }
            context = JAXBContext_newInstance(contextpath, cl);
            if (log.isDebugEnabled()) {
                log.debug("  Successfully created JAXBContext:" + context);
            }
        } catch (Throwable e) {
            if (log.isDebugEnabled()) {
                log.debug(
                        "  Unsuccessful: We will now use an alterative JAXBConstruct construction");
                log.debug("  Reason " + e.toString());
            }
        }
        return context;
    }

    /**
     * This method will return all the Class names needed to construct a JAXBContext
     *
     * @param pkg         Package
     * @param ClassLoader cl
     * @return
     * @throws ClassNotFoundException if error occurs getting package
     */
    private static List<Class> getAllClassesFromPackage(String pkg, ClassLoader cl) {
        if (pkg == null) {
            return new ArrayList<Class>();
        }

        /*
        * This method is a best effort method.  We should always return an object.
        */

        ArrayList<Class> classes = new ArrayList<Class>();

        try {
            // This will load classes from directory
            List<Class> classesFromDir = getClassesFromDirectory(pkg, cl);
            checkClasses(classesFromDir, pkg);
            classes.addAll(classesFromDir);
        } catch (ClassNotFoundException e) {
            if (log.isDebugEnabled()) {
                log.debug("getClassesFromDirectory failed to get Classes");
            }
        }
        try {
            //If Clases not found in directory then look for jar that has these classes
            if (classes.size() <= 0) {
                //This will load classes from jar file.
                ClassFinderFactory cff =
                        (ClassFinderFactory)FactoryRegistry.getFactory(ClassFinderFactory.class);
                ClassFinder cf = cff.getClassFinder();
                
                List<Class> classesFromJar = cf.getClassesFromJarFile(pkg, cl);
                
                checkClasses(classesFromJar, pkg);
                classes.addAll(classesFromJar);
            }
        } catch (ClassNotFoundException e) {
            if (log.isDebugEnabled()) {
                log.debug("getClassesFromJarFile failed to get Classes");
            }
        }

        return classes;
    }
    
    /**
     * @param list
     * @param pkg
     */
    private static void checkClasses(List<Class> list, String pkg) {
        // The installed classfinder or directory search may inadvertently add too many 
        // classes.  This rountine is a 'double check' to make sure that the classes
        // are acceptable.
        for (int i=0; i<list.size();) {
            Class cls = list.get(i);
            if (!cls.isInterface() &&
                ClassUtils.getDefaultPublicConstructor(cls) != null &&
               !ClassUtils.isJAXWSClass(cls) &&
               cls.getPackage().getName().equals(pkg)) {
                i++; // Acceptable class
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Removing class " + cls + " from consideration because it is not in package " + pkg +
                              " or is an interface or does not have a public constructor or is" +
                              " a jaxws class");
                }
                list.remove(i);
            }
        }
    }

    private static ArrayList<Class> getClassesFromDirectory(String pkg, ClassLoader cl)
            throws ClassNotFoundException {
        // This will hold a list of directories matching the pckgname. There may be more than one if a package is split over multiple jars/paths
        String pckgname = pkg;
        ArrayList<File> directories = new ArrayList<File>();
        try {
            String path = pckgname.replace('.', '/');
            // Ask for all resources for the path
            Enumeration<URL> resources = cl.getResources(path);
            while (resources.hasMoreElements()) {
                directories.add(new File(
                        URLDecoder.decode(resources.nextElement().getPath(), "UTF-8")));
            }
        } catch (UnsupportedEncodingException e) {
            if (log.isDebugEnabled()) {
                log.debug(
                        pckgname + " does not appear to be a valid package (Unsupported encoding)");
            }
            throw new ClassNotFoundException(Messages.getMessage("ClassUtilsErr2", pckgname));
        } catch (IOException e) {
            if (log.isDebugEnabled()) {
                log.debug(
                        "IOException was thrown when trying to get all resources for " + pckgname);
            }
            throw new ClassNotFoundException(Messages.getMessage("ClassUtilsErr3", pckgname));
        }

        ArrayList<Class> classes = new ArrayList<Class>();
        // For every directory identified capture all the .class files
        for (File directory : directories) {
            if (log.isDebugEnabled()) {
                log.debug("  Adding JAXB classes from directory: " + directory.getName());
            }
            if (directory.exists()) {
                // Get the list of the files contained in the package
                String[] files = directory.list();
                for (String file : files) {
                    // we are only interested in .class files
                    if (file.endsWith(".class")) {
                        // removes the .class extension
                        // TODO Java2 Sec
                        String className = pckgname + '.' + file.substring(0, file.length() - 6);
                        try {
                            Class clazz = forName(className,
                                                  false, getContextClassLoader());
                            // Don't add any interfaces or JAXWS specific classes.  
                            // Only classes that represent data and can be marshalled 
                            // by JAXB should be added.
                            if (!clazz.isInterface()
                                    && (clazz.isEnum() ||
                                        ClassUtils.getDefaultPublicConstructor(clazz) != null)
                                    && !ClassUtils.isJAXWSClass(clazz)
                                    && !java.lang.Exception.class.isAssignableFrom(clazz)) {

                                // Ensure that all the referenced classes are loadable too
                                clazz.getDeclaredMethods();
                                clazz.getDeclaredFields();

                                if (log.isDebugEnabled()) {
                                    log.debug("Adding class: " + file);
                                }
                                classes.add(clazz);

                                // REVIEW:
                                // Support of RPC list (and possibly other scenarios) requires that the array classes should also be present.
                                // This is a hack until we can determine how to get this information.

                                // The arrayName and loadable name are different.  Get the loadable
                                // name, load the array class, and add it to our list
                                //className += "[]";
                                //String loadableName = ClassUtils.getLoadableClassName(className);

                                //Class aClazz = Class.forName(loadableName, false, Thread.currentThread().getContextClassLoader());
                            }
                            //Catch Throwable as ClassLoader can throw an NoClassDefFoundError that
                            //does not extend Exception
                        } catch (Throwable e) {
                            if (log.isDebugEnabled()) {
                                log.debug("Tried to load class " + className +
                                        " while constructing a JAXBContext.  This class will be skipped.  Processing Continues.");
                                log.debug("  The reason that class could not be loaded:" +
                                        e.toString());
                                log.debug(JavaUtils.stackToString(e));
                            }
                        }

                    }
                }
            }
        }
        
        return classes;
    }

    private static String[] commonArrayClasses = new String[] {
            // primitives
            "boolean[]",
            "byte[]",
            "char[][]",
            "double[]",
            "float[]",
            "int[]",
            "long[]",
            "short[]",
            "java.lang.String[]",
            // Others
            "java.lang.Object[]",
            "java.awt.Image[]",
            "java.math.BigDecimal[]",
            "java.math.BigInteger[]",
            "java.util.Calendar[]",
            "javax.xml.namespace.QName[]" };

    private static void addCommonArrayClasses(List<Class> list) {
        // Add common primitives arrays (necessary for RPC list type support)
        ClassLoader cl = getContextClassLoader();


        for (int i = 0; i < commonArrayClasses.length; i++) {
            String className = commonArrayClasses[i];
            try {
                // Load and add the class
                Class cls = forName(ClassUtils.getLoadableClassName(className), false, cl);
                list.add(cls);
                //Catch Throwable as ClassLoader can throw an NoClassDefFoundError that
                //does not extend Exception
            } catch (Throwable e) {
                if (log.isDebugEnabled()) {
                    log.debug("Tried to load class " + className +
                            " while constructing a JAXBContext.  This class will be skipped.  Processing Continues.");
                    log.debug("  The reason that class could not be loaded:" + e.toString());
                    log.debug(JavaUtils.stackToString(e));
                }
            }
        }
    }

    /** @return ClassLoader */
    private static ClassLoader getContextClassLoader() {
        // NOTE: This method must remain private because it uses AccessController
        ClassLoader cl = null;
        try {
            cl = (ClassLoader)AccessController.doPrivileged(
                    new PrivilegedExceptionAction() {
                        public Object run() throws ClassNotFoundException {
                            return Thread.currentThread().getContextClassLoader();
                        }
                    }
            );
        } catch (PrivilegedActionException e) {
            if (log.isDebugEnabled()) {
                log.debug("Exception thrown from AccessController: " + e);
            }
            throw ExceptionFactory.makeWebServiceException(e.getException());
        }

        return cl;
    }

    /**
     * Return the class for this name
     *
     * @return Class
     */
    private static Class forName(final String className, final boolean initialize,
                                 final ClassLoader classloader) throws ClassNotFoundException {
        // NOTE: This method must remain private because it uses AccessController
        Class cl = null;
        try {
            cl = (Class)AccessController.doPrivileged(
                    new PrivilegedExceptionAction() {
                        public Object run() throws ClassNotFoundException {
                            // Class.forName does not support primitives
                            Class cls = ClassUtils.getPrimitiveClass(className);
                            if (cls == null) {
                                cls = Class.forName(className, initialize, classloader);
                            }
                            return cls;
                        }
                    }
            );
        } catch (PrivilegedActionException e) {
            if (log.isDebugEnabled()) {
                log.debug("Exception thrown from AccessController: " + e);
            }
            throw (ClassNotFoundException)e.getException();
        }

        return cl;
    }

    /**
     * Create JAXBContext from context String and ClassLoader
     *
     * @param context
     * @param classloader
     * @return
     * @throws Exception
     */
    private static JAXBContext JAXBContext_newInstance(final String context,
                                                       final ClassLoader classloader)
            throws Exception {
        // NOTE: This method must remain private because it uses AccessController
        JAXBContext jaxbContext = null;
        try {
            if (log.isDebugEnabled()) {
                if (context == null || context.length() == 0) {
                    log.debug("JAXBContext is constructed without a context String.");
                } else {
                    log.debug("JAXBContext is constructed with a context of:" + context);
                }
            }
            jaxbContext = (JAXBContext)AccessController.doPrivileged(
                    new PrivilegedExceptionAction() {
                        public Object run() throws JAXBException {
                            return JAXBContext.newInstance(context, classloader);
                        }
                    }
            );
        } catch (PrivilegedActionException e) {
            if (log.isDebugEnabled()) {
                log.debug("Exception thrown from AccessController: " + e);
            }
            throw e.getException();
        }

        return jaxbContext;
    }

    /**
     * Create JAXBContext from Class[]
     *
     * @param classArray
     * @param cl ClassLoader that loaded the classes
     * @return
     * @throws Exception
     */
    private static JAXBContext JAXBContext_newInstance(final Class[] classArray, 
                                                       final ClassLoader cl)
            throws JAXBException {
        // NOTE: This method must remain private because it uses AccessController
        JAXBContext jaxbContext = null;
        try {
            if (log.isDebugEnabled()) {
                if (classArray == null || classArray.length == 0) {
                    log.debug("JAXBContext is constructed with 0 input classes.");
                } else {
                    log.debug("JAXBContext is constructed with " + classArray.length +
                            " input classes.");
                }
            }
            jaxbContext = (JAXBContext)AccessController.doPrivileged(
                    new PrivilegedExceptionAction() {
                        public Object run() throws JAXBException {
                            // Unlike the JAXBContext.newInstance(Class[]) method
                            // does now accept a classloader.  To workaround this
                            // issue, the classloader is temporarily changed to cl
                            Thread currentThread = Thread.currentThread();
                            ClassLoader savedClassLoader = currentThread.getContextClassLoader();
                            try {
                                currentThread.setContextClassLoader(cl);
                                return JAXBContext.newInstance(classArray);
                            } finally {
                                currentThread.setContextClassLoader(savedClassLoader);
                            }
                        }
                    }
            );
        } catch (PrivilegedActionException e) {
            if (log.isDebugEnabled()) {
                log.debug("Exception thrown from AccessController: " + e);
                log.debug("  Exception is " + e.getException());
            }
            if (e.getException() instanceof JAXBException) {
                throw (JAXBException)e.getException();
            } else if (e.getException() instanceof RuntimeException) {
                throw ExceptionFactory.makeWebServiceException(e.getException());
            }
        }

        return jaxbContext;
    }

    /** Holds the JAXBContext and the manner by which it was constructed */
    static class JAXBContextValue {

        public JAXBContext jaxbContext;
        public CONSTRUCTION_TYPE constructionType;

        public JAXBContextValue(JAXBContext jaxbContext, CONSTRUCTION_TYPE constructionType) {
            this.jaxbContext = jaxbContext;
            this.constructionType = constructionType;
        }
    }
    
    static private void adjustPoolSize(Map map) {
        if (map.size() > MAX_LOAD_FACTOR) {
            // Remove every other Entry in the map.
            Iterator it = map.entrySet().iterator();
            boolean removeIt = false;
            while (it.hasNext()) {
                it.next();
                if (removeIt) {
                    it.remove();
                }
                removeIt = !removeIt;
            }
        }
    }

    /**
     * Pool a list of items for a specific key
     *
     * @param <K> Key
     * @param <V> Pooled object
     */
    private static class Pool<K,V> {
        private Map<K,List<V>> map = new ConcurrentHashMap<K, List<V>>();

        // The maps are freed up when a LOAD FACTOR is hit
        private static int MAX_LIST_FACTOR = 10;
        
        /**
         * @param key
         * @return removed item from pool or null.
         */
        public V get(K key) {
            List<V> values = getValues(key);
            synchronized (values) {
                if(values.size()>0) {
                    return values.remove(values.size()-1);
                }
            }
            return null;
        }

        /**
         * Add item back to pool
         * @param key
         * @param value
         */
        public void put(K key, V value) {
            adjustSize();
            List<V> values = getValues(key);
            synchronized (values) {
                if (values.size() < MAX_LIST_FACTOR) {
                    values.add(value);
                }
            }
        }

        /**
         * Get or create a list of the values for the key
         * @param key
         * @return list of values.
         */
        private List<V> getValues(K key) {
            List<V> values = map.get(key);
            if(values !=null) {
                return values;
            }
            synchronized (this) {
                values = map.get(key);
                if(values==null) {
                    values = new ArrayList<V>();
                    map.put(key, values);
                }
                return values;
            }
        }
        
        /**
         * AdjustSize
         * When the number of keys exceeds the maximum load, half
         * of the entries are deleted.
         * 
         * The assumption is that the JAXBContexts, UnMarshallers, Marshallers, etc. require
         * a large footprint.
         */
        private void adjustSize() {
            if (map.size() > MAX_LOAD_FACTOR) {
                // Remove every other Entry in the map.
                Iterator it = map.entrySet().iterator();
                boolean removeIt = false;
                while (it.hasNext()) {
                    it.next();
                    if (removeIt) {
                        it.remove();
                    }
                    removeIt = !removeIt;
                }
            }
        }
    }

}
