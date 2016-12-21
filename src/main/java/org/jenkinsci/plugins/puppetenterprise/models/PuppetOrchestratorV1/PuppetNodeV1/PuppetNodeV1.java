import java.io.*;
import org.jenkensci.plugins.puppetenterprise.models.PuppetOrchestratorV1.PuppetNodeV1.PuppetNodeMetricsV1;
import org.jenkensci.plugins.puppetenterprise.models.PuppetOrchestratorV1.PuppetNodeV1.PuppetNodeItemV1;

public class PuppetNodeV1 {
  private String message = null;
  private ArrayList<PuppetNodeItemV1> items = null;

  public getMessage() {
    return this.message;
  }

  public getItems() {
    return this.items;
  }
}
