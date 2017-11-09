package org.opencds.cqf.cds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.StringType;
import org.opencds.cqf.cql.data.fhir.BaseFhirDataProvider;
import org.opencds.cqf.cql.execution.Context;

import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import ca.uhn.fhir.model.primitive.IdDt;

public abstract class CdsRequestProcessor implements Processor {
    CdsHooksRequest request;
    PlanDefinition planDefinition;
    LibraryResourceProvider libraryResourceProvider;

    CdsRequestProcessor(CdsHooksRequest request, PlanDefinition planDefinition, LibraryResourceProvider libraryResourceProvider) {
        this.request = request;
        this.planDefinition = planDefinition;
        this.libraryResourceProvider = libraryResourceProvider;
    }

    List<CdsCard> resolveActions(Context executionContext) {
        List<CdsCard> cards = new ArrayList<>();

        walkAction(executionContext, cards, planDefinition.getAction());

        return cards;
    }
    
    private String getSummary(Context executionContext, PlanDefinition.PlanDefinitionActionComponent action) {
    		String summary = null;
    		// TODO - this throws runtime exception if expression is not found in current library, should return null??
//    		ExpressionDef expressionDef = executionContext.resolveExpressionRef("getSummary");
//    		if (expressionDef != null && expressionDef.getExpression() != null) {
//	        	summary = (String) expressionDef.getExpression().evaluate(executionContext);
//    		}
        if (summary == null && action.getTitle() != null) {
    			summary = action.getTitle();
        }
        if (summary == null && action.hasDefinition() && action.getDefinition().getReferenceElement().getResourceType().equals("ActivityDefinition")) {
        		// TODO - this returns null, fix to resolve ActivityDefinition reference	
            ActivityDefinition definition = (ActivityDefinition) action.getDefinition().getResource();
    			if (definition != null) {
    				summary = definition.getTitle();
    			}
        }
        
        return summary;
    }

    private String getDetail(Context executionContext, PlanDefinition.PlanDefinitionActionComponent action) {
    		String detail = null;
//        Expression expression = executionContext.resolveExpressionRef("getDetail").getExpression();
//        if (expression != null) {
//        		detail = (String) expression.evaluate(executionContext);
//        }
        if (detail == null && action.getDescription() != null) {
        		detail = action.getDescription();
        }
        if (detail == null && action.hasDefinition() && action.getDefinition().getReferenceElement().getResourceType().equals("ActivityDefinition")) {
    		// TODO - this returns null, fix to resolve ActivityDefinition reference	
        	ActivityDefinition definition = (ActivityDefinition) action.getDefinition().getResource();
    			if (definition != null) {
    				detail = definition.getDescription();
    			}
        }
        
        return detail;
    }

    private String getIndicator(Context executionContext, PlanDefinition.PlanDefinitionActionComponent action) {
    		String indicator = null;
//        Expression expression = executionContext.resolveExpressionRef("getIndicator").getExpression();
//        if (expression != null) {
//        		indicator = (String) expression.evaluate(executionContext);
//        }
        if (indicator == null) {
        		indicator = "info";
        }
        
        return indicator;
    }

    private void walkAction(Context executionContext, List<CdsCard> cards, List<PlanDefinition.PlanDefinitionActionComponent> actions) {
        for (PlanDefinition.PlanDefinitionActionComponent action : actions) {
            boolean conditionsMet = true;
            for (PlanDefinition.PlanDefinitionActionConditionComponent condition: action.getCondition()) {
                if (condition.getKind() == PlanDefinition.ActionConditionKind.APPLICABILITY) {
                    if (!condition.hasExpression()) {
                        continue;
                    }

                    Object result = executionContext.resolveExpressionRef(condition.getExpression()).getExpression().evaluate(executionContext);

                    if (!(result instanceof Boolean)) {
                        continue;
                    }

                    if (!(Boolean) result) {
                        conditionsMet = false;
                    }
                }
            }
            if (conditionsMet) {

                /*
                    Cases:
                        Definition element provides guidance for action
                        Nested actions
                        Standardized CQL (when first 2 aren't present)
                */

                if (action.hasDefinition()) {
                    if (action.getDefinition().getReferenceElement().getResourceType().equals("ActivityDefinition")) {
                        BaseFhirDataProvider provider = (BaseFhirDataProvider) executionContext.resolveDataProvider(new QName("http://hl7.org/fhir", ""));
                        Parameters inParams = new Parameters();
                        inParams.addParameter().setName("patient").setValue(new StringType(request.getPatientId()));

                        Parameters outParams = provider.getFhirClient()
                                .operation()
                                .onInstance(new IdDt("ActivityDefinition", action.getDefinition().getReferenceElement().getIdPart()))
                                .named("$apply")
                                .withParameters(inParams)
                                .useHttpGet()
                                .execute();

                        List<Parameters.ParametersParameterComponent> response = outParams.getParameter();
                        Resource resource = response.get(0).getResource();

                        if (resource == null) {
                            continue;
                        }

                        // TODO - currently only have suggestions that create resources - implement delete and update.
                        CdsCard card = new CdsCard();
                        card.setIndicator(getIndicator(executionContext, action));
                        String summary = getSummary(executionContext, action);
                        if (summary == null) {
                        		summary = "Suggestion for " + resource.getResourceType().toString();
                        }
                        card.setSummary(summary);
                        card.setDetail(getDetail(executionContext, action));
                        
                        CdsCard.Suggestions suggestion = new CdsCard.Suggestions();
                        suggestion.setActions(
                                Collections.singletonList(
                                        new CdsCard.Suggestions.Action()
                                                .setType(CdsCard.Suggestions.Action.ActionType.create)
                                                .setResource(resource)
                                )
                        );
                        card.getSuggestions().add(suggestion);
                        cards.add(card);
                    }

                    else {
                        // PlanDefinition $apply
                        // TODO

                        // TODO - suggestion to create CarePlan
                    }
                }

                else if (action.hasAction()) {
                    walkAction(executionContext, cards, action.getAction());
                }

                // TODO - dynamicValues

                else {
                    CdsCard card = new CdsCard();
                    card.setSummary(getSummary(executionContext, action));
                    card.setDetail(getDetail(executionContext, action));
                    card.setIndicator(getIndicator(executionContext, action));
                    
                    cards.add(card);
                }
            }
        }
    }
}
