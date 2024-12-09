    package org.example;

    import java.io.*;
    import java.nio.file.Files;
    import java.nio.file.Path;
    import java.nio.file.Paths;
    import java.util.HashSet;
    import java.util.Set;

    public class GraphMutator {
        /**
         * 주어진 inputPath에 있는 .dot 파일을 읽어서
         * packagePrefix로 시작하는 src -> tgt dependency만 필터링하여
         * 중복 없이 outputPath에 dot 형식으로 출력한다.
         *
         * @param inputPath      입력 dot 파일 경로
         * @param outputPath     출력 dot 파일 경로
         * @param packagePrefix  필터링할 패키지 접두사 (예: org.jfree.)
         */
        public void filterDependencies(String inputPath, String outputPath, String packagePrefix) throws IOException {
            Set<String> edges = new HashSet<>();
            boolean inGraph = false;

            // 출력 경로의 디렉터리 생성
            ensureOutputDirectoryExists(outputPath);

            try (BufferedReader br = new BufferedReader(new FileReader(inputPath));
                 BufferedWriter bw = new BufferedWriter(new FileWriter(outputPath))) {

                String line;
                while ((line = br.readLine()) != null) {
                    String trimmed = line.trim();

                    // "digraph G {" 부분을 찾아서 출력 시작
                    if (trimmed.startsWith("digraph")) {
                        bw.write(line);
                        bw.newLine();
                        inGraph = true;
                        continue;
                    }

                    // 그래프 종료 부분이 있으면 그대로 출력
                    if (trimmed.equals("}")) {
                        inGraph = false;
                        // 이제 모아둔 edges를 출력
                        for (String edge : edges) {
                            bw.write(edge);
                            bw.newLine();
                        }
                        bw.write("}");
                        bw.newLine();
                        continue;
                    }

                    // 그래프 안의 라인 처리
                    if (inGraph && trimmed.contains("->")) {
                        // 예: "<java.text.Normalizer$Form: ...>" -> "<org.jfree.chart.util.StrokeList: ...>";
                        // 형태이므로 "->"로 스플릿
                        String[] parts = trimmed.split("->");
                        if (parts.length == 2) {
                            String src = parts[0].trim();
                            String tgt = parts[1].trim();
                            String checkSrc = src.substring(2, src.length() - 2);
                            String checkTgt = tgt.substring(2, tgt.length() - 2);
                            // 마지막에 세미콜론 제거
                            if (tgt.endsWith(";")) {
                                tgt = tgt.substring(0, tgt.length() - 1).trim();
                            }
    //                        System.out.println(String.format("src : %s, tgt : %s", src, tgt));
                            // src, tgt 모두 packagePrefix로 시작하는지 체크
                            if (containsPackagePrefix(checkSrc, packagePrefix) && containsPackagePrefix(checkTgt, packagePrefix)) {
                                String edgeLine = "    " + src + " -> " + tgt + ";";
                                edges.add(edgeLine);
                            }
                        }
                    }
                }
            }
        }

        /**
         * 출력 경로의 디렉터리가 없으면 생성한다.
         *
         * @param outputPath 출력 파일 경로
         */
        private void ensureOutputDirectoryExists(String outputPath) throws IOException {
            Path outputDir = Paths.get(outputPath).getParent(); // 파일의 디렉터리 경로 추출
            if (outputDir != null && !Files.exists(outputDir)) {
                Files.createDirectories(outputDir); // 디렉터리가 없으면 생성
                System.out.println("Created directory: " + outputDir.toAbsolutePath());
            }
        }

        /**
         * 주어진 노드 문자열이 packagePrefix를 포함하는지 확인한다.
         * 노드 문자열은 "<...>" 형태이므로, 내부 내용을 파싱한다.
         */
        private boolean containsPackagePrefix(String node, String packagePrefix) {
            // "<org.jfree.chart.util.StrokeList: java.lang.Object clone()>" 형태에서
            // <>를 제거
            String content = node;
            if (content.startsWith("<") && content.endsWith(">")) {
                content = content.substring(1, content.length() - 1);
            }
            // packagePrefix로 시작하는 클래스 이름이 있는지 확인
            // 클래스명은 첫 번째 콜론(:) 이전까지가 클래스명일 가능성이 큼.
            int colonIndex = content.indexOf(":");
            String className = (colonIndex > 0) ? content.substring(0, colonIndex).trim() : content.trim();

            return className.startsWith(packagePrefix);
        }
    }
