package org.spigotmc;

import com.google.common.collect.ImmutableList;
import lombok.AllArgsConstructor;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.EntityMinecartCommandBlock;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntityCommandBlock;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.command.CraftBlockCommandSender;
import org.bukkit.craftbukkit.entity.CraftMinecartCommand;
import org.bukkit.craftbukkit.entity.CraftPlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class VanillaCommandWrapper {
    public static final HashSet<String> allowedCommands = new HashSet<>();

    public static int dispatch(CommandSender sender, String commandLine) {
        int pos = commandLine.indexOf(' ');
        if (pos == -1) {
            pos = commandLine.length();
        }
        String name = commandLine.substring(0, pos);
        if (!allowedCommands.contains(name)) {
            return -1;
        }
        if (!sender.hasPermission("bukkit.command." + name)) {
            sender.sendMessage(ChatColor.RED + "You do not have permission for this command");
            return 0;
        }
        ICommandSender listener = getListener(sender);
        if (listener == null) {
            return -1;
        }
        return MinecraftServer.getServer().getCommandManager().executeCommand(listener, commandLine);
    }

    @SuppressWarnings("unchecked")
    public static List<String> complete(CommandSender sender, String commandLine) {
        int pos = commandLine.indexOf(' ');
        if (pos == -1) {
            List<String> completions = new ArrayList<>();
            commandLine = commandLine.toLowerCase();
            for (String command : allowedCommands) {
                if (command.startsWith(commandLine) && sender.hasPermission("bukkit.command." + command)) {
                    completions.add("/" + command);
                }
            }
            return completions;
        }
        String name = commandLine.substring(0, pos);
        if (!allowedCommands.contains(name) || !sender.hasPermission("bukkit.command." + name)) {
            return ImmutableList.of();
        }
        net.minecraft.command.ICommandSender listener = getListener(sender);
        if (listener == null) {
            return ImmutableList.of();
        }
        return MinecraftServer.getServer().getCommandManager().getPossibleCommands(listener, commandLine);
    }

    private static ICommandSender getListener(CommandSender sender) {
        if (sender instanceof CraftPlayer) {
            return new PlayerListener(((CraftPlayer) sender).getHandle());
        }
        if (sender instanceof CraftBlockCommandSender) {
            CraftBlockCommandSender commandBlock = (CraftBlockCommandSender) sender;
            Block block = commandBlock.getBlock();
            return ((TileEntityCommandBlock) ((CraftWorld) block.getWorld()).getTileEntityAt(block.getX(), block.getY(), block.getZ())).func_145993_a();
        }
        if (sender instanceof CraftMinecartCommand) {
            return ((EntityMinecartCommandBlock) ((CraftMinecartCommand) sender).getHandle()).func_145822_e();
        }
        return new ConsoleListener(sender); // Assume console/rcon
    }

    @AllArgsConstructor
    private static class PlayerListener implements ICommandSender {
        private final ICommandSender handle;

        @Override
        public String getCommandSenderName() {
            return handle.getCommandSenderName();
        }

        @Override
        public IChatComponent func_145748_c_() {
            return handle.func_145748_c_();
        }

        @Override
        public void addChatMessage(net.minecraft.util.IChatComponent iChatBaseComponent) {
            handle.addChatMessage(iChatBaseComponent);
        }

        @Override
        public boolean canCommandSenderUseCommand(int i, String s) {
            return true;
        }

        @Override
        public ChunkCoordinates getPlayerCoordinates() {
            return handle.getPlayerCoordinates();
        }

        @Override
        public World getEntityWorld() {
            return handle.getEntityWorld();
        }
    }

    @AllArgsConstructor
    private static class ConsoleListener implements ICommandSender {
        private final CommandSender sender;

        @Override
        public String getCommandSenderName() {
            return sender.getName();
        }

        @Override
        public IChatComponent func_145748_c_() {
            return new ChatComponentText(getCommandSenderName());
        }

        @Override
        public void addChatMessage(IChatComponent iChatBaseComponent) {
            sender.sendMessage(iChatBaseComponent.getUnformattedTextForChat());
        }

        @Override
        public boolean canCommandSenderUseCommand(int i, String s) {
            return true;
        }

        @Override
        public ChunkCoordinates getPlayerCoordinates() {
            return new ChunkCoordinates(0, 0, 0);
        }

        @Override
        public World getEntityWorld() {
            return MinecraftServer.getServer().getEntityWorld();
        }
    }
}
