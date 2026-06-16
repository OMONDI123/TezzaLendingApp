package co.ke.tezza.loanapp.util;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

@Component
public class BuildInfo {
	@PostConstruct
	public void printDetails() {
		printData();
	}

	 public static String getBuildName() {
	        // Method 1: Get from the JAR/WAR file directly
	        String name = getBuildNameFromCodeSource();
	        if (name != null && !name.isEmpty()) return name;
	        
	        // Method 2: Get from classloader resource
	        name = getBuildNameFromClasspath();
	        if (name != null && !name.isEmpty()) return name;
	        
	        // Method 3: Get from manifest
	        name = getBuildNameFromManifest();
	        if (name != null && !name.isEmpty()) return name;
	        
	        return "unknown";
	    }
	    
	    /**
	     * Gets just the base name without version (e.g., "smartSystem" from "smartSystem-0.0.1-SNAPSHOT.jar")
	     */
	    public static String getBaseName() {
	        String fullName = getBuildName();
	        if (fullName == null || fullName.equals("unknown")) return fullName;
	        
	        // Remove version pattern if present
	        return fullName.replaceFirst("-\\d+.*$", "");
	    }
	    
	    /**
	     * Gets the version from the build name (e.g., "0.0.1-SNAPSHOT" from "smartSystem-0.0.1-SNAPSHOT.jar")
	     */
	    public static String getVersion() {
	        String fullName = getBuildName();
	        if (fullName == null || fullName.equals("unknown")) return null;
	        
	        int firstDash = fullName.indexOf('-');
	        if (firstDash > 0) {
	            return fullName.substring(firstDash + 1);
	        }
	        return null;
	    }
	    
	    private static String getBuildNameFromCodeSource() {
	        try {
	            CodeSource codeSource = BuildInfo.class.getProtectionDomain().getCodeSource();
	            
	            if (codeSource != null) {
	                URL location = codeSource.getLocation();
	                
	                if (location != null) {
	                    // Decode the URL to handle spaces and special characters
	                    String decodedPath = URLDecoder.decode(location.getPath(), "UTF-8");
	                    
	                    // Handle different URL protocols
	                    String filePath = null;
	                    
	                    if ("jar".equals(location.getProtocol())) {
	                        // For JAR URLs like: jar:file:/path/to/file.jar!/
	                        if (decodedPath.contains("!")) {
	                            filePath = decodedPath.substring(0, decodedPath.indexOf("!"));
	                            // Remove "file:" prefix if present
	                            if (filePath.startsWith("file:")) {
	                                filePath = filePath.substring(5);
	                            }
	                        }
	                    } else if ("file".equals(location.getProtocol())) {
	                        // For file URLs
	                        filePath = decodedPath;
	                    } else if ("war".equals(location.getProtocol())) {
	                        // For WAR URLs in some containers
	                        if (decodedPath.contains("!")) {
	                            filePath = decodedPath.substring(0, decodedPath.indexOf("!"));
	                        }
	                    }
	                    
	                    if (filePath != null) {
	                        File file = new File(filePath);
	                        
	                        // Check if it's a file (JAR/WAR) or directory (exploded WAR)
	                        if (file.exists()) {
	                            if (file.isFile()) {
	                                String fileName = file.getName();
	                                // Return filename without extension if it's a JAR or WAR
	                                if (fileName.endsWith(".jar") || fileName.endsWith(".war")) {
	                                    return fileName.substring(0, fileName.lastIndexOf("."));
	                                }
	                            } else if (file.isDirectory()) {
	                                // For exploded WAR, try to get the directory name
	                                if (file.getPath().contains("WEB-INF")) {
	                                    // Navigate up to get the webapp name
	                                    File webAppDir = file.getParentFile();
	                                    while (webAppDir != null && !new File(webAppDir, "WEB-INF").exists()) {
	                                        webAppDir = webAppDir.getParentFile();
	                                    }
	                                    if (webAppDir != null) {
	                                        return webAppDir.getName();
	                                    }
	                                }
	                                return file.getName();
	                            }
	                        }
	                    }
	                }
	            }
	        } catch (Exception e) {
	            // Fall through to next method
	        }
	        return null;
	    }
	    
	    private static String getBuildNameFromClasspath() {
	        try {
	            String className = BuildInfo.class.getName().replace('.', '/') + ".class";
	            URL classUrl = BuildInfo.class.getClassLoader().getResource(className);
	            
	            if (classUrl != null) {
	                String urlStr = URLDecoder.decode(classUrl.toString(), "UTF-8");
	                
	                if (urlStr.startsWith("jar:")) {
	                    // Extract from jar:file:/path/to/file.jar!/path/to/class
	                    String jarPath = urlStr.substring(4, urlStr.indexOf("!"));
	                    
	                    // Remove "file:" prefix if present
	                    if (jarPath.startsWith("file:")) {
	                        jarPath = jarPath.substring(5);
	                    }
	                    
	                    // Extract filename
	                    int lastSlash = jarPath.lastIndexOf("/");
	                    if (lastSlash >= 0) {
	                        String fileName = jarPath.substring(lastSlash + 1);
	                        if (fileName.endsWith(".jar") || fileName.endsWith(".war")) {
	                            return fileName.substring(0, fileName.lastIndexOf("."));
	                        }
	                    }
	                } else if (urlStr.startsWith("file:")) {
	                    // Running from classes directory - try to find the JAR/WAR in target/build
	                    String path = urlStr.substring(5);
	                    if (path.contains("/target/classes/")) {
	                        String basePath = path.substring(0, path.indexOf("/target/classes/") + 8);
	                        File targetDir = new File(basePath);
	                        if (targetDir.exists() && targetDir.isDirectory()) {
	                            // Look for any JAR/WAR file
	                            File[] files = targetDir.listFiles((dir, name) -> 
	                                (name.endsWith(".jar") || name.endsWith(".war")) && 
	                                !name.contains("original") &&
	                                !name.contains("javadoc") &&
	                                !name.contains("sources"));
	                            
	                            if (files != null && files.length > 0) {
	                                String fileName = files[0].getName();
	                                return fileName.substring(0, fileName.lastIndexOf("."));
	                            }
	                        }
	                    } else if (path.contains("/build/classes/")) {
	                        String basePath = path.substring(0, path.indexOf("/build/classes/") + 6);
	                        File buildDir = new File(basePath);
	                        if (buildDir.exists() && buildDir.isDirectory()) {
	                            File[] files = buildDir.listFiles((dir, name) -> 
	                                name.endsWith(".jar") || name.endsWith(".war"));
	                            if (files != null && files.length > 0) {
	                                String fileName = files[0].getName();
	                                return fileName.substring(0, fileName.lastIndexOf("."));
	                            }
	                        }
	                    }
	                }
	            }
	        } catch (Exception e) {
	            // Fall through
	        }
	        return null;
	    }
	    
	    private static String getBuildNameFromManifest() {
	        try {
	            Enumeration<URL> resources = BuildInfo.class.getClassLoader()
	                .getResources("META-INF/MANIFEST.MF");
	            
	            while (resources.hasMoreElements()) {
	                URL url = resources.nextElement();
	                String urlStr = url.toString();
	                
	                // Skip JRE system manifests
	                if (!urlStr.contains("jre") && !urlStr.contains("Java Runtime")) {
	                    try (java.io.InputStream is = url.openStream()) {
	                        Manifest manifest = new Manifest(is);
	                        Attributes attrs = manifest.getMainAttributes();
	                        
	                        // Try various manifest attributes that might contain the name
	                        String[] possibleAttributes = {
	                            "Implementation-Title",
	                            "Specification-Title",
	                            "Bundle-Name",
	                            "Application-Name",
	                            "Name",
	                            "Main-Class",
	                            "Start-Class"
	                        };
	                        
	                        for (String attrName : possibleAttributes) {
	                            String value = attrs.getValue(attrName);
	                            if (value != null && !value.isEmpty()) {
	                                // Clean up the value
	                                value = value.trim();
	                                
	                                // If it's a class name, try to extract project name
	                                if (value.contains(".") && (value.contains("Application") || value.contains("Main"))) {
	                                    String[] parts = value.split("\\.");
	                                    for (int i = parts.length - 1; i >= 0; i--) {
	                                        if (parts[i].contains("Application") || parts[i].contains("Main")) {
	                                            String name = parts[i].replace("Application", "").replace("Main", "");
	                                            if (!name.isEmpty()) {
	                                                // Try to get version
	                                                String version = attrs.getValue("Implementation-Version");
	                                                if (version != null && !version.isEmpty()) {
	                                                    return name + "-" + version;
	                                                }
	                                                return name;
	                                            }
	                                        }
	                                    }
	                                }
	                                
	                                // If we got a name and it's not too generic
	                                if (!value.equals("Java") && !value.equals("Runtime") && 
	                                    !value.startsWith("Spring") && !value.startsWith("Starter")) {
	                                    
	                                    // Try to get version
	                                    String version = attrs.getValue("Implementation-Version");
	                                    if (version != null && !version.isEmpty() && !value.contains(version)) {
	                                        return value + "-" + version;
	                                    }
	                                    return value;
	                                }
	                            }
	                        }
	                        
	                        // If we still don't have a name, try to construct from JAR filename in Class-Path
	                        String classPath = attrs.getValue("Class-Path");
	                        if (classPath != null && !classPath.isEmpty()) {
	                            String[] paths = classPath.split(" ");
	                            for (String cp : paths) {
	                                if (cp.contains(".jar") && !cp.contains("spring") && !cp.contains("tomcat")) {
	                                    File jarFile = new File(cp);
	                                    String fileName = jarFile.getName();
	                                    return fileName.substring(0, fileName.lastIndexOf("."));
	                                }
	                            }
	                        }
	                    }
	                }
	            }
	        } catch (Exception e) {
	            // Fall through
	        }
	        return null;
	    }
	    
	    /**
	     * Gets the full path of the running JAR/WAR file
	     */
	    public static String getBuildPath() {
	        try {
	            CodeSource codeSource = BuildInfo.class.getProtectionDomain().getCodeSource();
	            if (codeSource != null) {
	                URL location = codeSource.getLocation();
	                String decodedPath = URLDecoder.decode(location.toString(), "UTF-8");
	                
	                if (decodedPath.startsWith("jar:")) {
	                    decodedPath = decodedPath.substring(4, decodedPath.indexOf("!"));
	                }
	                
	                if (decodedPath.startsWith("file:")) {
	                    decodedPath = decodedPath.substring(5);
	                }
	                
	                return decodedPath;
	            }
	        } catch (Exception e) {
	            // Fall through
	        }
	        return null;
	    }
	    
	    /**
	     * Checks if running from a JAR file
	     */
	    public static boolean isRunningFromJar() {
	        try {
	            CodeSource codeSource = BuildInfo.class.getProtectionDomain().getCodeSource();
	            if (codeSource != null) {
	                URL location = codeSource.getLocation();
	                return "jar".equals(location.getProtocol()) || 
	                       location.toString().contains(".jar!");
	            }
	        } catch (Exception e) {
	            // Fall through
	        }
	        return false;
	    }
	    
	    /**
	     * Checks if running from a WAR file
	     */
	    public static boolean isRunningFromWar() {
	        try {
	            CodeSource codeSource = BuildInfo.class.getProtectionDomain().getCodeSource();
	            if (codeSource != null) {
	                URL location = codeSource.getLocation();
	                String urlStr = location.toString();
	                return urlStr.contains(".war!") || urlStr.contains(".war/");
	            }
	        } catch (Exception e) {
	            // Fall through
	        }
	        return false;
	    }
	    
	    // Test method - completely dynamic, no hardcoded names
	    public static void printData() {
	        System.out.println("=================================");
	        System.out.println("Build Information Test");
	        System.out.println("=================================");
	        
	        System.out.println("Running from JAR: " + isRunningFromJar());
	        System.out.println("Running from WAR: " + isRunningFromWar());
	        System.out.println("Build Path: " + getBuildPath());
	        System.out.println("\nBuild Name (full): " + getBuildName());
	        System.out.println("Base Name: " + getBaseName());
	        System.out.println("Version: " + getVersion());
	        
	        System.out.println("\n=================================");
	    }

}