package com.github.mythicalmontie.mobmarket.client;

import com.github.mythicalmontie.mobmarket.MobMarket;
import com.github.mythicalmontie.mobmarket.blockentity.MobMarketBlockEntity;
import com.github.mythicalmontie.mobmarket.blockentity.MobMarketGUI;
import com.github.mythicalmontie.mobmarket.network.ModMessages;
import com.github.mythicalmontie.mobmarket.network.ToggleSecurity;
import com.github.mythicalmontie.mobmarket.util.TeamProtectionHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MobMarketScreen extends AbstractContainerScreen<MobMarketGUI> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MobMarket.MODID, "textures/gui/mob_market.png");
    private Button privacyButton;

    public MobMarketScreen(MobMarketGUI menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 160;

        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 66;
    }

    @Override
    protected void init() {
        super.init();

        MobMarketBlockEntity be = this.menu.getBlockEntity();

        this.privacyButton = Button.builder(getPrivacyLabel(be), (btn) -> {
            if (be != null) {
                ModMessages.sendToServer(new ToggleSecurity(be.getBlockPos()));
                be.togglePublicState();
                btn.setMessage(getPrivacyLabel(be));
                updatePrivacyTooltip(be);
            }
        }).bounds(this.leftPos + 104, this.topPos - 14, 64, 12).build();

        updatePrivacyTooltip(be);
        this.addRenderableWidget(this.privacyButton);
    }

    private Component getPrivacyLabel(MobMarketBlockEntity be) {
        if (be == null) return Component.literal("Access");

        if (TeamProtectionHelper.isFTBTeamsLoaded()) {
            if (be.isPrivate()) {return Component.translatable("gui.mobmarket.button.private");}
            else if (be.isTeam()) {return Component.translatable("gui.mobmarket.button.team");}
            else {return Component.translatable("gui.mobmarket.button.public");}
        }

        return Component.translatable(be.isPublic() ? "gui.mobmarket.button.public" : "gui.mobmarket.button.private");
    }

    private void updatePrivacyTooltip(MobMarketBlockEntity be) {
        if (be == null) return;

        Component tooltipKey;
        if (TeamProtectionHelper.isFTBTeamsLoaded()) {
            if (be.isPrivate()) {
                tooltipKey = Component.translatable("gui.mobmarket.button.private_tooltip");
            } else if (be.isTeam()) {
                tooltipKey = Component.translatable("gui.mobmarket.button.team_tooltip");
            } else {
                tooltipKey = Component.translatable("gui.mobmarket.button.public_tooltip");
            }
        } else {
            tooltipKey = Component.translatable(be.isPublic() ? "gui.mobmarket.button.public_tooltip" : "gui.mobmarket.button.private_tooltip");
        }

        this.privacyButton.setTooltip(Tooltip.create(tooltipKey));
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        int eligible = this.menu.getLiveEligibleCount();
        int ineligible = this.menu.getLiveIneligibleCount();
        String ownerName = this.menu.getBlockEntity().getOwnerName();

        String ownerText = Component.translatable("gui.mobmarket.owner_label").getString() + " " + ownerName;
        guiGraphics.drawString(this.font, ownerText, 8, 41, 4210752, false);

        String eligibleText = "§2✔ §a" + eligible;
        String ineligibleText = "§4✕ §c" + ineligible;

        guiGraphics.drawString(this.font, eligibleText, 8, 54, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, ineligibleText, 44, 54, 0xFFFFFF, false);

        Map<String, Integer> currencyTotals = this.menu.getLiveCurrencyTotals();
        Map<String, ItemStack> currencyIcons = this.menu.getLiveCurrencyIcons();

        int currentXOffset = 80;

        if (!currencyTotals.isEmpty()) {
            String valueLabelText = Component.translatable("gui.mobmarket.label.value").getString() + ": ";
            guiGraphics.drawString(this.font, valueLabelText, currentXOffset, 54, 4210752, false);
            currentXOffset += this.font.width(valueLabelText);

            for (Map.Entry<String, Integer> entry : currencyTotals.entrySet()) {
                String currencyId = entry.getKey();
                int currentTotalScore = entry.getValue();
                ItemStack renderIcon = currencyIcons.getOrDefault(currencyId, ItemStack.EMPTY);

                String scoreString = String.valueOf(currentTotalScore);
                guiGraphics.drawString(this.font, scoreString, currentXOffset, 54, 4210752, false);
                currentXOffset += this.font.width(scoreString) + 2;

                if (!renderIcon.isEmpty()) {
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(-this.leftPos, -this.topPos, 0);

                    float renderX = this.leftPos + currentXOffset;
                    float renderY = this.topPos + 52;

                    guiGraphics.pose().translate(renderX, renderY, 0);
                    guiGraphics.pose().scale(0.75F, 0.75F, 1.0F);
                    guiGraphics.pose().translate(-renderX, -renderY, 0);

                    guiGraphics.renderFakeItem(renderIcon, (int)renderX, (int)renderY);
                    guiGraphics.pose().popPose();

                    currentXOffset += 14;
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        MobMarketBlockEntity be = this.menu.getBlockEntity();
        if (be != null) {
            this.privacyButton.setMessage(getPrivacyLabel(be));
            updatePrivacyTooltip(be);
        }

        int textTop = this.topPos + 54;
        int textHeight = this.font.lineHeight;

        int ineligibleLeft = this.leftPos + 44;
        String rawIneligibleText = "✕ " + this.menu.getLiveIneligibleCount();
        int ineligibleWidth = this.font.width(rawIneligibleText);

        if (mouseX >= ineligibleLeft && mouseX <= ineligibleLeft + ineligibleWidth && mouseY >= textTop && mouseY <= textTop + textHeight) {
            int babyCount = this.menu.getLiveBabyIneligibleCount();
            int unconfiguredCount = this.menu.getLiveUnconfiguredIneligibleCount();

            List<Component> tooltipLines = new ArrayList<>();

            if (babyCount > 0 || unconfiguredCount > 0) {
                tooltipLines.add(Component.translatable("gui.mobmarket.tooltip.ineligible_header"));

                if (babyCount > 0) {
                    tooltipLines.add(Component.translatable("gui.mobmarket.tooltip.ineligible_baby", babyCount));
                }
                if (unconfiguredCount > 0) {
                    tooltipLines.add(Component.translatable("gui.mobmarket.tooltip.ineligible_unconfigured", unconfiguredCount));
                }
            } else {
                tooltipLines.add(Component.translatable("gui.mobmarket.ineligible_count"));
            }

            guiGraphics.renderComponentTooltip(this.font, tooltipLines, mouseX, mouseY);
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }
}