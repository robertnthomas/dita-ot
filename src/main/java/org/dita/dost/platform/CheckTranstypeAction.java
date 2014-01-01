/*
 * This file is part of the DITA Open Toolkit project.
 * See the accompanying license.txt file for applicable licenses.
 */

/*
 * (c) Copyright IBM Corp. 2008 All Rights Reserved.
 */
package org.dita.dost.platform;

import java.io.IOException;

import org.dita.dost.util.StringUtils;
/**
 * CheckTranstypeAction class.
 *
 */
final class CheckTranstypeAction extends ImportAction {

    /**
     * Get result.
     * @return result
     */
    @Override
    public void getResult(final Appendable retBuf) throws IOException {
        final String property = paramTable.containsKey("property") ? paramTable.get("property") : "transtype";
        for (final String value: valueSet) {
            retBuf.append("<not><equals arg1=\"${")
                .append(StringUtils.escapeXML(property))
                .append("}\" arg2=\"")
                .append(StringUtils.escapeXML(value))
                .append("\" casesensitive=\"false\"/></not>");
        }
    }

}
