package fr.crazycat256.cafouillagedupe;

import fr.crazycat256.cafouillagedupe.commands.ItemFrameDrop;
import fr.crazycat256.cafouillagedupe.modules.CafouillageDupe;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class Addon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        LOG.info("Initializing CafouillageDupe");

        // Modules
        Modules.get().add(new CafouillageDupe());

        // Commands
        Commands.add(new ItemFrameDrop());

    }

    @Override
    public String getPackage() {
        return "fr.crazycat256.cafouillagedupe";
    }
}
