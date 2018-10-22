package com.wynprice.boneophone.gui;

import com.google.common.collect.Lists;
import com.wynprice.boneophone.SkeletalBand;
import com.wynprice.boneophone.entity.MusicalSkeleton;
import com.wynprice.boneophone.midi.MidiFileHandler;
import com.wynprice.boneophone.midi.MidiStream;
import com.wynprice.boneophone.network.C4SkeletonChangeType;
import com.wynprice.boneophone.network.C6SkeletonChangeChannel;
import com.wynprice.boneophone.types.ConductorType;
import com.wynprice.boneophone.types.MusicianTypeFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiPageButtonList;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.datafix.fixes.EntityId;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.util.FileUtils;
import org.lwjgl.opengl.GL11;

import javax.sound.midi.InvalidMidiDataException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class GuiSelectMidis extends GuiScreen {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private final int entityID;
    private final Supplier<MusicianTypeFactory> typeGetter;
    private final IntSupplier channelSupplier;

    private GuiSelectList midiSelect;
    private GuiTextField channelField;
    private GuiSelectList musicianTypes;

    private GuiButton playButton;

    private String error = "";

    public GuiSelectMidis(int entityID, Supplier<MusicianTypeFactory> typeGetter, IntSupplier channelSupplier) {
        this.entityID = entityID;
        this.typeGetter = typeGetter;
        this.channelSupplier = channelSupplier;
    }

    @Override
    public void initGui() {
        List<GuiSelectList.SelectListEntry> list = Lists.newArrayList();
        for (File file : MidiFileHandler.getAllStreams()) {
            list.add(new MidiEntry(file, HSBtoRGB(new Random().nextInt(360), 0.7F, 0.7F)));
        }
        this.midiSelect = new GuiSelectList(this.width / 8, this.height / 4, (this.width / 4) * 3, 20, this.height / 40, () -> list);//() -> list

        this.playButton = this.addButton(new GuiButtonExt(0, 7, this.height - 25, this.width / 2 - 10, 20, I18n.format(SkeletalBand.MODID + ".uploadplay")));
        this.addButton(new GuiButtonExt(1, this.width / 2 + 3, this.height - 25, this.width / 2 - 10, 20, I18n.format(SkeletalBand.MODID + ".openmidifolder")));

        List<MusicianTypeEntry> mucicianList = Lists.newArrayList();
        MusicianTypeFactory activeType = this.typeGetter.get();
        MusicianTypeEntry active = null;
        for (MusicianTypeFactory type : SkeletalBand.MUSICIAN_REGISTRY) {
            mucicianList.add(type == activeType ? active = new MusicianTypeEntry(type) : new MusicianTypeEntry(type));
        }

        this.musicianTypes = new GuiSelectList(20, 20, this.width / 2 - 30, 20, 5, () -> mucicianList);
        this.musicianTypes.setActive(active);

        this.channelField = new GuiTextField(1, mc.fontRenderer, this.width / 2 + 10, 5, this.width / 2 - 30, 18);
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
                    SkeletalBand.NETWORK.sendToServer(new C6SkeletonChangeChannel(GuiSelectMidis.this.entityID, Integer.valueOf(value)));
                }
            }
        });

        super.initGui();
    }

    @Override
    public void updateScreen() {
        this.playButton.enabled = this.midiSelect.getActive() != null;
        super.updateScreen();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.drawCenteredString(mc.fontRenderer, this.error, this.width / 2, this.height / 4 + 40, 0xFFFF5555);
        this.midiSelect.render(mouseX, mouseY);
        this.musicianTypes.render(mouseX, mouseY);
        this.channelField.drawTextBox();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.midiSelect.mouseClicked(mouseX, mouseY, mouseButton);
        this.musicianTypes.mouseClicked(mouseX, mouseY, mouseButton);
        this.channelField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        this.musicianTypes.handleMouseInput();
        this.midiSelect.handleMouseInput();
    }

    @Override
    public void handleKeyboardInput() throws IOException {
        super.handleKeyboardInput();
        this.musicianTypes.handleKeyboardInput();
        this.midiSelect.handleKeyboardInput();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        this.channelField.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        super.actionPerformed(button);
        if(button.id == 0 && this.midiSelect.getActive() instanceof MidiEntry) {
            this.error = "";
            try {
                MidiSplitNetworkHandler.sendMidiData(this.entityID, MidiFileHandler.writeMidiFile(((MidiEntry)this.midiSelect.getActive()).file));
            } catch (Exception e) {
                if(e.getCause() != null && e.getCause() instanceof InvalidMidiDataException) {
                    this.error = "Invalid file: " + e.getCause().getMessage(); //Localize
                } else {
                    this.error = e.getClass().getSimpleName() + ": " + e.getLocalizedMessage();
                }
                SkeletalBand.LOGGER.error("Error reading midi file", e);
            }
            if(this.error.isEmpty()) {
                Minecraft.getMinecraft().displayGuiScreen(null);
            }
        } else if(button.id == 1) {
            OpenGlHelper.openFile(MidiFileHandler.folder);
        }
    }

    //Copied from com.sun.javafx.util.Utils class as package renaming was causing issues
    public static double[] HSBtoRGB(double hue, double saturation, double brightness) {
        // normalize the hue
        double normalizedHue = ((hue % 360) + 360) % 360;
        hue = normalizedHue/360;

        double r = 0, g = 0, b = 0;
        if (saturation == 0) {
            r = g = b = brightness;
        } else {
            double h = (hue - Math.floor(hue)) * 6.0;
            double f = h - Math.floor(h);
            double p = brightness * (1.0 - saturation);
            double q = brightness * (1.0 - saturation * f);
            double t = brightness * (1.0 - (saturation * (1.0 - f)));
            switch ((int) h) {
                case 0:
                    r = brightness;
                    g = t;
                    b = p;
                    break;
                case 1:
                    r = q;
                    g = brightness;
                    b = p;
                    break;
                case 2:
                    r = p;
                    g = brightness;
                    b = t;
                    break;
                case 3:
                    r = p;
                    g = q;
                    b = brightness;
                    break;
                case 4:
                    r = t;
                    g = p;
                    b = brightness;
                    break;
                case 5:
                    r = brightness;
                    g = p;
                    b = q;
                    break;
            }
        }
        return new double[]{r, g, b};
    }

    private final static List<String> ACCEPTED_FILE_NAMES = Lists.newArrayList("midi", "mid");

   class MidiEntry implements GuiSelectList.SelectListEntry {

        private final File file;
        private final String displayName;
        private final double[] rgb;

        MidiEntry(File file, double[] rgb) {
            this.file = file;
            this.rgb = rgb;

            String fileName = this.file.getPath().substring("midis/".length());
            if(this.file.getName().lastIndexOf(".") != 0) {
                String ext = FileUtils.getFileExtension(this.file);
                if(ACCEPTED_FILE_NAMES.contains(ext)) {
                    fileName = fileName.substring(0, fileName.length() - ext.length() - 1);
                }
            }
            this.displayName = fileName;

        }

        @Override
        public void draw(int x, int y) {
            mc.fontRenderer.drawString(this.displayName, x + 21, y + 6, -1);

            BufferBuilder buff = Tessellator.getInstance().getBuffer();

            buff.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

            mc.renderEngine.bindTexture(new ResourceLocation("textures/particle/particles.png"));

            int h = 12;

            buff.pos(x + 4, y + 4, 100)         .tex(0, 4/16F)      .color((float)this.rgb[0], (float)this.rgb[1], (float)this.rgb[2], 1.0F).endVertex();
            buff.pos(x + 4, y + 4 + h, 100)     .tex(0, 5/16F)      .color((float)this.rgb[0], (float)this.rgb[1], (float)this.rgb[2], 1.0F).endVertex();
            buff.pos(x + 4 + h, y + 4 + h, 100) .tex(1/16F, 5/16F)  .color((float)this.rgb[0], (float)this.rgb[1], (float)this.rgb[2], 1.0F).endVertex();
            buff.pos(x + 4 + h, y + 4, 100)     .tex(1/16F, 4/16F)  .color((float)this.rgb[0], (float)this.rgb[1], (float)this.rgb[2], 1.0F).endVertex();

            Tessellator.getInstance().draw();
        }

       @Override
       public String getSearch() {
           return this.displayName;
       }
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
            SkeletalBand.NETWORK.sendToServer(new C4SkeletonChangeType(GuiSelectMidis.this.entityID, this.entry));
        }
    }


}
