class PuppetOrchestratorException extends PEException {
  public PuppetOrchestratorException(String kind, String message, LinkedTreeMap details) {
    super("Puppet orchestration failure: (" + kind + ") " + message + ". DETAILS: " + details.toString());
  }
}
