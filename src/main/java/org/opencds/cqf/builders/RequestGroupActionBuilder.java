package org.opencds.cqf.builders;

import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.exceptions.FHIRException;

import java.util.Collections;
import java.util.List;

public class RequestGroupActionBuilder extends BaseBuilder<RequestGroup.RequestGroupActionComponent> {

    public RequestGroupActionBuilder() {
        super(new RequestGroup.RequestGroupActionComponent());
    }

    // TODO - incomplete

    public RequestGroupActionBuilder buildLabel(String label) {
        complexProperty.setLabel(label);
        return this;
    }

    public RequestGroupActionBuilder buildTitle(String title) {
        complexProperty.setTitle(title);
        return this;
    }

    public RequestGroupActionBuilder buildDescripition(String description) {
        complexProperty.setDescription(description);
        return this;
    }

    public RequestGroupActionBuilder buildDocumentation(List<RelatedArtifact> documentation) {
        complexProperty.setDocumentation(documentation);
        return this;
    }

    public RequestGroupActionBuilder buildType(Coding type) {
        complexProperty.setType(type);
        return this;
    }

    public RequestGroupActionBuilder buildResource(Reference resource) {
        complexProperty.setResource(resource);
        return this;
    }

    public RequestGroupActionBuilder buildResourceTarget(Resource resource) {
        complexProperty.setResourceTarget(resource);
        return this;
    }

    public RequestGroupActionBuilder buildExtension(String extension) {
        complexProperty.setExtension(Collections.singletonList(new Extension().setUrl("http://example.org").setValue(new StringType(extension))));
        return this;
    }

    public RequestGroupActionBuilder buildExtension(List<Extension> extensions) {
        complexProperty.setExtension(extensions);
        return this;
    }

    public RequestGroupActionBuilder setCardinalityBehavior(PlanDefinition.ActionCardinalityBehavior cardinalityBehavior) {
        try {
            complexProperty.setCardinalityBehavior( RequestGroup.ActionCardinalityBehavior.fromCode(cardinalityBehavior.toCode()));
        } catch (FHIRException e) {
            e.printStackTrace();
        }
        return this;
    }

    public RequestGroupActionBuilder setGroupingBehavior( PlanDefinition.ActionGroupingBehavior actionGroupingBehavior) {
        try {
            complexProperty.setGroupingBehavior( RequestGroup.ActionGroupingBehavior.fromCode(actionGroupingBehavior.toCode()));
        } catch (FHIRException e) {
            e.printStackTrace();
        }
        return this;
    }

    public RequestGroupActionBuilder setPreCheckBehavior(PlanDefinition.ActionPrecheckBehavior precheckBehavior) {
        try {
            complexProperty.setPrecheckBehavior( RequestGroup.ActionPrecheckBehavior.fromCode(precheckBehavior.toCode()));
        } catch (FHIRException e) {
            e.printStackTrace();
        }
        return this;
    }

    public RequestGroupActionBuilder setSelectionBehavior(PlanDefinition.ActionSelectionBehavior selectionBehavior) {
        try {
            complexProperty.setSelectionBehavior( RequestGroup.ActionSelectionBehavior.fromCode(selectionBehavior.toCode()));
        } catch (FHIRException e) {
            e.printStackTrace();
        }
        return this;
    }

    public RequestGroupActionBuilder buildAction(RequestGroup.RequestGroupActionComponent requestGroupActionComponent) {
        complexProperty.addAction( requestGroupActionComponent );
        return this;
    }
}
