package fr.crazycat256.cafouillagedupe.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class CafouillageDupe extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<List<Item>> dupeItems = sgGeneral.add(new ItemListSetting.Builder()
        .name("items")
        .description("Items to dupe.")
        .defaultValue(Arrays.asList(Items.SHULKER_BOX, Items.WHITE_SHULKER_BOX, Items.ORANGE_SHULKER_BOX, Items.MAGENTA_SHULKER_BOX,
            Items.LIGHT_BLUE_SHULKER_BOX, Items.YELLOW_SHULKER_BOX, Items.LIME_SHULKER_BOX, Items.PINK_SHULKER_BOX,
            Items.GRAY_SHULKER_BOX, Items.LIGHT_GRAY_SHULKER_BOX, Items.CYAN_SHULKER_BOX, Items.PURPLE_SHULKER_BOX,
            Items.BLUE_SHULKER_BOX, Items.BROWN_SHULKER_BOX, Items.GREEN_SHULKER_BOX, Items.RED_SHULKER_BOX, Items.BLACK_SHULKER_BOX))
        .build()
    );

    private final Setting<Boolean> allowInventory = sgGeneral.add(new BoolSetting.Builder()
        .name("allow-inventory")
        .description("Allow to use items from your inventory (may possibly kick you for sending too many packets).")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> renderPlace = sgRender.add(new BoolSetting.Builder()
        .name("render-place")
        .description("Renders the item frames where the items will be placed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> placeColor = sgRender.add(new ColorSetting.Builder()
        .name("place-color")
        .description("The color of the item frame where the item will be placed.")
        .defaultValue(new Color(0, 255, 0, 32))
        .visible(renderPlace::get)
        .build()
    );

    private final Setting<Boolean> renderDrop = sgRender.add(new BoolSetting.Builder()
        .name("render-drop")
        .description("Renders the item frames where the items will be dropped.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> dropColor = sgRender.add(new ColorSetting.Builder()
        .name("drop-color")
        .description("The color of the item frame where the item will be dropped.")
        .defaultValue(new Color(255, 0, 0, 32))
        .visible(renderDrop::get)
        .build()
    );

    public CafouillageDupe() {
        super(Categories.Misc, "cafouillage-dupe", "Automate Cafouillage item frame dupe.");
    }

    private final List<ItemFrameEntity> emptyItemFrames = new ArrayList<>();
    private final List<ItemFrameEntity> toDropitemFrames = new ArrayList<>();

    private final List<ItemFrameEntity> dontHit = new ArrayList<>();

    @Override
    public void onActivate() {
        emptyItemFrames.clear();
        toDropitemFrames.clear();
        dontHit.clear();
    }

    @Override
    public void onDeactivate() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        MeteorExecutor.execute(() -> {
            try {Thread.sleep(50);}
            catch (InterruptedException e) {e.printStackTrace();}

            List<ItemFrameEntity> itemFrames = mc.world.getEntitiesByClass(ItemFrameEntity.class, mc.player.getBoundingBox().expand(4), entity -> entity.getHeldItemStack().getItem() != Items.AIR && !dontHit.contains(entity));
            for (ItemFrameEntity itemFrame : itemFrames) {
                mc.interactionManager.attackEntity(mc.player, itemFrame);
            }
        });
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        PlayerInventory inv = mc.player.getInventory();

        emptyItemFrames.clear();
        emptyItemFrames.addAll(mc.world.getEntitiesByClass(ItemFrameEntity.class, mc.player.getBoundingBox().expand(4), entity -> entity.getHeldItemStack().getItem() == Items.AIR));

        itemPlaceLoop:
        for (ItemFrameEntity emptyItemFrame: emptyItemFrames) {
            if (dupeItems.get().contains(inv.getMainHandStack().getItem())) {
                mc.interactionManager.interactEntity(mc.player, emptyItemFrame, Hand.MAIN_HAND);
                dontHit.remove(emptyItemFrame);
                continue;
            }
            for (int i = 0; i < 9; i++) {
                if (dupeItems.get().contains(inv.getStack(i).getItem())) {
                    InvUtils.swap(i, true);
                    mc.interactionManager.interactEntity(mc.player, emptyItemFrame, Hand.MAIN_HAND);
                    dontHit.remove(emptyItemFrame);
                    continue itemPlaceLoop;
                }
            }
            if (allowInventory.get()) {
                for (int i = 0; i < inv.size(); i++) {
                    if (dupeItems.get().contains(inv.getStack(i).getItem())) {
                        InvUtils.move().from(i).toHotbar(inv.selectedSlot);
                        mc.interactionManager.interactEntity(mc.player, emptyItemFrame, Hand.MAIN_HAND);
                        dontHit.remove(emptyItemFrame);
                    }
                }
            }
        }

        toDropitemFrames.clear();
        toDropitemFrames.addAll(mc.world.getEntitiesByClass(ItemFrameEntity.class, mc.player.getBoundingBox().expand(4), entity -> dupeItems.get().contains(entity.getHeldItemStack().getItem())));

        for (ItemFrameEntity itemFrame: toDropitemFrames) {
            if (!dontHit.contains(itemFrame)) {
                mc.interactionManager.attackEntity(mc.player, itemFrame);
                dontHit.add(itemFrame);
            }
        }
    }

    private void putInChest(ScreenHandler handler, List<Integer> slots) {
        for (int slot : slots) {
            if (mc.currentScreen == null || !Utils.canUpdate()) break;
            if (dupeItems.get().contains(handler.getSlot(slot).getStack().getItem())) {
                InvUtils.quickMove().slotId(slot);
                try{
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        mc.player.closeHandledScreen();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (renderPlace.get()) {
            for (ItemFrameEntity itemFrame : emptyItemFrames) {
                renderItemFrame(event.renderer, itemFrame, placeColor.get());
            }
        }

        if (renderDrop.get()) {
            for (ItemFrameEntity itemFrame : toDropitemFrames) {
                renderItemFrame(event.renderer, itemFrame, dropColor.get());
            }
        }
    }

    private void renderItemFrame(Renderer3D renderer, ItemFrameEntity itemFrame, Color color) {
        Vec3d pos = itemFrame.getPos();
        renderer.boxSides(pos.x-0.25, pos.y-0.25, pos.z-0.25, pos.x+0.25, pos.y+0.25, pos.z+0.25, color, 0);
    }

}
