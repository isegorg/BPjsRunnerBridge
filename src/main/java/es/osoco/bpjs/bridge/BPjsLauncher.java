package es.osoco.bpjs.bridge;

import il.ac.bgu.cs.bp.bpjs.model.BProgram;

public class BPjsLauncher extends BPjsBridgeBase {

    public static void main(String[] args) throws Exception {

        if (switchPresent("--verify", args)) {
            BPjsVerifierBridge.runVerification(args);
        } else {
            BPjsRunnerBridge.runExecution(args);
        }
    }
}
