package dev.isxander.controlify.ingame;

import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.controller.Controller;
import dev.isxander.controlify.utils.Log;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;

public class ControllerPlayerMovement extends Input {
    private final ControllerEntity controller;
    private final LocalPlayer player;
    private boolean wasFlying, wasPassenger;
    private float accumulatorForward, accumulatorLeft = 0.0F;
    //int currentTick = 0;

    public ControllerPlayerMovement(ControllerEntity controller, LocalPlayer player) {
        this.controller = controller;
        this.player = player;
    }

    @Override
    public void tick(boolean slowDown, float movementMultiplier) {
        if (Minecraft.getInstance().screen != null || player == null) {
            this.up = false;
            this.down = false;
            this.left = false;
            this.right = false;
            this.leftImpulse = 0;
            this.forwardImpulse = 0;
            this.jumping = false;
            this.shiftKeyDown = false;
            return;
        }

        var bindings = controller.bindings();

        this.forwardImpulse = bindings.WALK_FORWARD.state() - bindings.WALK_BACKWARD.state();
        this.leftImpulse = bindings.WALK_LEFT.state() - bindings.WALK_RIGHT.state();

        if (Controlify.instance().config().globalSettings().shouldUseKeyboardMovement()) {
            float threshold = controller.input().orElseThrow().confObj().buttonActivationThreshold;

            //this.forwardImpulse = Math.abs(this.forwardImpulse) >= threshold ? Math.copySign(1, this.forwardImpulse) : 0;
            //this.leftImpulse = Math.abs(this.leftImpulse) >= threshold ? Math.copySign(1, this.leftImpulse) : 0;

            this.forwardImpulse = roundToPrecision(this.forwardImpulse, 0.05F);
            this.leftImpulse = roundToPrecision(this.leftImpulse, 0.05F);

            float busyRateForward = Math.abs(this.forwardImpulse);
            float busyRateLeft = Math.abs(this.leftImpulse);

            boolean isBusyForward;
            if (busyRateForward >= 1.0F) {
                isBusyForward = true;
            }
            else if (busyRateForward >= 0.5F) {
                // Calculate busy ticks per second based on the busy rate
                float busyTicksPerSecond = 20 * busyRateForward;
                // Calculate how much each tick contributes towards the next busy tick
                float increment = 1.0F / (20 / busyTicksPerSecond);
                isBusyForward = false;
                this.accumulatorForward += increment;
                // Check if the accumulator has reached or exceeded the threshold for a busy tick
                if (this.accumulatorForward >= 1) {
                    isBusyForward = true;
                    this.accumulatorForward -= 1; // Adjust the accumulator
                }
            } else if (busyRateForward != 0.0F) {
                // For rates of 50% or below, alternate based on the rate
                float busyTicksPerSecond = 20 * busyRateForward;
                float ticksPerBusyCycle = 20 / busyTicksPerSecond;
                // Use modulo operation for evenly distributed busy ticks
                //isBusyForward = (this.currentTick % (int)ticksPerBusyCycle) != 0;
                isBusyForward = true;
                accumulatorForward = 0.0F;
            }
            else {
                isBusyForward = false;
                accumulatorForward = 0.0F;
            }

            if (!isBusyForward) {
                this.forwardImpulse = 0.0F;
            }
            else if (busyRateForward >= 0.5F) {
                this.forwardImpulse = Math.copySign(1, this.forwardImpulse);
            }
            else if (busyRateForward != 0.0F) {
                this.forwardImpulse = Math.copySign(0.3F, this.forwardImpulse);
            }

            boolean isBusyLeft;
            if (busyRateLeft >= 1.0F) {
                isBusyLeft = true;
            }
            else if (busyRateLeft >= 0.5F) {
                // Calculate busy ticks per second based on the busy rate
                float busyTicksPerSecond = 20 * busyRateLeft;
                // Calculate how much each tick contributes towards the next busy tick
                float increment = 1.0F / (20 / busyTicksPerSecond);
                isBusyLeft = false;
                this.accumulatorLeft += increment;
                // Check if the accumulator has reached or exceeded the threshold for a busy tick
                if (this.accumulatorLeft >= 1.0F) {
                    isBusyLeft = true;
                    this.accumulatorLeft -= 1; // Adjust the accumulator
                }
            } else if (busyRateLeft != 0.0F) {
                // For rates of 50% or below, alternate based on the rate
                float busyTicksPerSecond = 20 * busyRateLeft;
                float ticksPerBusyCycle = 20 / busyTicksPerSecond;
                // Use modulo operation for evenly distributed busy ticks
                //isBusyLeft = (this.currentTick % (int)ticksPerBusyCycle) != 0;
                isBusyLeft = true;
                accumulatorLeft = 0.0F;
            }
            else {
                isBusyLeft = false;
                accumulatorLeft = 0.0F;
            }

            if (!isBusyLeft) {
                this.leftImpulse = 0.0F;
            }
            else if (busyRateLeft >= 0.5F) {
                this.leftImpulse = Math.copySign(1, this.leftImpulse);
            }
            else if (busyRateLeft != 0.0F) {
                this.leftImpulse = Math.copySign(0.3F, this.leftImpulse);
            }
        }
        else {
            this.forwardImpulse = roundToPrecision(this.forwardImpulse, 0.1F);
            this.leftImpulse = roundToPrecision(this.leftImpulse, 0.1F);
        }

        this.up = this.forwardImpulse > 0;
        this.down = this.forwardImpulse < 0;
        this.left = this.leftImpulse > 0;
        this.right = this.leftImpulse < 0;

        if (slowDown) {
            this.leftImpulse *= movementMultiplier;
            this.forwardImpulse *= movementMultiplier;
        }

        // this over-complication is so exiting a GUI with the button still held doesn't trigger a jump.
        if (bindings.JUMP.justPressed())
            this.jumping = true;
        if (!bindings.JUMP.held())
            this.jumping = false;

        if (player.getAbilities().flying || (player.isInWater() && !player.onGround()) || player.getVehicle() != null || !controller.genericConfig().config().toggleSneak) {
            if (bindings.SNEAK.justPressed())
                this.shiftKeyDown = true;
            if (!bindings.SNEAK.held())
                this.shiftKeyDown = false;
        } else {
            if (bindings.SNEAK.justPressed()) {
                this.shiftKeyDown = !this.shiftKeyDown;
            }
        }
        if ((!player.getAbilities().flying && wasFlying && player.onGround()) || (!player.isPassenger() && wasPassenger)) {
            this.shiftKeyDown = false;
        }

        this.wasFlying = player.getAbilities().flying;
        this.wasPassenger = player.isPassenger();

        //this.currentTick = (this.currentTick + 1) % 20;
    }

    public static void updatePlayerInput(@Nullable LocalPlayer player) {
        if (player == null)
            return;

        if (shouldBeControllerInput()) {
            player.input = new DualInput(
                    new KeyboardInput(Minecraft.getInstance().options),
                    new ControllerPlayerMovement(Controlify.instance().getCurrentController().get(), player)
            );
        } else if (!(player.input instanceof KeyboardInput)) {
            player.input = new KeyboardInput(Minecraft.getInstance().options);
        }
    }

    public static void ensureCorrectInput(@Nullable LocalPlayer player) {
        if (player == null)
            return;

        if (shouldBeControllerInput() && player.input.getClass() == KeyboardInput.class) {
            updatePlayerInput(player);
        }
    }

    public static boolean shouldBeControllerInput() {
        return Controlify.instance().getCurrentController().isPresent() && Controlify.instance().currentInputMode().isController();
    }

    public static float roundToPrecision(float number, float precision) {
        if (precision <= 0) {
            return number;
        }
        float factor = 1 / precision;
        return Math.round(number * factor) / factor;
    }
}
