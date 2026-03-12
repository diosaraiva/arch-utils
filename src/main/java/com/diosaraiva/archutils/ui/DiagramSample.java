package com.diosaraiva.archutils.ui;

/**
 * Available PlantUML diagram samples with their resource paths.
 */
public enum DiagramSample {

    C4_CONTEXT("C4 Context", "c4_context.puml"),
    C4_CONTAINER("C4 Container", "c4_container.puml"),
    C4_COMPONENT("C4 Component", "c4_component.puml"),
    C4_DEPLOYMENT("C4 Deployment", "c4_deployment.puml"),
    SEQUENCE("Sequence", "sequence.puml"),
    CLASS("Class", "class.puml"),
    ACTIVITY("Activity", "activity.puml"),
    USE_CASE("Use Case", "usecase.puml"),
    STATE("State", "state.puml"),
    COMPONENT("Component", "component.puml"),
    OBJECT("Object", "object.puml"),
    DEPLOYMENT("Deployment", "deployment.puml"),
    TIMING("Timing", "timing.puml"),
    MINDMAP("Mind Map", "mindmap.puml"),
    GANTT("Gantt", "gantt.puml");

    private final String displayName;
    private final String fileName;

    DiagramSample(String displayName, String fileName) {
        this.displayName = displayName;
        this.fileName = fileName;
    }

    public String getFileName() { return fileName; }

    @Override
    public String toString() { return displayName; }
}
