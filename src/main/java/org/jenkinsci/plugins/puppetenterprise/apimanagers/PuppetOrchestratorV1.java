package org.jenkinsci.plugins.puppetenterprise.apimanagers;

import java.util.*;
import java.net.URI;
import java.net.URISyntaxException;
import org.jenkinsci.plugins.puppetenterprise.models.PuppetEnterpriseConfig;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.PERequest;

public abstract class PuppetOrchestratorV1 extends PERequest {
  private String getOrchestratorAddress() {
    return PuppetEnterpriseConfig.getPuppetMasterUrl();
  }

  private Integer getOrchestratorPort() {
    return 8143;
  }

  protected URI getURI(String endpoint) throws Exception {
    String uriString = "https://" + getOrchestratorAddress() + ":" + getOrchestratorPort() + "/orchestrator/v1" + endpoint;
    URI uri = null;

    try {
      uri = new URI(uriString);
    } catch(URISyntaxException e) {
      StringBuilder message = new StringBuilder();

      message.append("Bad Orchestrator Service Configuration.\n");

      if (getOrchestratorAddress() == null || getOrchestratorAddress().isEmpty()) {
        message.append("The Puppet Enterprise master address has not been configured yet.\nConfigure the Puppet Enterprise page under Manage Jenkins.");
      }

      message.append("Service Address: " + getOrchestratorAddress() + "\n");
      message.append("Service Port: " + getOrchestratorPort() + "\n");
      message.append("Details: " + e.getMessage());

      throw new Exception(message.toString());
    }

    return uri;
  }
}
