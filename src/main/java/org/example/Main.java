package org.example;

import soot.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.HashMutableDirectedGraph;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class CallGraphExtractor {

    public static void main(String[] args) throws IOException {
        // Input directory for Spring project build output
        String currentPath = String.format("%s\\out\\production\\resources", Paths.get("").toAbsolutePath());
        String inputPath = String.format("%s\\input\\main", currentPath);
        String outputPath = String.format("%s\\output", currentPath);

        System.out.println("Input Directory: " + inputPath);
        // Output DOT file for visualization
        String outputFile = outputPath + "\\dependency_graph.dot";

        // Initialize Soot
        G.reset();
        Options.v().set_prepend_classpath(true);
        Options.v().set_process_dir(Arrays.asList(inputPath));
        Options.v().set_soot_classpath(inputPath);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_output_format(Options.output_format_none);
        Options.v().set_whole_program(true);

        Scene.v().loadNecessaryClasses();
        System.out.println("Loaded Classes: " + Scene.v().getClasses().size());
//        for (SootClass sootClass : Scene.v().getClasses()) {
//            System.out.println("Class: " + sootClass.getName());
//        }


        // Entry Point 설정
        List<SootMethod> entryPoints = new ArrayList<>();
        for (SootClass sootClass : Scene.v().getClasses()) {
            String className = sootClass.getName().replace("/", ".");
            System.out.println("Class Name: " + className);

            if (className.startsWith("SNUBH")) { // 수정된 조건
                System.out.println("This class is included: " + className);
                for (SootMethod method : sootClass.getMethods()) {
                    System.out.println("\t\t this method is " + method.toString() + ", is concrete : " + method.isConcrete());
                    entryPoints.add(method);
                }
            }
        }
        Scene.v().setEntryPoints(entryPoints);

        System.out.println("Entry Points Set: " + entryPoints.size());
        // CallGraph 생성
        PackManager.v().runPacks();
        CallGraph callGraph = Scene.v().getCallGraph();

        // 종속성 체크 및 출력
        System.out.println("\nMethod Dependencies:");
        for (Edge edge : callGraph) {
            SootMethod src = edge.src();
            SootMethod tgt = edge.tgt();

            if (src.getDeclaringClass().getName().startsWith("SNUBH") &&
                    tgt.getDeclaringClass().getName().startsWith("SNUBH")) {
                System.out.println(src + " -> " + tgt);
            }
        }

        // DOT 파일 생성
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write("digraph G {\n");
            for (Edge edge : callGraph) {
                SootMethod src = edge.src();
                SootMethod tgt = edge.tgt();

                if (src.getDeclaringClass().getName().startsWith("SNUBH") &&
                        tgt.getDeclaringClass().getName().startsWith("SNUBH")) {
                    writer.write("\"" + src + "\" -> \"" + tgt + "\";\n");
                }
            }
            writer.write("}\n");
            System.out.println("DOT file created at: " + outputFile);
        }
    }


}