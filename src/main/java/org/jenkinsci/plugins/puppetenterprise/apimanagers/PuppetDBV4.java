package org.jenkinsci.plugins.puppetenterprise.apimanagers;

import java.util.*;
import java.net.URI;
import java.net.URISyntaxException;
import org.jenkinsci.plugins.puppetenterprise.models.PuppetEnterpriseConfig;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.PERequest;

public abstract class PuppetDBV4 extends PERequest {
  private String getPuppetDBAddress() {
    return PuppetEnterpriseConfig.getPuppetMasterUrl();
  }

  private Integer getPuppetDBPort() {
    return 8081;
  }

  protected URI getURI(String endpoint) throws Exception {
    String uriString = "https://" + getPuppetDBAddress() + ":" + getPuppetDBPort() + "/pdb" + endpoint;
    URI uri = null;

    try {
      uri = new URI(uriString);
    } catch(URISyntaxException e) {
      StringBuilder message = new StringBuilder();

      message.append("Bad PuppetDB Service Configuration.\n");

      if (getPuppetDBAddress() == null || getPuppetDBAddress().isEmpty()) {
        message.append("The Puppet Enterprise master address has not been configured yet.\nConfigure the Puppet Enterprise page under Manage Jenkins.");
      }

      message.append("Service Address: " + getPuppetDBAddress() + "\n");
      message.append("Service Port: " + getPuppetDBPort() + "\n");
      message.append("Details: " + e.getMessage());

      throw new Exception(message.toString());
    }

    return uri;
  }
}
