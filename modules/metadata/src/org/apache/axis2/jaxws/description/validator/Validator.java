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
package org.apache.axis2.jaxws.description.validator;

import java.util.Iterator;

/**
 * 
 */
public abstract class Validator {
    public enum ValidationLevel {
        OFF, FULL}

    ;
    public static final boolean VALID = true;
    public static final boolean INVALID = false;

    protected ValidationFailures validationFailures = new ValidationFailures();
    // TODO: turn on validation and change the ValidateWSDL test to start checking for failures.
    private ValidationLevel validationLevel = ValidationLevel.FULL;

    abstract public boolean validate();

    void addValidationFailure(Validator failingValidator, String message) {
        validationFailures.add(failingValidator, message);
    }

    ValidationLevel getValidationLevel() {
        return validationLevel;
    }

    public String toString() {
        String messageString = "";
        Iterator<ValidationFailure> failureIter = validationFailures.
                getValidationFailures().iterator();
        while (failureIter.hasNext()) {
            ValidationFailure failure = failureIter.next();
            messageString = messageString + " :: " + failure.getMessage();
            Validator validator = failure.getValidator();
            // if this is a different validator reference we want to call
            // toString on it also
            if (validator != this) {
                messageString = messageString + " :: " + validator.toString();
            }
        }
        return messageString;
    }
}
