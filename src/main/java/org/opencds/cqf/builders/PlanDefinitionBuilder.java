package org.opencds.cqf.builders;

import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.codesystems.PlanDefinitionType;

public class PlanDefinitionBuilder extends BaseBuilder<PlanDefinition> {

    public PlanDefinitionBuilder() {
        super(new PlanDefinition());
    }

    public PlanDefinitionBuilder( PlanDefinition planDefinition ) {
        super(planDefinition);
    }

    public PlanDefinitionBuilder buildId(String id) {
        complexProperty.setId(id);
        return this;
    }

    public PlanDefinitionBuilder buildVersion(String version) {
        complexProperty.setVersion(version);
        return this;
    }

    public PlanDefinitionBuilder buildNameTitle(String titleName) {
        complexProperty.setName(titleName);
        complexProperty.setTitle(titleName);
        return this;
    }

    public PlanDefinitionBuilder buildType( PlanDefinitionType planDefinitionType ) {
        complexProperty.setType( new CodeableConceptBuilder()
            .buildCoding(
                new Coding()
                    .setCode(planDefinitionType.toCode())
                    .setDisplay(planDefinitionType.getDisplay())
                    .setSystem(planDefinitionType.getSystem())
            )
            .build()
        );
        return this;
    }

    public PlanDefinitionBuilder buildPublisher(String publisher) {
        complexProperty.setPublisher(publisher);
        return this;
    }

    public PlanDefinitionBuilder buildDescription(String s) {
        complexProperty.setDescription(s);
        return this;
    }

    public PlanDefinitionBuilder buildAction(PlanDefinition.PlanDefinitionActionComponent planDefinitionActionComponent) {
        complexProperty.addAction(planDefinitionActionComponent);
        return this;
    }
}
