library bmi version '0.1.0'

using FHIR version '3.0.0'

include FHIRHelpers version '3.0.0' called FHIRHelpers

context Patient

//UTILITY////////////////////////////////////////////////////

define function mostRecentObservation( system String, code String ) :
  [FHIR.Observation] O
    with  O.code.coding C
      such that C.code.value = code
      and       C.system.value = system
  sort by issued.value

//NORMALIZE//////////////////////////////////////////////////

define function normalizeLenght( quantity FHIR.Quantity ) :
  case quantity.unit.value
     when 'cm' then quantity.value.value/100
     when 'mm' then quantity.value.value/1000
     when 'km' then quantity.value.value*1000
     else quantity.value.value
  end


//WEIGHT AND HEIGHT//////////////////////////////////////////

define getHeightObservations:
  mostRecentObservation( 'http://loinc.org', '8302-2')

define lastHeight:
  normalizeLenght( Last( mostRecentObservation( 'http://loinc.org', '8302-2') ).value )

define getWeightObservations:
  mostRecentObservation( 'http://snomed.info/sct', '27113001' )

define lastWeight:
  Last( mostRecentObservation( 'http://snomed.info/sct', '27113001') ).value.value

define recentWeightMeasurement:
  Exists( getWeightObservations )

define recentHeightMeasurement:
  Exists( getHeightObservations )

define recentBmiObservation:
  Exists( mostRecentObservation( 'http://loinc.org', '39156-5' ))

//BMI///////////////////////////////////////////////////////////

define function calculateBMI( weight Decimal, heigth Decimal ):
  weight / ( heigth*heigth)


//CREATE OBSERVATION////////////////////////////////////////////
define BmiCoding: FHIR.Coding {
  system:  FHIR.uri{ value: 'http://loinc.org' },
  display: FHIR.string{ value: '39156-5' },
  code :   FHIR.code{ value: 'Body mass index (BMI) [Ratio]' }
}

define VitalSignsCoding: FHIR.Coding {
  system:  FHIR.uri{ value: 'vital-signs' },
  display: FHIR.string{ value: 'Vital Signs' }
}

define VitalSignsCodeableConcept: FHIR.CodeableConcept {
  text:    FHIR.string{ value: 'Vital Signs' },
  coding : { VitalSignsCoding }
}

define bmivalue: FHIR.Quantity{
   system:  FHIR.uri{ value: 'http://unitsofmeasure.org' },
   unit:    FHIR.string{ value: 'kg/m2' },
   code :   FHIR.code{ value: 'kg/m2' },
   value:   FHIR.decimal{ value: calculateBMI( lastWeight, lastHeight ) }
}

define observationFromCdr: FHIR.Observation {
  issued  : FHIR.instant{ value: Now() },
  //status  : FHIR.ObservationStatus{ value: 'registered' },
  category: { VitalSignsCodeableConcept },
  code:     FHIR.CodeableConcept {
                text:   FHIR.string{ value: 'BMI' },
                coding: { BmiCoding }
              },
  subject: FHIR.Reference{
            reference: FHIR.string{ value: 'Patient/'+Patient.id.value }
          }

}
