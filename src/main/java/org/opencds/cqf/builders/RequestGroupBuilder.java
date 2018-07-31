package org.opencds.cqf.builders;

import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RequestGroup;
import org.hl7.fhir.exceptions.FHIRException;

import java.util.Date;
import java.util.List;

public class RequestGroupBuilder extends BaseBuilder<RequestGroup> {

    public RequestGroupBuilder() {
        super(new RequestGroup());
    }

    // TODO - incomplete

    public RequestGroupBuilder buildStatus() {
        complexProperty.setStatus(RequestGroup.RequestStatus.DRAFT);
        return this;
    }

    public RequestGroupBuilder buildStatus(RequestGroup.RequestStatus status) {
        complexProperty.setStatus(status);
        return this;
    }

    public RequestGroupBuilder buildStatus(String status) throws FHIRException {
        complexProperty.setStatus(RequestGroup.RequestStatus.fromCode(status));
        return this;
    }

    public RequestGroupBuilder buildIntent() {
        complexProperty.setIntent(RequestGroup.RequestIntent.PROPOSAL);
        return this;
    }

    public RequestGroupBuilder buildIntent(RequestGroup.RequestIntent intent) {
        complexProperty.setIntent(intent);
        return this;
    }

    public RequestGroupBuilder buildIntent(String intent) throws FHIRException {
        complexProperty.setIntent(RequestGroup.RequestIntent.fromCode(intent));
        return this;
    }

    public RequestGroupBuilder buildAction(List<RequestGroup.RequestGroupActionComponent> actions) {
        complexProperty.setAction(actions);
        return this;
    }
    public void buildAction(RequestGroup.RequestGroupActionComponent requestGroupActionComponent) {
        complexProperty.addAction(requestGroupActionComponent);
    }
    public RequestGroupBuilder buildExtension(List<Extension> extensions) {
        complexProperty.setExtension(extensions);
        return this;
    }

    public RequestGroupBuilder buildId(String id) {
        complexProperty.setId(id);
        return  this;
    }

    public RequestGroupBuilder buildSubject(Reference reference) {
        complexProperty.setSubject(reference);
        return this;
    }

    public RequestGroupBuilder buildContext(Reference reference) {
        complexProperty.setContext(reference);
        return this;
    }

    public RequestGroupBuilder buildAuthoredOnToNow() {
        complexProperty.setAuthoredOn(new Date());
        return this;
    }


}
