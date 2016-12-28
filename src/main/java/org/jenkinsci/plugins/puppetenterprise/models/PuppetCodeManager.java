package org.jenkinsci.plugins.puppetenterprise.models;

import java.io.*;
import java.util.*;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetcodemanagerv1.CodeManagerDeploysV1;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetcodemanagerv1.CodeManagerEnvironmentV1;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetcodemanagerv1.CodeManagerEnvironmentErrorV1;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetcodemanagerv1.CodeManagerException;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.PERequest;
import com.google.gson.internal.LinkedTreeMap;

public class PuppetCodeManager {
  private ArrayList<String> environments = new ArrayList();
  private String token = "";
  private Boolean wait = null;
  private ArrayList<CodeManagerEnvironmentV1> errors = new ArrayList();
  private ArrayList<CodeManagerEnvironmentV1> deployedEnvironments = new ArrayList();
  private PrintStream logger = null;

  public PuppetCodeManager() { }

  public void setEnvironments(ArrayList environments) {
    this.environments = environments;
  }

  public void setLogger(PrintStream logger) {
    this.logger = logger;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public void setWait(Boolean wait) {
    this.wait = wait;
  }

  public ArrayList<CodeManagerEnvironmentV1> getErrors() {
    return this.errors;
  }

  public Boolean hasErrors() {
    return (errors.size() > 0);
  }

  public void deploy() throws CodeManagerException, Exception {
    CodeManagerDeploysV1 deploys = new CodeManagerDeploysV1();
    deploys.setEnvironments(this.environments);
    deploys.setWait(this.wait);
    deploys.setToken(this.token);
    deploys.execute();

    this.deployedEnvironments = deploys.getDeployedEnvironments();
    this.errors = deploys.getErrors();
  }

  public String formatReport() {
    StringBuilder formattedReport = new StringBuilder();
    Integer totalEnvironments = this.environments.size();
    Integer failedEnvironments = this.errors.size();
    Integer successfulEnvironments = totalEnvironments - failedEnvironments;

    formattedReport.append("Puppet environments deployed: ");
    formattedReport.append(successfulEnvironments + " successful. " + failedEnvironments + " failed.\n");

    for (CodeManagerEnvironmentV1 environment : this.deployedEnvironments ) {
      formattedReport.append("  " + environment.getName() + ": " + environment.getStatus() + "\n");

      if (environment.hasError()) {
        CodeManagerEnvironmentErrorV1 error = environment.getError();
        formattedReport.append("    Kind:    " + error.getKind() + "\n");
        formattedReport.append("    Message: " + error.getMessage() + "\n");

        if (error.getDetails() != null) {
          formattedReport.append("    Details: " + error.getDetails().toString() + "\n");
        }
      }
    }

    return formattedReport.toString();
  }
}
