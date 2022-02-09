package com.trifork.common;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import org.hl7.fhir.common.hapi.validation.support.*;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;

import java.io.IOException;

public class Validator {

  public static final String SOR_OID = "urn:oid:1.2.208.176.1.1";
  public static final String EAN_OID = "urn:oid:1.3.88";
  public static final String UNKNOWN_ENDPOINT = "http://medcomfhir.dk/unknown";
  public static final String MESSAGE_EVENT_CODE_SYSTEM = "http://medcomfhir.dk/fhir/core/1.0/CodeSystem/medcom-messaging-eventCodes";
  public static final String CARE_COMMUNICATION_MESSAGE = "care-communication-message";
  public static final String MESSAGING_ACTIVITY_CODES = "http://medcomfhir.dk/fhir/core/1.0/CodeSystem/medcom-messaging-activityCodes";
  public static final String MEDCOM_CARE_COMMUNICATION_PROFILE = "http://medcomfhir.dk/fhir/core/1.0/StructureDefinition/medcom-careCommunication-message";
  public static final String DATETIME_EXTENSION_URL = "http://medcomfhir.dk/fhir/core/1.0/StructureDefinition/medcom-core-datetime-extension";
  public static final String AUTHOR_EXTENSION_URL = "http://medcomfhir.dk/fhir/core/1.0/StructureDefinition/medcom-core-author-extension";
  public static final String DESTINATION_USE_EXTENSION = "http://medcomfhir.dk/fhir/core/1.0/StructureDefinition/medcom-messaging-destinationUseExtension";
  public static final String DESTINATION_USE = "http://medcomfhir.dk/fhir/core/1.0/CodeSystem/medcom-messaging-destinationUse";
  public static final String COMMUNICATION_CATEGORY_CODES = "http://medcomfhir.dk/fhir/core/1.0/CodeSystem/medcom-careCommunication-categoryCodes";

  public static FhirValidator buildValidator() {
    FhirContext ctx = FhirContext.forR4();
    NpmPackageValidationSupport npmPackageSupport = new NpmPackageValidationSupport(ctx);

    try {
      npmPackageSupport.loadPackageFromClasspath("classpath:dk-core.tgz");
      npmPackageSupport.loadPackageFromClasspath("classpath:dk-medcom.tgz");
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }

    // Create a support chain including the NPM Package Support
    ValidationSupportChain validationSupportChain = new ValidationSupportChain(npmPackageSupport,
        new DefaultProfileValidationSupport(ctx), new CommonCodeSystemsTerminologyService(ctx),
        new InMemoryTerminologyServerValidationSupport(ctx),
        new SnapshotGeneratingValidationSupport(ctx));
    CachingValidationSupport validationSupport = new CachingValidationSupport(
        validationSupportChain);

    // Create a validator. Note that for good performance you can create as many validator objects
    // as you like, but you should reuse the same validation support object in all of the,.
    FhirValidator validator = ctx.newValidator();
    FhirInstanceValidator instanceValidator = new FhirInstanceValidator(validationSupport);
    validator.registerValidatorModule(instanceValidator);
    return validator;
  }
}
