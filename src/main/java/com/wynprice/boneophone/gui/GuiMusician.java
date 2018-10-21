package com.wynprice.boneophone.gui;

import com.google.common.collect.Lists;
import com.wynprice.boneophone.SkeletalBand;
import com.wynprice.boneophone.network.C4SkeletonChangeType;
import com.wynprice.boneophone.network.C6SkeletonChangeChannel;
import com.wynprice.boneophone.types.MusicianTypeFactory;
import net.minecraft.client.gui.GuiPageButtonList;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class GuiMusician extends GuiScreen {

    private final int entityID;
    private final Supplier<MusicianTypeFactory> typeGetter;
    private final IntSupplier channelSupplier;
    private GuiTextField channelField;

    private GuiSelectList musicianTypes;


    public GuiMusician(int entityID, Supplier<MusicianTypeFactory> typeGetter, IntSupplier channelSupplier) {
        this.entityID = entityID;
        this.typeGetter = typeGetter;
        this.channelSupplier = channelSupplier;
    }

    @Override
    public void initGui() {

        List<MusicianTypeEntry> list = Lists.newArrayList();
        MusicianTypeFactory activeType = this.typeGetter.get();
        MusicianTypeEntry active = null;
        for (MusicianTypeFactory type : SkeletalBand.MUSICIAN_REGISTRY) {
            list.add(type == activeType ? active = new MusicianTypeEntry(type) : new MusicianTypeEntry(type));
        }

        this.musicianTypes = new GuiSelectList(20, 20, this.width / 2 - 30, 20, 5, () -> list);
        this.musicianTypes.setActive(active);

        this.channelField = new GuiTextField(0, mc.fontRenderer, this.width / 2 + 10, 21, this.width / 2 - 30, 18);
        this.channelField.setValidator(s -> (s != null && s.isEmpty()) || StringUtils.isNumeric(s));
        this.channelField.setText(String.valueOf(this.channelSupplier.getAsInt()));
        this.channelField.setGuiResponder(new GuiPageButtonList.GuiResponder() {
            @Override
            public void setEntryValue(int id, boolean value) {
            }

            @Override
            public void setEntryValue(int id, float value) {
            }

            @Override
            public void setEntryValue(int id, String value) {
                if(!value.isEmpty()) {
                    SkeletalBand.NETWORK.sendToServer(new C6SkeletonChangeChannel(GuiMusician.this.entityID, Integer.valueOf(value)));
                }
            }
        });

        super.initGui();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.musicianTypes.render(mouseX, mouseY);
        this.channelField.drawTextBox();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.musicianTypes.mouseClicked(mouseX, mouseY, mouseButton);
        this.channelField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        this.musicianTypes.handleMouseInput();
    }

    @Override
    public void handleKeyboardInput() throws IOException {
        super.handleKeyboardInput();
        if(!this.channelField.isFocused()) {
            this.musicianTypes.handleKeyboardInput();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        this.channelField.textboxKeyTyped(typedChar, keyCode);
    }

    private class MusicianTypeEntry implements GuiSelectList.SelectListEntry {

        private final MusicianTypeFactory entry;

        private MusicianTypeEntry(MusicianTypeFactory entry) {
            this.entry = entry;
        }

        @Override
        public void draw(int x, int y) {
            mc.fontRenderer.drawString(this.getSearch(), x + 21, y + 6, -1);
        }

        @Override
        public String getSearch() {
            return Objects.requireNonNull(entry.getRegistryName()).toString();
        }

        @Override
        public void onClicked(int relMouseX, int relMouseY) {
            SkeletalBand.NETWORK.sendToServer(new C4SkeletonChangeType(GuiMusician.this.entityID, this.entry));
        }
    }
}
