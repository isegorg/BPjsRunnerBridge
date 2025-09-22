package es.osoco.bpjs.bridge;

import il.ac.bgu.cs.bp.bpjs.execution.BProgramRunner;
import il.ac.bgu.cs.bp.bpjs.execution.listeners.BProgramRunnerListenerAdapter;
import il.ac.bgu.cs.bp.bpjs.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * BPjs Runner simple que comunica vía stdin/stdout en formato JSON.
 */
public class BPjsRunnerStdInOutBridge {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String DEFAULT_EXAMPLES_DIR = "src/main/examples";

    public static void main(String[] args) throws Exception {

        String path;
        if (args.length > 0) {
            path = args[0];
        } else {
            path = DEFAULT_EXAMPLES_DIR;
        }

        File f = new File(path);
        StringBuilder combinedCode = new StringBuilder();

        if (f.isDirectory()) {
            File[] jsFiles = f.listFiles((d, name) -> name.endsWith(".js"));
            if (jsFiles == null || jsFiles.length == 0) {
                throw new RuntimeException("No JS files found in directory: " + path);
            }
            for (File js : jsFiles) {
                String code = Files.readString(js.toPath());
                combinedCode.append(code).append("\n");
                System.out.println("Loaded BProgram: " + js.getName());
            }
        } else if (f.isFile()) {
            String code = Files.readString(f.toPath());
            combinedCode.append(code);
            System.out.println("Loaded BProgram: " + f.getName());
        } else {
            throw new RuntimeException("File or directory not found: " + path);
        }

        // Create single BProgram with all combined JS code
        BProgram combinedBProgram = new StringBProgram(combinedCode.toString());
	combinedBProgram.setWaitForExternalEvents(true);
	
        BProgramRunner runner = new BProgramRunner(combinedBProgram);

        // Listener para eventos ejecutados
        runner.addListener(new BProgramRunnerListenerAdapter() {
            @Override
            public void eventSelected(BProgram bp, BEvent event) {
                try {
		    Map<String, Object> map = new HashMap<>();
		    map.put("type", "eventFired");
		    map.put("name", event.getName());		    
                    event.getDataField().ifPresent(data -> map.put("data", data.toString()));
		    
		    String json = mapper.writeValueAsString(map);
                    System.out.println(json);
                    System.out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        // Hilo separado para leer stdin y encolar eventos
        Thread stdinThread = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
                String line;
                while ((line = br.readLine()) != null) {
                    try {
                        Map<String, Object> msg = mapper.readValue(line, Map.class);
                        if ("enqueueEvent".equals(msg.get("type"))) {
                            String evName = (String) msg.get("name");
                            Object data = msg.get("data");
                            combinedBProgram.enqueueExternalEvent(new BEvent(evName, data));
                        }
                    } catch (Exception ex) {
                        System.err.println("Error reading from stdin: " + ex.getMessage());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        stdinThread.setDaemon(true);
        stdinThread.start();

        runner.run();
    }
}
