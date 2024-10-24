package org.test.aurora.interceptor;

import org.aurora.interceptor.AurPassiveInterceptor;
import org.aurora.scanner.AurScannedData;
import org.aurora.util.AurFile;

public class TestInterceptor implements AurPassiveInterceptor<AurFile, AurScannedData> {
    @Override
    public void beforeState(AurFile aurFile) {
        System.out.println("Test before pass");
    }

    @Override
    public void afterState(AurScannedData o) {
        System.out.println("Test after pass");
    }

    @Override
    public String getName() {
        return "test001";
    }
}
