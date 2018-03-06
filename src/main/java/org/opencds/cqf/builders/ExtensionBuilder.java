package org.opencds.cqf.builders;

import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.Type;

public class ExtensionBuilder extends BaseBuilder<Extension> {

    public ExtensionBuilder() {
        this( new Extension());
    }

    public ExtensionBuilder(Extension complexProperty) {
            super(complexProperty);
    }

    public ExtensionBuilder buildUrl(String url) {
        complexProperty.setUrl(url);
        return this;
    }

    public ExtensionBuilder buildValue(Type value) {
        complexProperty.setValue(value);
        return this;
    }

    public ExtensionBuilder buildValue(String value) {
        complexProperty.setValue(new StringType(value));
        return this;
    }
}
