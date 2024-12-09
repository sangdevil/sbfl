package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        String currentPath = String.format("%s", Paths.get("").toAbsolutePath());

        // GraphMutator 사용
        GraphMutator mutator = new GraphMutator();
        try {
            for (int i = 1; i <= 100; i++) {
                String inputPath = currentPath + "\\sootOutput\\Math" + i + "_dependency_graph.dot";
                String outputPath = currentPath + "\\sootOutputNew\\Math" + i + "_dependency_graph.dot";
                String packagePrefixToFilter = "org.apache.commons.math3";

                // 파일 존재 여부 확인
                if (Files.exists(Paths.get(inputPath))) {
                    System.out.println("Processing: " + inputPath);
                    mutator.filterDependencies(inputPath, outputPath, packagePrefixToFilter);
                } else {
                    System.out.println("Skipping: " + inputPath + " (File not found)");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
