package es.osoco.bpjs.bridge;

import il.ac.bgu.cs.bp.bpjs.model.*;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class BPjsBridgeBase {

    protected static final ObjectMapper mapper = new ObjectMapper();
    protected static final String DEFAULT_EXAMPLES_DIR = "src/main/examples";

    protected static BProgram loadCombinedBProgram(String[] args) throws Exception {
        StringBuilder combinedCode = new StringBuilder();

        for (String arg : args) {
            if (!arg.startsWith("-")) {
                File f = new File(arg);
                if (f.isDirectory()) {
                    File[] jsFiles = f.listFiles((d, name) -> name.endsWith(".js"));
                    if (jsFiles == null || jsFiles.length == 0) {
                        throw new RuntimeException("No JS files found in directory: " + arg);
                    }
                    for (File js : jsFiles) {
                        String code = Files.readString(js.toPath());
                        combinedCode.append(code).append("\n");
                        println("Loaded BProgram: " + js.getName());
                    }
                } else if (f.isFile()) {
                    String code = Files.readString(f.toPath());
                    combinedCode.append(code);
                    println("Loaded BProgram: " + f.getName());
                } else {
                    throw new RuntimeException("File or directory not found: " + arg);
                }
            }
        }

        BProgram combined = new StringBProgram(combinedCode.toString());
        combined.setWaitForExternalEvents(true);
        return combined;
    }

    protected static boolean switchPresent(String aSwitch, String[] args) {
        return Arrays.stream(args).anyMatch(s -> s.trim().equals(aSwitch));
    }

    protected static String keyForValue(String key, String[] args) {
        for (String arg : args) {
            if (arg.startsWith(key + "=")) {
                String[] comps = arg.split("=", 2);
                return comps.length == 2 ? comps[1] : null;
            }
        }
        return null;
    }

    protected static void println(String text) {
        System.out.println(text);
    }

    protected static void println(String template, String... params) {
        print(template + "\n", params);
    }

    protected static void print(String template, String... params) {
        if (params.length == 0) {
            System.out.print(template);
        } else {
            System.out.printf(template, (Object[]) params);
        }
    }
}
