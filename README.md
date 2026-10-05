# Cucumber Step Documentation Generator

**Turn your Cucumber step definitions into searchable, tester-friendly documentation.**

[![Java 17+](https://img.shields.io/badge/Java-17%2B-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/build-Maven-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![Cucumber](https://img.shields.io/badge/Cucumber-7%20%7C%208-23D96C?logo=cucumber&logoColor=white)](https://cucumber.io/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

The Maven plugin scans compiled Cucumber step definitions and writes a searchable HTML report with a companion stylesheet. Testers can search expressions, descriptions, classes, and methods, then filter by `Given`, `When`, `Then`, `And`, or `But`.

<img src="cucumber-doc-example.PNG" alt="Example of the generated searchable Cucumber step documentation" width="960">

## What you get

- An HTML report with a companion CSS file; keep both files together when sharing the report.
- Searchable step expressions and descriptions.
- Filters for Cucumber step keywords.
- The step definition class and method shown with each step.
- Optional human-written descriptions with `@StepDescription`.
- Optional input field documentation, including whether each field is required.

## Modules

| Module | Purpose |
| --- | --- |
| `cucumber-steps-docs-core` | Step model, version-neutral annotation scanner, and `@StepDescription` |
| `cucumber-steps-docs-plugin` | Maven goal that generates the HTML documentation |

## Quick start

Build and install the snapshot artifacts locally:

```bash
mvn install
```

Add the core module to the consuming project so its step definitions can compile with `@StepDescription`:

```xml
<dependency>
    <groupId>io.github.dann634</groupId>
    <artifactId>cucumber-steps-docs-core</artifactId>
    <version>1.0.6</version>
</dependency>
```

Configure the Maven plugin in that project. It runs during `process-test-classes` and writes the report to the consuming project's `target` directory by default.

```xml
<build>
    <plugins>
        <plugin>
            <groupId>io.github.dann634</groupId>
            <artifactId>cucumber-steps-docs-plugin</artifactId>
            <version>1.0.6</version>
            <executions>
                <execution>
                    <goals>
                        <goal>generate</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

Run the lifecycle through `process-test-classes` (or a later phase):

```bash
mvn test
```

The generated report is `target/cucumber-step-documentation.html`, with its stylesheet at
`target/cucumber-step-documentation.css`.

### Publish the report with Jenkins

The report links to its stylesheet by a relative filename. Publish the directory containing both files with the HTML Publisher plugin:

```groovy
publishHTML(target: [
    allowMissing: false,
    alwaysLinkToLastBuild: true,
    keepAll: true,
    reportDir: 'target',
    reportFiles: 'cucumber-step-documentation.html',
    includes: 'cucumber-step-documentation.*',
    reportName: 'Cucumber Step Documentation'
])
```

The `includes` pattern archives both the HTML and CSS files. If you change the report filename, update the pattern to match both files.

## Add descriptions to steps

Import the annotation and add it to a Cucumber step method. The description appears beneath the expression in the generated report.

```java
import com.jackson.cucumberdocs.StepDescription;
import io.cucumber.java.en.Given;

@Given("a customer exists")
@StepDescription("Creates a customer that can be used by the scenario")
public void aCustomerExists() {
    // Set up the customer
}
```

Descriptions are optional. The plugin reads them from the consuming project's test classpath when generating the report.

### Document Gherkin input values

Set `input` to a class literal to list that class's instance fields below the step description. Fields annotated with `NotNull` or `NotBlank` are marked **Required**; other fields are marked **Optional**. The values are written directly into the generated HTML and styled with the companion CSS file.

```java
import com.jackson.cucumberdocs.StepDescription;
import io.cucumber.java.en.When;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

class CustomerInput {
    @NotBlank
    private String name;

    @NotNull
    private Integer age;

    private String note;
}

@When("a customer is registered")
@StepDescription(value = "Registers a customer", input = CustomerInput.class)
public void registerCustomer() {
    // Register the customer
}
```

### Document step argument values

For string arguments in a step expression, add one `arguments` entry per value, in the same order as the `{string}` placeholders. Each key replaces the corresponding placeholder in the generated documentation, and enum types show their constants beneath that key. Other class types are shown by name. The Cucumber expression and Gherkin syntax are unchanged. Data-table fields continue to use `input` as described above.

```java
enum CustomerStatus {
    NEW, ACTIVE, SUSPENDED
}

enum ContactPreference {
    EMAIL, PHONE, POST
}

@When("a customer is updated")
@StepDescription(value = "Updates a customer's status and contact preference",
        arguments = {
                @StepDescription.Argument(key = "status", type = CustomerStatus.class),
                @StepDescription.Argument(key = "contactPreference", type = ContactPreference.class)
        })
public void updateCustomer() {
    // Update the customer
}
```

## Configuration

The plugin supports these properties:

| Property | Default | Purpose |
| --- | --- | --- |
| `cucumber.step.docs.skip` | `false` | Skip report generation |
| `cucumber.step.docs.failOnError` | `true` | Fail the Maven build if generation fails |

To change the output path, configure the plugin's `outputFile` parameter:

```xml
<configuration>
    <outputFile>${project.build.directory}/reports/steps.html</outputFile>
</configuration>
```

## Requirements

- Maven must run on JDK 17 or newer.
- Cucumber Java step definitions must be on the project's test classpath. Cucumber 7 and 8 are supported.
- The consuming project can target an older Java release if Maven runs on JDK 17+ and that project's Cucumber version supports the target runtime.

The scanner inspects compiled main and test classes, plus dependency JARs on the test classpath. It reads bytecode annotations directly, so it does not need to load or control the consuming project's Cucumber version.

## License

This project is licensed under the [MIT License](LICENSE).
