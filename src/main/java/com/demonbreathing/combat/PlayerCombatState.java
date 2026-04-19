package com.demonbreathing.combat;

import com.demonbreathing.model.BreathingStyle;

import java.util.HashMap;
import java.util.Map;

public final class PlayerCombatState {
    private BreathingStyle style = BreathingStyle.THUNDER;
    private int selectedForm = 0;
    private long chargeStartMillis = -1L;
    private long storedChargeMillis = 0L;
    private long releaseWindowExpiresAt = 0L;
    private final Map<String, Long> cooldowns = new HashMap<>();

    public BreathingStyle style() {
        return style;
    }

    public void setStyle(BreathingStyle style) {
        this.style = style;
    }

    public int selectedForm() {
        return selectedForm;
    }

    public void setSelectedForm(int selectedForm) {
        this.selectedForm = selectedForm;
    }

    public void cycleFormForward() {
        selectedForm = (selectedForm + 1) % 3;
    }

    public void cycleFormBackward() {
        selectedForm = (selectedForm + 2) % 3;
    }

    public long chargeStartMillis() {
        return chargeStartMillis;
    }

    public void setChargeStartMillis(long chargeStartMillis) {
        this.chargeStartMillis = chargeStartMillis;
    }

    public long storedChargeMillis() {
        return storedChargeMillis;
    }

    public void setStoredChargeMillis(long storedChargeMillis) {
        this.storedChargeMillis = storedChargeMillis;
    }

    public long releaseWindowExpiresAt() {
        return releaseWindowExpiresAt;
    }

    public void setReleaseWindowExpiresAt(long releaseWindowExpiresAt) {
        this.releaseWindowExpiresAt = releaseWindowExpiresAt;
    }

    public long cooldownUntil(String key) {
        return cooldowns.getOrDefault(key, 0L);
    }

    public void setCooldown(String key, long untilMillis) {
        cooldowns.put(key, untilMillis);
    }

    public void resetCharge() {
        chargeStartMillis = -1L;
        storedChargeMillis = 0L;
        releaseWindowExpiresAt = 0L;
    }
}
