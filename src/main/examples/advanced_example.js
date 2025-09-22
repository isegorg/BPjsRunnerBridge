// Thread 1: Requests Event A after ButtonPressed
bp.registerBThread("RequesterA", function () {
  bp.sync({ waitFor: bp.Event("ButtonPressed") });
  bp.sync({ request: bp.Event("EventA") });
});

// Thread 2: Requests Event B after ButtonPressed
bp.registerBThread("RequesterB", function () {
  bp.sync({ waitFor: bp.Event("ButtonPressed") });
  bp.sync({ request: bp.Event("EventB") });
});

// Thread 3: Blocks EventB if EventA was already selected
bp.registerBThread("Blocker", function () {
  bp.sync({ waitFor: bp.Event("EventA") }); // wait until A happens
  while (true) {
    bp.sync({ block: bp.Event("EventB") }); // block EventB forever
  }
});

// Logger: Observes all events
bp.registerBThread("Logger", function () {
  while (true) {
    var e = bp.sync({ waitFor: bp.all });
    bp.log.info("Logger observed: " + e.name);
  }
});
