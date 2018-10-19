package com.wynprice.boneophone.gui;

import com.google.common.collect.Lists;
import com.sun.javafx.util.Utils;
import com.wynprice.boneophone.SkeletalBand;
import com.wynprice.boneophone.midi.MidiFileHandler;
import com.wynprice.boneophone.network.C1UploadMidiFile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import org.lwjgl.opengl.GL11;

import javax.sound.midi.InvalidMidiDataException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

public class GuiSelectMidis extends GuiScreen {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private final int entityID;

    private GuiSelectList midiSelect;

    private GuiButton playButton;

    private String error = "";

    public GuiSelectMidis(int entityID) {
        this.entityID = entityID;
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
        this.midiSelect.render(mouseX, mouseY);
        this.drawCenteredString(mc.fontRenderer, this.error, this.width / 2, this.height / 4 + 40, 0xFFFF5555);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.midiSelect.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        this.midiSelect.handleMouseInput();
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
                    this.error = e.getLocalizedMessage();
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

   class MidiEntry implements GuiSelectList.SelectListEntry {

        private final File file;
        private final double[] rgb;

        MidiEntry(File file, double[] rgb) {
            this.file = file;
            this.rgb = rgb;
        }

        @Override
        public void draw(int x, int y) {
            String fileName = this.file.getPath().substring("midis/".length());
            if(fileName.endsWith(".mid")) {
                fileName = fileName.substring(0, fileName.length() - ".mid".length());
            } else if(fileName.endsWith(".midi")) {
                fileName = fileName.substring(0, fileName.length() - ".midi".length());
            }

            mc.fontRenderer.drawString(fileName, x + 21, y + 6, -1);

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
    }
}
