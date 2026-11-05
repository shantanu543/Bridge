# Terraform infrastructure (not provisioned)

The approved target architecture is AWS-first in `ap-south-1`, isolated across `dev`, `stag`, and `prod`. Terraform modules and remote state are intentionally deferred until the owner approves account boundaries, domain/cost controls, and cloud implementation. No cloud resources, credentials, or Terraform state have been created by the foundation phase.
