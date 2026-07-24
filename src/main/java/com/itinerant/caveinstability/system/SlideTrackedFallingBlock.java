package com.itinerant.caveinstability.system;

public interface SlideTrackedFallingBlock {
    int caveinstability$getSlideCount();

    void caveinstability$setSlideCount(int slideCount);

    int caveinstability$getStartY();

    void caveinstability$setStartY(int startY);

    boolean caveinstability$hasTrackedStart();

    void caveinstability$setTrackedStart(boolean trackedStart);

    boolean caveinstability$hasHandledLanding();

    void caveinstability$setHandledLanding(boolean handledLanding);
}