package org.example;

import soot.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.io.File;

public class CallGraphExtractor {
    private static String getJarFilesClasspath(String rootPath) {
        StringBuilder classpath = new StringBuilder();
        File rootDir = new File(rootPath);

        // Check if the path is a directory
        if (rootDir.isDirectory()) {
            // List all files in the directory
            File[] files = rootDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (files != null) {
                for (File jarFile : files) {
                    if (classpath.length() > 0) {
                        classpath.append(File.pathSeparator); // Add platform-specific path separator
                    }
                    classpath.append(jarFile.getAbsolutePath());
                }
            }
        }

        return classpath.toString();
    }


    public static void extractCallGraph(String inputRootPath, String outputFileName, String packagePrefixToRemove) throws IOException {
        System.out.println("Input Directory: " + inputRootPath);

        // .class 파일이 있는 디렉토리들을 재귀적으로 찾습니다.
        List<String> directoriesWithClassFiles = new ArrayList<>();
        Files.walkFileTree(Paths.get(inputRootPath), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".class")) {
                    directoriesWithClassFiles.add(file.getParent().toString());
                }
                return FileVisitResult.CONTINUE;
            }
        });

        // 중복된 디렉토리를 제거합니다.
        HashSet<String> uniqueDirectories = new HashSet<>(directoriesWithClassFiles);
//        uniqueDirectories.add("C:\\Users\\yyson\\OneDrive\\바탕 화면\\2024 가을학기\\CS454\\Project\\새 폴더\\sbfl\\sootInput\\chart1\\build\\org\\jfree\\chart\\annotations");
        // 디렉토리를 패키지 구조로 매핑합니다.
        HashSet<String> validPackages = new HashSet<>();
        for (String dir : uniqueDirectories) {
//            System.out.println("uniqueDirectories : " +  dir);
            // 파일 경로를 패키지 이름으로 변환합니다.
            String packagePath = dir.replace(inputRootPath, "").replace("\\", ".").replace("/", ".");
//            System.out.println("packagePath " + packagePath);
            if (packagePath.startsWith(".")) packagePath = packagePath.substring(1);
            // 지정된 접두사를 제거합니다.
            if (packagePrefixToRemove != null && packagePrefixToRemove.length() > 0) {
                if (packagePath.startsWith(packagePrefixToRemove)) {
                    packagePath = packagePath.substring(packagePrefixToRemove.length());
                    if (packagePath.startsWith(".")) packagePath = packagePath.substring(1);
                }
            }
//            System.out.println("packagePath with prefix removed " + packagePath);

            validPackages.add(packagePath);
        }

        System.out.println("Valid Packages:");
        for (String pkg : validPackages) {
            System.out.println(pkg);
        }

        // Soot 초기화
        G.reset();
        Options.v().set_prepend_classpath(true);
        Options.v().set_soot_classpath(getJarFilesClasspath(inputRootPath));
        Options.v().set_process_dir(new ArrayList<>(uniqueDirectories)); // 클래스 파일이 있는 모든 디렉토리를 설정합니다.
        Options.v().set_allow_phantom_refs(false);
        Options.v().set_output_format(Options.output_format_none);
        Options.v().set_whole_program(true);
        System.out.println("Soot Classpath: " + Options.v().soot_classpath());

        // 클래스 로드
        Scene.v().loadNecessaryClasses();
        System.out.println("Loaded Classes: " + Scene.v().getClasses().size());

        List<SootMethod> entryPoints = new ArrayList<>();
        Set<SootMethod> uniqueEntryPoints = new HashSet<>();

        for (SootClass sootClass : Scene.v().getClasses()) {
            String classPackage = sootClass.getPackageName();
//            System.out.println("Class Name: " + sootClass.getName() + " (Package: " + classPackage + ")");

            for (SootMethod method : sootClass.getMethods()) {
                if (method.isConcrete()) {
//                    System.out.println("\tAdding Method: " + method.getSignature());
                    uniqueEntryPoints.add(method);
                } else {
//                    System.out.println("\tSkipping Non-Concrete Method: " + method.getSignature());
                }
            }
        }

        entryPoints.addAll(uniqueEntryPoints);
        Scene.v().setEntryPoints(entryPoints);

        System.out.println("Entry Points Set: " + entryPoints.size());

        // CallGraph 생성
        PackManager.v().runPacks();
        CallGraph callGraph = Scene.v().getCallGraph();

        // 의존성 추출
        HashSet<String> uniqueDependencies = new HashSet<>();
//        System.out.println("\nMethod Dependencies:");
        for (Edge edge : callGraph) {
            SootMethod src = edge.src();
            SootMethod tgt = edge.tgt();

            boolean srcValid = validPackages.stream().anyMatch(src.getDeclaringClass().getPackageName()::contains);
            boolean tgtValid = validPackages.stream().anyMatch(tgt.getDeclaringClass().getPackageName()::contains);

            String dependency = "\"" + src + "\" -> \"" + tgt + "\"";
            if (srcValid || tgtValid) {
                uniqueDependencies.add(dependency);
            }
        }

        // DOT 파일 생성
        try (FileWriter writer = new FileWriter(outputFileName)) {
            writer.write("digraph G {\n");
            for (String dependency : uniqueDependencies) {
                writer.write(dependency + ";\n");
            }
            writer.write("}\n");
            System.out.println("DOT file created at: " + outputFileName);
        }
    }
}