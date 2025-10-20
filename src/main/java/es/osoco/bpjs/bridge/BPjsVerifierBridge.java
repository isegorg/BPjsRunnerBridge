package es.osoco.bpjs.bridge;

import il.ac.bgu.cs.bp.bpjs.analysis.*;
import il.ac.bgu.cs.bp.bpjs.analysis.violations.Violation;
import il.ac.bgu.cs.bp.bpjs.model.*;
import il.ac.bgu.cs.bp.bpjs.model.eventselection.PrioritizedBSyncEventSelectionStrategy;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;
import java.lang.reflect.Field;

public class BPjsVerifierBridge extends BPjsBridgeBase {

    public static void runVerification(String[] args) throws Exception {
        BProgram bprog = loadCombinedBProgram(args);
        PrioritizedBSyncEventSelectionStrategy pess = new PrioritizedBSyncEventSelectionStrategy();

        DfsBProgramVerifier verifier = new DfsBProgramVerifier();
        verifier.setDebugMode(switchPresent("-v", args));
        bprog.setEventSelectionStrategy(pess);

        // Choose state storage
        if (switchPresent("--full-state-storage", args)) {
            verifier.setVisitedStateStore(new BThreadSnapshotVisitedStateStore());
        } else {
            verifier.setVisitedStateStore(new BProgramSnapshotVisitedStateStore());
        }

        // Load inspections
        List<ExecutionTraceInspection> inspections = new ArrayList<>();
        String inspectionsStr = keyForValue("--inspections", args);
        if (inspectionsStr != null) {
            Arrays.stream(inspectionsStr.split(","))
                  .map(String::trim)
                  .filter(n -> !n.isEmpty())
                  .forEach(n -> {
                      try {
                          Field field = ExecutionTraceInspections.class.getDeclaredField(n);
                          ExecutionTraceInspection inspection = (ExecutionTraceInspection) field.get(null);
                          inspections.add(inspection);
                      } catch (Exception e) {
                          println("Unknown inspection: " + n);
                      }
                  });
        }
        if (inspections.isEmpty()) {
            ExecutionTraceInspections.DEFAULT_SET.forEach(inspections::add);
        }
        inspections.forEach(verifier::addInspection);

        // Max trace length
        String maxDepthStr = keyForValue("--max-trace-length", args);
        if (maxDepthStr != null) {
            try {
                verifier.setMaxTraceLength(Long.parseLong(maxDepthStr.trim()));
            } catch (NumberFormatException nfe) {
                println("Invalid max trace length: " + maxDepthStr);
            }
        }

	// --pretty optional switch
        boolean pretty = switchPresent("--pretty", args);
	
        // Run verification and build JSON result
        ObjectNode resultJson = mapper.createObjectNode();
        resultJson.put("type", "verificationResult");

        try {
            VerificationResult res = verifier.verify(bprog);

            resultJson.put("timeMillis", res.getTimeMillies());
            resultJson.put("statesScanned", res.getScannedStatesCount());
            resultJson.put("edgesScanned", res.getScannedEdgesCount());
            resultJson.put("isViolation", res.getViolation().isPresent());

            if (res.getViolation().isPresent()) {
                Violation vio = res.getViolation().get();
                ObjectNode vioJson = mapper.createObjectNode();
                vioJson.put("description", vio.decsribe());

                // Counterexample events
                ArrayNode eventsArray = mapper.createArrayNode();
                vio.getCounterExampleTrace().getNodes().stream()
                        .flatMap(n -> n.getEvent().stream())
                        .forEach(evt -> {
                            ObjectNode evJson = mapper.createObjectNode();
                            evJson.put("type", "eventFired");
                            evJson.put("name", evt.getName());
                            evt.getDataField().ifPresent(data ->
                                evJson.set("data", mapper.valueToTree(data))
                            );
                            eventsArray.add(evJson);
                        });

                vioJson.set("counterExampleTrace", eventsArray);
                resultJson.set("violation", vioJson);
            }

            // Print JSON to stdout
            String jsonOutput = pretty
                ? mapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultJson)
                : mapper.writeValueAsString(resultJson);
            System.out.println(jsonOutput);
            System.out.flush();

        } catch (Exception e) {
            ObjectNode error = mapper.createObjectNode();
            error.put("type", "error");
            error.put("message", e.getMessage());
            String jsonOutput = pretty
                ? mapper.writerWithDefaultPrettyPrinter().writeValueAsString(error)
                : mapper.writeValueAsString(error);
            System.out.println(jsonOutput);
            System.out.flush();
        }
    }
}
