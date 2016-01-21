/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.decorators;

/**
 * DTO for Embeded UI decoration.
 * 
 *
 *
 */
public class SPUIEmbedValue {
    private String id;
    private String data;
    private String styleName;
    private boolean immediate;
    private String source;
    private int type;
    private String mimeType;
    private String description;

    /**
     * GET - ID.
     * 
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * SET - ID.
     * 
     * @param id
     *            as ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * GET - DATA.
     * 
     * @return data
     */
    public String getData() {
        return data;
    }

    /**
     * SET - DATA.
     * 
     * @param data
     *            as data
     */
    public void setData(String data) {
        this.data = data;
    }

    /**
     * GET - STYLE NAME.
     * 
     * @return style name
     */
    public String getStyleName() {
        return styleName;
    }

    /**
     * SET - STYLE NAME.
     * 
     * @param styleName
     *            as Style
     */
    public void setStyleName(String styleName) {
        this.styleName = styleName;
    }

    /**
     * CHECK - IMM.
     * 
     * @return flag
     */
    public boolean isImmediate() {
        return immediate;
    }

    /**
     * SET - IMM.
     * 
     * @param immediate
     *            as flag
     */
    public void setImmediate(boolean immediate) {
        this.immediate = immediate;
    }

    /**
     * GET - SOURCE.
     * 
     * @return source
     */
    public String getSource() {
        return source;
    }

    /**
     * SET - SOURCE.
     * 
     * @param source
     *            as source
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * GET - TYPE.
     * 
     * @return type
     */
    public int getType() {
        return type;
    }

    /**
     * SET - TYPE.
     * 
     * @param type
     *            as type
     */
    public void setType(int type) {
        this.type = type;
    }

    /**
     * GET - MIME.
     * 
     * @return mime
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * SET - MIME.
     * 
     * @param mimeType
     *            as mime
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * GET - DESC.
     * 
     * @return desc
     */
    public String getDescription() {
        return description;
    }

    /**
     * SET - DESC.
     * 
     * @param description
     *            as desc
     */
    public void setDescription(String description) {
        this.description = description;
    }

}
