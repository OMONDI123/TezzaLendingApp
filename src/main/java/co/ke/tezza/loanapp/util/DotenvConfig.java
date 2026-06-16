package co.ke.tezza.loanapp.util;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.util.HashMap;
import java.util.Map;

public class DotenvConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        try {
            // Load .env file from project root
            Dotenv dotenv = Dotenv.configure()
                    .directory("./")
                    .ignoreIfMissing()
                    .load();
            
            // Create a map to hold all properties
            Map<String, Object> envMap = new HashMap<>();
            
            // Add all entries from .env to the map
            dotenv.entries().forEach(entry -> {
                envMap.put(entry.getKey(), entry.getValue());
                // Also set as system property for Spring to pick up
                System.setProperty(entry.getKey(), entry.getValue());
            });
            
            // Get the environment and property sources
            ConfigurableEnvironment environment = applicationContext.getEnvironment();
            MutablePropertySources propertySources = environment.getPropertySources();
            
            // Add the map as a property source (addFirst is available in MutablePropertySources)
            propertySources.addFirst(new MapPropertySource("dotenv", envMap));
            
            System.out.println("✅ .env file loaded successfully!");
            System.out.println("Loaded properties: " + envMap.keySet());
            
        } catch (Exception e) {
            System.err.println("❌ Error loading .env file: " + e.getMessage());
            System.err.println("Make sure .env file exists in the project root directory: " + 
                             new java.io.File(".").getAbsolutePath());
        }
    }
}