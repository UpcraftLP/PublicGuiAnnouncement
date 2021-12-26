package subaraki.pga.util;

import com.google.common.collect.Lists;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import subaraki.pga.mod.ScreenMod;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

@EventBusSubscriber(modid = ScreenMod.MODID, bus = Bus.MOD)
public class ScreenPackReader extends SimplePreparableReloadListener<ArrayList<JsonObject>> {

    private static HashMap<String, ScreenEntry> mappedScreens = new HashMap<>();

    @SubscribeEvent
    public static void registerReloadListener(RegisterClientReloadListenersEvent event) {

        event.registerReloadListener(new ScreenPackReader());
    }


    @Override
    protected ArrayList<JsonObject> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {

        ArrayList<JsonObject> theJsonFiles = Lists.newArrayList();
        try {

            Collection<ResourceLocation> jsonfiles = resourceManager.listResources("load_screens", (filename) -> filename.endsWith(".json"));

            List<Resource> jsons = new ArrayList<>();

            for (ResourceLocation resLoc : jsonfiles) {
                jsons.addAll(Minecraft.getInstance().getResourceManager().getResources(resLoc));
            }

            Gson gson = new GsonBuilder().create();

            for (Resource res : jsons) {
                InputStream stream = res.getInputStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                JsonElement je = gson.fromJson(reader, JsonElement.class);
                JsonObject json = je.getAsJsonObject();

                if (json.has("screens")) {
                    theJsonFiles.add(json);
                }
            }
        } catch (IOException e) {
            ScreenMod.LOG.warn("************************************");
            ScreenMod.LOG.warn("!*!*!*!*!");
            ScreenMod.LOG.warn("No Screens Detected. You will not be able to use ");
            ScreenMod.LOG.warn("the Public Gui Announcement Mod correctly.");
            ScreenMod.LOG.warn("Make sure to select or set some in the resourcepack gui !");
            ScreenMod.LOG.warn("Or verify your painting json in assets/any_modid/load_screens  !");
            ScreenMod.LOG.warn("!*!*!*!*!");
            ScreenMod.LOG.warn("************************************");

            e.printStackTrace();
        }
        return theJsonFiles;
    }

    @Override
    protected void apply(ArrayList<JsonObject> o, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        if (o != null && !o.isEmpty()) {

            Runnable run = () -> {
                for (JsonObject json : o)
                    if (json.has("screens")) {
                        JsonArray array = json.getAsJsonArray("screens");
                        for (int i = 0; i < array.size(); i++) {

                            JsonObject jsonObject = array.get(i).getAsJsonObject();

                            String fullName = jsonObject.get("class").getAsString();

                            String path = jsonObject.get("texture").getAsString();

                            int sizeX = 0;
                            int sizeY = 0;
                            int texX = 0;
                            int texY = 0;

                            if (jsonObject.has("size")) {
                                JsonArray list = jsonObject.getAsJsonArray("size");
                                if (list.size() == 2) {
                                    sizeX = list.get(0).getAsInt();
                                    sizeY = list.get(1).getAsInt();
                                }
                            }

                            if (jsonObject.has("texSize")) {
                                JsonArray list = jsonObject.getAsJsonArray("texSize");
                                if (list.size() == 2) {
                                    texX = list.get(0).getAsInt();
                                    texY = list.get(1).getAsInt();
                                }
                            }

                            if (jsonObject.has("fullSize")) {
                                int size = jsonObject.get("fullSize").getAsInt();
                                sizeX = sizeY = texX = texY = size;
                            }

                            ScreenEntry entry = new ScreenEntry(fullName, path, sizeX, sizeY, texX, texY);
                            ScreenMod.LOG.info(String.format("Loaded %s for %s : file size %d x %d , tex size %d x %d", entry.getResLoc(), entry.getRefName(),
                                    entry.getTexX(), entry.getTexY(), entry.getSizeX(), entry.getSizeY()));

                            mappedScreens.put(entry.getRefName(), entry);

                        }
                    }
            };
            run.run();
        }
    }

    public static ScreenEntry getEntryForSimpleClassName(String simpleclassname) {

        if (mappedScreens.containsKey(simpleclassname))
            return mappedScreens.get(simpleclassname);

        return null;
    }
}