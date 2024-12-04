package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ChartCallGraphExtractor {

    public static void main(String[] args) throws IOException {
        String currentPath = String.format("%s", Paths.get("").toAbsolutePath());
        for (int i = 1; i <= 100; i++) {
            String inputPath = currentPath + "\\sootInput\\chart" + i + "\\build";
            String outputPath = currentPath + "\\sootOutput\\chart" + i + "_dependency_graph.dot";
            String packagePrefixToRemove = "build.";

            if (Files.exists(Paths.get(inputPath))) {
                System.out.println("Processing chart" + i);
                CallGraphExtractor.extractCallGraph(inputPath, outputPath, packagePrefixToRemove);
            } else {
                System.out.println("Input path does not exist for chart" + i + ": " + inputPath);
            }
        }
    }
}
