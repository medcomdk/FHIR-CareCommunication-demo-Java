package com.trifork.receiver;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.BundleUtil;
import com.trifork.common.Validator;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Communication;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Provenance;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ReceiverApplication implements CommandLineRunner {

  public static void main(String[] args) {
    SpringApplication.run(ReceiverApplication.class, args);
  }

  Scanner scanner = new Scanner(System.in);


  @Override
  public void run(String... args) {

    System.out.println("Read file from path:");
    var path = getInputString("$: ");

    var fhirContext = FhirContext.forR4();
    Bundle messageBundle = null;
    try {
      messageBundle = fhirContext.newJsonParser()
          .parseResource(Bundle.class, new FileInputStream(path));
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e.getMessage(), e);
    }

    var outcome = Validator.buildValidator().validateWithResult(messageBundle);


    /*
    //Explicit validation to a profile could be done using eg.
    var outcomeAccordingToWrongProfile = Validator.buildValidator()
        .validateWithResult(messageBundle, new ValidationOptions().addProfile(
            "http://medcomfhir.dk/fhir/core/1.0/StructureDefinition/medcom-hospitalNotification-message"));

     */

    System.out.println("---------------------------");
    // Inspect validation outcome
    outcome.getMessages().stream()
        .forEach(c -> System.out.println(c.getSeverity() + " - " + c.getMessage()));

    var resources = BundleUtil.toListOfResources(fhirContext, messageBundle);

    Patient patient = getSingleResource(Patient.class, resources);
    Communication communication = getSingleResource(Communication.class, resources);
    Provenance provenance = getSingleResource(Provenance.class, resources);
    MessageHeader messageHeader = getSingleResource(MessageHeader.class, resources);
    List<Organization> organizations = getResources(Organization.class, resources);

    System.out.println("---------------------------");
    patient.getName().stream().forEach(name -> {
      System.out.println(name.getGiven() + " " + name.getFamily());
    });

    System.out.println("---------------------------");
    communication.getPayload().stream().forEach(payload -> {
      System.out.println(payload.getContent());
    });


  }

  static <T> T getSingleResource(Class clazz, List<IBaseResource> resourceList) {
    return (T) resourceList.stream().filter(clazz::isInstance).map(clazz::cast)
        .collect(singleElement()).get();
  }

  static List getResources(Class clazz, List<IBaseResource> resourceList) {
    return resourceList.stream().filter(clazz::isInstance).map(clazz::cast)
        .collect(Collectors.toList());
  }

  static <T> Collector<T, ?, Optional<T>> singleElement() {
    return Collectors.collectingAndThen(Collectors.toList(),
        list -> list.size() == 1 ? Optional.of(list.get(0)) : Optional.empty());
  }

  String getInputString(String input) {
    var result = scanner.nextLine();
    return result.isBlank() ? input : result;
  }
}
