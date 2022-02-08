package com.trifork.sender;

import org.hl7.fhir.r4.model.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.r4.model.HumanName.NameUse;


public class ResourceFactory {

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

  static String nextRandomId() {
    return UUID.randomUUID().toString();
  }

  public static Organization buildOrganization(String senderName, String senderSORCode,
      String senderEANCode) {
    var organization = new Organization().setName(senderName)
        .addIdentifier(new Identifier().setSystem(SOR_OID).setValue(senderSORCode))
        .addIdentifier(new Identifier().setSystem(EAN_OID).setValue(senderEANCode));
    organization.setId(nextRandomId());
    return organization;
  }

  public static Address buildAddress(String addressStreet, String addressPostalCode,
      String addressCity) {
    return new Address().addLine(addressStreet).setCity(addressCity)
        .setPostalCode(addressPostalCode);
  }

  public static Patient buildPatient(String patientName, List<Address> address, boolean deceased) {
    var patient = new Patient().addName(new HumanName().addGiven(patientName).setFamily(patientName).setUse(NameUse.OFFICIAL)).setAddress(address)
        .setDeceased(new BooleanType(deceased)).addIdentifier(new Identifier().setSystem("urn:oid:1.2.208.176.1.2").setValue("0506504005"));
    patient.setId(nextRandomId());
    return patient;
  }

  public static Communication buildCommunication(Reference author, String text, Reference patient,
      LocalDateTime date) {
    var communication = new Communication().setSubject(patient)
        .setStatus(Communication.CommunicationStatus.UNKNOWN)
        .setSent(new Date())
        .addCategory(new CodeableConcept().addCoding(new Coding().setSystem(
            COMMUNICATION_CATEGORY_CODES).setCode("carecoordination")));
    var payload = new Communication.CommunicationPayloadComponent().setContent(
        new StringType(text));
    payload.addExtension(DATETIME_EXTENSION_URL, new DateTimeType(Timestamp.valueOf((date))));
    payload.addExtension(AUTHOR_EXTENSION_URL, author);
    communication.addPayload(payload);
    communication.setId(nextRandomId());
    return communication;
  }

  public static Practitioner buildPractitioner(String given, String family) {
    var practitioner = new Practitioner().addName(
        new HumanName().addGiven(given).setFamily(family));
    practitioner.setId(nextRandomId());
    return practitioner;
  }

  public static Bundle buildCareCommunicationMessageBundle(Patient patient, Practitioner author,
      Communication communication, Organization sender, Organization receiver) {

    var messageHeader = buildMessageHeader(new Reference(communication), new Reference(sender),
        receiver);

    var provenance = buildProvenance(new Reference(messageHeader), new Reference(sender));

    var bundle = new Bundle().addEntry(new Bundle.BundleEntryComponent().setResource(messageHeader)
            .setFullUrl("MessageHeader/" + messageHeader.getId())).addEntry(
            new Bundle.BundleEntryComponent().setResource(sender)
                .setFullUrl("Organization/" + sender.getId())).addEntry(
            new Bundle.BundleEntryComponent().setResource(receiver)
                .setFullUrl("Organization/" + receiver.getId())).addEntry(
            new Bundle.BundleEntryComponent().setResource(patient)
                .setFullUrl("Patient/" + patient.getId())).addEntry(
            new Bundle.BundleEntryComponent().setResource(author)
                .setFullUrl("Practitioner/" + author.getId())).addEntry(
            new Bundle.BundleEntryComponent().setResource(provenance)
                .setFullUrl("Provenance/" + provenance.getId())).addEntry(
            new Bundle.BundleEntryComponent().setResource(communication)
                .setFullUrl("Communication/" + communication.getId()))
        .setType(Bundle.BundleType.MESSAGE).setTimestamp(new Date());
    bundle.getMeta().addProfile(MEDCOM_CARE_COMMUNICATION_PROFILE);
    bundle.setId(nextRandomId());
    return bundle;
  }

  private static MessageHeader buildMessageHeader(Reference communication, Reference sender,
      Organization receiver) {
    var messageHeader = new MessageHeader().setEvent(
            new Coding().setSystem(MESSAGE_EVENT_CODE_SYSTEM).setCode(CARE_COMMUNICATION_MESSAGE))
        .setSource(new MessageHeader.MessageSourceComponent().setEndpoint(UNKNOWN_ENDPOINT))
        .setSender(sender).addFocus(communication);

    var primaryDestination = new MessageHeader.MessageDestinationComponent().setEndpoint(
            UNKNOWN_ENDPOINT)
        .setReceiver(new Reference(receiver));
    primaryDestination.addExtension(new Extension().setUrl(DESTINATION_USE_EXTENSION)
        .setValue(new Coding()
            .setSystem(DESTINATION_USE)
            .setCode("primary")));
    messageHeader.addDestination(primaryDestination);

    messageHeader.setId(nextRandomId());
    return messageHeader;
  }

  private static Resource buildProvenance(Reference messageHeader, Reference sender) {
    var provenance = new Provenance().addTarget(messageHeader)
        .setOccurred(new DateTimeType(new Date())).setRecorded(new Date()).setActivity(
            new CodeableConcept(
                new Coding().setCode("new-message").setSystem(MESSAGING_ACTIVITY_CODES)))

        .addAgent(new Provenance.ProvenanceAgentComponent().setWho(sender));

    provenance.setId(nextRandomId());
    return provenance;
  }
}
