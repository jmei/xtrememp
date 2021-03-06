/**
 * Xtreme Media Player a cross-platform media player.
 * Copyright (C) 2005-2008  Besmir Beqiri
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package xtrememp;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xtrememp.util.Log4jProperties;

/**
 *
 * @author Besmir Beqiri
 */
public final class Settings {

    private static final Logger logger = LoggerFactory.getLogger(Settings.class);
    private static final String CACHE_DIR = ".xtrememp";
    private static final String SETTINGS_FILE = "settings.xml";
    private static Properties properties;

    public static int getDefaultPort() {
        return Integer.parseInt(properties.getProperty("xtrememp.defaultPort", "12521"));
    }

    public static int getEqualizerPresetIndex() {
        return Integer.parseInt(properties.getProperty("xtrememp.equilazer.preset.index", "0"));
    }

    public static void setEqualizerPresetIndex(int value) {
        properties.setProperty("xtrememp.equilazer.preset.index", Integer.toString(value));
    }

    public static void setLastView(String lv) {
        properties.setProperty("xtrememp.lastView", lv);
    }

    public static String getLastView() {
        return properties.getProperty("xtrememp.lastView", "PLAYLIST MANAGER");
    }

    public static void setLastDir(String ld) {
        properties.setProperty("xtrememp.lastDir", ld);
    }

    public static String getLastDir() {
        return properties.getProperty("xtrememp.lastDir", System.getProperty("user.dir"));
    }

    public static String getVisualization() {
        return properties.getProperty("xtrememp.visualization", "SPECTROGRAM");
    }

    public static void setVisualization(String visualization) {
        properties.setProperty("xtrememp.visualization", visualization);
    }

    public static int getPlaylistPosition() {
        return Integer.parseInt(properties.getProperty("xtrememp.playlist.position", "0"));
    }

    public static void setPlaylistPosition(int pos) {
        properties.setProperty("xtrememp.playlist.position", Integer.toString(pos));
    }

    public static boolean isMuted() {
        return Boolean.parseBoolean(properties.getProperty("xtrememp.player.audio.muted", Boolean.toString(false)));
    }

    public static void setMuted(boolean mute) {
        properties.setProperty("xtrememp.player.audio.muted", Boolean.toString(mute));
    }

    public static int getGain() {
        return Integer.parseInt(properties.getProperty("xtrememp.player.audio.gain", "100"));
    }

    public static void setGain(int gain) {
        properties.setProperty("xtrememp.player.audio.gain", Integer.toString(gain));
    }

    public static int getPan() {
        return Integer.parseInt(properties.getProperty("xtrememp.player.audio.pan", "0"));
    }

    public static void setPan(int value) {
        properties.setProperty("xtrememp.player.audio.pan", Integer.toString(value));
    }

    public static String getMixerName() {
        return properties.getProperty("xtrememp.player.audio.mixerName", "");
    }

    public static void setMixerName(String name) {
        properties.setProperty("xtrememp.player.audio.mixerName", name);
    }

    public static File getCacheDir() {
        File cacheDir = new File(properties.getProperty("xtrememp.cache.dir", System.getProperty("user.home")), CACHE_DIR);
        if (!cacheDir.exists()) {
            cacheDir.mkdir();
        }
        return cacheDir;
    }

    public static void setCacheDir(File parent) {
        properties.setProperty("xtrememp.cache.dir", parent.getPath());
        // Reload log4j properties
        PropertyConfigurator.configure(new Log4jProperties());
    }

    public static boolean isAutomaticCheckForUpdatesEnabled() {
        return Boolean.parseBoolean(properties.getProperty("xtrememp.update.automatic.checkforupdates", Boolean.toString(true)));
    }

    public static void setAutomaticCheckForUpdatesEnabled(boolean b) {
        properties.setProperty("xtrememp.update.automatic.checkforupdates", Boolean.toString(b));
    }

    /**
     * Gets the bounds of the application main frame in the form of a
     * <code>Rectangle</code> object.
     * 
     * @returna a rectangle indicating this component's bounds
     */
    public static Rectangle getMainFrameBounds() {
        String x = properties.getProperty("xtrememp.mainFrame.x", "200");
        String y = properties.getProperty("xtrememp.mainFrame.y", "200");
        String width = properties.getProperty("xtrememp.mainFrame.width", "538");
        String height = properties.getProperty("xtrememp.mainFrame.height", "460");
        return new Rectangle(Integer.parseInt(x), Integer.parseInt(y), Integer.parseInt(width), Integer.parseInt(height));
    }

    /**
     * Sets the application main frame new size and location.
     * 
     * @param r the bounding rectangle for this component
     */
    public static void setMainFrameBounds(Rectangle r) {
        properties.setProperty("xtrememp.mainFrame.x", Integer.toString(r.x));
        properties.setProperty("xtrememp.mainFrame.y", Integer.toString(r.y));
        properties.setProperty("xtrememp.mainFrame.width", Integer.toString(r.width));
        properties.setProperty("xtrememp.mainFrame.height", Integer.toString(r.height));
    }

    public static boolean containsKey(Object key) {
        return properties.containsKey(key);
    }

    /**
     * Reads all the properties from the settings file.
     */
    public static void loadSettings() {
        properties = new Properties();
        File file = new File(getCacheDir(), SETTINGS_FILE);
        if (file.exists()) {
            try {
                FileInputStream fis = new FileInputStream(file);
                properties.loadFromXML(fis);
                fis.close();
            } catch (IOException ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
    }

    /**
     * Writes all the properties in the settings file.
     */
    public static void storeSettings() {
        try {
            File file = new File(getCacheDir(), SETTINGS_FILE);
            FileOutputStream fos = new FileOutputStream(file);
            properties.storeToXML(fos, "Xtreme Media Player Settings");
            fos.close();
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
}
