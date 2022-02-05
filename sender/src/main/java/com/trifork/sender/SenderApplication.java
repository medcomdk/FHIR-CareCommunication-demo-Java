package com.trifork.sender;

import ca.uhn.fhir.context.FhirContext;
import com.trifork.common.Validator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

@SpringBootApplication
public class SenderApplication implements CommandLineRunner {

  public static void main(String[] args) {
    SpringApplication.run(SenderApplication.class, args);
  }

  Scanner scanner = new Scanner(System.in);

  @Override
  public void run(String... args) {

    System.out.println("Sender Name:");
    var senderName = getInputString("Sender Name");
    System.out.println("  - SOR Code:");
    var senderSORCode = getInputString("123456789012345");
    System.out.println("  - EAN Code:");
    var senderEANCode = getInputString("123456789012");
    var sender = Builder.buildOrganization(senderName, senderSORCode, senderEANCode);

    System.out.println("Receiver Name:");
    var receiverName = getInputString("Receiver Name");
    System.out.println("  - SOR Code:");
    var receiverSORCode = getInputString("0987654321098765");
    System.out.println("  - EAN Code:");
    var receiverEANCode = getInputString("098765432109");
    var receiver = Builder.buildOrganization(receiverName, receiverSORCode, receiverEANCode);

    System.out.println("Patient Name:");
    var patientName = getInputString("Patient Name");
    System.out.println("  - Address (street):");
    var addressStreet = getInputString("Street Name X");
    System.out.println("  - Address (postal code):");
    var addressPostalCode = getInputString("Postal Code");
    System.out.println("  - Address (city):");
    var addressCity = getInputString("City");
    var address = Builder.buildAddress(addressStreet, addressPostalCode, addressCity);
    System.out.println("  - Deceased (Y/N):");
    var deceased = getInputString("Y").toUpperCase().startsWith("Y");
    var patient = Builder.buildPatient(patientName, List.of(address), deceased);
    var category = "carecoordination";

    var author = Builder.buildPractitioner("Michael", "Burns");
    System.out.println("Text:");
    var text = getInputString("Text content:");

    var communication = Builder.buildCommunication(new Reference(author), text,
        new Reference(patient),
        LocalDateTime.now());

    var messageBundle = Builder.buildCareCommunicationMessageBundle(patient, author, communication,
        sender,
        receiver);

    // Conduct validation
    var outcome = Validator.buildValidator().validateWithResult(messageBundle);

    System.out.println("---------------------------");
    // Inspect validation outcome
    outcome.getMessages().stream()
        .forEach(c -> System.out.println(c.getSeverity() + " - " + c.getMessage()));

    System.out.println("---------------------------");
    // Serialize the model to Json
    var asJson = FhirContext.forR4().newJsonParser().encodeResourceToString(messageBundle);
    System.out.println("Wire format: " + asJson);

    var tmpdir = System.getProperty("java.io.tmpdir");
    var path = Path.of(tmpdir, UUID.randomUUID() + ".json");
    System.out.println("---------------------------");
    System.out.println("Writing file to file path: " + path);
    try {
      Files.writeString(path, asJson, StandardOpenOption.CREATE_NEW);
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }


  String getInputString(String input) {
    var result = scanner.nextLine();
    return result.isBlank() ? input : result;
  }
}
