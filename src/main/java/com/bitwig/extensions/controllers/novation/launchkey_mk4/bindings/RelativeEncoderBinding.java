package com.bitwig.extensions.controllers.novation.launchkey_mk4.bindings;

import com.bitwig.extension.controller.api.HardwareBinding;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.LaunchRelEncoder;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.display.DisplayControl;

public class RelativeEncoderBinding extends LauncherBinding<LaunchRelEncoder> {
    
    private int parameterValue;
    private boolean incoming = false;
    
    public RelativeEncoderBinding(final int index, final Parameter parameter, final LaunchRelEncoder knob,
        final DisplayControl displayControl, final StringValue trackName, final StringValue parameterName) {
        super(index + 0x15, parameter, knob, displayControl, trackName, parameterName);
        
        parameter.value().addValueObserver(128, this::handleParameterValue);
        this.parameterValue = (int) (parameter.value().get() * 127);
    }
    
    private void handleParameterValue(final int value) {
        this.parameterValue = value;
        if (isActive()) {
            if (incoming) {
                incoming = false;
            }
        }
    }
    
    @Override
    protected HardwareBinding getHardwareBinding() {
        return getSource().addBinding(getTarget().getEncoder());
    }
    
    @Override
    protected void updateValue() {
    
    }
    
}
