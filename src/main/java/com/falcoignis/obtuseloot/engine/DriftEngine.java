package com.falcoignis.obtuseloot.engine;

final class DriftEngine implements EngineSubsystem {
    private final ObtuseEngine engine;

    DriftEngine(ObtuseEngine engine) {
        this.engine = engine;
    }
}
