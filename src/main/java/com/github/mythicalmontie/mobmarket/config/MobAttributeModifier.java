package com.github.mythicalmontie.mobmarket.config;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.registries.ForgeRegistries;

public class MobAttributeModifier {
    private final String key;
    private final String type;
    private final double baseline;
    private final double multiplierPerUnit;
    private final double threshold;
    private final int bonusAmount;
    private final double minMultiplier;
    private final double maxMultiplier;

    public MobAttributeModifier(String key, String type, double baseline, double multiplayerPerUnit, double threshold, int bonusAmount, double minMultiplayer, double maxMultiplayer) {
        this.key = key;
        this.type = type;
        this.baseline = baseline;
        this.multiplierPerUnit = multiplayerPerUnit;
        this.threshold = threshold;
        this.bonusAmount = bonusAmount;
        this.minMultiplier = minMultiplayer;
        this.maxMultiplier = maxMultiplayer;
    }

    private boolean hasNbtKey(CompoundTag tag, String targetKey) {
        if (tag.contains(targetKey)) {
            int typeId = tag.getTagType(targetKey);
            if (typeId >= 1 && typeId <= 6) {
                return true;
            }
        }
        for (String keyName : tag.getAllKeys()) {
            if (tag.contains(keyName, 10)) {
                if (hasNbtKey(tag.getCompound(keyName), targetKey)) {
                    return true;
                }
            }
        }
        return false;
    }

    private double findNbtValue(CompoundTag tag, String targetKey) {

        if (tag.contains(targetKey)) {
            int typeId = tag.getTagType(targetKey);
            double val = switch (typeId) {
                case 1 -> tag.getByte(targetKey);
                case 2 -> tag.getShort(targetKey);
                case 3 -> tag.getInt(targetKey);
                case 4 -> tag.getLong(targetKey);
                case 5 -> tag.getFloat(targetKey);
                case 6 -> tag.getDouble(targetKey);
                default -> Double.NaN;
            };
            if (!Double.isNaN(val)) {
                return val;
            }
        }

        for (String keyName : tag.getAllKeys()) {
            if (tag.contains(keyName, 10)) {
                double nestedValue = findNbtValue(tag.getCompound(keyName), targetKey);
                if (!Double.isNaN(nestedValue)) {
                    return nestedValue;
                }
            }
        }
        return Double.NaN;
    }

    public double applyModifier(LivingEntity entity, double currentPrice) {
        double rawValue = Double.NaN;

        ResourceLocation resLoc = ResourceLocation.tryParse(this.key);
        if (resLoc != null) {
            Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(resLoc);
            if (attr != null) {
                AttributeInstance instance = entity.getAttribute(attr);
                if (instance != null) {
                    rawValue = instance.getValue();
                }
            }
        }
        if (Double.isNaN(rawValue)) {
            CompoundTag nbt = new CompoundTag();
            entity.save(nbt);
            rawValue = findNbtValue(nbt, this.key);
        }
        if (Double.isNaN(rawValue)) {
            return currentPrice;
        }
        return switch (this.type.toLowerCase()) {
            case "linear" -> {
                double diff = rawValue - this.baseline;
                double scaleFactor = 1.0 + (diff * this.multiplierPerUnit);
                scaleFactor = Mth.clamp(scaleFactor, (float) this.minMultiplier, (float) this.maxMultiplier);
                yield currentPrice * scaleFactor;
            }
            case "flat" -> {
                if (rawValue >= this.threshold) {
                    yield currentPrice + this.bonusAmount;
                }
                yield currentPrice;
            }
            case "exponential" -> {
                if (rawValue > this.baseline) {
                    double scaleFactor = Math.pow(rawValue / this.baseline, this.multiplierPerUnit);
                    scaleFactor = Mth.clamp(scaleFactor, (float) this.minMultiplier, (float) this.maxMultiplier);
                    yield currentPrice * scaleFactor;
                }
                yield currentPrice;
            }
            case "match", "equals" -> {
                if (Math.abs(rawValue - this.threshold) < 0.0001) {
                    yield currentPrice + this.bonusAmount;
                }
                yield currentPrice;
            }
            default -> currentPrice;
        };
    }
    public String getKey() { return key; }
    public String getType() { return type; }
    public double getBaseline() { return baseline; }
    public double getMultiplierPerUnit() { return multiplierPerUnit; }
    public double getThreshold() { return threshold; }
    public int getBonusAmount() { return bonusAmount; }
    public double getMinMultiplier() { return minMultiplier; }
    public double getMaxMultiplier() { return maxMultiplier; }
}
