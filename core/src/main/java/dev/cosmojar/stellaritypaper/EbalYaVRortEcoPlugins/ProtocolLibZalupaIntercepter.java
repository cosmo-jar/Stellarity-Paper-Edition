package dev.cosmojar.stellaritypaper.EbalYaVRortEcoPlugins;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import dev.cosmojar.stellaritypaper.StellarityPaperPlugin;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ProtocolLibZalupaIntercepter {

    private static PacketAdapter adapterInstance;

    private ProtocolLibZalupaIntercepter() {
    }

    public static synchronized void vkluchit(final StellarityPaperPlugin plugin) {
        if (adapterInstance != null) {
            return;
        }

        try {
            final ProtocolManager manager = ProtocolLibrary.getProtocolManager();
            final ProtocolLibZalupaAdapter adapter = new ProtocolLibZalupaAdapter(plugin);
            manager.addPacketListener(adapter);
            adapterInstance = adapter;
            plugin.getLogger().info("ProtocolLib hook successfully registered!");
        } catch (final Throwable t) {
            plugin.getLogger().warning("Failed to start the ProtocolLib interceptor.: " + t.getMessage());
        }
    }

    public static synchronized void vykluchit() {
        if (adapterInstance == null) {
            return;
        }
        try {
            final ProtocolManager manager = ProtocolLibrary.getProtocolManager();
            manager.removePacketListener(adapterInstance);
        } catch (final Throwable ignored) {
        }
        adapterInstance = null;
    }

    private static final class ProtocolLibZalupaAdapter extends PacketAdapter {

        public ProtocolLibZalupaAdapter(final StellarityPaperPlugin plugin) {
            super(plugin, ListenerPriority.MONITOR,
                    PacketType.Play.Server.SET_SLOT,
                    PacketType.Play.Server.WINDOW_ITEMS,
                    PacketType.Play.Server.ENTITY_EQUIPMENT);
        }

        @Override
        public void onPacketSending(final PacketEvent event) {
            otmenasuka(event);
        }

        private void otmenasuka(final PacketEvent event) {
            final PacketContainer packet = event.getPacket();
            final PacketType type = event.getPacketType();

            try {
                if (type == PacketType.Play.Server.SET_SLOT) {
                    final StructureModifier<ItemStack> itemMod = packet.getItemModifier();
                    if (itemMod != null && itemMod.size() > 0) {
                        final ItemStack item = itemMod.read(0);
                        if (ecohook.nadoLiChinitEtuHuynu(item)) {
                            itemMod.write(0, ecohook.pochiniLoreBlyat(item));
                        }
                    }
                } else if (type == PacketType.Play.Server.WINDOW_ITEMS) {
                    final StructureModifier<List<ItemStack>> listMod = packet.getItemListModifier();
                    if (listMod != null && listMod.size() > 0) {
                        final List<ItemStack> items = listMod.read(0);
                        if (items != null) {
                            boolean izmeneno = false;
                            final List<ItemStack> fixedList = new ArrayList<>(items.size());
                            for (final ItemStack it : items) {
                                if (ecohook.nadoLiChinitEtuHuynu(it)) {
                                    fixedList.add(ecohook.pochiniLoreBlyat(it));
                                    izmeneno = true;
                                } else {
                                    fixedList.add(it);
                                }
                            }
                            if (izmeneno) {
                                listMod.write(0, fixedList);
                            }
                        }
                    }
                    final StructureModifier<ItemStack> itemMod = packet.getItemModifier();
                    if (itemMod != null && itemMod.size() > 0) {
                        final ItemStack carried = itemMod.read(0);
                        if (ecohook.nadoLiChinitEtuHuynu(carried)) {
                            itemMod.write(0, ecohook.pochiniLoreBlyat(carried));
                        }
                    }
                } else if (type == PacketType.Play.Server.ENTITY_EQUIPMENT) {
                    final StructureModifier<List<Pair<EnumWrappers.ItemSlot, ItemStack>>> pairMod = packet.getSlotStackPairLists();
                    if (pairMod != null && pairMod.size() > 0) {
                        final List<Pair<EnumWrappers.ItemSlot, ItemStack>> pairs = pairMod.read(0);
                        if (pairs != null) {
                            boolean izmeneno = false;
                            final List<Pair<EnumWrappers.ItemSlot, ItemStack>> fixedPairs = new ArrayList<>(pairs.size());
                            for (final Pair<EnumWrappers.ItemSlot, ItemStack> pair : pairs) {
                                final ItemStack it = pair.getSecond();
                                if (ecohook.nadoLiChinitEtuHuynu(it)) {
                                    fixedPairs.add(new Pair<>(pair.getFirst(), ecohook.pochiniLoreBlyat(it)));
                                    izmeneno = true;
                                } else {
                                    fixedPairs.add(pair);
                                }
                            }
                            if (izmeneno) {
                                pairMod.write(0, fixedPairs);
                            }
                        }
                    }
                }
            } catch (final Throwable ignored) {
            }
        }
    }
}
