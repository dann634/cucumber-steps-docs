package com.jackson.cucumberdocs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Creates a searchable HTML reference and companion stylesheet for discovered Cucumber steps.
 */
public class HtmlDocumentationGenerator {

    private static final String STYLESHEET = """
            * {
                box-sizing: border-box;
            }

            body {
                margin: 0;
                padding: 0;
                font-family:
                    -apple-system,
                    BlinkMacSystemFont,
                    "Segoe UI",
                    sans-serif;
                background: #f5f6f8;
                color: #222;
            }

            header {
                background: #24292f;
                color: white;
                padding: 32px 40px;
            }

            header h1 {
                margin: 0;
                font-size: 28px;
            }

            header p {
                margin: 8px 0 0;
                color: #c9d1d9;
            }

            main {
                max-width: 1200px;
                margin: 40px auto;
                padding: 0 24px;
            }

            .section {
                margin-bottom: 40px;
            }

            .section h2 {
                margin-bottom: 16px;
                font-size: 22px;
            }

            .step {
                background: white;
                border-radius: 8px;
                padding: 20px;
                margin-bottom: 12px;
                box-shadow:
                    0 1px 3px rgba(0, 0, 0, 0.08);
            }

            .expression {
                font-family: monospace;
                font-size: 16px;
                font-weight: 600;
            }

            .description {
                margin-top: 10px;
                color: #555;
                line-height: 1.5;
            }

            .implementation {
                margin-top: 12px;
                font-family: monospace;
                font-size: 13px;
                color: #777;
            }

            .input-parameters {
                margin-top: 18px;
                padding-top: 16px;
                border-top: 1px solid #eaeef2;
            }

            .input-parameters h3 {
                margin: 0 0 4px;
                font-size: 14px;
                color: #344054;
            }

            .input-class {
                margin: 0 0 12px;
                color: #777;
                font-family: monospace;
                font-size: 12px;
                overflow-wrap: anywhere;
            }

            .input-list {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
                gap: 8px;
                margin: 0;
                padding: 0;
                list-style: none;
            }

            .input-item {
                display: flex;
                align-items: center;
                justify-content: space-between;
                gap: 12px;
                min-width: 0;
                padding: 10px 12px;
                border: 1px solid #eaeef2;
                border-radius: 7px;
                background: #f8fafc;
            }

            .input-name {
                display: block;
                color: #24292f;
                font-family: monospace;
                font-size: 13px;
                font-weight: 600;
                overflow-wrap: anywhere;
            }

            .input-type {
                display: block;
                margin-top: 3px;
                color: #667085;
                font-family: monospace;
                font-size: 11px;
                overflow-wrap: anywhere;
            }

            .input-requirement {
                flex: 0 0 auto;
                padding: 4px 7px;
                border-radius: 999px;
                font-size: 11px;
                font-weight: 600;
            }

            .input-requirement.required {
                color: #9a3412;
                background: #ffedd5;
            }

            .input-requirement.optional {
                color: #475467;
                background: #eaecf0;
            }

            .input-empty {
                margin: 0;
                color: #777;
                font-size: 13px;
                font-style: italic;
            }

            .step-arguments {
                margin-top: 16px;
            }

            .step-arguments h3 {
                margin: 0 0 10px;
                font-size: 14px;
                color: #344054;
            }

            .argument-list {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
                gap: 8px;
                margin: 0;
                padding: 0;
                list-style: none;
            }

            .argument-item {
                min-width: 0;
                padding: 11px 12px;
                border: 1px solid #dbeafe;
                border-radius: 7px;
                background: #f8fbff;
            }

            .argument-heading {
                display: flex;
                flex-wrap: wrap;
                align-items: baseline;
                justify-content: space-between;
                gap: 4px 10px;
                margin-bottom: 8px;
            }

            .argument-key {
                color: #1d4ed8;
                font-family: monospace;
                font-size: 13px;
                font-weight: 700;
            }

            .argument-type {
                color: #667085;
                font-family: monospace;
                font-size: 11px;
                overflow-wrap: anywhere;
            }

            .argument-values {
                display: flex;
                flex-wrap: wrap;
                gap: 6px;
                margin: 0;
                padding: 0;
                list-style: none;
            }

            .argument-option {
                display: inline-block;
                padding: 5px 9px;
                border: 1px solid #dbeafe;
                border-radius: 999px;
                background: #eff6ff;
                color: #1d4ed8;
                font-family: monospace;
                font-size: 12px;
            }

            .argument-empty {
                margin: 0;
                color: #777;
                font-size: 13px;
                font-style: italic;
            }

            .badge {
                display: inline-block;
                padding: 3px 8px;
                margin-right: 8px;
                border-radius: 4px;
                background: #eaeef2;
                font-family: monospace;
                font-size: 12px;
            }

            .empty {
                color: #777;
                font-style: italic;
            }

            .controls { display: flex; gap: 12px; flex-wrap: wrap; margin-bottom: 18px; }
            .controls input, .controls select { min-height: 42px; padding: 8px 12px; border: 1px solid #c9d1d9; border-radius: 6px; background-color: white; font: inherit; }
            .controls select { padding-right: 36px; }
            .controls input { flex: 1; min-width: 220px; }
            .step-class { color: #0969da; background: #ddf4ff; }
            .step[hidden], .section[hidden] { display: none; }
            """;

    /** Creates a generator for HTML step documentation and its stylesheet. */
    public HtmlDocumentationGenerator() {
    }

    /**
     * Writes the step documentation and its companion stylesheet as UTF-8 files.
     *
     * @param steps step definitions to include in the report
     * @param outputFile destination path for the generated HTML
     * @throws IOException if the output directory cannot be created or either file cannot be written
     */
    public void generate(
            List<CucumberStep> steps,
            Path outputFile
    ) throws IOException {

        Files.createDirectories(outputFile.getParent());

        Path stylesheetFile = stylesheetPathFor(outputFile);
        String html = buildHtml(steps, stylesheetFile.getFileName().toString());

        Files.writeString(
                outputFile,
                html,
                StandardCharsets.UTF_8
        );

        Files.writeString(
                stylesheetFile,
                STYLESHEET,
                StandardCharsets.UTF_8
        );
    }

    private Path stylesheetPathFor(Path outputFile) {
        String fileName = outputFile.getFileName().toString();
        int extensionIndex = fileName.lastIndexOf('.');
        String stylesheetName = extensionIndex > 0
                ? fileName.substring(0, extensionIndex) + ".css"
                : fileName + ".css";
        return outputFile.resolveSibling(stylesheetName);
    }

    private String buildHtml(List<CucumberStep> steps, String stylesheetName) {

        Map<String, List<CucumberStep>> grouped =
                steps.stream()
                        .collect(Collectors.groupingBy(
                                CucumberStep::keyword,
                                java.util.LinkedHashMap::new,
                                Collectors.toList()
                        ));

        StringBuilder html = new StringBuilder();

        html.append("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport"
                          content="width=device-width, initial-scale=1.0">
                    <title>Cucumber Step Documentation</title>
                    <link rel="stylesheet" href="%s">
                </head>

                <body>
                    <header>
                        <h1>Cucumber Step Documentation</h1>
                        <p>
                            Automatically generated from Cucumber step definitions.
                        </p>
                    </header>

                    <main>
                        <div class="controls" role="search">
                            <input id="step-search" type="search" placeholder="Search steps, descriptions, and input values" aria-label="Search steps">
                            <select id="keyword-filter" aria-label="Filter by step keyword">
                                <option value="">All step keywords</option><option>Given</option><option>When</option><option>Then</option><option>And</option><option>But</option>
                            </select>
                        </div>
                        <p id="result-count" aria-live="polite"></p>
                """.formatted(escapeHtml(stylesheetName)));

        String[] order = {
                "Given",
                "When",
                "Then",
                "And",
                "But"
        };

        for (String keyword : order) {

            List<CucumberStep> sectionSteps =
                    grouped.getOrDefault(keyword, List.of());

            if (sectionSteps.isEmpty()) {
                continue;
            }

            html.append("<section class=\"section\">");
            html.append("<h2>")
                    .append(escapeHtml(keyword))
                    .append("</h2>");

            for (CucumberStep step : sectionSteps) {

                html.append("<div class=\"step\" data-keyword=\"")
                        .append(escapeHtml(step.keyword())).append("\" data-search=\"")
                        .append(escapeHtml((step.expression() + " " + step.documentedExpression() + " " + step.description() + " " + step.className() + " " + step.methodName()
                                + " " + step.inputClassName() + " " + step.inputs().stream()
                                .map(input -> input.name() + " " + input.type()).collect(Collectors.joining(" "))
                                + " " + step.arguments().stream().map(argument -> argument.key() + " "
                                + argument.className() + " " + String.join(" ", argument.values()))
                                .collect(Collectors.joining(" "))).toLowerCase()))
                        .append("\">");

                html.append("<span class=\"badge\">")
                        .append(escapeHtml(step.keyword()))
                        .append("</span>");

                html.append("<span class=\"expression\">")
                        .append(escapeHtml(step.documentedExpression()))
                        .append("</span>");

                if (!step.description().isBlank()) {
                    html.append("<div class=\"description\">")
                            .append(escapeHtml(step.description()))
                            .append("</div>");
                }

                if (!step.inputClassName().isBlank()) {
                    html.append("<div class=\"input-parameters\">")
                            .append("<h3>Gherkin input values</h3>")
                            .append("<p class=\"input-class\">")
                            .append(escapeHtml(step.inputClassName()))
                            .append("</p>");
                    if (step.inputs().isEmpty()) {
                        html.append("<p class=\"input-empty\">No fields were found on this input class.</p>");
                    } else {
                        html.append("<ul class=\"input-list\">");
                        step.inputs().forEach(input -> {
                            String requirement = input.required() ? "required" : "optional";
                            html.append("<li class=\"input-item\"><span><span class=\"input-name\">")
                                    .append(escapeHtml(input.name()))
                                    .append("</span><span class=\"input-type\">")
                                    .append(escapeHtml(input.type()))
                                    .append("</span></span><span class=\"input-requirement ")
                                    .append(requirement)
                                    .append("\">")
                                    .append(input.required() ? "Required" : "Optional")
                                    .append("</span></li>");
                        });
                        html.append("</ul>");
                    }
                    html.append("</div>");
                }

                if (!step.arguments().isEmpty()) {
                    html.append("<div class=\"step-arguments\"><h3>Step values</h3><ul class=\"argument-list\">");
                    step.arguments().forEach(argument -> {
                        html.append("<li class=\"argument-item\"><div class=\"argument-heading\"><span class=\"argument-key\">{")
                                .append(escapeHtml(argument.key()))
                                .append("}</span><span class=\"argument-type\">")
                                .append(escapeHtml(argument.className()))
                                .append("</span></div>");
                        if (argument.enumType() && !argument.values().isEmpty()) {
                            html.append("<ul class=\"argument-values\">");
                            argument.values().forEach(value -> html.append("<li><code class=\"argument-option\">")
                                    .append(escapeHtml(value)).append("</code></li>"));
                            html.append("</ul>");
                        } else if (argument.enumType()) {
                            html.append("<p class=\"argument-empty\">No enum constants were found on this class.</p>");
                        }
                        html.append("</li>");
                    });
                    html.append("</ul></div>");
                }

                html.append("<div class=\"implementation\">")
                        .append("<span class=\"badge step-class\" title=\"Step definition class\">")
                        .append(escapeHtml(step.className())).append("</span><span>")
                        .append(escapeHtml(step.methodName()))
                        .append("()")
                        .append("</span>")
                        .append("</div>");

                html.append("</div>");
            }

            html.append("</section>");
        }

        if (steps.isEmpty()) {
            html.append("""
                    <p class="empty">
                        No Cucumber step definitions were found.
                    </p>
                    """);
        }

        html.append("""
                    <script>
                        const search = document.getElementById('step-search');
                        const filter = document.getElementById('keyword-filter');
                        const cards = [...document.querySelectorAll('.step')];
                        const count = document.getElementById('result-count');
                        function update() {
                            const query = search.value.trim().toLowerCase();
                            let visible = 0;
                            cards.forEach(step => {
                                const show = (!filter.value || step.dataset.keyword === filter.value) &&
                                    (!query || step.dataset.search.includes(query));
                                step.hidden = !show;
                                if (show) visible++;
                            });
                            document.querySelectorAll('.section').forEach(section => {
                                section.hidden = !section.querySelector('.step:not([hidden])');
                            });
                            count.textContent = `Showing ${visible} of ${cards.length} steps`;
                        }
                        search.addEventListener('input', update);
                        filter.addEventListener('change', update);
                        update();
                    </script>
                    </main>
                </body>
                </html>
                """);

        return html.toString();
    }

    private String escapeHtml(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
