/*
 * This file is part of the DITA Open Toolkit project hosted on
 * Sourceforge.net. See the accompanying license.txt file for
 * applicable licenses.
 */

/*
 * (c) Copyright IBM Corp. 2005 All Rights Reserved.
 */
package org.dita.dost.log;

import static org.dita.dost.log.MessageBean.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.taskdefs.condition.ConditionBase;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Exit;
import org.dita.dost.exception.DITAOTException;
import org.dita.dost.invoker.ExtensibleAntInvoker.Param;

/**
 * Ant echo task for custom error message.
 */
public final class DITAOTFailTask extends Exit {
    private String id = null;

    private final Properties prop = new Properties();
    /** Nested params. */
    private final ArrayList<Param> params = new ArrayList<Param>();

    /**
     * Default Construtor.
     *
     */
    public DITAOTFailTask(){
    }
    
    /**
     * Set the id.
     * @param identifier The id to set.
     * 
     */
    public void setId(final String identifier) {
        this.id = identifier;
    }

    /**
     * Set the parameters.
     * @param params The prop to set.
     * @deprecated use nested {@code param} elements instead with {@link #createParam()}
     */
    @Deprecated
    public void setParams(final String params) {
        final StringTokenizer tokenizer = new StringTokenizer(params, ";");
        while (tokenizer.hasMoreTokens()) {
            final String token = tokenizer.nextToken();
            final int pos = token.indexOf("=");
            this.prop.put(token.substring(0, pos), token.substring(pos + 1));
        }
    }
    
    /**
     * Handle nested parameters. Add the key/value to the pipeline hash, unless
     * the "if" attribute is set and refers to a unset property.
     * @return parameter
     */
    public Param createParam() {
        final Param p = new Param();
        params.add(p);
        return p;
    }

    /**
     * Task execute point.
     * @throws BuildException exception
     * @see org.apache.tools.ant.taskdefs.Exit#execute()
     */
    @Override
    public void execute() throws BuildException {
        boolean fail = nestedConditionPresent()
                       ? testNestedCondition()
                       : (testIfCondition() && testUnlessCondition());
        if (!fail) {
            return;
        }
        
        if (id == null) {
            throw new BuildException("id attribute must be specified");
        }
        for (final Param p : params) {
            if (!p.isValid()) {
                throw new BuildException("Incomplete parameter");
            }
            final String ifProperty = p.getIf();
            final String unlessProperty = p.getUnless();
            if ((ifProperty == null || getProject().getProperties().containsKey(ifProperty))
                    && (unlessProperty == null || !getProject().getProperties().containsKey(unlessProperty))) {
                prop.put("%" + p.getName(), p.getValue());
            }
        }
        
        MessageUtils.loadDefaultMessages();
        final MessageBean msgBean = MessageUtils.getMessage(id, prop);
        final DITAOTLogger logger = new DITAOTAntLogger(getProject());
        if (msgBean != null) {
            final String type = msgBean.getType();
            if(FATAL.equals(type)){
                setMessage(msgBean.toString());
                try{
                    super.execute();
                }catch(final BuildException ex){
                    throw new BuildException(msgBean.toString(),new DITAOTException(msgBean,ex,msgBean.toString()));
                }
            } else if(ERROR.equals(type)){
                logger.logError(msgBean.toString());
            } else if(WARN.equals(type)){
                logger.logWarn(msgBean.toString());
            } else if(INFO.equals(type)){
                logger.logInfo(msgBean.toString());
            } else if(DEBUG.equals(type)){
                logger.logDebug(msgBean.toString());
            }
        }
        
        
    }
    
    // Ant Exit class methods --------------------------------------------------
    
    private static class NestedCondition extends ConditionBase implements Condition {
        public boolean eval() {
            if (countConditions() != 1) {
                throw new BuildException(
                    "A single nested condition is required.");
            }
            return ((Condition) (getConditions().nextElement())).eval();
        }
    }

    private String message;
    private String ifCondition, unlessCondition;
    private NestedCondition nestedCondition;
    private Integer status;

    /**
     * A message giving further information on why the build exited.
     *
     * @param value message to output
     */
    public void setMessage(String value) {
        this.message = value;
    }

    /**
     * Only fail if a property of the given name exists in the current project.
     * @param c property name
     */
    public void setIf(String c) {
        ifCondition = c;
    }

    /**
     * Only fail if a property of the given name does not
     * exist in the current project.
     * @param c property name
     */
    public void setUnless(String c) {
        unlessCondition = c;
    }

    /**
     * Set the status code to associate with the thrown Exception.
     * @param i   the <code>int</code> status
     */
    public void setStatus(int i) {
        status = new Integer(i);
    }

    /**
     * Set a multiline message.
     * @param msg the message to display
     */
    public void addText(String msg) {
        if (message == null) {
            message = "";
        }
        message += getProject().replaceProperties(msg);
    }

    /**
     * Add a condition element.
     * @return <code>ConditionBase</code>.
     * @since Ant 1.6.2
     */
    public ConditionBase createCondition() {
        if (nestedCondition != null) {
            throw new BuildException("Only one nested condition is allowed.");
        }
        nestedCondition = new NestedCondition();
        return nestedCondition;
    }

    /**
     * test the if condition
     * @return true if there is no if condition, or the named property exists
     */
    private boolean testIfCondition() {
        if (ifCondition == null || "".equals(ifCondition)) {
            return true;
        }
        return getProject().getProperty(ifCondition) != null;
    }

    /**
     * test the unless condition
     * @return true if there is no unless condition,
     *  or there is a named property but it doesn't exist
     */
    private boolean testUnlessCondition() {
        if (unlessCondition == null || "".equals(unlessCondition)) {
            return true;
        }
        return getProject().getProperty(unlessCondition) == null;
    }

    /**
     * test the nested condition
     * @return true if there is none, or it evaluates to true
     */
    private boolean testNestedCondition() {
        boolean result = nestedConditionPresent();

        if (result && ifCondition != null || unlessCondition != null) {
            throw new BuildException("Nested conditions "
                + "not permitted in conjunction with if/unless attributes");
        }

        return result && nestedCondition.eval();
    }

    /**
     * test whether there is a nested condition.
     * @return <code>boolean</code>.
     */
    private boolean nestedConditionPresent() {
        return (nestedCondition != null);
    }

}