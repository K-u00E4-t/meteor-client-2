/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EntityColorListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.render.color.RainbowColors;
import meteordevelopment.meteorclient.utils.render.color.SettingBooleanColor;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Pair;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class EntityColorListSettingScreen extends WindowScreen {
    private final EntityColorListSetting setting;

    private WVerticalList list;
    private final WTextBox filter;

    private String filterText = "";

    private WSection animals, waterAnimals, monsters, ambient, misc;
    private WTable animalsT, waterAnimalsT, monstersT, ambientT, miscT;
    int hasAnimal = 0, hasWaterAnimal = 0, hasMonster = 0, hasAmbient = 0, hasMisc = 0;

    public EntityColorListSettingScreen(GuiTheme theme, EntityColorListSetting setting) {
        super(theme, "Select entities");
        this.setting = setting;

        // Filter
        filter = super.add(theme.textBox("")).minWidth(400).expandX().widget();
        filter.setFocused(true);
        filter.action = () -> {
            filterText = filter.get().trim();

            list.clear();
            initWidgets();
        };

        list = super.add(theme.verticalList()).expandX().widget();

    }

    @Override
    public <W extends WWidget> Cell<W> add(W widget) {
        return list.add(widget);
    }

    @Override
    public void initWidgets() {
        hasAnimal = hasWaterAnimal = hasMonster = hasAmbient = hasMisc = 0;

        for (EntityType<?> entityType : setting.get().keySet()) {
            if (!setting.isActive(entityType)) continue;

            switch (entityType.getSpawnGroup()) {
                case CREATURE -> hasAnimal++;
                case WATER_AMBIENT, WATER_CREATURE, UNDERGROUND_WATER_CREATURE, AXOLOTLS -> hasWaterAnimal++;
                case MONSTER -> hasMonster++;
                case AMBIENT -> hasAmbient++;
                case MISC -> hasMisc++;
            }
        }

        boolean first = animals == null;

        // Animals
        List<EntityType<?>> animalsE = new ArrayList<>();
        WCheckbox animalsC = theme.checkbox(hasAnimal > 0);

        animals = theme.section("Animals", animals != null && animals.isExpanded(), animalsC);
        animalsC.action = () -> tableChecked(animalsE, animalsC.checked);

        Cell<WSection> animalsCell = add(animals).expandX();
        animalsT = animals.add(theme.table()).expandX().widget();

        // Water animals
        List<EntityType<?>> waterAnimalsE = new ArrayList<>();
        WCheckbox waterAnimalsC = theme.checkbox(hasWaterAnimal > 0);

        waterAnimals = theme.section("Water Animals", waterAnimals != null && waterAnimals.isExpanded(), waterAnimalsC);
        waterAnimalsC.action = () -> tableChecked(waterAnimalsE, waterAnimalsC.checked);

        Cell<WSection> waterAnimalsCell = add(waterAnimals).expandX();
        waterAnimalsT = waterAnimals.add(theme.table()).expandX().widget();

        // Monsters
        List<EntityType<?>> monstersE = new ArrayList<>();
        WCheckbox monstersC = theme.checkbox(hasMonster > 0);

        monsters = theme.section("Monsters", monsters != null && monsters.isExpanded(), monstersC);
        monstersC.action = () -> tableChecked(monstersE, monstersC.checked);

        Cell<WSection> monstersCell = add(monsters).expandX();
        monstersT = monsters.add(theme.table()).expandX().widget();

        // Ambient
        List<EntityType<?>> ambientE = new ArrayList<>();
        WCheckbox ambientC = theme.checkbox(hasAmbient > 0);

        ambient = theme.section("Ambient", ambient != null && ambient.isExpanded(), ambientC);
        ambientC.action = () -> tableChecked(ambientE, ambientC.checked);

        Cell<WSection> ambientCell = add(ambient).expandX();
        ambientT = ambient.add(theme.table()).expandX().widget();

        // Misc
        List<EntityType<?>> miscE = new ArrayList<>();
        WCheckbox miscC = theme.checkbox(hasMisc > 0);

        misc = theme.section("Misc", misc != null && misc.isExpanded(), miscC);
        miscC.action = () -> tableChecked(miscE, miscC.checked);

        Cell<WSection> miscCell = add(misc).expandX();
        miscT = misc.add(theme.table()).expandX().widget();

        Consumer<EntityType<?>> entityTypeForEach = entityType -> {
            switch (entityType.getSpawnGroup()) {
                case CREATURE -> {
                    animalsE.add(entityType);
                    addEntityType(animalsT, animalsC, entityType);
                }
                case WATER_AMBIENT, WATER_CREATURE, UNDERGROUND_WATER_CREATURE, AXOLOTLS -> {
                    waterAnimalsE.add(entityType);
                    addEntityType(waterAnimalsT, waterAnimalsC, entityType);
                }
                case MONSTER -> {
                    monstersE.add(entityType);
                    addEntityType(monstersT, monstersC, entityType);
                }
                case AMBIENT -> {
                    ambientE.add(entityType);
                    addEntityType(ambientT, ambientC, entityType);
                }
                case MISC -> {
                    miscE.add(entityType);
                    addEntityType(miscT, miscC, entityType);
                }
            }
        };

        // Sort all entities
        if (filterText.isEmpty()) {
            Registry.ENTITY_TYPE.forEach(entityTypeForEach);
        } else {
            List<Pair<EntityType<?>, Integer>> entities = new ArrayList<>();
            Registry.ENTITY_TYPE.forEach(entity -> {
                int words = Utils.search(Names.get(entity), filterText);
                if (words > 0) entities.add(new Pair<>(entity, words));
            });
            entities.sort(Comparator.comparingInt(value -> -value.getRight()));
            for (Pair<EntityType<?>, Integer> pair : entities) entityTypeForEach.accept(pair.getLeft());
        }

        if (animalsT.cells.size() == 0) list.cells.remove(animalsCell);
        if (waterAnimalsT.cells.size() == 0) list.cells.remove(waterAnimalsCell);
        if (monstersT.cells.size() == 0) list.cells.remove(monstersCell);
        if (ambientT.cells.size() == 0) list.cells.remove(ambientCell);
        if (miscT.cells.size() == 0) list.cells.remove(miscCell);

        if (first) {
            int totalCount = (hasWaterAnimal + waterAnimals.cells.size() + monsters.cells.size() + ambient.cells.size() + misc.cells.size()) / 2;

            if (totalCount <= 20) {
                if (animalsT.cells.size() > 0) animals.setExpanded(true);
                if (waterAnimalsT.cells.size() > 0) waterAnimals.setExpanded(true);
                if (monstersT.cells.size() > 0) monsters.setExpanded(true);
                if (ambientT.cells.size() > 0) ambient.setExpanded(true);
                if (miscT.cells.size() > 0) misc.setExpanded(true);
            }
            else {
                if (animalsT.cells.size() > 0) animals.setExpanded(false);
                if (waterAnimalsT.cells.size() > 0) waterAnimals.setExpanded(false);
                if (monstersT.cells.size() > 0) monsters.setExpanded(false);
                if (ambientT.cells.size() > 0) ambient.setExpanded(false);
                if (miscT.cells.size() > 0) misc.setExpanded(false);
            }
        }
    }

    private void tableChecked(List<EntityType<?>> entityTypes, boolean checked) {
        boolean changed = false;

        for (EntityType<?> entityType : entityTypes) {
            if (checked) {
                setting.activate(entityType);
                changed = true;
            } else {
                if (setting.deactivate(entityType)) {
                    changed = true;
                }
            }
        }

        if (changed) {
            list.clear();
            initWidgets();
            setting.onChanged();
        }
    }

    private void addEntityType(WTable table, WCheckbox tableCheckbox, EntityType<?> entityType) {
        table.add(theme.label(Names.get(entityType)));

        WCheckbox a = table.add(theme.checkbox(setting.isActive(entityType))).expandCellX().right().widget();
        a.action = () -> {
            if (a.checked) {
                setting.activate(entityType);
                switch (entityType.getSpawnGroup()) {
                    case CREATURE -> {
                        if (hasAnimal == 0) tableCheckbox.checked = true;
                        hasAnimal++;
                    }
                    case WATER_AMBIENT, WATER_CREATURE, UNDERGROUND_WATER_CREATURE, AXOLOTLS -> {
                        if (hasWaterAnimal == 0) tableCheckbox.checked = true;
                        hasWaterAnimal++;
                    }
                    case MONSTER -> {
                        if (hasMonster == 0) tableCheckbox.checked = true;
                        hasMonster++;
                    }
                    case AMBIENT -> {
                        if (hasAmbient == 0) tableCheckbox.checked = true;
                        hasAmbient++;
                    }
                    case MISC -> {
                        if (hasMisc == 0) tableCheckbox.checked = true;
                        hasMisc++;
                    }
                }
            } else {
                if (setting.deactivate(entityType)) {
                    switch (entityType.getSpawnGroup()) {
                        case CREATURE -> {
                            hasAnimal--;
                            if (hasAnimal == 0) tableCheckbox.checked = false;
                        }
                        case WATER_AMBIENT, WATER_CREATURE, UNDERGROUND_WATER_CREATURE, AXOLOTLS -> {
                            hasWaterAnimal--;
                            if (hasWaterAnimal == 0) tableCheckbox.checked = false;
                        }
                        case MONSTER -> {
                            hasMonster--;
                            if (hasMonster == 0) tableCheckbox.checked = false;
                        }
                        case AMBIENT -> {
                            hasAmbient--;
                            if (hasAmbient == 0) tableCheckbox.checked = false;
                        }
                        case MISC -> {
                            hasMisc--;
                            if (hasMisc == 0) tableCheckbox.checked = false;
                        }
                    }
                }
            }

            setting.onChanged();
        };
        SettingBooleanColor color = setting.get().get(entityType);

        table.add(theme.quad(Objects.requireNonNullElse(color.getColor(), new SettingColor(0, 0, 0, 0)))).right();

        WButton edit = table.add(theme.button(GuiRenderer.EDIT)).right().widget();
        edit.action = () -> {
            if (color.getColor() == null)
                color.setColor(new SettingColor(255, 255, 255, 255));

            Setting<SettingColor> colorSetting = new ColorSetting("", "", color.getColor(), v -> {
                color.getColor().set(v);
                if (v.rainbow) RainbowColors.add(color.getColor());
            }, null, null);

            mc.setScreen(new ColorSettingScreen(theme, colorSetting));
            list.clear(); // Required because otherwise when you first set the color and exit the quad wouldn't update
            initWidgets();
        };

        WButton reset = table.add(theme.button(GuiRenderer.RESET)).right().widget();
        reset.action = () -> {
            setting.get().remove(entityType);
            list.clear();
            initWidgets();
            setting.onChanged();
        };
        table.row();
    }
}
