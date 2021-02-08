package com.dfsek.biome;

import com.dfsek.tectonic.abstraction.AbstractConfigLoader;
import com.dfsek.tectonic.annotations.Abstractable;
import com.dfsek.tectonic.annotations.Default;
import com.dfsek.tectonic.annotations.Value;
import com.dfsek.tectonic.config.ConfigTemplate;
import com.dfsek.tectonic.config.ValidatedConfigTemplate;
import com.dfsek.tectonic.exception.ConfigException;
import com.dfsek.tectonic.exception.LoadException;
import com.dfsek.tectonic.loading.ConfigLoader;
import com.dfsek.tectonic.loading.TypeRegistry;
import com.dfsek.terra.api.core.TerraPlugin;
import com.dfsek.terra.api.math.ProbabilityCollection;
import com.dfsek.terra.api.platform.handle.ItemHandle;
import com.dfsek.terra.api.platform.handle.WorldHandle;
import com.dfsek.terra.api.platform.world.Biome;
import com.dfsek.terra.api.platform.world.World;
import com.dfsek.terra.api.util.seeded.NoiseSeeded;
import com.dfsek.terra.api.world.biome.Generator;
import com.dfsek.terra.biome.TerraBiome;
import com.dfsek.terra.biome.provider.BiomeProvider;
import com.dfsek.terra.config.GenericLoaders;
import com.dfsek.terra.config.PluginConfig;
import com.dfsek.terra.config.fileloaders.FolderLoader;
import com.dfsek.terra.config.fileloaders.Loader;
import com.dfsek.terra.config.fileloaders.ZIPLoader;
import com.dfsek.terra.config.lang.Language;
import com.dfsek.terra.config.loaders.ProbabilityCollectionLoader;
import com.dfsek.terra.config.loaders.config.biome.BiomeProviderBuilderLoader;
import com.dfsek.terra.config.loaders.config.sampler.NoiseSamplerBuilderLoader;
import com.dfsek.terra.config.pack.ConfigPack;
import com.dfsek.terra.config.templates.AbstractableTemplate;
import com.dfsek.terra.debug.DebugLogger;
import com.dfsek.terra.registry.BiomeRegistry;
import com.dfsek.terra.registry.ConfigRegistry;
import com.dfsek.terra.world.TerraWorld;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;
import java.util.zip.ZipFile;

public class BiomeTool {
    private static final TerraPlugin MAIN = new TerraPlugin() {
        @Override
        public WorldHandle getWorldHandle() {
            return null;
        }

        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public TerraWorld getWorld(World world) {
            return null;
        }

        @Override
        public Logger getLogger() {
            return Logger.getLogger("Terra");
        }

        @Override
        public PluginConfig getTerraConfig() {
            return new PluginConfig();
        }

        @Override
        public File getDataFolder() {
            return null;
        }

        @Override
        public boolean isDebug() {
            return false;
        }

        @Override
        public Language getLanguage() {
            return null;
        }

        @Override
        public ConfigRegistry getRegistry() {
            return null;
        }

        @Override
        public void reload() {

        }

        @Override
        public ItemHandle getItemHandle() {
            return null;
        }

        @Override
        public void saveDefaultConfig() {

        }

        @Override
        public String platformName() {
            return null;
        }

        @Override
        public DebugLogger getDebugLogger() {
            return new DebugLogger(Logger.getLogger("Terra"));
        }

        @Override
        public void register(TypeRegistry registry) {

        }
    };


    private static BiomeProvider getProvider(long seed) throws ConfigException, IOException {
        System.out.println(seed);
        System.out.println("Searching dir: " + new File(".").getAbsolutePath());
        File pack = new File("./pack");
        Loader folderLoader;
        if(pack.exists()) {
            folderLoader = new FolderLoader(pack.toPath());
        } else {
            File zip = new File("./pack.zip");
            if(!zip.exists()) throw new LoadException("No config pack found.");
            ZipFile zipFile = new ZipFile(zip);
            folderLoader = new ZIPLoader(zipFile);
        }


        AbstractConfigLoader loader = new AbstractConfigLoader();
        new GenericLoaders(null).register(loader);

        BiomeRegistry biomeRegistry = new BiomeRegistry();
        folderLoader.open("biomes", ".yml").then(inputStreams -> ConfigPack.buildAll((template, main) -> template, biomeRegistry, loader.load(inputStreams, TestBiome::new), MAIN));

        BiomeProviderTemplate template = new BiomeProviderTemplate();
        ConfigLoader pipeLoader = new ConfigLoader()
                .registerLoader(BiomeProvider.BiomeProviderBuilder.class, new BiomeProviderBuilderLoader(MAIN, biomeRegistry, folderLoader))
                .registerLoader(ProbabilityCollection.class, new ProbabilityCollectionLoader())
                .registerLoader(TerraBiome.class, biomeRegistry);
        new GenericLoaders(null).register(pipeLoader);

        pipeLoader.registerLoader(NoiseSeeded.class, new NoiseSamplerBuilderLoader());

        pipeLoader.load(template, folderLoader.get("pack.yml"));
        return template.getBiomeProviderBuilder().build(seed);
    }

    public static void main(String... args) throws ConfigException, IOException {
        JFrame testFrame = new JFrame("Biome Viewer");


        final BiomeProvider[] provider = {getProvider(2403)};


        int size = 1024;
        final BufferedImage[] image = {new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)};
        for(int x = 0; x < size; x++) {
            for(int z = 0; z < size; z++) {
                image[0].setRGB(x, z, provider[0].getBiome(x, z).getColor());
            }
        }

        JLabel img = new JLabel(new ImageIcon(image[0]));

        testFrame.add(img);
        testFrame.pack();
        img.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                BufferedImage newImage = deepCopy(image[0]);
                Graphics graphics = newImage.getGraphics();
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, 512, 24);
                graphics.setColor(Color.BLACK);
                graphics.setFont(new Font("Monospace", Font.BOLD, 20));
                graphics.drawString(provider[0].getBiome(e.getX(), e.getY()).toString(), 0, 20);

                graphics.setColor(Color.WHITE);
                graphics.fillOval(e.getX() - 2, e.getY() - 2, 12, 12);
                graphics.setColor(Color.BLACK);
                graphics.fillOval(e.getX(), e.getY(), 8, 8);

                img.setIcon(new ImageIcon(newImage));
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });

        testFrame.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                if(e.getKeyChar() == 's') {
                    long l = System.nanoTime();
                    try {
                        provider[0] = getProvider(ThreadLocalRandom.current().nextLong());
                    } catch(ConfigException | IOException configException) {
                        configException.printStackTrace();
                    }
                    image[0] = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                    for(int x = 0; x < size; x++) {
                        for(int z = 0; z < size; z++) {
                            image[0].setRGB(x, z, provider[0].getBiome(x, z).getColor());
                        }
                    }
                    long n = System.nanoTime();
                    double t = n - l;
                    System.out.println("Time: " + t / 1000000 + "ms");
                    img.setIcon(new ImageIcon(image[0]));
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {

            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });

        testFrame.setResizable(false);
        testFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        testFrame.setVisible(true);

    }

    private static BufferedImage deepCopy(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    private static final class BiomeProviderTemplate implements ConfigTemplate {
        @Value("biomes")
        BiomeProvider.BiomeProviderBuilder biomeProviderBuilder;

        public BiomeProvider.BiomeProviderBuilder getBiomeProviderBuilder() {
            return biomeProviderBuilder;
        }
    }

    private static final class TestBiome extends AbstractableTemplate implements TerraBiome, ValidatedConfigTemplate {

        @Value("color")
        @Default
        @Abstractable
        private int color;

        @Value("tags")
        @Abstractable
        @Default
        private Set<String> tags = new HashSet<>();

        @Value("id")
        private String id;

        @Override
        public ProbabilityCollection<Biome> getVanillaBiomes() {
            return null;
        }

        @Override
        public Generator getGenerator(World w) {
            return null;
        }

        @Override
        public int getColor() {
            return color;
        }

        @Override
        public Set<String> getTags() {
            return tags;
        }

        @Override
        public boolean validate() {
            color |= 0xff000000; // Alpha adjustment
            tags.add("BIOME:" + id);
            return true;
        }

        @Override
        public String getID() {
            return id;
        }

        @Override
        public String toString() {
            return id;
        }
    }
}
