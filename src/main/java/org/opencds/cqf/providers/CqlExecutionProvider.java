package org.opencds.cqf.providers;

import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.hl7.fhir.dstu3.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.dstu3.hapi.validation.DefaultProfileValidationSupport;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.utils.FHIRPathEngine;
import org.hl7.fhir.exceptions.FHIRException;
import org.opencds.cqf.config.STU3LibraryLoader;
import org.opencds.cqf.config.STU3LibrarySourceProvider;
import org.opencds.cqf.cql.execution.Context;
import org.opencds.cqf.cql.execution.LibraryLoader;
import org.opencds.cqf.helpers.LibraryHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Bryn on 1/16/2017.
 */
public class CqlExecutionProvider {
    private JpaDataProvider provider;
    private final FHIRPathEngine fhirPathEngine;
    private static final Logger logger = LoggerFactory.getLogger(CqlExecutionProvider.class );

    public CqlExecutionProvider(JpaDataProvider provider) {
        this.provider = provider;
        HapiWorkerContext hapiWorkerContext = new HapiWorkerContext(provider.getFhirContext(), new DefaultProfileValidationSupport());
        fhirPathEngine = new FHIRPathEngine(hapiWorkerContext);
    }

    private ModelManager modelManager;
    private ModelManager getModelManager() {
        if (modelManager == null) {
            modelManager = new ModelManager();
        }
        return modelManager;
    }

    private LibraryManager libraryManager;
    private LibraryManager getLibraryManager() {
        if (libraryManager == null) {
            libraryManager = new LibraryManager(getModelManager());
            libraryManager.getLibrarySourceLoader().clearProviders();
            libraryManager.getLibrarySourceLoader().registerProvider(getLibrarySourceProvider());
        }
        return libraryManager;
    }

    private STU3LibraryLoader libraryLoader;
    private STU3LibraryLoader getLibraryLoader() {
        if (libraryLoader == null) {
            libraryLoader = new STU3LibraryLoader(getLibraryResourceProvider(), getLibraryManager(), getModelManager());
        }
        return libraryLoader;
    }

    private STU3LibrarySourceProvider librarySourceProvider;
    private STU3LibrarySourceProvider getLibrarySourceProvider() {
        if (librarySourceProvider == null) {
            librarySourceProvider = new STU3LibrarySourceProvider(getLibraryResourceProvider());
        }
        return librarySourceProvider;
    }

    private LibraryResourceProvider getLibraryResourceProvider() {
        return (LibraryResourceProvider)provider.resolveResourceProvider("Library");
    }

    private List<Reference> cleanReferences(List<Reference> references) {
        List<Reference> cleanRefs = new ArrayList<>();
        List<Reference> noDupes = new ArrayList<>();

        for (Reference reference : references) {
            boolean dup = false;
            for (Reference ref : noDupes) {
                if (ref.equalsDeep(reference))
                {
                    dup = true;
                }
            }
            if (!dup) {
                noDupes.add(reference);
            }
        }
        for (Reference reference : noDupes) {
            cleanRefs.add(
                    new Reference(
                            new IdType(
                                    reference.getReferenceElement().getResourceType(),
                                    reference.getReferenceElement().getIdPart().replace("#", ""),
                                    reference.getReferenceElement().getVersionIdPart()
                            )
                    )
            );
        }
        return cleanRefs;
    }

    private Iterable<Reference> getLibraryReferences(DomainResource instance) {
        List<Reference> references = new ArrayList<>();

        if (instance.hasContained()) {
            for (Resource resource : instance.getContained()) {
                if (resource instanceof Library) {
                    resource.setId(resource.getIdElement().getIdPart().replace("#", ""));
                    getLibraryResourceProvider().getDao().update((Library) resource);
//                    getLibraryLoader().putLibrary(resource.getIdElement().getIdPart(), getLibraryLoader().toElmLibrary((Library) resource));
                }
            }
        }

        if (instance instanceof ActivityDefinition) {
            references.addAll(((ActivityDefinition)instance).getLibrary());
        }

        else if (instance instanceof PlanDefinition) {
            references.addAll(((PlanDefinition)instance).getLibrary());
        }

        else if (instance instanceof Measure) {
            references.addAll(((Measure)instance).getLibrary());
        }

        for (Extension extension : instance.getExtensionsByUrl("http://hl7.org/fhir/StructureDefinition/cqif-library"))
        {
            Type value = extension.getValue();

            if (value instanceof Reference) {
                references.add((Reference)value);
            }

            else {
                throw new RuntimeException("Library extension does not have a value of type reference");
            }
        }

        return cleanReferences(references);
    }

    private String buildIncludes(Iterable<Reference> references) {
        StringBuilder builder = new StringBuilder();
        for (Reference reference : references) {

            if (builder.length() > 0) {
                builder.append(" ");
            }

            builder.append("include ");

            // TODO: This assumes the libraries resource id is the same as the library name, need to work this out better
            builder.append(reference.getReferenceElement().getIdPart());

            if (reference.getReferenceElement().getVersionIdPart() != null) {
                builder.append(" version '");
                builder.append(reference.getReferenceElement().getVersionIdPart());
                builder.append("'");
            }

            builder.append(" called ");
            builder.append(reference.getReferenceElement().getIdPart());
        }

        return builder.toString();
    }

    /**
     * Evaluate a dynamic value in this context
     * @return
     */
    public Object evaluateInContext(DomainResource instance, String language, String dynamicValue, String patientId) throws FHIRException {
        Object result = null;
        logger.info("Execute ("+language+") "+dynamicValue);

        if ( language==null || language.equals("text/cql")) {
            result = evaluateInContext(instance,dynamicValue,patientId );
        } else if (language.equals("text/fhirpath")) {
            result = fhirPathEngine.evaluate( instance, dynamicValue );
        } else { // assume cql
            result = evaluateInContext(instance,dynamicValue,patientId );
        }
        return result;
    }

        /* Evaluates the given CQL expression in the context of the given resource */
    /* If the resource has a library extension, or a library element, that library is loaded into the context for the expression */
    private Object evaluateInContext(DomainResource instance, String cql, String patientId) {


        Iterable<Reference> libraries = getLibraryReferences(instance);

        // Provide the instance as the value of the '%context' parameter, as well as the value of a parameter named the same as the resource
        // This enables expressions to access the resource by root, as well as through the %context attribute
        String source = String.format("library LocalLibrary using FHIR version '3.0.0' include FHIRHelpers version '3.0.0' called FHIRHelpers %s parameter %s %s parameter \"%%context\" %s define Expression: %s",
                buildIncludes(libraries), instance.fhirType(), instance.fhirType(), instance.fhirType(), cql);
//        String source = String.format("library LocalLibrary using FHIR version '1.8' include FHIRHelpers version '1.8' called FHIRHelpers %s parameter %s %s parameter \"%%context\" %s define Expression: %s",
//                buildIncludes(libraries), instance.fhirType(), instance.fhirType(), instance.fhirType(), cql);

        org.cqframework.cql.elm.execution.Library library = LibraryHelper.translateLibrary(source, getLibraryManager(), getModelManager());
        Context context = new Context(library);
        context.setParameter(null, instance.fhirType(), instance);
        context.setParameter(null, "%context", instance);
        context.setExpressionCaching(true);
        context.registerLibraryLoader(getLibraryLoader());
        context.setContextValue("Patient", patientId);
        context.registerDataProvider("http://hl7.org/fhir", provider);
        return context.resolveExpressionRef("Expression").evaluate(context);
    }
}
