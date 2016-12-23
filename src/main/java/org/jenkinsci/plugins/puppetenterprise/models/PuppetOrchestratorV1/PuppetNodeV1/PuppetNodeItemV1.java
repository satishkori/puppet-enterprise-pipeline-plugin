package org.jenkinsci.plugins.puppetenterprise.models.puppetorchestratorv1.puppetnodev1;

import com.google.gson.annotations.SerializedName;
import java.net.*;
import java.util.Date;

public class PuppetNodeItemV1 {
  private Date timestamp = null;
  private String state = "";
  private String transaction_uuid = "";
  private String name = "";
  private PuppetNodeItemDetails details  = null;
  private String message = "";

  public String getState() {
    return this.state;
  }

  public Date getTimeStamp() {
    return this.timestamp;
  }

  public String getTransactionUUID() {
    return this.transaction_uuid;
  }

  public String getName() {
    return this.name;
  }

  public String getMessage() {
    return this.message;
  }

  public URL getReportURL() {
    return details.getReportURL();
  }

  public PuppetNodeMetricsV1 getMetrics() {
    return details.getMetrics();
  }

  class PuppetNodeItemDetails {
    //Instruct GSON how to map the "report-url"
    //  key to a valid Java variable name
    @SerializedName("report-url")
    private URL reportUrl = null;

    private PuppetNodeMetricsV1 metrics = null;

    public URL getReportURL() {
      return this.reportUrl;
    }

    public PuppetNodeMetricsV1 getMetrics() {
      return metrics;
    }
  }
}
