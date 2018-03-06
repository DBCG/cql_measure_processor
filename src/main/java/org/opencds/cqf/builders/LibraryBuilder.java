package org.opencds.cqf.builders;

import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.codesystems.LibraryType;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;

public class LibraryBuilder extends BaseBuilder<Library> {

    public LibraryBuilder( Library library ){
        super(library);
    }
    public LibraryBuilder() {
        this(new Library());
    }


    public LibraryBuilder buildId(String id) {
        complexProperty.setId( id );
        return this;
    }

    public LibraryBuilder buildVersion(String version) {
        complexProperty.setVersion(version);
        return this;
    }

    public LibraryBuilder buildStatus( Enumerations.PublicationStatus status ) {
        complexProperty.setStatus( status );
        return this;
    }

    public LibraryBuilder buildExperimental(boolean experimental) {
        complexProperty.setExperimental( experimental );
        return this;
    }

    public LibraryBuilder buildType( LibraryType libraryType ){
        CodeableConcept codeableConcept = new CodeableConceptBuilder()
            .buildCoding( new CodingBuilder()
                .buildCode( libraryType.getSystem(), libraryType.toCode(), libraryType.getDisplay() )
                .build()
            )
            .build();
        complexProperty.setType(codeableConcept);

        return this;
    }

    public LibraryBuilder buildCqlContent(String cqlString) throws UnsupportedEncodingException {
        Attachment attachment = new Attachment();
        attachment.setContentType("text/cql");
        byte[] cqlData =cqlString.getBytes("utf-8");
        attachment.setData( cqlData );
        complexProperty.addContent(attachment);
        return this;
    }
}
