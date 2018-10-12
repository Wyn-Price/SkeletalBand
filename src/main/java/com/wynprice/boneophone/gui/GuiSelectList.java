package com.wynprice.boneophone.gui;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

public class GuiSelectList {

    private static Minecraft mc = Minecraft.getMinecraft();

    public static final int CELL_WIDTH = 150;
    public static final int CELL_HEIGHT = 20;

    public static final int CELL_MAX = 5;

    private final int xPos;
    private final int yPos;

    private boolean open;
    private int scroll;

    private SelectListEntry active;

    private final Supplier<List<SelectListEntry>> listSupplier;

    public GuiSelectList(int xPos, int yPos, Supplier<List<SelectListEntry>> listSupplier) {
        this.xPos = xPos;
        this.yPos = yPos;
        this.listSupplier = listSupplier;
    }

    public void render(int mouseX, int mouseY) {
        int relX = mouseX - this.xPos;
        int relY = mouseY - this.yPos;

        List<SelectListEntry> entries = this.listSupplier.get();

        int listedCells = Math.min(entries.size(), CELL_MAX);

        int height = CELL_HEIGHT + (this.open ? listedCells * CELL_HEIGHT : 0);
        int borderSize = 1;
        int borderColor = -1;
        int insideColor = 0xFF000000;
        int insideSelectionColor = 0xFF303030;
        int highlightColor = 0x2299bbff;
        mc.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        RenderHelper.enableGUIStandardItemLighting();

        Gui.drawRect(this.xPos, this.yPos, this.xPos + CELL_WIDTH, this.yPos + CELL_HEIGHT, insideColor);

        if(this.active != null) {
            this.active.draw(this.xPos, this.yPos);
        }

        if(this.open) {
            for (int i = 0; i < listedCells; i++) {
                int actual = i + this.scroll;
                int yStart = this.yPos + CELL_HEIGHT * (i + 1);
                Gui.drawRect(this.xPos, yStart, this.xPos + CELL_WIDTH, yStart + CELL_HEIGHT, insideSelectionColor);
                Gui.drawRect(this.xPos, yStart, this.xPos + CELL_WIDTH, yStart + borderSize, borderColor);
                entries.get(actual).draw(this.xPos, yStart);
            }
        }
        Gui.drawRect(this.xPos, this.yPos, this.xPos + CELL_WIDTH, this.yPos + borderSize, borderColor);
        Gui.drawRect(this.xPos, this.yPos + height, this.xPos + CELL_WIDTH, this.yPos + height - borderSize, borderColor);
        Gui.drawRect(this.xPos, this.yPos, this.xPos + borderSize, this.yPos + height, borderColor);
        Gui.drawRect(this.xPos + CELL_WIDTH, this.yPos, this.xPos + CELL_WIDTH - borderSize, this.yPos + height, borderColor);
        if(relX > 0 && relY > 0) {
            if(relX <= CELL_WIDTH){
                if (relY <= CELL_HEIGHT) {
                    Gui.drawRect(this.xPos, this.yPos, this.xPos + CELL_WIDTH, this.yPos + CELL_HEIGHT, highlightColor);
                } else if(this.open) {
                    for (int i = 0; i < Math.min(CELL_MAX, entries.size()); i++) {
                        if(relY <= CELL_HEIGHT * (i + 2)) {
                            int yStart = this.yPos + CELL_HEIGHT * (i + 1);
                            Gui.drawRect(this.xPos, yStart, this.xPos + CELL_WIDTH, yStart + CELL_HEIGHT, highlightColor);
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
                if(relX <= CELL_WIDTH ) {
                    if(relY <= CELL_HEIGHT) {
                        this.open = !this.open;
                        return;
                    } else if(this.open){
                        for (int i = 0; i < entries.size(); i++) {
                            int i1 = i + this.scroll;
                            if(relY <= CELL_HEIGHT * (i1 + 2)) {
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
            if(relX <= CELL_WIDTH ) {
                if(relY <= CELL_HEIGHT) {
                    return true;
                } else if(this.open){
                    return relY <= CELL_HEIGHT * (Math.min(this.listSupplier.get().size(), CELL_MAX) + 1);
                }
            }
        }
        return false;
    }

    public void scroll(int amount) {
        this.scroll -= amount;
        this.scroll = MathHelper.clamp(this.scroll, 0, Math.max(this.listSupplier.get().size() - CELL_MAX, 0));
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
