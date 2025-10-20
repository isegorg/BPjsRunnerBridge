package es.osoco.bpjs.bridge;

import il.ac.bgu.cs.bp.bpjs.execution.*;
import il.ac.bgu.cs.bp.bpjs.execution.listeners.BProgramRunnerListenerAdapter;
import il.ac.bgu.cs.bp.bpjs.model.*;
import il.ac.bgu.cs.bp.bpjs.model.eventselection.*;
import java.io.*;
import java.util.*;

public class BPjsRunnerBridge extends BPjsBridgeBase {

    public static void runExecution(String[] args) throws Exception {
        BProgram combinedBProgram = loadCombinedBProgram(args);
        PrioritizedBSyncEventSelectionStrategy pess = new PrioritizedBSyncEventSelectionStrategy();

        EventSelectionStrategy ess = switchPresent("-v", args)
            ? new LoggingEventSelectionStrategyDecorator(pess)
            : pess;

        combinedBProgram.setEventSelectionStrategy(ess);
        BProgramRunner runner = new BProgramRunner(combinedBProgram);

        runner.addListener(new BProgramRunnerListenerAdapter() {
            @Override
            public void eventSelected(BProgram bp, BEvent event) {
                try {
                    Map<String, Object> map = new HashMap<>();
                    map.put("type", "eventFired");
                    map.put("name", event.getName());
                    event.getDataField().ifPresent(data -> map.put("data", mapper.valueToTree(data)));

                    String json = mapper.writeValueAsString(map);
                    System.out.println(json);
                    System.out.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // Thread for reading stdin and enqueuing events
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
                        System.out.println("Error reading from stdin: " + ex.getMessage());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        stdinThread.setDaemon(true);
        stdinThread.start();

        runner.run();
    }
}
