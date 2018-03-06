package org.opencds.cqf.builders;

import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.ValueSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

public class ValueSetComposeComponentBuilder extends BaseBuilder<ValueSet.ValueSetComposeComponent> {
    public ValueSetComposeComponentBuilder() {
        super(new ValueSet.ValueSetComposeComponent());
    }
    public ValueSetComposeComponentBuilder(ValueSet.ValueSetComposeComponent complexProperty) {
        super(complexProperty);
    }

    public ValueSetComposeComponentBuilder buildInclude(Coding[] mriCodingsp) {
        HashMap<String,ArrayList<Coding>> systemMap = new HashMap<>();
        Arrays.stream( mriCodingsp ).forEach(
            coding -> {
                if( !systemMap.containsKey(coding.getSystem())){
                    systemMap.put(coding.getSystem(), new ArrayList<>());
                }
                systemMap.get( coding.getSystem() ).add(coding);
            }
        );

        systemMap.entrySet().stream()
            .forEach( stringArrayListEntry -> {
                complexProperty.addInclude()
                    .setSystem(stringArrayListEntry.getKey())
                    .setConcept(
                        stringArrayListEntry.getValue().stream()
                            .map(coding -> new ValueSet.ConceptReferenceComponent()
                                    .setCode( coding.getCode() ).setDisplay( coding.getDisplay() )
                            )
                            .collect(Collectors.toList())
                        );
                }
            );

        return this;
    }
}
