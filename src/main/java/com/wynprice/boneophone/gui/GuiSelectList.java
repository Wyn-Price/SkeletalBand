package com.wynprice.boneophone.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

public class GuiSelectList {

    private static Minecraft mc = Minecraft.getMinecraft();

    public final int width;
    public final int cellHeight;

    public final int cellMax;

    private final int xPos;
    private final int yPos;

    private boolean open;
    private int scroll;

    private SelectListEntry active;

    private final Supplier<List<SelectListEntry>> listSupplier;

    public GuiSelectList(int xPos, int yPos, int width, int cellHeight, int cellMax, Supplier<List<SelectListEntry>> listSupplier) {
        this.xPos = xPos;
        this.yPos = yPos;
        this.width = width;
        this.cellHeight = cellHeight;
        this.cellMax = cellMax;
        this.listSupplier = listSupplier;
    }

    public void render(int mouseX, int mouseY) {
        int relX = mouseX - this.xPos;
        int relY = mouseY - this.yPos;

        List<SelectListEntry> entries = this.listSupplier.get();

        int listedCells = Math.min(entries.size(), cellMax);

        int height = cellHeight + (this.open ? listedCells * cellHeight : 0);
        int borderSize = 1;
        int borderColor = -1;
        int insideColor = 0xFF000000;
        int insideSelectionColor = 0xFF303030;
        int highlightColor = 0x2299bbff;
        mc.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        RenderHelper.enableGUIStandardItemLighting();

        Gui.drawRect(this.xPos, this.yPos, this.xPos + width, this.yPos + cellHeight, insideColor);

        if(this.active != null) {
            this.active.draw(this.xPos, this.yPos);
        }

        if(this.open) {
            for (int i = 0; i < listedCells; i++) {
                int actual = i + this.scroll;
                int yStart = this.yPos + cellHeight * (i + 1);
                Gui.drawRect(this.xPos, yStart, this.xPos + width, yStart + cellHeight, insideSelectionColor);
                Gui.drawRect(this.xPos, yStart, this.xPos + width, yStart + borderSize, borderColor);
                entries.get(actual).draw(this.xPos, yStart);
            }
        }
        Gui.drawRect(this.xPos, this.yPos, this.xPos + width, this.yPos + borderSize, borderColor);
        Gui.drawRect(this.xPos, this.yPos + height, this.xPos + width, this.yPos + height - borderSize, borderColor);
        Gui.drawRect(this.xPos, this.yPos, this.xPos + borderSize, this.yPos + height, borderColor);
        Gui.drawRect(this.xPos + width, this.yPos, this.xPos + width - borderSize, this.yPos + height, borderColor);
        if(relX > 0 && relY > 0) {
            if(relX <= width){
                if (relY <= cellHeight) {
                    Gui.drawRect(this.xPos, this.yPos, this.xPos + width, this.yPos + cellHeight, highlightColor);
                } else if(this.open) {
                    for (int i = 0; i < Math.min(this.cellMax, entries.size()); i++) {
                        if(relY <= cellHeight * (i + 2)) {
                            int yStart = this.yPos + cellHeight * (i + 1);
                            Gui.drawRect(this.xPos, yStart, this.xPos + width, yStart + cellHeight, highlightColor);
                            break;
                        }
                    }
                }
            }
        }
        RenderHelper.disableStandardItemLighting();
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if(mouseButton == 0) {
            List<SelectListEntry> entries = this.listSupplier.get();

            int relX = mouseX - this.xPos;
            int relY = mouseY - this.yPos;
            if(relX > 0 && relY > 0) {
                if(relX <= width) {
                    if(relY <= cellHeight) {
                        this.open = !this.open;
                        return;
                    } else if(this.open){
                        for (int i = 0; i < entries.size(); i++) {
                            if(relY <= cellHeight * (i + 2)) {
                                int i1 = i + this.scroll;
                                entries.get(i1).onClicked(relX, relY);
                                this.active = entries.get(i1);
                                break;
                            }
                        }
                    }
                }
            }
        }
        this.open = false;
    }

    public void handleMouseInput() {
        int mouseInput = Mouse.getEventDWheel();
        if(mouseInput != 0) {
            this.scroll(mouseInput < 0 ? -1 : 1);
        }
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        int relX = mouseX - this.xPos;
        int relY = mouseY - this.yPos;
        if(relX > 0 && relY > 0) {
            if(relX <= width) {
                if(relY <= cellHeight) {
                    return true;
                } else if(this.open){
                    return relY <= cellHeight * (Math.min(this.listSupplier.get().size(), cellMax) + 1);
                }
            }
        }
        return false;
    }

    public void scroll(int amount) {
        this.scroll -= amount;
        this.scroll = MathHelper.clamp(this.scroll, 0, Math.max(this.listSupplier.get().size() - cellMax, 0));
    }

    public SelectListEntry getActive() {
        return this.active;
    }

    interface SelectListEntry {
        void draw(int x, int y);

        default void onClicked(int relMouseX, int relMouseY) {

        }
    }
}
