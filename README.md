# Care Communication Demo in Java
This project demonstrates how to use the official HL7 FHIR libraries in Java for parsing and serializing a Care Communication message according to the implementation guide provided by MedCom. It does not intend to be a full implementation but serves as a guiding sample that will make it easier to implement this and other profiles provided by MedCom. A similar sample exists for .NET (https://github.com/medcomdk/FHIR-CareCommunication-demo-C-)

The implementation guide containing the Care Commmunication profile is found here: http://build.fhir.org/ig/hl7dk/kl-medcom/

Documentation of the leading HL7 FHIR libraries in Java are provided by the HAPI FHIR project found here: https://github.com/hapifhir

## Prerequisites
- Maven
- Java SDK 11

## Projects
The solution contains 3 projects 
- A console app to create a care communication FHIR message
- A console app to parse a care communication FHIR message
- A common library for validating care communication FHIR messages

## FHIR Related Maven Packages
```xml
    <dependency>
      <groupId>ca.uhn.hapi.fhir</groupId>
      <artifactId>hapi-fhir-validation</artifactId>
      <version>5.6.2</version>
    </dependency>

    <dependency>
      <groupId>ca.uhn.hapi.fhir</groupId>
      <artifactId>hapi-fhir-structures-r4</artifactId>
      <version>5.6.2</version>
    </dependency>

    <dependency>
      <groupId>ca.uhn.hapi.fhir</groupId>
      <artifactId>hapi-fhir-validation-resources-r4</artifactId>
      <version>5.6.2</version>
    </dependency>
```


## Parsing from FHIR
The CareCommunication parser uses the standard parsers from HAPI FHIR. The HAPI FHIR libraries contains separate parsers for Json and Xml that are used as follows:
```java
var parser = FhirContext.forR4().newJsonParser().parseResource(Bundle.class, /* ... */ );
```
```java
var parser = FhirContext.forR4().newXmlParser().parseResource(Bundle.class, /* ... */ );
```
Both return a generic Fhir POCO (in this case a Bundle).


### Extensions and References
The following snippet illustrates how to extract the author extension value from the payload component of the message and handle the reference. The extension value is extracted using `getValue()`. In this case we extract a resource reference which points to a resource in the bundle.
```java
 communication.getPayload().stream().forEach(payload -> {
      var valueReference = payload.getExtensionByUrl(AUTHOR_EXTENSION_URL).getValue();

      // The IG says that `valueReference` is of type `Reference`, and since we at this point have already validated the bundle, the cast will be safe to make
      // Futhermore the IG says that the reference SHALL resolve within the bundle. Hence it is safe to extract the resource and cast that as Practitioner
      var practitioner = (Practitioner) ((Reference) valueReference).getResource();

      System.out.println(
          "Author: " + practitioner.getName().get(0).getNameAsSingleString());
    });
```

The following snippet illustrates how to add an extension and value to and existing eleement

```java
payload.addExtension(DATETIME_EXTENSION_URL, new DateTimeType(Timestamp.valueOf((date))));
```

## Serializing to FHIR
The serializer used is the standard serializers from the HL7 HAPI FHIR libraries. The HL7 HAPI FHIR libraries contains separate serializers for Json and Xml that are used as follows:
```java
var json = FhirContext.forR4().newJsonParser().encodeResourceToString(messageBundle);
```
```java
var xml = FhirContext.forR4().newXmlParser().encodeResourceToString(messageBundle);
```

## Validating FHIR
HAPI FHIR supports validating incoming/outgoing/data-at-rest FHIR resources in multiple ways - see https://hapifhir.io/hapi-fhir/docs/validation/validation_support_modules.html for more details. The snippet below uses the `NpmPackageValidationSupport` which can be used without any database being present. Basically, the required IG's which are published in the NPM format which are then parsed and loaded into memory for quick evaluation. 

```java
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
```

## External validation
Another way to validate is to use the official validator (HAPI FHIR uses the same under the hood), which can be downloaded [here](https://github.com/hapifhir/org.hl7.fhir.core/releases/latest/download/validator_cli.jar)

Example using it:

```
java -jar validator_cli.jar file.json -ig https://build.fhir.org/ig/hl7dk/dk-medcom/package.tgz
```

