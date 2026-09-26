package com.jackson.cucumberdocs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Creates a standalone, searchable HTML reference for discovered Cucumber steps.
 */
public class HtmlDocumentationGenerator {

    /** Creates a generator for standalone HTML step documentation. */
    public HtmlDocumentationGenerator() {
    }

    /**
     * Writes the step documentation to a UTF-8-encoded HTML file.
     *
     * @param steps step definitions to include in the report
     * @param outputFile destination path for the generated HTML
     * @throws IOException if the output directory cannot be created or the file cannot be written
     */
    public void generate(
            List<CucumberStep> steps,
            Path outputFile
    ) throws IOException {

        Files.createDirectories(outputFile.getParent());

        String html = buildHtml(steps);

        Files.writeString(
                outputFile,
                html,
                StandardCharsets.UTF_8
        );
    }

    private String buildHtml(List<CucumberStep> steps) {

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

                    <style>
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
                    </style>
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
                            <input id="step-search" type="search" placeholder="Search steps, descriptions, or classes" aria-label="Search steps">
                            <select id="keyword-filter" aria-label="Filter by step keyword">
                                <option value="">All step keywords</option><option>Given</option><option>When</option><option>Then</option><option>And</option><option>But</option>
                            </select>
                        </div>
                        <p id="result-count" aria-live="polite"></p>
                """);

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
                        .append(escapeHtml((step.expression() + " " + step.description() + " " + step.className() + " " + step.methodName()).toLowerCase()))
                        .append("\">");

                html.append("<span class=\"badge\">")
                        .append(escapeHtml(step.keyword()))
                        .append("</span>");

                html.append("<span class=\"expression\">")
                        .append(escapeHtml(step.expression()))
                        .append("</span>");

                if (!step.description().isBlank()) {
                    html.append("<div class=\"description\">")
                            .append(escapeHtml(step.description()))
                            .append("</div>");
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
