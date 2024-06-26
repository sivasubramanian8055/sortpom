package sortpom;

import java.io.File;
import sortpom.exception.FailureException;
import sortpom.logger.SortPomLogger;
import sortpom.parameter.PluginParameters;
import sortpom.parameter.VerifyFailOnType;
import sortpom.parameter.VerifyFailType;
import sortpom.util.XmlOrderedResult;

/** The implementation of the Mojo (Maven plugin) that sorts the pom file for a Maven project. */
public class SortPomImpl {
  private static final String TEXT_FILE_NOT_SORTED = "The file %s is not sorted";

  private final SortPomService sortPomService;

  private SortPomLogger log;
  private File pomFile;
  private VerifyFailType verifyFailType;
  private VerifyFailOnType verifyFailOn;

  public SortPomImpl() {
    this.sortPomService = new SortPomService();
  }

  public void setup(SortPomLogger log, PluginParameters pluginParameters) {
    this.log = log;
    this.pomFile = pluginParameters.pomFile;
    this.verifyFailType = pluginParameters.verifyFailType;
    this.verifyFailOn = pluginParameters.verifyFailOn;

    sortPomService.setup(log, pluginParameters);

    // Call to the extracted method to warn about deprecated arguments
    warnAboutDeprecatedArguments(pluginParameters);
  }

  // Extracted method to warn about deprecated arguments
  private void warnAboutDeprecatedArguments(PluginParameters pluginParameters) {
    warnAboutDeprecatedValue(
        "sortDependencies",
        pluginParameters.sortDependencies.isDeprecatedValueTrue(),
        pluginParameters.sortDependencies.isDeprecatedValueFalse(),
        "The 'true' value in sortDependencies is not supported anymore, please use value 'groupId,artifactId' instead.",
        "The 'false' value in sortDependencies is not supported anymore, please use empty value '' or omit sortDependencies instead.");
    warnAboutDeprecatedValue(
        "sortDependencyExclusions",
        pluginParameters.sortDependencyExclusions.isDeprecatedValueTrue(),
        pluginParameters.sortDependencyExclusions.isDeprecatedValueFalse(),
        "The 'true' value in sortDependencyExclusions is not supported, please use value 'groupId,artifactId' instead.",
        "The 'false' value in sortDependencyExclusions is not supported, please use empty value '' or omit sortDependencyExclusions instead.");
    warnAboutDeprecatedValue(
        "sortPlugins",
        pluginParameters.sortPlugins.isDeprecatedValueTrue(),
        pluginParameters.sortPlugins.isDeprecatedValueFalse(),
        "The 'true' value in sortPlugins is not supported anymore, please use value 'groupId,artifactId' instead.",
        "The 'false' value in sortPlugins is not supported anymore, please use empty value '' or omit sortPlugins instead.");
  }

  // Extracted method to warn about deprecated values for individual parameters
  private void warnAboutDeprecatedValue(
      String parameterName,
      boolean isTrueDeprecated,
      boolean isFalseDeprecated,
      String trueMessage,
      String falseMessage) {
    if (isTrueDeprecated) {
      throw new FailureException(trueMessage);
    }
    if (isFalseDeprecated) {
      throw new FailureException(falseMessage);
    }
  }

  /** Sorts the pom file. */
  public void sortPom() {
    log.info("Sorting file " + pomFile.getAbsolutePath());

    sortPomService.sortOriginalXml();
    sortPomService.generateSortedXml();
    if (sortPomService.isOriginalXmlStringSorted().isOrdered()) {
      log.info("Pom file is already sorted, exiting");
      return;
    }
    sortPomService.createBackupFile();
    sortPomService.saveGeneratedXml();
    log.info("Saved sorted pom file to " + pomFile.getAbsolutePath());
  }

  /** Verify that the pom-file is sorted regardless of formatting */
  public void verifyPom() {
    var xmlOrderedResult = getVerificationResult();
    // Rename method/variable
    performVerificationResult(xmlOrderedResult);
  }

  private XmlOrderedResult getVerificationResult() {
    log.info("Verifying file " + pomFile.getAbsolutePath());

    sortPomService.sortOriginalXml();

    XmlOrderedResult xmlOrderedResult;
    if (verifyFailOn == VerifyFailOnType.XMLELEMENTS) {
      xmlOrderedResult = sortPomService.isOriginalXmlElementsSorted();
    } else {
      sortPomService.generateSortedXml();
      xmlOrderedResult = sortPomService.isOriginalXmlStringSorted();
    }
    return xmlOrderedResult;
  }

  // Rename method/variable
  private void performVerificationResult(XmlOrderedResult xmlOrderedResult) {
    if (!xmlOrderedResult.isOrdered()) {
      // Decompose conditional
      handleUnorderedXml(xmlOrderedResult);
    }
  }

  private void handleUnorderedXml(XmlOrderedResult xmlOrderedResult) {
    switch (verifyFailType) {
      case WARN:
        handleWarning(xmlOrderedResult);
        break;
      case SORT:
        handleSort(xmlOrderedResult);
        break;
      case STOP:
        handleStop(xmlOrderedResult);
        break;
    }
  }

  private void handleWarning(XmlOrderedResult xmlOrderedResult) {
    log.warn(xmlOrderedResult.getErrorMessage());
    sortPomService.saveViolationFile(xmlOrderedResult);
    log.warn(String.format(TEXT_FILE_NOT_SORTED, pomFile.getAbsolutePath()));
  }

  private void handleSort(XmlOrderedResult xmlOrderedResult) {
    log.info(xmlOrderedResult.getErrorMessage());
    sortPomService.saveViolationFile(xmlOrderedResult);
    log.info(String.format(TEXT_FILE_NOT_SORTED, pomFile.getAbsolutePath()));
    log.info("Sorting file " + pomFile.getAbsolutePath());
    sortPomService.generateSortedXml();
    sortPomService.createBackupFile();
    sortPomService.saveGeneratedXml();
    log.info("Saved sorted pom file to " + pomFile.getAbsolutePath());
  }

  private void handleStop(XmlOrderedResult xmlOrderedResult) {
    log.error(xmlOrderedResult.getErrorMessage());
    sortPomService.saveViolationFile(xmlOrderedResult);
    log.error(String.format(TEXT_FILE_NOT_SORTED, pomFile.getAbsolutePath()));
    throw new FailureException(String.format(TEXT_FILE_NOT_SORTED, pomFile.getAbsolutePath()));
  }
}
