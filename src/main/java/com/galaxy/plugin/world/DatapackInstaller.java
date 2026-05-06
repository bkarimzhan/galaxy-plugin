package com.galaxy.plugin.world;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

public final class DatapackInstaller {

    private DatapackInstaller() {}

    private static final String ROOT = "galaxy-datapack/";

    public static void installFor(Plugin plugin, String levelNameWorld) {
        Logger log = plugin.getLogger();
        File container = Bukkit.getWorldContainer();
        File dpDir = new File(container, levelNameWorld + "/datapacks/galaxy");
        int copied = 0;
        try {
            URL jarUrl = DatapackInstaller.class.getProtectionDomain().getCodeSource().getLocation();
            try (JarFile jar = new JarFile(new File(jarUrl.toURI()))) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry e = entries.nextElement();
                    if (e.isDirectory()) continue;
                    String n = e.getName();
                    if (!n.startsWith(ROOT)) continue;
                    String relative = n.substring(ROOT.length());
                    File target = new File(dpDir, relative);
                    target.getParentFile().mkdirs();
                    try (InputStream in = jar.getInputStream(e)) {
                        Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        copied++;
                    }
                }
            }
            log.info("Datapack 'galaxy' installed at " + dpDir.getAbsolutePath() + " (" + copied + " files).");
        } catch (Exception ex) {
            log.severe("Failed to install galaxy datapack into '" + levelNameWorld + "': " + ex.getMessage());
        }
    }
}
