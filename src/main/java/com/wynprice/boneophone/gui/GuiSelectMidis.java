package com.wynprice.boneophone.gui;

import com.google.common.collect.Lists;
import com.sun.javafx.util.Utils;
import com.wynprice.boneophone.Boneophone;
import com.wynprice.boneophone.midi.MidiFileHandler;
import com.wynprice.boneophone.network.C1UploadMidiFile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

public class GuiSelectMidis extends GuiScreen {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private final int entityID;

    private GuiSelectList midiSelect;

    public GuiSelectMidis(int entityID) {
        this.entityID = entityID;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.midiSelect.render(mouseX, mouseY);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.midiSelect.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleKeyboardInput() throws IOException {
        super.handleKeyboardInput();
        this.midiSelect.handleMouseInput();
    }

    @Override
    public void initGui() {
        List<GuiSelectList.SelectListEntry> list = Lists.newArrayList();
        Random rnd = new Random();
        for (File file : MidiFileHandler.getAllStreams()) {
            list.add(new MidiEntry(file, Utils.HSBtoRGB(rnd.nextDouble() * 360D, 0.7F, 0.7F)));
        }
        this.midiSelect = new GuiSelectList(this.width / 2 - 10, this.height / 2, () -> list);

        this.addButton(new GuiButtonExt(0, this.width / 2 - 100, this.height - 25, "Upload and play"));

        super.initGui();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        super.actionPerformed(button);
        if(button.id == 0 && this.midiSelect.getActive() instanceof MidiEntry) {
            Boneophone.NETWORK.sendToServer(new C1UploadMidiFile(this.entityID, ((MidiEntry)this.midiSelect.getActive()).file));
            Minecraft.getMinecraft().displayGuiScreen(null);
        }
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
            mc.fontRenderer.drawString(this.file.getPath().substring(6), x + 21, y + 6, -1);

            BufferBuilder buff = Tessellator.getInstance().getBuffer();

            buff.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

            mc.renderEngine.bindTexture(new ResourceLocation("textures/particle/particles.png"));

            int h = GuiSelectList.CELL_HEIGHT - 8;


            buff.pos(x + 4, y + 4, 100)         .tex(0, 4/16F)      .color((float)this.rgb[0], (float)this.rgb[1], (float)this.rgb[2], 1.0F).endVertex();
            buff.pos(x + 4, y + 4 + h, 100)     .tex(0, 5/16F)      .color((float)this.rgb[0], (float)this.rgb[1], (float)this.rgb[2], 1.0F).endVertex();
            buff.pos(x + 4 + h, y + 4 + h, 100) .tex(1/16F, 5/16F)  .color((float)this.rgb[0], (float)this.rgb[1], (float)this.rgb[2], 1.0F).endVertex();
            buff.pos(x + 4 + h, y + 4, 100)     .tex(1/16F, 4/16F)  .color((float)this.rgb[0], (float)this.rgb[1], (float)this.rgb[2], 1.0F).endVertex();

            Tessellator.getInstance().draw();
        }
    }
}
