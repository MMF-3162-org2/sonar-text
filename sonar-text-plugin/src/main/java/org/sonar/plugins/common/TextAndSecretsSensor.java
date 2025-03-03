/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.common;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.SonarProduct;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.plugins.secrets.SecretsCheckList;
import org.sonar.plugins.secrets.SecretsRulesDefinition;
import org.sonar.plugins.secrets.api.SpecificationBasedCheck;
import org.sonar.plugins.secrets.api.SpecificationLoader;
import org.sonar.plugins.secrets.api.task.ExecutorServiceManager;
import org.sonar.plugins.text.TextCheckList;
import org.sonar.plugins.text.TextRuleDefinition;

public class TextAndSecretsSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(TextAndSecretsSensor.class);
  public static final String EXCLUDED_FILE_SUFFIXES_KEY = "sonar.text.excluded.file.suffixes";
  public static final String TEXT_INCLUSIONS_KEY = "sonar.text.inclusions";
  public static final String TEXT_INCLUSIONS_DEFAULT_VALUE = "**/*.sh,**/*.bash,**/*.zsh,**/*.ksh,**/*.ps1,**/*.properties," +
    "**/*.conf,**/*.pem,**/*.config,.env,.aws/config";
  private static final String ANALYZE_ALL_FILES_KEY = "sonar.text.analyzeAllFiles";
  public static final String REGEX_MATCH_TIMEOUT_KEY = "sonar.text.regex.timeout.match";
  public static final String REGEX_EXECUTION_TIMEOUT_KEY = "sonar.text.regex.timeout.execution";
  public static final String TEXT_CATEGORY = "Secrets";

  private static final FilePredicate LANGUAGE_FILE_PREDICATE = inputFile -> inputFile.language() != null;

  protected final CheckFactory checkFactory;

  private final GitSupplier gitSupplier = new GitSupplier();

  protected DurationStatistics durationStatistics;

  public TextAndSecretsSensor(CheckFactory checkFactory) {
    this.checkFactory = checkFactory;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name("TextAndSecretsSensor")
      .createIssuesForRuleRepositories(TextRuleDefinition.REPOSITORY_KEY, SecretsRulesDefinition.REPOSITORY_KEY)
      .processesFilesIndependently();
  }

  @Override
  public void execute(SensorContext sensorContext) {
    // Retrieve list of checks
    List<Check> activeChecks = getActiveChecks();
    durationStatistics = new DurationStatistics(sensorContext.config());
    initializeSpecificationBasedChecks(activeChecks);
    if (activeChecks.isEmpty()) {
      return;
    }

    // Retrieve list of files to analyse using the right FilePredicate
    boolean analyzeAllFiles = isSonarLintContext(sensorContext) || analyzeAllFiles(sensorContext);
    var notBinaryFilePredicate = notBinaryFilePredicate(sensorContext);
    var filePredicate = constructFilePredicate(sensorContext, notBinaryFilePredicate, analyzeAllFiles);

    List<InputFile> inputFiles = getInputFiles(sensorContext, filePredicate);
    if (inputFiles.isEmpty()) {
      return;
    }

    configureRegexEngineTimeout(sensorContext, REGEX_MATCH_TIMEOUT_KEY, ExecutorServiceManager::setTimeoutMs);
    configureRegexEngineTimeout(sensorContext, REGEX_EXECUTION_TIMEOUT_KEY, ExecutorServiceManager::setUninterruptibleTimeoutMs);

    Analyzer.analyzeFiles(sensorContext, activeChecks, notBinaryFilePredicate, analyzeAllFiles, inputFiles);
    durationStatistics.log();
  }

  private FilePredicate constructFilePredicate(SensorContext sensorContext, FilePredicate notBinaryFilePredicate, boolean analyzeAllFiles) {
    if (analyzeAllFiles) {
      // if we're in a sonarlint context, we return this predicate as well
      return notBinaryFilePredicate;
    }
    var trackedByGitPredicate = new GitTrackedFilePredicate(getGitSupplier());
    if (!trackedByGitPredicate.isGitStatusSuccessful()) {
      return LANGUAGE_FILE_PREDICATE;
    }
    FilePredicates predicates = sensorContext.fileSystem().predicates();
    // Retrieve list of files to analyse using the right FilePredicate
    var pathPatternPredicate = includedPathPatternsFilePredicate(sensorContext);

    return predicates.and(
      predicates.or(LANGUAGE_FILE_PREDICATE, pathPatternPredicate),
      trackedByGitPredicate);
  }

  /**
   * Blacklist approach: provide a predicate that exclude file that are considered as not-binary file.
   * Example: for 'exe', 'txt' and 'unknown', it will return true for 'txt' and 'unknown'
   * List of binary extension to exclude are provided by configuration key {@link TextAndSecretsSensor#EXCLUDED_FILE_SUFFIXES_KEY}
   */
  private static NotBinaryFilePredicate notBinaryFilePredicate(SensorContext sensorContext) {
    return new NotBinaryFilePredicate(sensorContext.config().getStringArray(TextAndSecretsSensor.EXCLUDED_FILE_SUFFIXES_KEY));
  }

  /**
   * Whitelist approach: provide a predicate that include file that are considered as text file.
   * Example: for 'exe', 'txt' and 'unknown', it will return true for 'txt'
   * List of path patterns to include are provided by configuration key {@link TextAndSecretsSensor#TEXT_INCLUSIONS_KEY}
   */
  private static FilePredicate includedPathPatternsFilePredicate(SensorContext sensorContext) {
    String[] includedPathPatterns = sensorContext.config().getStringArray(TEXT_INCLUSIONS_KEY);
    if (includedPathPatterns.length == 0) {
      return sensorContext.fileSystem().predicates().none();
    }

    List<FilePredicate> pathPatternsPredicates = new ArrayList<>();
    for (String pathPattern : includedPathPatterns) {
      var filePredicate = sensorContext.fileSystem().predicates().matchesPathPattern(pathPattern);
      pathPatternsPredicates.add(filePredicate);
    }
    return sensorContext.fileSystem().predicates().or(pathPatternsPredicates);
  }

  private static boolean isSonarLintContext(SensorContext sensorContext) {
    return sensorContext.runtime().getProduct() == SonarProduct.SONARLINT;
  }

  private static boolean analyzeAllFiles(SensorContext sensorContext) {
    return "true".equals(sensorContext.config().get(ANALYZE_ALL_FILES_KEY).orElse("false"));
  }

  /**
   * In SonarLint context we want to analyze all non-binary input files, even when they are not analyzed or assigned to a language.
   * To avoid analyzing all non-binary files to reduce time and memory consumption in a non SonarLint context only files assigned to a
   * language OR file with a text extension are analyzed.
   */
  private static List<InputFile> getInputFiles(SensorContext sensorContext, FilePredicate filePredicate) {
    List<InputFile> inputFiles = new ArrayList<>();
    var fileSystem = sensorContext.fileSystem();
    for (InputFile inputFile : fileSystem.inputFiles(filePredicate)) {
      inputFiles.add(inputFile);
    }
    return inputFiles;
  }

  protected List<Check> getActiveChecks() {
    List<Check> checks = new ArrayList<>(checkFactory.<Check>create(TextRuleDefinition.REPOSITORY_KEY)
      .addAnnotatedChecks(new TextCheckList().checks()).all());

    List<Class<?>> secretChecks = new SecretsCheckList().checks();
    checks.addAll(checkFactory.<Check>create(SecretsRulesDefinition.REPOSITORY_KEY)
      .addAnnotatedChecks(secretChecks).all());
    return checks;
  }

  protected void initializeSpecificationBasedChecks(List<Check> checks) {
    var specificationLoader = constructSpecificationLoader();
    Map<URI, List<TextRange>> reportedIssuesForCtx = new HashMap<>();
    for (Check activeCheck : checks) {
      if (activeCheck instanceof SpecificationBasedCheck) {
        ((SpecificationBasedCheck) activeCheck).initialize(specificationLoader, reportedIssuesForCtx, durationStatistics);
      }
    }
  }

  private static void configureRegexEngineTimeout(SensorContext sensorContext, String key, Consumer<Integer> setTimeoutMs) {
    try {
      Optional<Integer> valueAsInt = sensorContext.config().getInt(key);
      valueAsInt.ifPresent(setTimeoutMs);
    } catch (IllegalStateException e) {
      // provided value not in the expected format - do nothing
      LOG.debug("Provided value with key \"{}\" is not parseable as an integer", key, e);
    }
  }

  protected SpecificationLoader constructSpecificationLoader() {
    return new SpecificationLoader();
  }

  public GitSupplier getGitSupplier() {
    return gitSupplier;
  }
}
