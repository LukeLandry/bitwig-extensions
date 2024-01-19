package com.bitwig.extensions.controllers.akai.apcmk2.layer;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.akai.apc.common.AbstractSessionLayer;
import com.bitwig.extensions.controllers.akai.apc.common.PanelLayout;
import com.bitwig.extensions.controllers.akai.apc.common.control.RgbButton;
import com.bitwig.extensions.controllers.akai.apc.common.control.SingleLedButton;
import com.bitwig.extensions.controllers.akai.apc.common.led.SingleLedState;
import com.bitwig.extensions.controllers.akai.apcmk2.*;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Inject;
import com.bitwig.extensions.framework.di.PostConstruct;

public class SessionLayer extends AbstractSessionLayer {

    @Inject
    private ViewControl viewControl;
    @Inject
    private Transport transport;
    @Inject
    private ModifierStates modifiers;

    private int sceneOffset;
    private PanelLayout panelLayout = PanelLayout.VERTICAL;
    private final Layer verticalLayer;
    private final Layer horizontalLayer;
    private TrackBank trackBankVertical;
    private TrackBank trackBankHorizontal;
    private final SettableBooleanValue useAlt;

    public SessionLayer(final Layers layers, ApcPreferences preferences) {
        super(layers);
        this.horizontalLayer = new Layer(layers, "HORIZONTAL_LAYER");
        this.verticalLayer = new Layer(layers, "VERTICAL_LAYER");
        this.useAlt = preferences.getRecordButtonAsAlt();
    }

    @PostConstruct
    void init(ApcConfiguration configuration, HardwareElementsApc hwElements, Application application) {
        //application.panelLayout().addValueObserver(this::handlePanelLayoutChanged);
        initClipControl(configuration.getSceneRows(), hwElements);
    }

    private void initClipControl(int numberOfScenes, final HardwareElementsApc hwElements) {
        clipLauncherOverdub = transport.isClipLauncherOverdubEnabled();
        clipLauncherOverdub.markInterested();
        transport.isPlaying().markInterested();
        trackBankVertical = viewControl.getTrackBank();
        trackBankVertical.setShouldShowClipLauncherFeedback(true);
        initVerticalControl(numberOfScenes, hwElements, trackBankVertical);
        trackBankHorizontal = viewControl.getTrackBankHorizontal();
        initHorizontalControl(numberOfScenes, hwElements, trackBankHorizontal);

        final SceneBank sceneBank = trackBankVertical.sceneBank();
        final Scene targetScene = trackBankVertical.sceneBank().getScene(0);
        targetScene.clipCount().markInterested();
        sceneBank.setIndication(true);
        sceneBank.scrollPosition().addValueObserver(value -> sceneOffset = value);

        for (int sceneIndex = 0; sceneIndex < numberOfScenes; sceneIndex++) {
            final SingleLedButton sceneButton = hwElements.getSceneButton(sceneIndex);
            final int index = sceneIndex;
            final Scene scene = sceneBank.getScene(index);
            scene.clipCount().markInterested();
            sceneButton.bindPressed(this, () -> handleScenePressed(scene, index));
            sceneButton.bindPressed(this, () -> handleSceneReleased(scene));
            sceneButton.bindLight(this, () -> getSceneColor(index, scene));
        }
    }

    private void initVerticalControl(int numberOfScenes, HardwareElementsApc hwElements, TrackBank trackBank) {
        for (int i = 0; i < 8; i++) {
            final int trackIndex = i;
            final Track track = trackBank.getItemAt(trackIndex);
            markTrack(track);
            for (int j = 0; j < numberOfScenes; j++) {
                final int sceneIndex = j;
                final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(sceneIndex);
                prepareSlot(slot, sceneIndex, trackIndex);

                final RgbButton button = hwElements.getGridButton(sceneIndex, trackIndex);
                button.bindPressed(verticalLayer, () -> handleSlotPressed(slot));
                button.bindRelease(verticalLayer, () -> handleSlotReleased(slot));
                button.bindLight(verticalLayer, () -> getState(track, slot, trackIndex, sceneIndex));
            }
        }
    }

    private void initHorizontalControl(int numberOfScenes, HardwareElementsApc hwElements, TrackBank trackBank) {
        for (int i = 0; i < numberOfScenes; i++) {
            final int trackIndex = i;
            final Track track = trackBank.getItemAt(trackIndex);
            markTrack(track);
            for (int j = 0; j < 8; j++) {
                final int sceneIndex = j;
                final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(sceneIndex);
                prepareSlot(slot, sceneIndex, trackIndex);

                final RgbButton button = hwElements.getGridButton(trackIndex, sceneIndex);
                button.bindPressed(horizontalLayer, () -> handleSlotPressed(slot));
                button.bindRelease(horizontalLayer, () -> handleSlotReleased(slot));
                button.bindLight(horizontalLayer, () -> getState(track, slot, trackIndex, sceneIndex));
            }
        }
    }

    private void handlePanelLayoutChanged(final String panelLayout) {
        if (panelLayout.equals("MIX")) {
            setLayout(PanelLayout.VERTICAL);
            trackBankHorizontal.setShouldShowClipLauncherFeedback(false);
            trackBankVertical.setShouldShowClipLauncherFeedback(true);
        } else {
            setLayout(PanelLayout.HORIZONTAL);
            trackBankHorizontal.setShouldShowClipLauncherFeedback(true);
            trackBankVertical.setShouldShowClipLauncherFeedback(false);
        }
    }

    @Override
    protected boolean isPlaying() {
        return transport.isPlaying().get();
    }

    @Override
    protected boolean isShiftHeld() {
        return modifiers.isShift();
    }

    public void setLayout(final PanelLayout layout) {
        panelLayout = layout;
        horizontalLayer.setIsActive(panelLayout == PanelLayout.HORIZONTAL);
        verticalLayer.setIsActive(panelLayout == PanelLayout.VERTICAL);
    }

    private void handleScenePressed(final Scene scene, final int index) {
        viewControl.focusScene(index + sceneOffset);
        if (modifiers.isShift()) {
            if (useAlt.get()) {
                scene.launchAlt();
            } else {
                scene.selectInEditor();
            }
        } else {
            scene.launch();
        }
    }

    private void handleSceneReleased(final Scene scene) {
        if (modifiers.isShift()) {
            if (useAlt.get()) {
                scene.launchReleaseAlt();
            }
        } else {
            scene.launchRelease();
        }
    }

    private SingleLedState getSceneColor(final int index, final Scene scene) {
        if (scene.clipCount().get() > 0) {
            if (sceneOffset + index == viewControl.getFocusSceneIndex() && viewControl.hasQueuedForPlaying()) {
                return SingleLedState.BLINK;
            }
            return SingleLedState.OFF;
        }
        return SingleLedState.OFF;
    }

    private void handleSlotPressed(final ClipLauncherSlot slot) {
        if (modifiers.isShift()) {
            if (useAlt.get()) {
                slot.launchAlt();
            } else {
                slot.select();
            }
        } else {
            slot.launch();
        }
    }

    private void handleSlotReleased(final ClipLauncherSlot slot) {
        if (modifiers.isShift()) {
            if (useAlt.get()) {
                slot.launchReleaseAlt();
            }
        } else {
            slot.launchRelease();
        }
    }

    @Override
    protected void onActivate() {
        super.onActivate();
        horizontalLayer.setIsActive(panelLayout == PanelLayout.HORIZONTAL);
        verticalLayer.setIsActive(panelLayout == PanelLayout.VERTICAL);
    }

    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        horizontalLayer.setIsActive(false);
        verticalLayer.setIsActive(false);
    }

}
