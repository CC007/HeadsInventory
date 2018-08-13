/*
 * The MIT License
 *
 * Copyright 2018 Rik Schaaf aka CC007 (http://coolcat007.nl/).
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.cc007.headsinventory.locale;

import com.github.cc007.headsinventory.HeadsInventory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.LocaleUtils;

/**
 *
 * @author Rik Schaaf aka CC007 (http://coolcat007.nl/)
 */
public class Translator {
    
    private final Locale locale;
    private final String bundleName;
    private final ClassLoader classLoader;
    private final ResourceBundle translations;
    private final ResourceBundle fallbackTranslations;
    
    private static InputStream getTranslationsFileStream(String fileName, ClassLoader classLoader){
        try {
            URL url = classLoader.getResource(fileName);

            if (url == null) {
                return null;
            }
            
            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            return connection.getInputStream();
        } catch (IOException ex) {
            return null;
        }
    }
    
    public Translator(String bundleName, Locale locale, ClassLoader classLoader) {
        this.locale = locale;
        this.bundleName = bundleName;
        this.classLoader = classLoader;
        
        ResourceBundle translations = null;
        ResourceBundle fallbackTranslations = null;
        try {
            InputStream translationsStream = getTranslationsFileStream(
                    "locale/" + bundleName + "_" + locale.toString() + ".properties", 
                    classLoader
            );
            translations = new PropertyResourceBundle(translationsStream);
            InputStream fallbackTranslationsStream = getTranslationsFileStream(
                    "locale/" + bundleName + ".properties", 
                    classLoader
            );
            fallbackTranslations = new PropertyResourceBundle(fallbackTranslationsStream);
        } catch (IOException ex) {
            HeadsInventory.getPlugin().getLogger().log(Level.SEVERE, null, ex);
        }
        this.translations = translations;
        this.fallbackTranslations = fallbackTranslations;
    }

    /**
     * Get the value of locale
     *
     * @return the value of locale
     */
    public Locale getLocale() {
        return locale;
    }
    
    /**
     * Get the value of bundleName
     *
     * @return the value of bundleName
     */
    public String getBundleName() {
        return bundleName;
    }
    
    /**
     * Get the value of classLoader
     *
     * @return the value of classLoader
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }
    
    /**
     * Default translator, based on the locale. You can optionally provide variables. These will be 
     * 
     * @param key the identifier of the translatable text
     * @param variables the values of variables that are not going to be translated (like a name in a sentence)
     * @return the translated text
     */
    public String getText(String key, String... variables) {
        String translated = getText0(key);
        translated = String.format(locale, translated, variables);
        return translated;
    }
    
    /**
     * Default translator, based on the locale. You can optionally provide variables. These will be 
     * 
     * @param key the identifier of the translatable text
     * @param variables the values of named variables that are not going to be translated (like a name in a sentence)
     * @return the translated text
     */
    public String getText(String key, Map<String, String> variables) {
        String translated = getText0(key);
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            translated = translated.replaceAll("%" + entry.getKey() + "%", entry.getValue());
        }
        return translated;
    }
    
    private String getText0(String key) {
        String translated = key;
        try{
            translated = translations.getString(key);
        } catch (MissingResourceException | NullPointerException ex) {
            try{
                translated = fallbackTranslations.getString(key);
            } catch (MissingResourceException | NullPointerException ex2) {
                HeadsInventory.getPlugin().getLogger().warning("Missing translation in locale and fallback! Using the translation key instead.");
            }
        }
        return translated;
    }

}
