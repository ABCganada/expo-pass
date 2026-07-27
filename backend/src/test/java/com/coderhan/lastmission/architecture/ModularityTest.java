package com.coderhan.lastmission.architecture;

import com.coderhan.lastmission.LastMissionApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTest {
    @Test
    void verifiesModuleBoundariesAndCycles() {
        ApplicationModules.of(LastMissionApplication.class).verify();
    }
}
