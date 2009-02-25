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

package javax.xml.namespace;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * <code>QName</code> class represents the value of a qualified name as specified in <a
 * href="http://www.w3.org/TR/xmlschema-2/#QName">XML Schema Part2: Datatypes specification</a>.
 * <p/>
 * The value of a QName contains a <b>namespaceURI</b>, a <b>localPart</b> and a <b>prefix</b>. The
 * localPart provides the local part of the qualified name. The namespaceURI is a URI reference
 * identifying the namespace.
 *
 * @version 1.1
 */
public class QName implements Serializable {

    private static final long serialVersionUID = -6756054858541526837L;

    /** Comment/shared empty <code>String</code>. */
    private static final String emptyString = "".intern();

    private String namespaceURI;

    private String localPart;

    private String prefix;

    /**
     * Constructor for the QName.
     *
     * @param localPart local part of the QName
     */
    public QName(String localPart) {
        this(emptyString, localPart, emptyString);
    }

    /**
     * Constructor for the QName.
     *
     * @param namespaceURI namespace URI for the QName
     * @param localPart    local part of the QName.
     */
    public QName(String namespaceURI, String localPart) {
        this(namespaceURI, localPart, emptyString);
    }

    /**
     * Constructor for the QName.
     *
     * @param namespaceURI Namespace URI for the QName
     * @param localPart    Local part of the QName.
     * @param prefix       Prefix of the QName.
     */
    public QName(String namespaceURI, String localPart, String prefix) {
        this.namespaceURI = (namespaceURI == null)
                ? emptyString
                : namespaceURI.intern();
        if (localPart == null) {
            throw new IllegalArgumentException("invalid QName local part");
        } else {
            this.localPart = localPart.intern();
        }

        if (prefix == null) {
            throw new IllegalArgumentException("invalid QName prefix");
        } else {
            this.prefix = prefix.intern();
        }
    }

    /**
     * Gets the namespace URI for this QName.
     *
     * @return namespace URI
     */
    public String getNamespaceURI() {
        return namespaceURI;
    }

    /**
     * Gets the local part for this QName.
     *
     * @return the local part
     */
    public String getLocalPart() {
        return localPart;
    }

    /**
     * Gets the prefix for this QName.
     *
     * @return the prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Returns a string representation of this QName.
     *
     * @return a string representation of the QName
     */
    public String toString() {

        return ((namespaceURI == emptyString)
                ? localPart
                : '{' + namespaceURI + '}' + localPart);
    }

    /**
     * Tests this QName for equality with another object.
     * <p/>
     * If the given object is not a QName or is null then this method returns <tt>false</tt>.
     * <p/>
     * For two QNames to be considered equal requires that both localPart and namespaceURI must be
     * equal. This method uses <code>String.equals</code> to check equality of localPart and
     * namespaceURI. Any class that extends QName is required to satisfy this equality contract.
     * <p/>
     * This method satisfies the general contract of the <code>Object.equals</code> method.
     *
     * @param obj the reference object with which to compare
     * @return <code>true</code> if the given object is identical to this QName: <code>false</code>
     *         otherwise.
     */
    public boolean equals(Object obj) {

        if (obj == this) {
            return true;
        }

        if (!(obj instanceof QName)) {
            return false;
        }

        return (namespaceURI.equals(((QName)obj).namespaceURI))
                && (localPart.equals(((QName)obj).localPart));

    }

    /**
     * Returns a QName holding the value of the specified String.
     * <p/>
     * The string must be in the form returned by the QName.toString() method, i.e.
     * "{namespaceURI}localPart", with the "{namespaceURI}" part being optional.
     * <p/>
     * This method doesn't do a full validation of the resulting QName. In particular, it doesn't
     * check that the resulting namespace URI is a legal URI (per RFC 2396 and RFC 2732), nor that
     * the resulting local part is a legal NCName per the XML Namespaces specification.
     *
     * @param s the string to be parsed
     * @return QName corresponding to the given String
     * @throws IllegalArgumentException
     *          If the specified String cannot be parsed as a QName
     */
    public static QName valueOf(String s) {

        if ((s == null) || "".equals(s)) {
            throw new IllegalArgumentException("invalid QName literal");
        }

        if (s.charAt(0) == '{') {
            int i = s.indexOf('}');

            if (i == -1) {
                throw new IllegalArgumentException("invalid QName literal");
            }

            if (i == s.length() - 1) {
                throw new IllegalArgumentException("invalid QName literal");
            } else {
                return new QName(s.substring(1, i), s.substring(i + 1));
            }
        } else {
            return new QName(s);
        }
    }

    /**
     * Returns a hash code value for this QName object. The hash code is based on both the localPart
     * and namespaceURI parts of the QName. This method satisfies the  general contract of the
     * <code>Object.hashCode</code> method.
     *
     * @return a hash code value for this Qname object
     */
    public int hashCode() {
        return namespaceURI.hashCode() ^ localPart.hashCode();
    }

    /**
     * Ensure that deserialization properly interns the results.
     *
     * @param in the ObjectInputStream to be read
     * @throws IOException            if there was a failure in the object input stream
     * @throws ClassNotFoundException if the class of any sub-objects could not be found
     */
    private void readObject(ObjectInputStream in) throws
            IOException, ClassNotFoundException {
        in.defaultReadObject();

        namespaceURI = namespaceURI.intern();
        localPart = localPart.intern();
        prefix = prefix.intern();
    }
}

