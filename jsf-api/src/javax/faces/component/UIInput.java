/*
 * $Id: UIInput.java,v 1.96.8.1 2008/04/21 20:31:24 edburns Exp $
 */

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package javax.faces.component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.logging.Logger;
import javax.el.ELException;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.el.MethodBinding;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.ExceptionQueuedEventContext;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;
import javax.faces.event.ValueChangeListener;
import javax.faces.render.Renderer;
import javax.faces.validator.BeanValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

/**
 * <p><span class="changed_modified_2_0"><strong>UIInput</strong></span>
 * is a {@link UIComponent} that represents a component that both
 * displays output to the user (like {@link UIOutput} components do) and
 * processes request parameters on the subsequent request that need to
 * be decoded.  There are no restrictions on the data type of the local
 * value, or the object referenced by the value binding expression (if
 * any); however, individual {@link javax.faces.render.Renderer}s will
 * generally impose restrictions on the type of data they know how to
 * display.</p> 
 *
 * <p>During the <em>Apply Request Values</em> phase
 * of the request processing lifecycle, the decoded value of this
 * component, usually but not necessarily a String, must be stored - but
 * not yet converted - using <code>setSubmittedValue()</code>.  If the
 * component wishes to indicate that no particular value was submitted,
 * it can either do nothing, or set the submitted value to
 * <code>null</code>.</p>

 * <p>By default, during the <em>Process Validators</em> phase of the
 * request processing lifecycle, the submitted value will be converted
 * to a typesafe object, and, if validation succeeds, stored as a local
 * value using <code>setValue()</code>.  However, if the
 * <code>immediate</code> property is set to <code>true</code>, this
 * processing will occur instead at the end of the <em>Apply Request
 * Values</em> phase.</p> 

 * <p>During the <em>Render Response</em> phase of the request
 * processing lifecycle, conversion for output occurs as for {@link
 * UIOutput}.</p>

 * <p>When the <code>validate()</code> method of this {@link UIInput}
 * detects that a value change has actually occurred, and that all
 * validations have been successfully passed, it will queue a {@link
 * ValueChangeEvent}.  Later on, the <code>broadcast()</code> method
 * will ensure that this event is broadcast to all interested listeners.
 * This event will be delivered by default in the <em>Process
 * Validators</em> phase, but can be delivered instead during <em>Apply
 * Request Values</em> if the <code>immediate</code> property is set to
 * <code>true</code>. <span class="changed_added_2_0">If the validation
 * fails, the implementation must call {@link
 * FacesContext#validationFailed}.</span></p>

 * <p>By default, the <code>rendererType</code> property must be set to
 * "<code>Text</code>".  This value can be changed by calling the
 * <code>setRendererType()</code> method.</p>
 */

public class UIInput extends UIOutput implements EditableValueHolder {

    private static Logger LOGGER = Logger.getLogger("javax.faces.component",
            "javax.faces.LogStrings");

    // ------------------------------------------------------ Manifest Constants


    /**
     * <p>The standard component type for this component.</p>
     */
    public static final String COMPONENT_TYPE = "javax.faces.Input";


    /**
     * <p>The standard component family for this component.</p>
     */
    public static final String COMPONENT_FAMILY = "javax.faces.Input";


    /**
     * <p>The message identifier of the
     * {@link javax.faces.application.FacesMessage} to be created if
     * a conversion error occurs, and neither the page author nor
     * the {@link ConverterException} provides a message.</p>
     */
    public static final String CONVERSION_MESSAGE_ID =
         "javax.faces.component.UIInput.CONVERSION";


    /**
     * <p>The message identifier of the
     * {@link javax.faces.application.FacesMessage} to be created if
     * a required check fails.</p>
     */
    public static final String REQUIRED_MESSAGE_ID =
         "javax.faces.component.UIInput.REQUIRED";

    /**
     * <p>The message identifier of the
     * {@link javax.faces.application.FacesMessage} to be created if
     * a model update error occurs, and the thrown exception has
     * no message.</p>
     */
    public static final String UPDATE_MESSAGE_ID =
         "javax.faces.component.UIInput.UPDATE";


    /**
     * <p class="changed_added_2_0">The name of an application parameter
     * that indicates how empty values should be handled with respect to
     * validation.  See {@link #validateValue} for the allowable values
     * and specification of how they should be interpreted.</p>
     */

    public static final String VALIDATE_EMPTY_FIELDS_PARAM_NAME = 
	"javax.faces.VALIDATE_EMPTY_FIELDS";
    
    /**

     * <p class="changed_added_2_0">The value of this constant is used
     * as the attribute within the attributes <code>Map</code> on the
     * {@link UIComponent} to hold a <code>Map</code> of default
     * validators for a subtree of the component tree. Each key in the
     * <code>Map</code> is a validator id and the value is a
     * <code>Boolean</code> flag indicating whether the validator is
     * being activated or deactivated for this branch of the component
     * tree. Each activated validator gets registered on any {@link
     * EditableValueHolder} found in this subtree of the component
     * tree.</p>

     */
    public static final String DEFAULT_VALIDATOR_IDS_KEY = "javax.faces.component.DEFAULT_VALIDATOR_IDS";
    

    private static final Validator[] EMPTY_VALIDATOR = new Validator[0];

    private Boolean emptyStringIsNull;

    private Boolean validateEmptyFields;

    private boolean defaultValidatorsProcessed;

    enum PropertyKeys {
        /**
     * <p>The "localValueSet" state for this component.
     */
        localValueSet,

        /**
         * <p>If the input is required or not.</p>
         */
        required,

        /**
         * <p>Custom message to be displayed if input is required but non was submitted.</p>
         */
        requiredMessage,

        /**
         * <p>Custom message to be displayed when conversion fails.</p>
         */
        converterMessage,

        /**
         * <p>Custom message to be displayed when validation fails.</p>
         */
        validatorMessage,

        /**
         * <p>Flag indicating whether or not this component is valid.</p>
         */
        valid,

        /**
         * <p>Flag indicating when conversion/validation should occur.</p>
         */
        immediate,

    }

    // ------------------------------------------------------------ Constructors


    /**
     * <p>Create a new {@link UIInput} instance with default property
     * values.</p>
     */
    public UIInput() {

        super();
        setRendererType("javax.faces.Text");

    }

    // -------------------------------------------------------------- Properties


    public String getFamily() {

        return (COMPONENT_FAMILY);

    }


    /**
     * <p>The submittedValue value of this {@link UIInput} component.</p>
     */
    private Object submittedValue = null;


    /**
     * <p>Return the submittedValue value of this {@link UIInput} component.
     * This method should only be used by the <code>decode()</code> and
     * <code>validate()</code> method of this component, or
     * its corresponding {@link Renderer}.</p>
     */
    public Object getSubmittedValue() {

        return (this.submittedValue);

    }


    /**
     * <p>Set the submittedValue value of this {@link UIInput} component.
     * This method should only be used by the <code>decode()</code> and
     * <code>validate()</code> method of this component, or
     * its corresponding {@link Renderer}.</p>
     *
     * @param submittedValue The new submitted value
     */
    public void setSubmittedValue(Object submittedValue) {

        this.submittedValue = submittedValue;

    }

    public void setValue(Object value) {
        super.setValue(value);
        // Mark the local value as set.
        setLocalValueSet(true);
    }

    /**
     * <p>Convenience method to reset this component's value to the
     * un-initialized state.  This method does the following:</p>
     * <p/>
     * <p>Call {@link #setValue} passing <code>null</code>.</p>
     * <p/>
     * <p>Call {@link #setSubmittedValue} passing <code>null</code>.</p>
     * <p/>
     * <p>Call {@link #setLocalValueSet} passing <code>false</code>.</p>
     * <p/>
     * <p>Call {@link #setValid} passing <code>true</code>.</p>
     * <p/>
     * <p>Upon return from this call if the instance had a
     * <code>ValueBinding</code> associated with it for the "value"
     * property, this binding is evaluated when {@link
     * UIOutput#getValue} is called.  Otherwise, <code>null</code> is
     * returned from <code>getValue()</code>.</p>
     */

    public void resetValue() {
        this.setValue(null);
        this.setSubmittedValue(null);
        this.setLocalValueSet(false);
        this.setValid(true);
    }


    /**
     * Return the "local value set" state for this component.
     * Calls to <code>setValue()</code> automatically reset
     * this property to <code>true</code>.
     */
    public boolean isLocalValueSet() {
        return (Boolean) getStateHelper().eval(PropertyKeys.localValueSet, false);
    }

    /**
     * Sets the "local value set" state for this component.
     */
    public void setLocalValueSet(boolean localValueSet) {
        getStateHelper().put(PropertyKeys.localValueSet, localValueSet);
    }


    /**
     * <p>Return the "required field" state for this component.</p>
     */
    public boolean isRequired() {

        return (Boolean) getStateHelper().eval(PropertyKeys.required, false);

    }


    /**
     * <p>If there has been a call to {@link #setRequiredMessage} on this
     * instance, return the message.  Otherwise, call {@link #getValueExpression}
     * passing the key "requiredMessage", get the result of the expression, and return it.
     * Any {@link ELException}s thrown during the call to <code>getValue()</code>
     * must be wrapped in a {@link FacesException} and rethrown.
     */

    public String getRequiredMessage() {

        return (String) getStateHelper().eval(PropertyKeys.requiredMessage);

    }

    /**
     * <p>Override any {@link ValueExpression} set for the "requiredMessage"
     * with the literal argument provided to this method.  Subsequent calls
     * to {@link #getRequiredMessage} will return this value;</p>
     *
     * @param message the literal message value to be displayed in the event
     *                the user hasn't supplied a value and one is required.
     */

    public void setRequiredMessage(String message) {

        getStateHelper().put(PropertyKeys.requiredMessage,  message);

    }


    /**
     * <p>If there has been a call to {@link #setConverterMessage} on this
     * instance, return the message.  Otherwise, call {@link #getValueExpression}
     * passing the key "converterMessage", get the result of the expression, and return it.
     * Any {@link ELException}s thrown during the call to <code>getValue()</code>
     * must be wrapped in a {@link FacesException} and rethrown.
     */

    public String getConverterMessage() {

        return (String) getStateHelper().eval(PropertyKeys.converterMessage);

    }

    /**
     * <p>Override any {@link ValueExpression} set for the "converterMessage"
     * with the literal argument provided to this method.  Subsequent calls
     * to {@link #getConverterMessage} will return this value;</p>
     *
     * @param message the literal message value to be displayed in the event
     *                conversion fails.
     */

    public void setConverterMessage(String message) {

        getStateHelper().put(PropertyKeys.converterMessage, message);

    }


    /**
     * <p>If there has been a call to {@link #setValidatorMessage} on this
     * instance, return the message.  Otherwise, call {@link #getValueExpression}
     * passing the key "validatorMessage", get the result of the expression, and return it.
     * Any {@link ELException}s thrown during the call to <code>getValue()</code>
     * must be wrapped in a {@link FacesException} and rethrown.
     */

    public String getValidatorMessage() {

        return (String) getStateHelper().eval(PropertyKeys.validatorMessage);

    }

    /**
     * <p>Override any {@link ValueExpression} set for the "validatorMessage"
     * with the literal argument provided to this method.  Subsequent calls
     * to {@link #getValidatorMessage} will return this value;</p>
     *
     * @param message the literal message value to be displayed in the event
     *                validation fails.
     */

    public void setValidatorMessage(String message) {

        getStateHelper().put(PropertyKeys.validatorMessage, message);

    }


    public boolean isValid() {

        return (Boolean) getStateHelper().eval(PropertyKeys.valid, true);

    }


    public void setValid(boolean valid) {

        getStateHelper().put(PropertyKeys.valid, valid);

    }


    /**
     * <p>Set the "required field" state for this component.</p>
     *
     * @param required The new "required field" state
     */
    public void setRequired(boolean required) {

        getStateHelper().put(PropertyKeys.required, required);

    }


    public boolean isImmediate() {

        return (Boolean) getStateHelper().eval(PropertyKeys.immediate, false);

    }


    public void setImmediate(boolean immediate) {

        getStateHelper().put(PropertyKeys.immediate, immediate);

    }


    /**
     * <p>Return a <code>MethodBinding</code> pointing at a
     * method that will be called during <em>Process Validations</em>
     * phase of the request processing lifecycle, to validate the current
     * value of this component.</p>
     *
     * @deprecated {@link #getValidators} should be used instead.
     */
    public MethodBinding getValidator() {
        MethodBinding result = null;

        Validator[] curValidators = getValidators();
        // go through our lisetners list and find the one and only
        // MethodBindingValidator instance, if present.
        if (null != curValidators) {
            for (int i = 0; i < curValidators.length; i++) {
                // We are guaranteed to have at most one instance of
                // MethodBindingValidator in the curValidators list.
                if (MethodBindingValidator.class ==
                     curValidators[i].getClass()) {
                    result = ((MethodBindingValidator) curValidators[i]).
                         getWrapped();
                    break;
                }
            }
        }
        return result;

    }


    /**
     * <p>Set a <code>MethodBinding</code> pointing at a
     * method that will be called during <em>Process Validations</em>
     * phase of the request processing lifecycle, to validate the current
     * value of this component.</p>
     * <p/>
     * <p>Any method referenced by such an expression must be public, with
     * a return type of <code>void</code>, and accept parameters of type
     * {@link FacesContext}, {@link UIComponent}, and <code>Object</code>.</p>
     *
     * @param validatorBinding The new <code>MethodBinding</code> instance
     * @deprecated Use {@link #addValidator} instead, obtaining the
     *             argument {@link Validator} by creating an instance of {@link
     *             javax.faces.validator.MethodExpressionValidator}.
     */
    public void setValidator(MethodBinding validatorBinding) {
        Validator[] curValidators = getValidators();
        // see if we need to null-out, or replace an existing validator
        if (null != curValidators) {
            for (int i = 0; i < curValidators.length; i++) {
                // if we want to remove the validatorBinding
                if (null == validatorBinding) {
                    // We are guaranteed to have at most one instance of
                    // MethodBindingValidator in the curValidators
                    // list.
                    if (MethodBindingValidator.class ==
                         curValidators[i].getClass()) {
                        removeValidator(curValidators[i]);
                        return;
                    }
                }
                // if we want to replace the validatorBinding
                else //noinspection ObjectEquality
                    if (validatorBinding == curValidators[i]) {
                    removeValidator(curValidators[i]);
                    break;
                }
            }
        }
        addValidator(new MethodBindingValidator(validatorBinding));

    }

    public MethodBinding getValueChangeListener() {
        MethodBinding result = null;

        ValueChangeListener[] curListeners = getValueChangeListeners();
        // go through our lisetners list and find the one and only
        // MethodBindingValueChangeListener instance, if present.
        if (null != curListeners) {
            for (int i = 0; i < curListeners.length; i++) {
                // We are guaranteed to have at most one instance of
                // MethodBindingValueChangeListener in the curListeners list.
                if (MethodBindingValueChangeListener.class ==
                     curListeners[i].getClass()) {
                    result = ((MethodBindingValueChangeListener) curListeners[i]).
                         getWrapped();
                    break;
                }
            }
        }
        return result;
    }


    /**
     * {@inheritDoc}
     *
     * @deprecated Use {@link #addValueChangeListener} instead, obtaining the
     *             argument {@link ValueChangeListener} by creating an instance of {@link
     *             javax.faces.event.MethodExpressionValueChangeListener}.
     */
    public void setValueChangeListener(MethodBinding valueChangeListener) {

        ValueChangeListener[] curListeners = getValueChangeListeners();
        // see if we need to null-out, or replace an existing listener
        if (null != curListeners) {
            for (int i = 0; i < curListeners.length; i++) {
                // if we want to remove the valueChangeListener
                if (null == valueChangeListener) {
                    // We are guaranteed to have at most one instance of
                    // MethodBindingValueChangeListener in the curListeners
                    // list.
                    if (MethodBindingValueChangeListener.class ==
                         curListeners[i].getClass()) {
                        removeFacesListener(curListeners[i]);
                        return;
                    }
                }
                // if we want to replace the valueChangeListener
                else //noinspection ObjectEquality
                    if (valueChangeListener == curListeners[i]) {
                    removeFacesListener(curListeners[i]);
                    break;
                }
            }
        }
        addValueChangeListener(new MethodBindingValueChangeListener(valueChangeListener));
    }

    // ----------------------------------------------------- UIComponent Methods


    /**
     * <p>
     * In addition to the actions taken in {@link UIOutput}
     * when {@link PartialStateHolder#markInitialState()} is called,
     * check if any of the installed {@link Validator}s are PartialStateHolders and
     * if so, call {@link javax.faces.component.PartialStateHolder#markInitialState()}
     * as appropriate.
     * </p>
     */
    @Override
    public void markInitialState() {

        addDefaultValidators(FacesContext.getCurrentInstance());
        super.markInitialState();
        if (validators != null) {
            validators.markInitialState();
        }

    }


    @Override
    public void clearInitialState() {

        if (initialStateMarked()) {
            super.clearInitialState();
            if (validators != null) {
                validators.clearInitialState();
            }
        }

    }

    /**
     * <p class="changed_added_2_0">Override the default superclass
     * behavior to provide conditional support for Beans Validation behavior.
     * If Beans Validation is not available in the current environment,
     * this method must take no action.  After
     * encoding the component, add any default validators. Putting the
     * logic in this method allows this service to be provided even for
     * inputs that are added programmatically because this method is
     * guaranteed to be called before any validations happen on a
     * subsequent postback.</p>

     * <div class="changed_added_2_0">

     * <p>Append the default validators after
     * any locally defined validators.  The validators to be appended
     * must be discovered in the following manner.  Let <em>toAdd</em>
     * be a logical list of validatorIds to add.</p>

     * <ul>
     *
     * <li><p>Call {@link Application#getDefaultValidatorInfo} and add
     * each key from the returned <code>Map</code> to
     * <em>toAdd</em>.</p></li>

     * <li><p>Perform following algorithm
     * <em>collectDefaultValidatorIds</em> (or its semantic equivalent)
     * for each ancestor component between <em>this</em> component
     * instance up to and including the {@link UIViewRoot}.</p>

     * <ul>

     * 	  <li><p>Let <em>defaultValidatiorIds</em> be the value in the
     * 	  current component's attribute <code>Map</code> under the key
     * 	  given by the value of the symbolic constant {@link
     * 	  #DEFAULT_VALIDATOR_IDS_KEY}.</p></li>

     * 	  <li><p>If <em>defaultValidatiorIds</em> is a
     * 	  <code>List</code>, assume it is a
     * 	  <code>List&lt;String&gt;</code> and add all of its values to
     * 	  <em>toAdd</em>.</p></li>

     * 	  <li><p>Perform <em>collectDefaultValidatorIds</em> on the
     * 	  component's parent, if present.</p></li>

     * 	  <li><p>If <em>defaultValidatiorIds</em> is a <code>Map</code>,
     * 	  assume it is a <code>Map&lt;String, Boolean&gt;</code> and
     * 	  interpret the key to be a validatorId, and the value to be a
     * 	  boolean indicating whether or not the user has flagged this
     * 	  particular validator instance as enabled or disabled.  For
     * 	  each entry in the map whose value is enabled, add the
     * 	  corresponding validatorId to <em>toAdd</em>.</p></li>

     * </ul>

     * </li>

     * <p>For each validatorId in <em>toAdd</em>, the validator is
     * created in the normal way using the <code>Application</code>
     * object. If a validator with the same validator id already exists
     * on the component, then that default validator is skipped.</p>

     * </div>

     * @throws NullPointerException {@inheritDoc}
     * @throws IOException {@inheritDoc}

     */
    @Override
    public void encodeEnd(FacesContext context) throws IOException {
        super.encodeEnd(context);
        // QUESTION is there another dependable way to perform this only once?
        addDefaultValidators(context);
    }

    /**
     * <p>Specialized decode behavior on top of that provided by the
     * superclass.  In addition to the standard
     * <code>processDecodes</code> behavior inherited from {@link
     * UIComponentBase}, calls <code>validate()</code> if the the
     * <code>immediate</code> property is true; if the component is
     * invalid afterwards or a <code>RuntimeException</code> is thrown,
     * calls {@link FacesContext#renderResponse}.  </p>
     *
     * @throws NullPointerException {@inheritDoc}
     */
    public void processDecodes(FacesContext context) {

        if (context == null) {
            throw new NullPointerException();
        }

        // Skip processing if our rendered flag is false
        if (!isRendered()) {
            return;
        }

        super.processDecodes(context);

        if (isImmediate()) {
            executeValidate(context);
        }
    }

    /**
     * <p>In addition to the standard <code>processValidators</code> behavior
     * inherited from {@link UIComponentBase}, calls <code>validate()</code>
     * if the <code>immediate</code> property is false (which is the
     * default);  if the component is invalid afterwards, calls
     * {@link FacesContext#renderResponse}.
     * If a <code>RuntimeException</code> is thrown during
     * validation processing, calls {@link FacesContext#renderResponse}
     * and re-throw the exception.
     * </p>
     *
     * @throws NullPointerException {@inheritDoc}
     */
    public void processValidators(FacesContext context) {

        if (context == null) {
            throw new NullPointerException();
        }

        // Skip processing if our rendered flag is false
        if (!isRendered()) {
            return;
        }

        super.processValidators(context);
        if (!isImmediate()) {
            executeValidate(context);
        }
    }

    /**
     * <p>In addition to the standard <code>processUpdates</code> behavior
     * inherited from {@link UIComponentBase}, calls
     * <code>updateModel()</code>.
     * If the component is invalid afterwards, calls
     * {@link FacesContext#renderResponse}.
     * If a <code>RuntimeException</code> is thrown during
     * update processing, calls {@link FacesContext#renderResponse}
     * and re-throw the exception.
     * </p>
     *
     * @throws NullPointerException {@inheritDoc}
     */
    public void processUpdates(FacesContext context) {

        if (context == null) {
            throw new NullPointerException();
        }

        // Skip processing if our rendered flag is false
        if (!isRendered()) {
            return;
        }

        super.processUpdates(context);

        try {
            updateModel(context);
        } catch (RuntimeException e) {
            context.renderResponse();
            throw e;
        }

        if (!isValid()) {
            context.renderResponse();
        }
    }

    /**
     * @throws NullPointerException {@inheritDoc}
     */
    public void decode(FacesContext context) {

        if (context == null) {
            throw new NullPointerException();
        }

        // Force validity back to "true"
        setValid(true);
        super.decode(context);
    }

    /**
     * <p><span class="changed_modified_2_0">Perform</span>
     * the following algorithm to update the model data
     * associated with this {@link UIInput}, if any, as appropriate.</p>
     * <ul>
     * <li>If the <code>valid</code> property of this component is
     * <code>false</code>, take no further action.</li>
     * <li>If the <code>localValueSet</code> property of this component is
     * <code>false</code>, take no further action.</li>
     * <li>If no {@link ValueExpression} for <code>value</code> exists,
     * take no further action.</li>
     * <li>Call <code>setValue()</code> method of the {@link ValueExpression}
     * to update the value that the {@link ValueExpression} points at.</li>
     * <li>If the <code>setValue()</code> method returns successfully:
     * <ul>
     * <li>Clear the local value of this {@link UIInput}.</li>
     * <li>Set the <code>localValueSet</code> property of this
     * {@link UIInput} to false.</li>
     * </ul></li>
     * <li>If the <code>setValue()</code> method throws an Exception:
     * <ul>
     * <li class="changed_modified_2_0">Enqueue an error message.  Create a 
     * {@link FacesMessage} with the id {@link #UPDATE_MESSAGE_ID}.  Create a
     * {@link UpdateModelException}, passing the <code>FacesMessage</code> and 
     * the caught exception to the constructor.  Create an 
     * {@link ExceptionQueuedEventContext}, passing the <code>FacesContext</code>, 
     * the <code>UpdateModelException</code>, this component instance, and
     * {@link PhaseId#UPDATE_MODEL_VALUES} to its constructor.  Call 
     * {@link FacesContext#getExceptionHandler} and then call 
     * {@link ExceptionHandler#processEvent}, passing the 
     * <code>ExceptionQueuedEventContext</code>.
     * </li>
     * <li>Set the <code>valid</code> property of this {@link UIInput}
     * to <code>false</code>.</li>
     * </ul></li>
     * The exception must not be re-thrown.  This enables tree traversal
     * to continue for this lifecycle phase, as in all the other lifecycle
     * phases. 
     * </ul>
     *
     * @param context {@link FacesContext} for the request we are processing
     * @throws NullPointerException if <code>context</code>
     *                              is <code>null</code>
     */
    public void updateModel(FacesContext context) {

        if (context == null) {
            throw new NullPointerException();
        }

        if (!isValid() || !isLocalValueSet()) {
            return;
        }
        ValueExpression ve = getValueExpression("value");
        if (ve != null) {
            Throwable caught = null;
            FacesMessage message = null;
            try {
                ve.setValue(context.getELContext(), getLocalValue());
                setValue(null);
                setLocalValueSet(false);
            } catch (ELException e) {
                caught = e;
                String messageStr = e.getMessage();
                Throwable result = e.getCause();
                while (null != result &&
                     result.getClass().isAssignableFrom(ELException.class)) {
                    messageStr = result.getMessage();
                    result = result.getCause();
                }
                if (null == messageStr) {
                    message =
                         MessageFactory.getMessage(context, UPDATE_MESSAGE_ID,
                              MessageFactory.getLabel(
                                   context, this));
                } else {
                    message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                               messageStr,
                                               messageStr);
                }
                setValid(false);
            } catch (Exception e) {
                caught = e;
                message =
                     MessageFactory.getMessage(context, UPDATE_MESSAGE_ID,
                          MessageFactory.getLabel(
                               context, this));
                setValid(false);
            }
            if (caught != null) {
                assert(message != null);
                @SuppressWarnings({"ThrowableInstanceNeverThrown"})
                UpdateModelException toQueue =
                      new UpdateModelException(message, caught);
                ExceptionQueuedEventContext eventContext =
                      new ExceptionQueuedEventContext(context,
                                                toQueue,
                                                this,
                                                PhaseId.UPDATE_MODEL_VALUES);
                context.getApplication().publishEvent(ExceptionQueuedEvent.class,
                                                      eventContext);
                
            }
            
        }
    }
    
    // ------------------------------------------------------ Validation Methods


    /**
     * <p><span class="changed_modified_2_0">Perform</span> the
     * following algorithm to validate the local value of this {@link
     * UIInput}.</p>

     * <ul>

     * <li>Retrieve the submitted value with {@link #getSubmittedValue}.
     * If this returns <code>null</code>, exit without further
     * processing.  (This indicates that no value was submitted for this
     * component.)</li>

     * <p/>

     * <li><span class="changed_modified_2_0">If the
     * <code>javax.faces.INTERPRET_EMPTY_STRING_SUBMITTED_VALUES_AS_NULL</code>
     * context parameter value is <code>true</code> (ignoring case), and
     * <code>getSubmittedValue()</code> returns a zero-length
     * <code>String</code> call <code>{@link #setSubmittedValue}</code>,
     * passing <code>null</code> as the argument and continue processing
     * using <code>null</code> as the current submitted
     * value.</code></span></li>

     * <p/>

     * <li> Convert the submitted value into a "local value" of the
     * appropriate data type by calling {@link #getConvertedValue}.</li>
     * <p/>
     * <li>Validate the property by calling {@link #validateValue}.</li>
     * <p/>
     * <li>If the <code>valid</code> property of this component is still
     * <code>true</code>, retrieve the previous value of the component
     * (with <code>getValue()</code>), store the new local value using
     * <code>setValue()</code>, and reset the submitted value to
     * null.  If the local value is different from
     * the previous value of this component, fire a
     * {@link ValueChangeEvent} to be broadcast to all interested
     * listeners.</li>
     * </ul>
     * <p/>
     * <p>Application components implementing {@link UIInput} that wish to
     * perform validation with logic embedded in the component should perform
     * their own correctness checks, and then call the
     * <code>super.validate()</code> method to perform the standard
     * processing described above.</p>
     *
     * @param context The {@link FacesContext} for the current request
     * @throws NullPointerException if <code>context</code>
     *                              is null
     */
    public void validate(FacesContext context) {

        if (context == null) {
            throw new NullPointerException();
        }

        // Submitted value == null means "the component was not submitted
        // at all".  
        Object submittedValue = getSubmittedValue();
        if (submittedValue == null) {
            return;
        }

        // If non-null, an instanceof String, and we're configured to treat
        // zero-length Strings as null:
        //   call setSubmittedValue(null)
        if ((considerEmptyStringNull(context)
             && submittedValue instanceof String 
             && ((String) submittedValue).length() == 0)) {
            setSubmittedValue(null);
            submittedValue = null;
        }

        Object newValue = null;

        try {
            newValue = getConvertedValue(context, submittedValue);
        }
        catch (ConverterException ce) {
            addConversionErrorMessage(context, ce);
            setValid(false);
        }

        validateValue(context, newValue);

        // If our value is valid, store the new value, erase the
        // "submitted" value, and emit a ValueChangeEvent if appropriate
        if (isValid()) {
            Object previous = getValue();
            setValue(newValue);
            setSubmittedValue(null);
            if (compareValues(previous, newValue)) {
                queueEvent(new ValueChangeEvent(this, previous, newValue));
            }
        }

    }

    /**
     * <p>Convert the submitted value into a "local value" of the
     * appropriate data type, if necessary.  Employ the following
     * algorithm to do so:</p>
     * <ul>
     * <li>If a <code>Renderer</code> is present, call
     * <code>getConvertedValue()</code> to convert the submitted
     * value.</li>
     * <li>If no <code>Renderer</code> is present, and the submitted
     * value is a String, locate a {@link Converter} as follows:
     * <ul>
     * <li>If <code>getConverter()</code> returns a non-null {@link Converter},
     * use that instance.</li>
     * <li>Otherwise, if a value binding for <code>value</code> exists,
     * call <code>getType()</code> on it.
     * <ul>
     * <li>If this call returns <code>null</code>, assume the output
     * type is <code>String</code> and perform no conversion.</li>
     * <li>Otherwise, call
     * <code>Application.createConverter(Class)</code>
     * to locate any registered {@link Converter} capable of
     * converting data values of the specified type.</li>
     * </ul>
     * </li>
     * </ul>
     * <li>If a {@link Converter} instance was located, call its
     * <code>getAsObject()</code> method to perform the conversion.
     * If conversion fails:
     * <ul>
     * <li>Enqueue an appropriate error message by calling the
     * <code>addMessage()</code> method on the
     * <code>FacesContext</code>.</li>
     * <li>Set the <code>valid</code> property
     * on this component to <code>false</code> </li>
     * </ul></li>
     * <li>Otherwise, use the submitted value without any conversion</li>
     * </ul>
     * </li>
     * <p/>
     * </p>
     * <p/>
     * <p>This method can be overridden by subclasses for more specific
     * behavior.</p>
     */


    protected Object getConvertedValue(FacesContext context,
                                       Object newSubmittedValue) throws ConverterException {
        Renderer renderer = getRenderer(context);
        Object newValue;

        if (renderer != null) {
            newValue = renderer.getConvertedValue(context, this,
                 newSubmittedValue);
        } else if (newSubmittedValue instanceof String) {
            // If there's no Renderer, and we've got a String,
            // run it through the Converter (if any)
            Converter converter = getConverterWithType(context);
            if (converter != null) {
                newValue = converter.getAsObject(context, this,
                     (String) newSubmittedValue);
            } else {
                newValue = newSubmittedValue;
            }
        } else {
            newValue = newSubmittedValue;
        }
        return newValue;
    }

    /**
     * <p><span class="changed_modified_2_0">Set</span> the "valid"
     * property according to the below algorithm.</p>

     * <ul>

     * <li>

     * <p>If the <code>valid</code> property on this component is
     * still <code>true</code>, and the <code>required</code> property
     * is also <code>true</code>, ensure that the local value is not
     * empty (where "empty" is defined as <code>null</code> or a
     * zero-length String).  If the local value is empty:</p>

     * <ul>

     * <li><p>Enqueue an appropriate error message by calling the
     * <code>addMessage()</code> method on the <code>FacesContext</code>
     * instance for the current request.  If the {@link
     * #getRequiredMessage} returns non-<code>null</code>, use the value
     * as the <code>summary</code> and <code>detail</code> in the {@link
     * FacesMessage} that is enqueued on the <code>FacesContext</code>,
     * otherwise use the message for the {@link #REQUIRED_MESSAGE_ID}.
     * </li> <li>Set the <code>valid</code> property on this component
     * to <code>false</code>.</p></li> 

     * <li><p class="changed_modified_2_0">If calling {@link
     * ValidatorException#getFacesMessages} returns
     * non-<code>null</code>, each message should be added to the
     * <code>FacesContext</code>.  Otherwise the single message returned
     * from {@link ValidatorException#getFacesMessage} should be
     * added.</p></li>
     *
     * </ul>

     * </li>
     *
     *
     * <li class="changed_added_2_0"><p>Otherwise, if the
     * <code>valid</code> property on this component is still
     * <code>true</code>, take the following action to determine if
     * validation of this component should proceed.</p>

     * <ul>
     *
     * <li><p>If the value is not empty, validation should proceed.</p></li>

     * <li><p>If the value is empty, but the system has been directed to
     * validate empty fields, validation should proceed.  The
     * implementation must obtain the init parameter <code>Map</code>
     * from the <code>ExternalContext</code> and inspect the value for
     * the key given by the value of the symbolic constant {@link
     * #VALIDATE_EMPTY_FIELDS_PARAM_NAME}.  If there is no value under
     * that key, use the same key and look in the application map from
     * the <code>ExternalContext</code>.  If the value is
     * <code>null</code> or equal to the string
     * &#8220;<code>auto</code>&#8221; (without the quotes) take
     * appropriate action to determine if Bean Validation is present in
     * the runtime environment.  If not, validation should not proceed.
     * If so, validation should proceed.  If the value is equal
     * (ignoring case) to &#8220;<code>true</code>&#8221; (without the
     * quotes) validation should proceed.  Otherwise, validation should
     * not proceed.</p></li>

     * <p>If the above determination indicates that validation should
     * proceed, call the <code>validate()</code> method of each {@link
     * Validator} registered for this {@link UIInput}, followed by the
     * method pointed at by the <code>validatorBinding</code> property
     * (if any).  If any of these validators or the method throws a
     * {@link ValidatorException}, catch the exception, add its message
     * (if any) to the {@link FacesContext}, and set the
     * <code>valid</code> property of this component to false.</li>

     * </ul>
     */

    protected void validateValue(FacesContext context, Object newValue) {
        // If our value is valid, enforce the required property if present
        if (isValid() && isRequired() && isEmpty(newValue)) {
            String requiredMessageStr = getRequiredMessage();
            FacesMessage message;
            if (null != requiredMessageStr) {
                message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                           requiredMessageStr,
                                           requiredMessageStr);
            } else {
                message =
                     MessageFactory.getMessage(context, REQUIRED_MESSAGE_ID,
                          MessageFactory.getLabel(
                               context, this));
            }
            context.addMessage(getClientId(context), message);
            setValid(false);
        }

        // If our value is valid and not empty or empty w/ validate empty fields enabled, call all validators
        if (isValid() && (!isEmpty(newValue) || validateEmptyFields(context))) {
            if (validators != null) {
                Validator[] validators = this.validators.asArray(Validator.class);
                for (Validator validator : validators) {
                    try {
                        validator.validate(context, this, newValue);
                    }
                    catch (ValidatorException ve) {
                        // If the validator throws an exception, we're
                        // invalid, and we need to add a message
                        setValid(false);
                        FacesMessage message;
                        String validatorMessageString = getValidatorMessage();

                        if (null != validatorMessageString) {
                            message =
                                  new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                                   validatorMessageString,
                                                   validatorMessageString);
                            message.setSeverity(FacesMessage.SEVERITY_ERROR);
                        } else {
                            Collection<FacesMessage> messages = ve.getFacesMessages();
                            if (null != messages) {
                                message = null;
                                Iterator<FacesMessage> iter = messages.iterator();
                                while (iter.hasNext()) {
                                    context.addMessage(getClientId(context), 
                                            iter.next());
                                }
                            } else {
                                message = ve.getFacesMessage();
                            }
                        }
                        if (message != null) {
                            context.addMessage(getClientId(context), message);
                        }
                    }
                }
            }
        }
    }


    /**
     * <p>Return <code>true</code> if the new value is different from the
     * previous value.</p>
     *
     * @param previous old value of this component (if any)
     * @param value    new value of this component (if any)
     */
    protected boolean compareValues(Object previous, Object value) {

        if (previous == null) {
            return (value != null);
        } else if (value == null) {
            return (true);
        } else {
            return (!(previous.equals(value)));
        }

    }


    /**
     * Executes validation logic.
     */
    private void executeValidate(FacesContext context) {
        try {
            validate(context);
        } catch (RuntimeException e) {
            context.renderResponse();
            throw e;
        }

        if (!isValid()) {
            context.validationFailed();
            context.renderResponse();
        }
    }

    public static boolean isEmpty(Object value) {

        if (value == null) {
            return (true);
        } else if ((value instanceof String) &&
             (((String) value).length() < 1)) {
            return (true);
        } else if (value.getClass().isArray()) {
            if (0 == java.lang.reflect.Array.getLength(value)) {
                return (true);
            }
        } else if (value instanceof List) {
            if (((List) value).isEmpty()) {
                return (true);
            }
        }
        return (false);
    }


    /**
     * <p>The set of {@link Validator}s associated with this
     * <code>UIComponent</code>.</p>
     */
    AttachedObjectListHolder<Validator> validators;


    /**
     * <p>Add a {@link Validator} instance to the set associated with
     * this {@link UIInput}.</p>
     *
     * @param validator The {@link Validator} to add
     * @throws NullPointerException if <code>validator</code>
     *                              is null
     */
    public void addValidator(Validator validator) {

        if (validator == null) {
            throw new NullPointerException();
        }

        if (validators == null) {
            validators = new AttachedObjectListHolder<Validator>();
        }
        validators.add(validator);

    }
    

    /**
     * <p>Return the set of registered {@link Validator}s for this
     * {@link UIInput} instance.  If there are no registered validators,
     * a zero-length array is returned.</p>
     */
    public Validator[] getValidators() {

        return ((validators != null) ? validators.asArray(Validator.class) : EMPTY_VALIDATOR);

    }


    /**
     * <p>Remove a {@link Validator} instance from the set associated with
     * this {@link UIInput}, if it was previously associated.
     * Otherwise, do nothing.</p>
     *
     * @param validator The {@link Validator} to remove
     */
    public void removeValidator(Validator validator) {

        if (validator == null) {
            return;
        }

        if (validators != null) {
            validators.remove(validator);
        }

    }

    // ------------------------------------------------ Event Processing Methods


    /**
     * <p>Add a new {@link ValueChangeListener} to the set of listeners
     * interested in being notified when {@link ValueChangeEvent}s occur.</p>
     *
     * @param listener The {@link ValueChangeListener} to be added
     * @throws NullPointerException if <code>listener</code>
     *                              is <code>null</code>
     */
    public void addValueChangeListener(ValueChangeListener listener) {

        addFacesListener(listener);

    }


    /**
     * <p>Return the set of registered {@link ValueChangeListener}s for this
     * {@link UIInput} instance.  If there are no registered listeners,
     * a zero-length array is returned.</p>
     */
    public ValueChangeListener[] getValueChangeListeners() {

        return (ValueChangeListener[]) getFacesListeners(ValueChangeListener.class);
    }


    /**
     * <p>Remove an existing {@link ValueChangeListener} (if any) from the
     * set of listeners interested in being notified when
     * {@link ValueChangeEvent}s occur.</p>
     *
     * @param listener The {@link ValueChangeListener} to be removed
     * @throws NullPointerException if <code>listener</code>
     *                              is <code>null</code>
     */
    public void removeValueChangeListener(ValueChangeListener listener) {

        removeFacesListener(listener);

    }

    // ----------------------------------------------------- StateHolder Methods



    private Object[] values;

    public Object saveState(FacesContext context) {

        if (values == null) {
            values = new Object[5];
        }

        values[0] = super.saveState(context);
        values[1] = emptyStringIsNull;
        values[2] = validateEmptyFields;
        values[3] = defaultValidatorsProcessed;
        values[4] = ((validators != null) ? validators.saveState(context) : null);
        return (values);

    }


    public void restoreState(FacesContext context, Object state) {

        values = (Object[]) state;
        super.restoreState(context, values[0]);
        emptyStringIsNull = (Boolean) values[1];
        validateEmptyFields = (Boolean) values[2];
        defaultValidatorsProcessed = (Boolean) values[3];
        if (values[4] != null) {
            if (validators == null) {
                validators = new AttachedObjectListHolder<Validator>();
            }
            validators.restoreState(context, values[4]);
        }

    }

    private Converter getConverterWithType(FacesContext context) {
        Converter converter = getConverter();
        if (converter != null) {
            return converter;
        }

        ValueExpression valueExpression = getValueExpression("value");
        if (valueExpression == null) {
            return null;
        }

        Class converterType;
        try {
            converterType = valueExpression.getType(context.getELContext());
        }
        catch (ELException e) {
            throw new FacesException(e);
        }

        // if converterType is null, String, or Object, assume
        // no conversion is needed
        if (converterType == null ||
             converterType == String.class ||
             converterType == Object.class) {
            return null;
        }

        // if getType returns a type for which we support a default
        // conversion, acquire an appropriate converter instance.
        try {
            Application application = context.getApplication();
            return application.createConverter(converterType);
        } catch (Exception e) {
            return (null);
        }
    }

    private void addConversionErrorMessage(FacesContext context,
                                           ConverterException ce) {
        FacesMessage message;
        String converterMessageString = getConverterMessage();
        if (null != converterMessageString) {
            message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                       converterMessageString,
                                       converterMessageString);
        } else {
            message = ce.getFacesMessage();
            if (message == null) {
                message = MessageFactory.getMessage(context,
                     CONVERSION_MESSAGE_ID);                
                if (message.getDetail() == null) {
                    message.setDetail(ce.getMessage());
                }
            }
        }

        context.addMessage(getClientId(context), message);
    }


    private boolean considerEmptyStringNull(FacesContext ctx) {

        if (emptyStringIsNull == null) {
            String val = ctx.getExternalContext()
              .getInitParameter("javax.faces.INTERPRET_EMPTY_STRING_SUBMITTED_VALUES_AS_NULL");
            emptyStringIsNull = Boolean.valueOf(val);
        }

        return emptyStringIsNull;
        
    }

    private boolean validateEmptyFields(FacesContext ctx) {

        if (validateEmptyFields == null) {
            ExternalContext extCtx = ctx.getExternalContext();
            String val = extCtx.getInitParameter(VALIDATE_EMPTY_FIELDS_PARAM_NAME);

            if (null == val) {
                val = (String) extCtx.getApplicationMap().get(VALIDATE_EMPTY_FIELDS_PARAM_NAME);
            }
            if (val == null || val.equals("auto")) {
                validateEmptyFields = isBeansValidationAvailable(ctx);
            }
            else {
                validateEmptyFields = Boolean.valueOf(val);
            }
        }

        return validateEmptyFields;

    }
    
    private boolean isBeansValidationAvailable(FacesContext context) {
        boolean result = false;
        final String beansValidationAvailabilityCacheKey = 
                "javax.faces.BEANS_VALIDATION_AVAILABLE";
        Map<String,Object> appMap = context.getExternalContext().getApplicationMap();
        
        if (appMap.containsKey(beansValidationAvailabilityCacheKey)) {
            result = (Boolean) appMap.get(beansValidationAvailabilityCacheKey);
        } else {
            try {
                new BeanValidator();
                appMap.put(beansValidationAvailabilityCacheKey, result = true);
            } catch (Throwable t) {
                appMap.put(beansValidationAvailabilityCacheKey, Boolean.FALSE);
            }
        }

        return result;
    }

    /**
     */
    private void addDefaultValidators(FacesContext context) {

        if (!defaultValidatorsProcessed) {

            Set<String> exclusions;
            Validator[] validators = getValidators();
            if (validators.length != 0) {
                exclusions = new HashSet<String>(validators.length, 1.0f);
                for (Validator v : validators) {
                    exclusions.add(v.getClass().getName());
                }
            } else {
                exclusions = Collections.emptySet();
            }

            // first add the registered default validators and then turn them off based on switches in tree
            List<String> defaultValidatorIds = new ArrayList<String>();
            Map<String, String> defaultValidatorInfo = context.getApplication()
                  .getDefaultValidatorInfo();
            if (!defaultValidatorInfo.isEmpty()) {
                for (Map.Entry<String, String> valInfo : defaultValidatorInfo.entrySet()) {
                    defaultValidatorIds.add(valInfo.getKey());
                }
            }

            collectDefaultValidatorIds(defaultValidatorIds, this);

            for (String validatorId : defaultValidatorIds) {
                String defaultValClassName = defaultValidatorInfo .get(validatorId);
                if (exclusions.contains(defaultValClassName)) {
                    continue;
                }

                addValidator(context.getApplication().createValidator(
                      validatorId));
            }
            defaultValidatorsProcessed = true;
        }
    }


    /**
     * <p>Work upwards through the component hierarchy and collect any default validators that have been defined.</p>
     */
    private void collectDefaultValidatorIds(List<String> defaultValidatorIds, UIComponent component) {
        Object branchState = component.getAttributes().get(DEFAULT_VALIDATOR_IDS_KEY);

        // check for the "resolved" list for this branch
        // NOTE we could use a separate key for the resolved list to avoid type hunting
        if (branchState instanceof List) {
            if (defaultValidatorIds.isEmpty()) {
                defaultValidatorIds.addAll((List<String>) branchState);
            }
            else {
                for (String validatorId : (List<String>) branchState) {
                    if (!defaultValidatorIds.contains(validatorId)) {
                        defaultValidatorIds.add(0, validatorId);
                    }
                }
            }
            return;
        }

        // process parent nodes first
        if (component.getParent() != null) {
            collectDefaultValidatorIds(defaultValidatorIds, component.getParent());
        }

        // collect any default validator ids registered at this branch point
        if (branchState instanceof Map) {
            Map<String, Boolean> validatorIdStates = (Map<String, Boolean>) branchState;
            for (Map.Entry<String, Boolean> e : validatorIdStates.entrySet()) {
                if (e.getValue()) {
                    // validators nearer to input component take precendence
                    // QUESTION if it already present, should we move it to the front?
                    if (!defaultValidatorIds.contains(e.getKey())) {
                        defaultValidatorIds.add(0, e.getKey());
                    }
                }
                else {
                    defaultValidatorIds.remove(e.getKey());
                }
            }
        }

        // make sure we never have to climb the mountain again (could we avoid creating new list if none added at this node?)
        component.getAttributes().put(DEFAULT_VALIDATOR_IDS_KEY, new ArrayList<String>(defaultValidatorIds));
    }

}
