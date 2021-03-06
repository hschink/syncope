/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.console.pages.panels;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.common.to.ConfTO;
import org.apache.syncope.common.to.MembershipTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.SchemaTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.AttributeSchemaType;
import org.apache.syncope.console.commons.JexlHelpUtil;
import org.apache.syncope.console.commons.AttrLayoutType;
import org.apache.syncope.console.commons.Mode;
import org.apache.syncope.console.markup.html.list.AltListView;
import org.apache.syncope.console.pages.panels.AttrTemplatesPanel.RoleAttrTemplatesChange;
import org.apache.syncope.console.rest.ConfigurationRestClient;
import org.apache.syncope.console.rest.RoleRestClient;
import org.apache.syncope.console.rest.SchemaRestClient;
import org.apache.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.BinaryFieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.DateTextFieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.DateTimeFieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.FieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.SpinnerFieldPanel;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class AttributesPanel extends Panel {

    private static final long serialVersionUID = 552437609667518888L;

    @SpringBean
    private SchemaRestClient schemaRestClient;

    @SpringBean
    private ConfigurationRestClient confRestClient;

    @SpringBean
    private RoleRestClient roleRestClient;

    private final AbstractAttributableTO entityTO;

    private final Mode mode;

    private final AttrTemplatesPanel attrTemplates;

    private Map<String, SchemaTO> schemas = new LinkedHashMap<String, SchemaTO>();

    public <T extends AbstractAttributableTO> AttributesPanel(final String id, final T entityTO, final Form form,
            final Mode mode) {

        this(id, entityTO, form, mode, null);
    }

    public <T extends AbstractAttributableTO> AttributesPanel(final String id, final T entityTO, final Form form,
            final Mode mode, final AttrTemplatesPanel attrTemplates) {

        super(id);
        this.entityTO = entityTO;
        this.mode = mode;
        this.attrTemplates = attrTemplates;
        this.setOutputMarkupId(true);

        setSchemas();
        setAttrs();

        add(new AltListView<AttributeTO>("schemas", new PropertyModel<List<? extends AttributeTO>>(entityTO, "attrs")) {

            private static final long serialVersionUID = 9101744072914090143L;

            @Override
            @SuppressWarnings({ "unchecked", "rawtypes" })
            protected void populateItem(final ListItem<AttributeTO> item) {
                final AttributeTO attributeTO = (AttributeTO) item.getDefaultModelObject();

                final WebMarkupContainer jexlHelp = JexlHelpUtil.getJexlHelpWebContainer("jexlHelp");

                final AjaxLink<Void> questionMarkJexlHelp = JexlHelpUtil.getAjaxLink(jexlHelp, "questionMarkJexlHelp");
                item.add(questionMarkJexlHelp);
                questionMarkJexlHelp.add(jexlHelp);

                if (mode != Mode.TEMPLATE) {
                    questionMarkJexlHelp.setVisible(false);
                }

                item.add(new Label("name", attributeTO.getSchema()));

                final FieldPanel panel = getFieldPanel(schemas.get(attributeTO.getSchema()), form, attributeTO);

                if (mode == Mode.TEMPLATE || !schemas.get(attributeTO.getSchema()).isMultivalue()) {
                    item.add(panel);
                } else {
                    item.add(new MultiFieldPanel<String>(
                            "panel", new PropertyModel<List<String>>(attributeTO, "values"), panel));
                }
            }
        }
        );
    }

    private void filter(final List<SchemaTO> schemaTOs, final Collection<String> allowed) {
        for (ListIterator<SchemaTO> itor = schemaTOs.listIterator(); itor.hasNext();) {
            SchemaTO schema = itor.next();
            if (!allowed.contains(schema.getName())) {
                itor.remove();
            }
        }
    }

    private void setSchemas() {
        AttributeTO attrLayout = null;
        List<SchemaTO> schemaTOs;

        if (entityTO instanceof RoleTO) {
            final RoleTO roleTO = (RoleTO) entityTO;

            attrLayout = confRestClient.readAttrLayout(AttrLayoutType.valueOf(mode, AttributableType.ROLE));
            schemaTOs = schemaRestClient.getSchemas(AttributableType.ROLE);
            Set<String> allowed;
            if (attrTemplates == null) {
                allowed = new HashSet<String>(roleTO.getRAttrTemplates());
            } else {
                allowed = new HashSet<String>(attrTemplates.getSelected(AttrTemplatesPanel.Type.rAttrTemplates));
                if (roleTO.isInheritTemplates() && roleTO.getParent() != 0) {
                    allowed.addAll(roleRestClient.read(roleTO.getParent()).getRAttrTemplates());
                }
            }
            filter(schemaTOs, allowed);
        } else if (entityTO instanceof UserTO) {
            attrLayout = confRestClient.readAttrLayout(AttrLayoutType.valueOf(mode, AttributableType.USER));
            schemaTOs = schemaRestClient.getSchemas(AttributableType.USER);
        } else if (entityTO instanceof MembershipTO) {
            attrLayout = confRestClient.readAttrLayout(AttrLayoutType.valueOf(mode, AttributableType.MEMBERSHIP));
            schemaTOs = schemaRestClient.getSchemas(AttributableType.MEMBERSHIP);
            Set<String> allowed = new HashSet<String>(
                    roleRestClient.read(((MembershipTO) entityTO).getRoleId()).getMAttrTemplates());
            filter(schemaTOs, allowed);
        } else {
            schemas = new TreeMap<String, SchemaTO>();
            schemaTOs = schemaRestClient.getSchemas(AttributableType.CONFIGURATION);
            for (Iterator<SchemaTO> it = schemaTOs.iterator(); it.hasNext();) {
                SchemaTO schemaTO = it.next();
                for (AttrLayoutType type : AttrLayoutType.values()) {
                    if (type.getConfKey().equals(schemaTO.getName())) {
                        it.remove();
                    }
                }
            }
        }

        schemas.clear();

        if (attrLayout != null && mode != Mode.TEMPLATE && !(entityTO instanceof ConfTO)) {
            // 1. remove attributes not selected for display
            filter(schemaTOs, attrLayout.getValues());
            // 2. sort remainig attributes according to configuration, e.g. attrLayout
            final Map<String, Integer> attrLayoutMap = new HashMap<String, Integer>(attrLayout.getValues().size());
            for (int i = 0; i < attrLayout.getValues().size(); i++) {
                attrLayoutMap.put(attrLayout.getValues().get(i), i);
            }
            Collections.sort(schemaTOs, new Comparator<SchemaTO>() {

                @Override
                public int compare(final SchemaTO schema1, final SchemaTO schema2) {
                    int value = 0;

                    if (attrLayoutMap.get(schema1.getName()) > attrLayoutMap.get(schema2.getName())) {
                        value = 1;
                    } else if (attrLayoutMap.get(schema1.getName()) < attrLayoutMap.get(schema2.getName())) {
                        value = -1;
                    }

                    return value;
                }
            });
        }
        for (SchemaTO schemaTO : schemaTOs) {
            schemas.put(schemaTO.getName(), schemaTO);
        }
    }

    private void setAttrs() {
        final List<AttributeTO> entityData = new ArrayList<AttributeTO>();

        final Map<String, AttributeTO> attrMap = entityTO.getAttrMap();

        for (SchemaTO schema : schemas.values()) {
            final AttributeTO attributeTO = new AttributeTO();
            attributeTO.setSchema(schema.getName());

            if (attrMap.get(schema.getName()) == null || attrMap.get(schema.getName()).getValues().isEmpty()) {
                attributeTO.getValues().add("");

                // is important to set readonly only after values setting
                attributeTO.setReadonly(schema.isReadonly());
            } else {
                attributeTO.getValues().addAll(attrMap.get(schema.getName()).getValues());
            }
            entityData.add(attributeTO);
        }

        entityTO.getAttrs().clear();
        entityTO.getAttrs().addAll(entityData);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private FieldPanel getFieldPanel(final SchemaTO schemaTO, final Form form, final AttributeTO attributeTO) {
        final boolean required = mode == Mode.TEMPLATE
                ? false
                : schemaTO.getMandatoryCondition().equalsIgnoreCase("true");

        final boolean readOnly = mode == Mode.TEMPLATE ? false : schemaTO.isReadonly();

        final AttributeSchemaType type = mode == Mode.TEMPLATE ? AttributeSchemaType.String : schemaTO.getType();

        final FieldPanel panel;
        switch (type) {
            case Boolean:
                panel = new AjaxCheckBoxPanel("panel", schemaTO.getName(), new Model<Boolean>());
                panel.setRequired(required);
                break;

            case Date:
                final String dataPattern = schemaTO.getConversionPattern() == null
                        ? SyncopeConstants.DEFAULT_DATE_PATTERN
                        : schemaTO.getConversionPattern();

                if (dataPattern.contains("H")) {
                    panel = new DateTimeFieldPanel("panel", schemaTO.getName(), new Model<Date>(), dataPattern);

                    if (required) {
                        panel.addRequiredLabel();
                        ((DateTimeFieldPanel) panel).setFormValidator(form);
                    }
                    panel.setStyleSheet("ui-widget-content ui-corner-all");
                } else {
                    panel = new DateTextFieldPanel("panel", schemaTO.getName(), new Model<Date>(), dataPattern);

                    if (required) {
                        panel.addRequiredLabel();
                    }
                }
                break;

            case Enum:
                panel = new AjaxDropDownChoicePanel<String>("panel", schemaTO.getName(), new Model<String>());
                ((AjaxDropDownChoicePanel<String>) panel).setChoices(getEnumeratedValues(schemaTO));

                if (StringUtils.isNotBlank(schemaTO.getEnumerationKeys())) {
                    ((AjaxDropDownChoicePanel) panel).setChoiceRenderer(new IChoiceRenderer<String>() {

                        private static final long serialVersionUID = -3724971416312135885L;

                        private final Map<String, String> valueMap = getEnumeratedKeyValues(schemaTO);

                        @Override
                        public String getDisplayValue(final String value) {
                            return valueMap.get(value) == null ? value : valueMap.get(value);
                        }

                        @Override
                        public String getIdValue(final String value, final int i) {
                            return value;
                        }
                    });
                }

                if (required) {
                    panel.addRequiredLabel();
                }
                break;

            case Long:
                panel = new SpinnerFieldPanel<Long>("panel", schemaTO.getName(),
                        Long.class, new Model<Long>(), null, null);

                if (required) {
                    panel.addRequiredLabel();
                }
                break;

            case Double:
                panel = new SpinnerFieldPanel<Double>("panel", schemaTO.getName(),
                        Double.class, new Model<Double>(), null, null);

                if (required) {
                    panel.addRequiredLabel();
                }
                break;

            case Binary:
                panel = new BinaryFieldPanel("panel", schemaTO.getName(), new Model<String>(),
                        schemas.containsKey(schemaTO.getName())
                                ? schemas.get(schemaTO.getName()).getMimeType()
                                : null);

                if (required) {
                    panel.addRequiredLabel();
                }
                break;

            default:
                panel = new AjaxTextFieldPanel("panel", schemaTO.getName(), new Model<String>());

                if (required) {
                    panel.addRequiredLabel();
                }
        }

        panel.setReadOnly(readOnly);
        panel.setNewModel(attributeTO.getValues());

        return panel;
    }

    private Map<String, String> getEnumeratedKeyValues(final SchemaTO schemaTO) {
        final Map<String, String> res = new HashMap<String, String>();

        final String[] values = StringUtils.isBlank(schemaTO.getEnumerationValues())
                ? new String[0]
                : schemaTO.getEnumerationValues().split(SyncopeConstants.ENUM_VALUES_SEPARATOR);

        final String[] keys = StringUtils.isBlank(schemaTO.getEnumerationKeys())
                ? new String[0]
                : schemaTO.getEnumerationKeys().split(SyncopeConstants.ENUM_VALUES_SEPARATOR);

        for (int i = 0; i < values.length; i++) {
            res.put(values[i].trim(), keys.length > i ? keys[i].trim() : null);
        }

        return res;
    }

    private List<String> getEnumeratedValues(final SchemaTO schemaTO) {
        final List<String> res = new ArrayList<String>();

        final String[] values = StringUtils.isBlank(schemaTO.getEnumerationValues())
                ? new String[0]
                : schemaTO.getEnumerationValues().split(SyncopeConstants.ENUM_VALUES_SEPARATOR);

        for (String value : values) {
            res.add(value.trim());
        }

        return res;
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if ((event.getPayload() instanceof RoleAttrTemplatesChange)) {
            final RoleAttrTemplatesChange update = (RoleAttrTemplatesChange) event.getPayload();
            if (attrTemplates != null && update.getType() == AttrTemplatesPanel.Type.rAttrTemplates) {
                setSchemas();
                setAttrs();
                update.getTarget().add(this);
            }
        }
    }
}
