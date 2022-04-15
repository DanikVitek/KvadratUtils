package com.danikvitek.kvadratutils.utils.annotations;

import com.danikvitek.kvadratutils.Main;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigVar {
    String value();
    String file_name() default "config.yml";

    class Manager {
        private static final List<Class<?>> reg = new ArrayList<>();

        public static void register(Class<?> c) {
            reg.add(c);
        }

        public static void unregister(Class<?> c) {
            reg.remove(c);
        }

        public static void update() {
            for (Class<?> c: reg) {
                for (Field f: c.getDeclaredFields()) {
                    if (f.isAnnotationPresent(ConfigVar.class)) {
                        try {
                            f.setAccessible(true);

                            if (!Main.getInstance().getDataFolder().exists())
                                Main.getInstance().getDataFolder().mkdirs();
                            File configFile = new File(Main.getInstance().getDataFolder(), f.getAnnotation(ConfigVar.class).file_name());
                            if (!configFile.exists())
                                configFile.createNewFile();
                            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

                            f.set(f, config.get(f.getAnnotation(ConfigVar.class).value()));
                        } catch (IllegalAccessException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
