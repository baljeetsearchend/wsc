/*
 * Copyright (c) 2005, salesforce.com, inc. All rights reserved. Redistribution and use in source and binary forms, with
 * or without modification, are permitted provided that the following conditions are met: Redistributions of source code
 * must retain the above copyright notice, this list of conditions and the following disclaimer. Redistributions in
 * binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution. Neither the name of salesforce.com, inc. nor the
 * names of its contributors may be used to endorse or promote products derived from this software without specific
 * prior written permission. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.sforce.ws.wsdl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import com.sforce.ws.parser.XmlInputStream;
import com.sforce.ws.util.Named;

/**
 * This class represents WSDL->definitions->types->schema->complexType->attribute
 * 
 * @author http://cheenath.com
 * @version 158
 */
public class Attribute implements Constants, Named {

    private Schema schema;
    private String name;
    private QName ref;
    private QName type;
    private String fixed;
    
    private static final int MAX_LENGTH = 255;

    private static final Pattern pattern = Pattern.compile("[\\s:]");
    
    public Attribute(Schema schema) {
        this.schema = schema;
    }

    @Override
    public String toString() {
        return "Attribute{" + "name=" + name + '}';
    }

    @Override
    public String getName() {
        return name;
    }
    
    public QName getRef() {
    	return ref;
    }

    public QName getType() {
        return type;
    }

    public String getFixed() {
		return fixed;
	}

	public String getNamespace() {
        return schema.getTargetNamespace();
    }
    
    void read(WsdlParser parser) throws WsdlParseException {
        name = parser.getAttributeValue(null, NAME);

        ref = parser.parseRef(schema);
        
        fixed = parser.getAttributeValue(null, FIXED);

        if (ref != null) {
            final String positionDescription = parser.getPositionDescription();
            final Attribute thisAttribute = this;
            parser.addPostParseProcessor(new WsdlParser.PostParseProcessor() {
                @Override
                public void postParse() throws WsdlParseException {
                    Attribute referencedAttribute = schema.getTypes().getAttribute(ref);
                    if (referencedAttribute != null) {
                        thisAttribute.schema = referencedAttribute.schema;
                        thisAttribute.name = referencedAttribute.name;
                        thisAttribute.type = referencedAttribute.type;
                    } else {
                        throw new WsdlParseException("attribute ref '" + ref
                                + "' could not be resolved at: " + positionDescription);
                    }
                }
            });
        }

        if (ref == null) {
            if (name == null) { throw new WsdlParseException("attribute name can not be null at: "
                    + parser.getPositionDescription()); }

            if ("".equals(name)) { throw new WsdlParseException("attribute name can not be empty at: "
                    + parser.getPositionDescription()); }

            if (name.length() > MAX_LENGTH) { throw new WsdlParseException("attribute name '" + name
                    + "' bigger than max length: " + MAX_LENGTH); }

            Matcher matcher = pattern.matcher(name);
            if (matcher.find()) { throw new WsdlParseException("attribute name '" + name
                    + "' is not a valid attribute name"); }
        }

        String t = parser.getAttributeValue(null, TYPE);

        if (t != null) {
            type = ParserUtil.toQName(t, parser);
        }

        int eventType = parser.getEventType();

        while (true) {
            if (eventType == XmlInputStream.START_TAG) {
                String name = parser.getName();
                String namespace = parser.getNamespace();

                if (SIMPLE_TYPE.equals(name) && SCHEMA_NS.equals(namespace)) {
                    if (type != null) { throw new WsdlParseException("type should not be specified: "
                            + parser.getPositionDescription()); }

                    SimpleType st = new SimpleType(schema);
                    st.read(parser, name);

                    type = new QName(SCHEMA_NS, "string");
                }

            } else if (eventType == XmlInputStream.END_TAG) {
                String name = parser.getName();
                String namespace = parser.getNamespace();

                if (ATTRIBUTE.equals(name) && SCHEMA_NS.equals(namespace)) {
                    break;
                }
            } else if (eventType == XmlInputStream.END_DOCUMENT) { throw new WsdlParseException(
                    "Failed to find end tag for 'attribute'"); }

            eventType = parser.next();
        }

        if (ref == null) {
            if (type == null) { throw new WsdlParseException("type not specified for attribute: " + name); }
        }
    }
}
