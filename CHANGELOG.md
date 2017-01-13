## 1.3.0

- Introduce a permission system for the Hiera Data Store page. Now if Jenkins
  is using a matrix authorization system, Jenkins users will require Hiera/View
  or Hiera/Delete permissions to view the Hiera Data Store page or delete
  key/value pairs. Jenkins users will also require Overall/Read permission to
  lookup key/value pairs through the API. This means a Jenkins user with only
  Overall/Read permissions can be used for Hiera on the Puppet server to lookup
  Hiera values from Jenkins.
- Fix JENKINS-40812 with the new permissions

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
