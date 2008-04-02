/*
 * $Id: ErrorsRenderer.java,v 1.11 2003/04/04 22:47:26 eburns Exp $
 */

/*
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// ErrorsRenderer.java

package com.sun.faces.renderkit.html_basic;

import com.sun.faces.util.Util;

import java.util.Iterator;

import javax.faces.component.AttributeDescriptor;
import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.context.Message;
import javax.faces.context.ResponseWriter;

import org.mozilla.util.Assert;
import org.mozilla.util.Debug;
import org.mozilla.util.Log;
import org.mozilla.util.ParameterCheck;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 *
 *  <B>ErrorsRenderer</B> is a class ...
 *
 * <B>Lifetime And Scope</B> <P>
 *
 * @version $Id: ErrorsRenderer.java,v 1.11 2003/04/04 22:47:26 eburns Exp $
 * 
 * @see	Blah
 * @see	Bloo
 *
 */

public class ErrorsRenderer extends HtmlBasicRenderer {
    //
    // Protected Constants
    //

    //
    // Class Variables
    //

    //
    // Instance Variables
    //

    // Attribute Instance Variables


    // Relationship Instance Variables

    //
    // Constructors and Initializers    
    //

    public ErrorsRenderer() {
        super();
    }

    //
    // Class methods
    //

    //
    // General Methods
    //

    //
    // Methods From Renderer
    //
    public boolean supportsComponentType(String componentType) {
        if ( componentType == null ) {
            throw new NullPointerException(Util.getExceptionMessage(Util.NULL_PARAMETERS_ERROR_MESSAGE_ID));
        }    
        return (componentType.equals(UIOutput.TYPE));
    }

    public void encodeBegin(FacesContext context, UIComponent component) 
            throws IOException {
        if (context == null || component == null) {
            throw new NullPointerException(Util.getExceptionMessage(Util.NULL_PARAMETERS_ERROR_MESSAGE_ID));
        }
    }

    public void encodeChildren(FacesContext context, UIComponent component) {
        if (context == null || component == null) {
            throw new NullPointerException(Util.getExceptionMessage(Util.NULL_PARAMETERS_ERROR_MESSAGE_ID));
        }
    }

    public void encodeEnd(FacesContext context, UIComponent component) 
            throws IOException {
        Iterator messageIter = null;
        String curColor = null;
        Message curMessage = null;
        ResponseWriter writer = null;
        
        if (context == null || component == null) {
            throw new NullPointerException(Util.getExceptionMessage(
                    Util.NULL_PARAMETERS_ERROR_MESSAGE_ID));
        }
       
        // suppress rendering if "rendered" property on the component is
        // false.
        if (!component.isRendered()) {
            return;
        }
        writer = context.getResponseWriter();
        Assert.assert_it(writer != null );
        
        // Attempt to use the "for" attribute to locate 
        // messages.  Threee possible scenarios here:
        // 1. valid "for" attribute - messages returned
        //    for valid component identified by "for" expression.
        // 2. zero length "for" expression - global errors
        //    not associated with any component returned
        // 3. no "for" expression - all messages returned.
        // 
        String forComponent = (String)component.getAttribute("for");
        if (null != forComponent) {
            if (forComponent.length() == 0) {
                messageIter = context.getMessages(null);
            } else {
                UIComponent root = context.getTree().getRoot();
                Assert.assert_it(null != root);
                UIComponent comp = null;
                try {
                    comp = root.findComponent(forComponent);
                } catch (Throwable t) {
                    Object[] params = {forComponent};
                    throw new RuntimeException(Util.getExceptionMessage(
                        Util.COMPONENT_NOT_FOUND_ERROR_MESSAGE_ID, params));
                }
                messageIter = context.getMessages(comp);
            }
        } else {
            messageIter = context.getMessages();
        }
        Assert.assert_it(null != messageIter);

        String color = (String)component.getAttribute("color");
        if (null == color) {
            color = "RED";
        }
	String outputClass = null;
        boolean wroteIt = false;
        if (messageIter.hasNext()) {
            writer.write("\n<font color=\"" + color + "\">");
            wroteIt = true;
        }
	if (null != (outputClass = (String) 
		     component.getAttribute("outputClass"))) {
	    writer.write("<span class=\"" + outputClass + "\">");
	}
        while (messageIter.hasNext()) {
            curMessage = (Message) messageIter.next();
            writer.write("\t" + curMessage.getSummary() + "<BR>");
            writer.write("\tSeverity: " + curMessage.getSeverity() + "<BR>");
            writer.write("\t" + curMessage.getDetail());
        }
	if (null != outputClass) {
	    writer.write("</span>");
	}
        if (wroteIt) {
            writer.write("</font>");
        }
    }
    
    // The testcase for this class is TestRenderers_2.java 

} // end of class ErrorsRenderer


