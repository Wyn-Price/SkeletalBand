package com.wynprice.boneophone.gui;

import com.google.common.collect.Lists;
import com.wynprice.boneophone.SkeletalBand;
import com.wynprice.boneophone.midi.MidiStream;
import com.wynprice.boneophone.network.C11SkeletonChangeVolume;
import com.wynprice.boneophone.network.C4SkeletonChangeType;
import com.wynprice.boneophone.network.C6SkeletonChangeChannel;
import com.wynprice.boneophone.network.C8SkeletonChangeTrack;
import com.wynprice.boneophone.types.ConductorType;
import com.wynprice.boneophone.types.MusicianTypeFactory;
import net.minecraft.client.gui.GuiPageButtonList;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.fml.client.config.GuiSlider;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class GuiMusician extends GuiScreen {

    private final int entityID;
    private final Supplier<MusicianTypeFactory> typeGetter;
    private final ConductorType conductor;
    private final IntSupplier channelSupplier;
    private final IntSupplier trackIDSupplier;
    private final DoubleSupplier volumeSupplier;
    private GuiTextField channelField;

    private GuiSelectList musicianTypes;
    private GuiSelectList trackList;

    private GuiSlider volumeSlider;

    public GuiMusician(int entityID, Supplier<MusicianTypeFactory> typeGetter, ConductorType conductor, IntSupplier channelSupplier, IntSupplier trackIDSupplier, DoubleSupplier volumeSupplier) {
        this.entityID = entityID;
        this.typeGetter = typeGetter;
        this.conductor = conductor;
        this.channelSupplier = channelSupplier;
        this.trackIDSupplier = trackIDSupplier;
        this.volumeSupplier = volumeSupplier;
    }

    @Override
    public void initGui() {
        List<MusicianTypeEntry> typeList = Lists.newArrayList();
        MusicianTypeFactory activeType = this.typeGetter.get();
        MusicianTypeEntry active = null;
        for (MusicianTypeFactory type : SkeletalBand.MUSICIAN_REGISTRY) {
            typeList.add(type == activeType ? active = new MusicianTypeEntry(type) : new MusicianTypeEntry(type));
        }

        this.musicianTypes = new GuiSelectList(20, 20, this.width / 2 - 30, 20, 5, () -> typeList);
        this.musicianTypes.setActive(active);

        List<TrackEntry> trackList = Lists.newArrayList();
        int trackID = this.trackIDSupplier.getAsInt();
        TrackEntry activeTrack = null;

        if(this.conductor != null) {
            List<MidiStream.MidiTrack> tracks = this.conductor.getCurrentlyPlaying().getTracks();
            for (int i = 0; i < tracks.size(); i++) {
                MidiStream.MidiTrack track = tracks.get(i);
                TrackEntry entry = new TrackEntry(this.conductor, track);
                trackList.add(i == trackID ? activeTrack = entry : entry);
            }
        }

        this.trackList = new GuiSelectList(this.width / 2 + 10, 50, this.width / 2 - 30, 20, 5, () -> trackList);
        this.trackList.setActive(activeTrack);

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

        this.volumeSlider = new GuiSlider(3, 20, this.height - 30, this.width / 2 - 30, 20, "Volume: ", "%", 0, 100, this.volumeSupplier.getAsDouble() * 100D, true, true, slider -> SkeletalBand.NETWORK.sendToServer(new C11SkeletonChangeVolume(this.entityID, (float) (slider.getValue() / 100D))));

        super.initGui();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.musicianTypes.render(mouseX, mouseY);
        this.trackList.render(mouseX, mouseY);
        this.channelField.drawTextBox();
        this.volumeSlider.drawButton(mc, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.musicianTypes.mouseClicked(mouseX, mouseY, mouseButton);
        this.trackList.mouseClicked(mouseX, mouseY, mouseButton);
        this.channelField.mouseClicked(mouseX, mouseY, mouseButton);
        this.volumeSlider.mousePressed(mc, mouseX, mouseY);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        this.volumeSlider.mouseReleased(mouseX, mouseY);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        this.musicianTypes.handleMouseInput();
        this.trackList.handleMouseInput();
    }

    @Override
    public void handleKeyboardInput() throws IOException {
        super.handleKeyboardInput();
        this.musicianTypes.handleKeyboardInput();
        this.trackList.handleKeyboardInput();
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

    private class TrackEntry implements GuiSelectList.SelectListEntry {

        private final String trackName;
        @Nonnull
        private final ConductorType conductor;
        private final MidiStream.MidiTrack track;

        private TrackEntry(@Nonnull ConductorType conductor, MidiStream.MidiTrack track) {
            this.conductor = conductor;
            this.track = track;
            this.trackName = (track.name.isEmpty() ? "Unknown Track" :  track.name.trim()) + " (" + track.totalNotes + " Notes)";
        }

        @Override
        public void draw(int x, int y) {
            mc.fontRenderer.drawString(this.getSearch(), x + 21, y + 6, -1);

            int assigned = this.conductor.getAmountPlaying(this.track);
            mc.fontRenderer.drawString(String.valueOf(assigned), x + GuiMusician.this.width / 2 - 40 - mc.fontRenderer.getStringWidth(String.valueOf(assigned)), y + 6, -1);

        }

        @Override
        public String getSearch() {
            return this.trackName;
        }

        @Override
        public void onClicked(int relMouseX, int relMouseY) {
            SkeletalBand.NETWORK.sendToServer(new C8SkeletonChangeTrack(GuiMusician.this.entityID, this.track.id));
        }
    }
}
