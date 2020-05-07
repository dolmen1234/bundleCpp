package fr.dgigon.codg.bundler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class Bundler {

    static final List<String> exludeDirNames = new ArrayList<>();
    static {
        exludeDirNames.add(".settings");
        exludeDirNames.add("test");
        exludeDirNames.add("debug");
        exludeDirNames.add("sync");
    };
    
    private List<String> pragmas = new ArrayList<>();
    private List<String> defines = new ArrayList<>();
    private List<String> includes = new ArrayList<>();
    private List<String> headers = new ArrayList<>();
    private List<String> implem = new ArrayList<>();
    private static int bundleCount = 0;

    void compile(File cppFile) throws IOException, InterruptedException {
        String oFile = cppFile.getAbsolutePath().replaceAll("cpp", "o");
        String compileLine = "g++ -std=c++0x -O3 -march=native -mtune=native -fopt-info-vec-all=vectorisation.log -g3 -Wall -c -fmessage-length=0 -Wpadded "
                + "-o \"" + oFile + "\" \"" + cppFile + "\"";
        System.err.println(compileLine);

        Runtime.getRuntime().exec(compileLine).waitFor();
        String exeFile = cppFile.getAbsolutePath().replaceAll("cpp", "exe");

        String linkLine = "g++ -o \"" + exeFile + "\" \"" + oFile + "\"";
        System.err.println(linkLine);
        Runtime.getRuntime().exec(linkLine);
    }

    void checkDependances(Collection<SourceFile> files) {
        for (SourceFile file : files) {
            file.checkDependances();
        }
    }

    void bundle(File dir, File target, List<String> exludeDirNames, String mainFileName) throws IOException {
        bundleCount ++;
        System.err.println("start bundle " + bundleCount + " at "+new Date());
        if (!dir.exists()) {
            throw new IllegalArgumentException("existe pas " + dir.getAbsolutePath());
        }
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("pas un rep" + dir.getAbsolutePath());
        }

        List<SourceFile> allFiles = getAllFiles(dir, exludeDirNames, mainFileName);

        checkDependances(allFiles);
        int nbParsed = 0;
        while (nbParsed < allFiles.size()) {
            for (SourceFile file : allFiles) {
                if (!file.isParsed && file.canBeParsed()) {
                    parseFile(file.file);
                    file.isParsed = true;
                    nbParsed++;
                }
            }
        }
        write(target);
        System.err.println("end bundle " + bundleCount + " at "+new Date());
    }

    private void parseFile(File file) throws FileNotFoundException {

        List<Integer> excludes = getExcludes(file);

        // System.err.println("parse "+file.getAbsolutePath());
        Scanner in = new Scanner(file);

        boolean isHeader = file.getName().endsWith(".h");

        int index = 0;
        while (in.hasNextLine()) {
            String line = in.nextLine().trim();
            if (!excludes.contains(index)) {
                parseLine(line, isHeader);

            }
            index++;
        }
        in.close();
    }

    private List<Integer> getExcludes(File file) throws FileNotFoundException {
        Scanner in = new Scanner(file);

        List<Integer> excludes = new ArrayList<>();

        int i = 0;

        int ifdefIndex = -1;
        int defineIndex = -1;
        int endifIndex = -1;

        while (in.hasNextLine()) {
            String line = in.nextLine().trim();
            if (ifdefIndex == -1
                    && line.toLowerCase().startsWith("#ifndef " + file.getName().toLowerCase().replace('.', '_'))) {
                ifdefIndex = i;
            } else if (ifdefIndex != -1 && defineIndex == -1
                    && line.toLowerCase().startsWith("#define " + file.getName().toLowerCase().replace('.', '_'))) {
                defineIndex = i;
            } else if (ifdefIndex != -1 && line.toLowerCase().startsWith("#endif")) {
                endifIndex = i;
            }
            i++;
        }
        in.close();

        if (ifdefIndex != -1 && defineIndex != -1 && endifIndex != -1) {
            excludes.add(ifdefIndex);
            excludes.add(defineIndex);
            excludes.add(endifIndex);
        }
        return excludes;
    }

    private void parseLine(String line, boolean isHeader) {
        // System.err.println(line);
        if (line.isEmpty()) {
            return;
        }

        if (line.startsWith("//")) {
            return;
        }

        if (line.startsWith("#pragma")) {
            if (!pragmas.contains(line)) {
                pragmas.add(line);
            }
        } else if (line.startsWith("#define ")) {
            if (!defines.contains(line)) {
                defines.add(line);
            }
        } else if (line.startsWith("#include ")) {
            if (line.startsWith("#include <")) {
                if (!includes.contains(line)) {
                    includes.add(line);
                }
            }
        } else if (isHeader) {
            headers.add(line);
        } else {
            implem.add(line);
        }

    }

    public static String cleanLine(String line) {
        String concat = line;
        concat = concat.replace('\t', ' ').trim();
        String clean;
        do {
            clean = concat;
            concat = clean.replaceAll("  ", " ");
        } while (!concat.equals(clean));

        clean = concat;
        for (String str : new String[] { "{", "}", "=", "(", ")", ",", "-", "+", "/", ";", "<", ">", "?", ":", }) {
            clean = clean.replaceAll(" \\" + str, str).replaceAll("\\" + str + " ", str);
        }
        for (String str : new String[] { "*", "&" }) {
            clean = clean.replaceAll("\\" + str + " ", str);
        }
        return clean;
    }

    @SuppressWarnings("unchecked")
    private void write(File target) throws IOException {
        FileWriter writer = new FileWriter(target);

        writer.write("#pragma GCC optimize(\"-O3\")\n");
        writer.write("#pragma GCC optimize(\"inline\")\n");
        writer.write("#pragma GCC optimize(\"omit-frame-pointer\")\n");
        writer.write("#pragma GCC optimize(\"unroll-loops\")\n");
        writer.write("#pragma GCC optimize(\"unsafe-math-optimizations\")\n");
        writer.write("#pragma GCC optimize(\"no-trapping-math\")\n");
        writer.write("#pragma GCC option(\"arch=native\",\"tune=native\",\"no-zero-upper\")\n");
        writer.write("#pragma GCC target(\"avx\")\n");

        writer.write("#include <x86intrin.h>\n");
        writer.write("#include <bits/stdc++.h>\n");
        
        writer.write("#define inline __attribute__((always_inline))");
        
        // writer.write("#pragma GCC optimize(\"Ofast,inline,omit-frame-pointer,unroll-loops\")\n");
        for (Collection<String> lines : new Collection[] { pragmas, includes, defines }) {
            for (String line : lines) {
                writer.write(line);
                writer.write("\n");
            }
        }
        for (String line : headers) {
            writer.write(cleanLine(line));
            writer.write("\n");
        }
        for (String line : implem) {
            writer.write(cleanLine(line));
            writer.write("\n");
        }

        writeMain(writer);
        writer.close();
    }

    private void writeMain(FileWriter writer) throws IOException {
        writer.write("int main(int argc, char *argv[]) {\n");
//        writer.write("initEvalConf(argc, argv);\n");
        writer.write("player::Player player;\n");
        writer.write("PlayerStreamSystem in;\n");
        writer.write("player.play(in);\n");
        writer.write("}\n");
    }

    private List<SourceFile> getAllFiles(File dir, List<String> exludeDirNames, String mainFileName)
            throws FileNotFoundException {
        List<SourceFile> files = new ArrayList<>();

        for (File subDir : dir.listFiles()) {
            if (exludeDirNames.contains(subDir.getName().toLowerCase())) {
                continue;
            }
            if (subDir.listFiles() != null) {
                for (File subFile : subDir.listFiles()) {
                    if ((subFile.getName().endsWith(".h") || subFile.getName().endsWith(".cpp")
                            || subFile.getName().endsWith(".c")) && !subFile.getName().equals(mainFileName)) {
                        if (!subFile.getName().startsWith("MiniMax")) {
                            files.add(new SourceFile(subFile));
                        }
                    }
                }
            }

        }

        for (SourceFile file : files) {
            file.updateDependances(files);
        }

        return files;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length != 2) {
            System.err.println("Unexpected number of arguments " + args.length + ". Expected 2 : src dir, dest dir");
        }
        
        File src = new File(args[0]);
        System.err.println("Start watch "+src.getAbsolutePath());
        Watcher watcher = new Watcher(src);
        File target = new File(args[1]);

        while (true) {
            if (watcher.needBundle(exludeDirNames)) {
                watcher.notifyBundle();
                Bundler bundler = new Bundler();
                bundler.bundle(src, target, exludeDirNames, "contestCpp.cpp");
            }
            Thread.sleep(2000);
        }
//        bundler.compile(target);
    }

    class SourceFile {

        final File file;
        boolean isParsed;
        final List<SourceFile> dependsOn = new ArrayList<>();

        SourceFile(File file) {
            this.file = file;
        }

        void checkDependances() {
            for (SourceFile depend : dependsOn) {
                if (depend.dependsOn.contains(this)) {
                    System.err.println("cycle de dependances " + file.getName() + " <-> " + depend.file.getName());
                    depend.dependsOn.remove(this);
                }
            }

        }

        @Override
        public String toString() {
            String str = file.getName() + " " + (isParsed ? "parsed" : "waiting") + ". Dep : ";
            for (SourceFile dep : dependsOn) {
                str += " " + dep.file.getName();
            }
            return str;
        }

        public void updateDependances(List<SourceFile> files) throws FileNotFoundException {
            Scanner in = new Scanner(file);

            while (in.hasNextLine()) {
                String line = in.nextLine().replaceAll("  ", " ").trim();
                if (line.startsWith("#include \"")) {
                    String fileName = line.split(" ")[1];
                    if (fileName.indexOf("/") != -1) {
                        fileName = fileName.substring(fileName.lastIndexOf("/") + 1, fileName.length() - 1);
                    } else {
                        fileName = fileName.substring(1, fileName.length() - 1);
                    }
                    for (SourceFile other : files) {
                        if (other.file.getName().equals(fileName)) {
                            dependsOn.add(other);
                            break;
                        }
                    }
                }
            }
            in.close();
//            System.err.println(this);
        }

        boolean canBeParsed() {
            for (SourceFile depend : dependsOn) {
                if (!depend.isParsed) {
                    return false;
                }
            }
            return true;
        }

    }

}
