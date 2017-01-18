package org.jenkinsci.plugins.puppetenterprise;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.model.Statement;
import static org.junit.Assert.*;
import org.jvnet.hudson.test.RestartableJenkinsRule;
import org.jvnet.hudson.test.JenkinsRule;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.WebClient;
import org.jenkinsci.plugins.puppetenterprise.models.PuppetEnterpriseConfig;
import org.jenkinsci.plugins.puppetenterprise.TestUtils;

public class PuppetEnterpriseManagementTest extends Assert {
  @Rule
  public RestartableJenkinsRule story = new RestartableJenkinsRule();

  @Test
  public void setNewPuppetServerAddress() throws Exception {
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        PuppetEnterpriseConfig.setPuppetMasterCACertificate("original CA string");
        PuppetEnterpriseConfig.setPuppetMasterUrl("notlocalhost", false);

        //TODO: Properly test this functionality by working through the configuration page
        // WebClient client = new WebClient();
        // HtmlPage page = client.getPage(story.j.jenkins.getRootUrl() + "/puppetenterprise/");
        // HtmlInput addressField = (HtmlInput) page.getElementByName("_.masterAddress");
        // HtmlButton saveButton = (HtmlButton) page.getElementById("yui-gen1-button");
        //
        // assertNotNull(addressField);
        // assertNotNull(saveButton);
        //
        // addressField.setValueAttribute("localhost");
        // saveButton.click();

        PuppetEnterpriseConfig.setPuppetMasterUrl("localhost");

        assertEquals("localhost", PuppetEnterpriseConfig.getPuppetMasterUrl());
        assertEquals(TestUtils.getCACertificateString(), PuppetEnterpriseConfig.getPuppetMasterCACertificate());
      }
    });
  }
}
