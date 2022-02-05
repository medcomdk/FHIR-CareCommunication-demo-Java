package com.trifork.common;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import org.hl7.fhir.common.hapi.validation.support.*;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;

import java.io.IOException;

public class Validator {

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
