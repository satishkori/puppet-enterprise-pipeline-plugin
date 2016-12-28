package org.jenkinsci.plugins.puppetenterprise.apimanagers;

import java.util.*;
import java.net.URI;
import java.net.URISyntaxException;
import org.jenkinsci.plugins.workflow.PEException;
import org.jenkinsci.plugins.puppetenterprise.models.PuppetEnterpriseConfig;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.PERequest;

public abstract class PuppetCodeManagerV1 extends PERequest {
  private String getCodeManagerAddress() {
    return PuppetEnterpriseConfig.getPuppetMasterUrl();
  }

  private Integer getCodeManagerPort() {
    return 8170;
  }

  protected URI getURI(String endpoint) throws Exception {
    String uriString = "https://" + getCodeManagerAddress() + ":" + getCodeManagerPort() + "/code-manager/v1" + endpoint;
    URI uri = null;

    try {
      uri = new URI(uriString);
    } catch(URISyntaxException e) {
      StringBuilder message = new StringBuilder();

      message.append("Bad Code Manager Service Configuration.\n");

      if (getCodeManagerAddress() == null || getCodeManagerAddress().isEmpty()) {
        message.append("The Puppet Enterprise master address has not been configured yet.\nConfigure the Puppet Enterprise page under Manage Jenkins.");
      }

      message.append("Service Address: " + getCodeManagerAddress() + "\n");
      message.append("Service Port: " + getCodeManagerPort() + "\n");
      message.append("Details: " + e.getMessage());

      throw new Exception(message.toString());
    }

    return uri;
  }
}
