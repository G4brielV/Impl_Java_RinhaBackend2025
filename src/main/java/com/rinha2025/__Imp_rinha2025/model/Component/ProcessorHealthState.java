package com.rinha2025.__Imp_rinha2025.model.Component;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class ProcessorHealthState {

    public enum Processor {
        DEFAULT,
        FALLBACK
    }

    // AtomicReference garante que a troca do estado seja atômica e segura entre as threads.
    private final AtomicReference<Processor> preferredProcessor = new AtomicReference<>(Processor.DEFAULT);

    public Processor getPreferredProcessor() {
        return preferredProcessor.get();
    }

    public void setPreferredProcessor(Processor processor) {
        this.preferredProcessor.set(processor);
    }
}
