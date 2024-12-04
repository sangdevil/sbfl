package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MathCallGraphExtractor {

    public static void main(String[] args) throws IOException {
        String currentPath = String.format("%s", Paths.get("").toAbsolutePath());
        for (int i = 1; i <= 100; i++) {
            String inputPath = currentPath + "\\sootInput\\math" + i;
            String outputPath = currentPath + "\\sootOutput\\math" + i + "_dependency_graph.dot";
            String packagePrefixToRemove = "classes.";

            if (Files.exists(Paths.get(inputPath))) {
                System.out.println("Processing math" + i);
                CallGraphExtractor.extractCallGraph(inputPath, outputPath, packagePrefixToRemove);
            } else {
                System.out.println("Input path does not exist for lang" + i + ": " + inputPath);
            }
        }
    }
}
