package dev.isxander.controlify.mixins.feature.screenop.yacl;

import dev.isxander.controlify.screenop.ComponentProcessor;
import dev.isxander.controlify.screenop.ComponentProcessorProvider;
import dev.isxander.controlify.screenop.compat.yacl.SliderControllerElementComponentProcessor;
import dev.isxander.yacl.gui.controllers.slider.SliderControllerElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SliderControllerElement.class)
public class SliderControllerElementMixin implements ComponentProcessorProvider {
    @Unique private final SliderControllerElementComponentProcessor controlify$processor
            = new SliderControllerElementComponentProcessor((SliderControllerElement) (Object) this);

    @Override
    public ComponentProcessor componentProcessor() {
        return controlify$processor;
    }
}