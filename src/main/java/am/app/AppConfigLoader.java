/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package am.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import org.slf4j.LoggerFactory;
import am.model.Volume;

/**
 * Load configuration information from a properties file to an {@link AppConfig} object.
 *
 */
public final class AppConfigLoader
{
  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AppConfigLoader.class);
  private static final String LOG_DIR = "logDir";

  private AppConfigLoader()
  {
    // prevent instantiation
  }

  private static void loadVolumes(final AppConfig config, final Properties props)
  {
    final List<Volume> volumes = config.getVolumes();
    int volNr = 1;
    boolean found = true;
    do
    {
      final String key = "volume" + volNr;
      final Object value = props.remove(key);
      if (value == null)
      {
        found = false;
      }
      else
      {
        final Volume vol = new Volume();
        vol.setPath(value.toString());
        volumes.add(vol);
        volNr++;
      }
    }
    while (found);
    LOGGER.debug(config.msg("init.debug.loaded_volumes", volumes.size()));
  }

  public static void interpretProperties(final AppConfig config)
  {
    final Properties props = config.getProperties();
    final LoggingHandler loggingHandler = config.getLoggingHandler();
    if (props.containsKey(LOG_DIR))
    {
      final Object logDir = props.remove(LOG_DIR);
      final String path = logDir.toString();
      final File dir = new File(path);
      if (dir.isDirectory())
      {
        loggingHandler.setLogDirectory(dir);
        loggingHandler.initializeFile(config);
      }
      else
      {
        LOGGER.error(config.msg("init.error.invalid_log_directory", path));
      }
    }
    loadVolumes(config, props);
  }

  public static void loadConfig(final AppConfig config)
  {
    final String fileName = config.getConfigFileName();
    File file;
    if (fileName == null)
    {
      final File dir = new File(System.getProperty("user.home"));
      file = new File(dir, ".am.properties");
    }
    else
    {
      file = new File(fileName);
    }

    if (file.exists())
    {
      BufferedReader reader = null;
      try
      {
        reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
        final Properties props = new Properties();
        props.load(reader);
        config.setProperties(props);
        LOGGER.debug(config.msg("init.info.success_load_configuration", fileName, props.size()));
      }
      catch (final IOException e)
      {
        LOGGER.error(config.msg("init.error.failure_load_configuration", fileName, e.getMessage()));
      }
      finally
      {
        try
        {
          if (reader != null)
          {
            reader.close();
          }
        }
        catch (final IOException e)
        {
          LOGGER.error(config.msg("init.error.failure_close_configuration", fileName, e.getMessage()));
        }
      }
    }
    else
    {
      LOGGER.info(config.msg("init.info.skip_load_configuration", fileName));
    }
  }
}