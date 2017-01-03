## 1.2.3
- Significantly improved error reporting throughout all plugin features
- Improved reporting for puppet.codeDeploy and puppet.job steps
- Reduce logging noise by only printing system errors to system log. All other
  errors are printed to job console.

## 1.2.2
- Improved puppet.job report format when there's an error in the Puppet agent run

## 1.2.1
- Fix bug where step would fail if puppet.query step returned zero results
- Reduce logging noise. Do not post all messages to both job log and system log

## 1.1.1
- Fix PQL examples with bad syntax in documentation
- Fix bug where plugin configuration wasn't loaded at Jenkins startup

## 1.1.0
- Add puppet.query step
- Fix credentials parameter for puppet.job step

## 1.0.1
- Initial release
