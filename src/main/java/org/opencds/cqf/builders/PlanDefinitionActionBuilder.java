package org.opencds.cqf.builders;

import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.r4.model.codesystems.ActionType;

public class PlanDefinitionActionBuilder extends BaseBuilder<PlanDefinition.PlanDefinitionActionComponent> {

    public PlanDefinitionActionBuilder() {
        super(new PlanDefinition.PlanDefinitionActionComponent());
    }

    public PlanDefinitionActionBuilder(PlanDefinition.PlanDefinitionActionComponent complexProperty) {
        super(complexProperty);
    }

    public PlanDefinitionActionBuilder buildDescription(String description) {
        complexProperty.setDescription(description);
        return this;
    }

    public PlanDefinitionActionBuilder buildTriggerDefinition(TriggerDefinition triggerDefinition) {
        complexProperty.addTriggerDefinition(triggerDefinition);
        return this;
    }

    public PlanDefinitionActionBuilder addCondition( PlanDefinition.PlanDefinitionActionConditionComponent planDefinitionActionConditionComponent) {
        complexProperty.addCondition(planDefinitionActionConditionComponent);
        return  this;
    }

    public PlanDefinitionActionBuilder buildDynamicCqlValue(String path, String expression) {
        PlanDefinition.PlanDefinitionActionDynamicValueComponent planDefinitionActionDynamicValueComponent =
            new PlanDefinition.PlanDefinitionActionDynamicValueComponent()
                .setPath(path)
                .setLanguage("text/cql")
                .setExpression(expression);

        complexProperty.addDynamicValue(planDefinitionActionDynamicValueComponent);
        return this;
    }

    public PlanDefinitionActionBuilder buildDynamicFhirPathValue(String path, String expression) {
        PlanDefinition.PlanDefinitionActionDynamicValueComponent planDefinitionActionDynamicValueComponent =
            new PlanDefinition.PlanDefinitionActionDynamicValueComponent()
                .setPath(path)
                .setLanguage("text/fhirpath")
                .setExpression(expression);

        complexProperty.addDynamicValue(planDefinitionActionDynamicValueComponent);
        return this;
    }

    public PlanDefinitionActionBuilder buildTitle(String title) {
        complexProperty.setTitle(title);
        return this;
    }

    public PlanDefinitionActionBuilder buildDescripition(String description) {
        complexProperty.setDescription(description);
        return this;
    }

    public PlanDefinitionActionBuilder buildSelectionBehavior(PlanDefinition.ActionSelectionBehavior actionSelectionBehavior) {
        complexProperty.setSelectionBehavior(actionSelectionBehavior);
        return this;
    }

    public PlanDefinitionActionBuilder buildType(ActionType action) {
        Coding coding = new Coding();
        coding.setSystem( action.getSystem() );
        coding.setCode(action.toCode());
        coding.setDisplay(action.getDisplay());

        complexProperty.setType(coding);

        return this;
    }

    public PlanDefinitionActionBuilder buildDefinition( String definition ){
        complexProperty.setDefinition( new Reference().setReference(definition ));
        return this;
    }
}
